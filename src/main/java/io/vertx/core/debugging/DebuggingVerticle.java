package io.vertx.core.debugging;

import io.vertx.core.*;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DebuggingVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DebuggingVerticle.class);

  private EventBus eventBus;

  private Nitrite storage;

  private Integer counter = 0;

  protected ObjectRepository<MessageInfo> objectRepository;

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

      storage = Nitrite.builder()
        .filePath(messagesInfoPath)
        .openOrCreate();
      objectRepository = storage.getRepository("messagesHistory", MessageInfo.class);

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

    });
    outputThread.start();
  }

  private void saveMessage(CaughtMessageInfo caughtMessageInfo) {
    MessageInfo messageInfo = createMessageEntity(caughtMessageInfo);
    objectRepository.insert(messageInfo);
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
