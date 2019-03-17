package io.vertx.core.debugging;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.rocksdb.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DebuggingVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DebuggingVerticle.class);

  private EventBus eventBus;

  private String debuggingOutputPath;

  private RocksDB storage;

  private DBOptions storageOptions;

  private ColumnFamilyHandle sentColumnFamily;

  private ColumnFamilyHandle sentMessageIdToRecordIdColumnFamily;

  private ColumnFamilyHandle receivedColumnFamily;

  private Thread outputThread;

  private BlockingQueue<CaughtMessageInfo> messagesBuffer = new LinkedBlockingQueue<>();

  private boolean debugging;

  private Integer counter = 0;

  private final String LOG_DB_PREFIX = "vertx-debug-";

  private final String SENT_COUNT_KEY = "SENT_MESSAGES_COUNT";

  private final String DEFAULT_COLUMN_FAMILY_NAME = "default";

  private final String SENT_COLUMN_FAMILY_NAME = "SENT_MESSAGES";

  private final String RECEIVED_COLUMN_FAMILY_NAME = "RECEIVED_MESSAGES";

  private final String SENT_MESSAGEID_TO_RECORDID_COLUMN_FAMILY_NAME = "SENT_MESSAGEID_TO_RECORDID";


  public DebuggingVerticle(EventBus eventBus, String debuggingOutputPath) {
    this.eventBus = eventBus;
    this.debuggingOutputPath = debuggingOutputPath;
  }

  public void start(Future<Void> startFuture) {
    debugging = true;
    startSavingMessages();
    setupInterceptors();
    startFuture.complete();
  }

  public void stop(Future<Void> stopFuture) {
    try {
      debugging = false;
      outputThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Messages saved: " + counter);
    stopFuture.complete();
  }

  private void setupInterceptors() {
    eventBus.addInboundInterceptor(context -> {
      handleInboundMessage(context.message());
      context.next();
    });
    eventBus.addOutboundInterceptor(context -> {
      handleOutboundMessage(context.message());
      context.next();
    });
  }

  private void handleOutboundMessage(Message message) {
    CaughtMessageInfo caughtMessageInfo = new CaughtMessageInfo(message, MessageType.sent);
    try {
      messagesBuffer.put(caughtMessageInfo);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void handleInboundMessage(Message message) {
    CaughtMessageInfo caughtMessageInfo = new CaughtMessageInfo(message, MessageType.received);
    try {
      messagesBuffer.put(caughtMessageInfo);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void startSavingMessages() {
    outputThread = new Thread(() -> {
      try {
        createLogDatabase();
        CaughtMessageInfo messageInfo = null;
        while (true) {
          try {
            if (messageInfo != null)
              saveMessage(messageInfo);
            else if (!debugging)
              break;

            messageInfo = messagesBuffer.poll(1, TimeUnit.SECONDS);
          }
          catch (Exception err) {
            err.printStackTrace();
          }
        }
        storage.put(SENT_COUNT_KEY.getBytes(), counter.toString().getBytes());
        closeLogDatabase();
      }
      catch (RocksDBException err) {
        log.error("Can't create log database.");
        err.printStackTrace();
      }
    });
    outputThread.start();
  }

  private void createLogDatabase() throws RocksDBException {
    String logPath = getLogPath();
    RocksDB.loadLibrary();

    List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
    List<ColumnFamilyDescriptor> columnFamilyDescriptors = Arrays.asList(
      new ColumnFamilyDescriptor(DEFAULT_COLUMN_FAMILY_NAME.getBytes()),
      new ColumnFamilyDescriptor(SENT_COLUMN_FAMILY_NAME.getBytes()),
      new ColumnFamilyDescriptor(RECEIVED_COLUMN_FAMILY_NAME.getBytes()),
      new ColumnFamilyDescriptor(SENT_MESSAGEID_TO_RECORDID_COLUMN_FAMILY_NAME.getBytes())
    );

    storageOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
    storage = RocksDB.open(storageOptions, logPath, columnFamilyDescriptors, columnFamilyHandles);

    sentColumnFamily = columnFamilyHandles.get(1);
    receivedColumnFamily = columnFamilyHandles.get(2);
    sentMessageIdToRecordIdColumnFamily = columnFamilyHandles.get(3);

    log.info("Start saving debugging info into file: " + logPath);
  }

  private String getLogPath() {
    String currentDateTime = LocalDateTime.now().toString().replaceAll("[:.]", "_");
    String debuggingLog = LOG_DB_PREFIX + currentDateTime;
    return debuggingOutputPath + File.separator + debuggingLog;
  }

  private void closeLogDatabase() {
    sentColumnFamily.close();
    receivedColumnFamily.close();
    sentMessageIdToRecordIdColumnFamily.close();
    storageOptions.close();
    storage.close();
  }

  private void saveMessage(CaughtMessageInfo caughtMessageInfo) {
    Pair<String, JsonObject> messageEntity = null;
    ColumnFamilyHandle targetColumnHandle = null;

    if (caughtMessageInfo.getMessageType().equals(MessageType.sent)) {
      messageEntity = createSentMessageEntity(caughtMessageInfo);
      targetColumnHandle = sentColumnFamily;
    }
    else if (caughtMessageInfo.getMessageType().equals(MessageType.received)) {
      messageEntity = createReceivedMessageEntity(caughtMessageInfo);
      targetColumnHandle = receivedColumnFamily;
    }

    saveMessage(targetColumnHandle, messageEntity);

  }

  private void saveMessage(ColumnFamilyHandle targetColumnHandle, Pair<String, JsonObject> messageEntity) {
    try {
      storage.put(targetColumnHandle, messageEntity.key.getBytes(), messageEntity.value.toString().getBytes());

      if (targetColumnHandle == sentColumnFamily) {
        String messageId = messageEntity.value.getString("messageId");
        storage.put(sentMessageIdToRecordIdColumnFamily, messageId.getBytes(), messageEntity.key.getBytes());
      }
    }
    catch (RocksDBException e) {
      e.printStackTrace();
    }
  }

  private Pair<String, JsonObject> createSentMessageEntity(CaughtMessageInfo caughtMessageInfo) {
    Message message = caughtMessageInfo.getMessage();
    MultiMap tmpHeaders = new CaseInsensitiveHeaders();
    tmpHeaders.addAll(message.headers());
    String debuggingHeaderString = tmpHeaders.get(DebuggingHeader.DEBUGGING_HEADER_NAME);
    DebuggingHeader debuggingHeader = DebuggingHeader.fromJsonString(debuggingHeaderString);
    tmpHeaders.remove(DebuggingHeader.DEBUGGING_HEADER_NAME);

    String key = (++counter).toString();
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("timestamp", caughtMessageInfo.getMessageOccuranceTime());
    jsonObject.put("messageId", debuggingHeader.getMessageId());
    jsonObject.put("prevMessageId", getStringValue(debuggingHeader.getPrevMessageId()));
    jsonObject.put("label", getStringValue(debuggingHeader.getDebuggingLabel()));
    jsonObject.put("targetAddress", getStringValue(message.address()));
    jsonObject.put("replyAddress", getStringValue(message.replyAddress()));
    jsonObject.put("headers", getStringValue(tmpHeaders));
    jsonObject.put("body", getStringValue(message.body()));

    return new Pair<>(key, jsonObject);
  }

  private Pair<String, JsonObject> createReceivedMessageEntity(CaughtMessageInfo caughtMessageInfo) {
    Message message = caughtMessageInfo.getMessage();
    String debuggingHeaderString = message.headers().get(DebuggingHeader.DEBUGGING_HEADER_NAME);
    DebuggingHeader debuggingHeader = DebuggingHeader.fromJsonString(debuggingHeaderString);

    String key = debuggingHeader.getMessageId();
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("timestamp", caughtMessageInfo.getMessageOccuranceTime());

    return new Pair<>(key, jsonObject);
  }

  private String getStringValue(Object object) {
    return object == null ? "" : object.toString();
  }

  private class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }
  }
}
