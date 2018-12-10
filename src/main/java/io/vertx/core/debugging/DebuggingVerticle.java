package io.vertx.core.debugging;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.impl.EventBusImpl;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DebuggingVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DebuggingVerticle.class);

  private EventBus eventBus;

  private Queue<CaughtMessageInfo> caughtMessageInfos = new ConcurrentLinkedQueue();

  private Thread outputThread;
  private int OUTPUT_THREAD_SLEEP_DURATION = 1000;

  private boolean debugging;

  private String debuggingOutputPath;

  private final String MESSAGES_OUTPUT_FILENAME = "messages_info";

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
      stopFuture.complete();
    }
    catch (InterruptedException err) {
      err.printStackTrace();
    }
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
    caughtMessageInfos.add(caughtMessageInfo);
  }

  private void handleInboundMessage(Message message) {
    CaughtMessageInfo caughtMessageInfo = new CaughtMessageInfo(message, MessageType.received);
    caughtMessageInfos.add(caughtMessageInfo);
  }

  private void startSavingMessages() {
    outputThread = new Thread(() -> {
      String messagesInfoFilePath = debuggingOutputPath + File.separator + MESSAGES_OUTPUT_FILENAME;
      log.info("Start saving debugging info into file:" + messagesInfoFilePath);

      FileWriter messagesInfoFileWriter = null;
      BufferedWriter messagesInfoBuffWriter = null;

      try {
        messagesInfoFileWriter = new FileWriter(messagesInfoFilePath);
        messagesInfoBuffWriter = new BufferedWriter(messagesInfoFileWriter);

        while (true) {
          saveAvailableMessagesInfo(caughtMessageInfos, messagesInfoBuffWriter);

          if (!debugging) {
            saveAvailableMessagesInfo(caughtMessageInfos, messagesInfoBuffWriter);
            break;
          }

          try {
            Thread.sleep(OUTPUT_THREAD_SLEEP_DURATION);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      catch (IOException err) {
        err.printStackTrace();
      }
      finally {
        try {
          if (messagesInfoFileWriter != null) {
            messagesInfoBuffWriter.flush();
            messagesInfoBuffWriter.close();
            messagesInfoFileWriter.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    outputThread.start();
  }

  private void saveAvailableMessagesInfo(Queue<CaughtMessageInfo> queue, BufferedWriter bufferedWriter) {
    int availableMessagesCount = queue.size();
    for (int i = 0; i < availableMessagesCount; i++) {
      CaughtMessageInfo caughtMessageInfo = queue.poll();
      String messageData = prepareOutputMessageData(caughtMessageInfo);
      try {
        bufferedWriter.write(messageData);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private String prepareOutputMessageData(CaughtMessageInfo caughtMessageInfo) {
    Message message = caughtMessageInfo.getMessage();
    MultiMap tmpHeaders = new CaseInsensitiveHeaders();
    tmpHeaders.addAll(message.headers());
    String debuggingHeaderString = tmpHeaders.get(DebuggingHeader.DEBUGGING_HEADER_NAME);
    DebuggingHeader debuggingHeader = DebuggingHeader.fromJsonString(debuggingHeaderString);
    tmpHeaders.remove(DebuggingHeader.DEBUGGING_HEADER_NAME);

    JsonObject outputData = new JsonObject();
    outputData.put("type", caughtMessageInfo.getMessageType().name());
    outputData.put("time", caughtMessageInfo.getMessageOccuranceTime().getTime());
    outputData.put("id", debuggingHeader.getMessageId());
    outputData.put("prevId", debuggingHeader.getMessageId());
    outputData.put("label", debuggingHeader.getDebuggingLabel());
    outputData.put("targetAddress", message.address());
    outputData.put("replyAddress", message.replyAddress());
    outputData.put("headers", tmpHeaders.toString());
    outputData.put("body", message.body().toString());
    return outputData.toString() + "\r\n";
  }
}
