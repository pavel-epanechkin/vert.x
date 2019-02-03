package io.vertx.core.debugging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DebuggingVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DebuggingVerticle.class);

  private EventBus eventBus;

  private RocksDB storage;

  private Integer counter = 0;

  private String debuggingOutputPath;

  private Thread outputThread;

  private BlockingQueue<CaughtMessageInfo> messagesBuffer = new LinkedBlockingQueue<>();

  private boolean debugging;

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
    storage.close();
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
      String debuggingLog = "vertx-debug-" + LocalDateTime.now().toString().replaceAll("[:.]", "_");
      String messagesInfoPath = debuggingOutputPath + File.separator + debuggingLog;
      log.info("Start saving debugging info into file: " + messagesInfoPath);

      RocksDB.loadLibrary();
      try (final Options options = new Options().setCreateIfMissing(true)) {
        storage = RocksDB.open(options, messagesInfoPath);

        try {
          CaughtMessageInfo messageInfo = null;

          while (debugging) {
            if (messageInfo != null)
              saveMessage(messageInfo);
            messageInfo = messagesBuffer.poll(1, TimeUnit.SECONDS);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } catch (RocksDBException err) {
        err.printStackTrace();
      }

    });
    outputThread.start();
  }

  private void saveMessage(CaughtMessageInfo caughtMessageInfo) {
    JsonObject messageInfo = createMessageEntity(caughtMessageInfo);

    try {
      String id = messageInfo.getInteger("recordId").toString();
      String content = messageInfo.toString();
      storage.put(id.getBytes(), content.getBytes());
    }
    catch (RocksDBException e) {
      e.printStackTrace();
    }

  }

  private JsonObject createMessageEntity(CaughtMessageInfo caughtMessageInfo) {
    Message message = caughtMessageInfo.getMessage();
    MultiMap tmpHeaders = new CaseInsensitiveHeaders();
    tmpHeaders.addAll(message.headers());
    String debuggingHeaderString = tmpHeaders.get(DebuggingHeader.DEBUGGING_HEADER_NAME);
    DebuggingHeader debuggingHeader = DebuggingHeader.fromJsonString(debuggingHeaderString);
    tmpHeaders.remove(DebuggingHeader.DEBUGGING_HEADER_NAME);

    JsonObject jsonObject = new JsonObject();
    jsonObject.put("recordId", ++counter);
    jsonObject.put("type", caughtMessageInfo.getMessageType().name());
    jsonObject.put("timestamp", caughtMessageInfo.getMessageOccuranceTime().getTime());
    jsonObject.put("messageId", debuggingHeader.getMessageId());
    jsonObject.put("prevMessageId", getStringValue(debuggingHeader.getPrevMessageId()));
    jsonObject.put("label", getStringValue(debuggingHeader.getDebuggingLabel()));
    jsonObject.put("targetAddress", getStringValue(message.address()));
    jsonObject.put("replyAddress", getStringValue(message.replyAddress()));
    jsonObject.put("headers", getStringValue(tmpHeaders));
    jsonObject.put("body", getStringValue(message.body()));

    return jsonObject;
  }

  private String getStringValue(Object object) {
    return object == null ? "" : object.toString();
  }
}
