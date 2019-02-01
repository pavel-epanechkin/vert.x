package io.vertx.core.debugging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

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
      String messagesInfoPath = debuggingOutputPath + "messages_info.db";
      log.info("Start saving debugging info into file: " + messagesInfoPath);

      RocksDB.loadLibrary();
      try (final Options options = new Options().setCreateIfMissing(true)) {
        storage = RocksDB.open(options, debuggingOutputPath);

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
    MessageInfo messageInfo = createMessageEntity(caughtMessageInfo);

    ObjectMapper objectMapper = new ObjectMapper();

    try {
      String id = messageInfo.getMessageId();
      String content = objectMapper.writeValueAsString(messageInfo);
      storage.put(id.getBytes(), content.getBytes());
    }
    catch (JsonProcessingException | RocksDBException e) {
      e.printStackTrace();
    }

  }

  private MessageInfo createMessageEntity(CaughtMessageInfo caughtMessageInfo) {
    Message message = caughtMessageInfo.getMessage();
    MultiMap tmpHeaders = new CaseInsensitiveHeaders();
    tmpHeaders.addAll(message.headers());
    String debuggingHeaderString = tmpHeaders.get(DebuggingHeader.DEBUGGING_HEADER_NAME);
    DebuggingHeader debuggingHeader = DebuggingHeader.fromJsonString(debuggingHeaderString);
    tmpHeaders.remove(DebuggingHeader.DEBUGGING_HEADER_NAME);

    MessageInfo messageInfo = new MessageInfo();
    messageInfo.setRecordId(++counter);
    messageInfo.setType(caughtMessageInfo.getMessageType().name());
    messageInfo.setTimestamp(caughtMessageInfo.getMessageOccuranceTime().getTime());
    messageInfo.setMessageId(debuggingHeader.getMessageId());
    messageInfo.setPrevMessageId(getStringValue(debuggingHeader.getPrevMessageId()));
    messageInfo.setLabel(getStringValue(debuggingHeader.getDebuggingLabel()));
    messageInfo.setTargetAddress(getStringValue(message.address()));
    messageInfo.setReplyAddress(getStringValue(message.replyAddress()));
    messageInfo.setHeaders(getStringValue(tmpHeaders));
    messageInfo.setBody(getStringValue(message.body()));

    return messageInfo;
  }

  private String getStringValue(Object object) {
    return object == null ? "" : object.toString();
  }
}
