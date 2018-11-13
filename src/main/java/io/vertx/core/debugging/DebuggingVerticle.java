package io.vertx.core.debugging;

import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;

import java.util.ArrayList;

public class DebuggingVerticle extends AbstractVerticle {

  private HttpServer server;

  private EventBus eventBus;

  private ArrayList<Message> messages = new ArrayList<>();

  private final String DEBUGGER_ADDRESS = "vertx.debugging";

  public DebuggingVerticle(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void start(Future<Void> startFuture) {
    startDebuggingHttpServer();
    setupDebuggingMessagesConsumer();
    setupInterceptors();

    server.listen(15001, result -> {
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
    });

    eventBus.addOutboundInterceptor(context -> {
      handleOutboundMessage(context.message());
    });
  }

  private void handleOutboundMessage(Message message) {
//    eventBus.publish(DEBUGGER_ADDRESS, message.body(), new DeliveryOptions().setHeaders(message.headers()));
  }

  private void handleInboundMessage(Message message) {
//    eventBus.publish(DEBUGGER_ADDRESS, message.body(), new DeliveryOptions().setHeaders(message.headers()));
  }

  private void setupDebuggingMessagesConsumer() {
    eventBus.consumer(DEBUGGER_ADDRESS, message -> {
      synchronized (messages) {
        messages.add(message);
      }
    });
  }

  private void startDebuggingHttpServer() {
    server = vertx.createHttpServer().requestHandler(request -> {
      request.response()
        .putHeader("content-type", "json/application")

        .end(messages.toString());
    });
  }
}
