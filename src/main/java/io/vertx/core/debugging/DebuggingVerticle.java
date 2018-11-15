package io.vertx.core.debugging;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;

import java.util.ArrayList;

public class DebuggingVerticle extends AbstractVerticle {

  private HttpServer server;

  private EventBus eventBus;

  private ArrayList<Message> sentMessages = new ArrayList<>();
  private ArrayList<Message> receivedMessages = new ArrayList<>();

  private final String DEBUGGER_ADDRESS = "vertx.debugging";

  private final int DEBUGGER_PORT = 15001;

  public DebuggingVerticle(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void start(Future<Void> startFuture) {
    startDebuggingHttpServer();
    setupInterceptors();

    server.listen(DEBUGGER_PORT, result -> {
      if (result.succeeded()) {
        startFuture.complete();
      }
      else {
        startFuture.fail(result.cause());
      }
    });
  }

  public void stop(Future<Void> stopFuture) {
    server.close();
  }

  private void setupInterceptors() {
    eventBus.addInboundInterceptor(context -> {
      handleInboundMessage(context.message());
      context.send();
    });
    eventBus.addOutboundInterceptor(context -> {
      handleOutboundMessage(context.message());
      context.send();
    });
  }

  private void handleOutboundMessage(Message message) {
    synchronized (sentMessages) {
      sentMessages.add(message);
    }
  }

  private void handleInboundMessage(Message message) {
    synchronized (receivedMessages) {
      sentMessages.add(message);
    }
  }

  private void startDebuggingHttpServer() {
    server = vertx.createHttpServer().requestHandler(request -> {
      request.response()
        .putHeader("content-type", "json/application")

        .end(sentMessages.toString());
    });
  }
}
