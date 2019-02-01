package io.vertx.core.debugging;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.*;

public class EventBusWrapper implements EventBus {

  private EventBus eventBus;

  private Message contextMessage;

  public EventBusWrapper(EventBus eventBus, Message contextMessage) {
    this.eventBus = eventBus;
    this.contextMessage = contextMessage;
  }

  private DebuggingOptions prepareOptions(DebuggingOptions debuggingOptions) {
    if (debuggingOptions.getContextMessage() == null) {
      debuggingOptions.setContextMessage(contextMessage);
    }
    return debuggingOptions;
  }

  @Override
  public EventBus send(String address, Object message) {
    return eventBus.send(address, message);
  }

  @Override
  public EventBus send(String address, Object message, DebuggingOptions debuggingOptions) {
    return send(address, message, prepareOptions(debuggingOptions));
  }

  @Override
  public <T> EventBus send(String address, Object message, Handler<AsyncResult<Message<T>>> replyHandler) {
    return eventBus.send(address, message, replyHandler);
  }

  @Override
  public <T> EventBus send(String address, Object message, DebuggingOptions debuggingOptions, Handler<AsyncResult<Message<T>>> replyHandler) {
    return eventBus.send(address, message, prepareOptions(debuggingOptions), replyHandler);
  }

  @Override
  public EventBus send(String address, Object message, DeliveryOptions options) {
    return eventBus.send(address, message, options);
  }

  @Override
  public EventBus send(String address, Object message, DeliveryOptions options, DebuggingOptions debuggingOptions) {
    return eventBus.send(address, message, options, prepareOptions(debuggingOptions));
  }

  @Override
  public <T> EventBus send(String address, Object message, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
    return eventBus.send(address, message, options, replyHandler);
  }

  @Override
  public <T> EventBus send(String address, Object message, DeliveryOptions options, DebuggingOptions debuggingOptions, Handler<AsyncResult<Message<T>>> replyHandler) {
    return eventBus.send(address, message, options, prepareOptions(debuggingOptions), replyHandler);
  }

  @Override
  public EventBus publish(String address, Object message) {
    return eventBus.publish(address, message);
  }

  @Override
  public EventBus publish(String address, Object message, DebuggingOptions debuggingOptions) {
    return eventBus.publish(address, message, prepareOptions(debuggingOptions));
  }

  @Override
  public EventBus publish(String address, Object message, DeliveryOptions options) {
    return eventBus.publish(address, message, options);
  }

  @Override
  public EventBus publish(String address, Object message, DeliveryOptions options, DebuggingOptions debuggingOptions) {
    return eventBus.publish(address, message, options, prepareOptions(debuggingOptions));
  }

  @Override
  public <T> MessageConsumer<T> consumer(String address) {
    return eventBus.consumer(address);
  }

  @Override
  public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
    return eventBus.consumer(address, handler);
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String address) {
    return eventBus.localConsumer(address);
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String address, Handler<Message<T>> handler) {
    return eventBus.localConsumer(address, handler);
  }

  @Override
  public <T> MessageProducer<T> sender(String address) {
    return eventBus.sender(address);
  }

  @Override
  public <T> MessageProducer<T> sender(String address, DeliveryOptions options) {
    return eventBus.sender(address, options);
  }

  @Override
  public <T> MessageProducer<T> publisher(String address) {
    return eventBus.publisher(address);
  }

  @Override
  public <T> MessageProducer<T> publisher(String address, DeliveryOptions options) {
    return eventBus.publisher(address, options);
  }

  @Override
  public EventBus registerCodec(MessageCodec codec) {
    return eventBus.registerCodec(codec);
  }

  @Override
  public EventBus unregisterCodec(String name) {
    return eventBus.unregisterCodec(name);
  }

  @Override
  public <T> EventBus registerDefaultCodec(Class<T> clazz, MessageCodec<T, ?> codec) {
    return eventBus.registerDefaultCodec(clazz, codec);
  }

  @Override
  public EventBus unregisterDefaultCodec(Class clazz) {
    return eventBus.unregisterDefaultCodec(clazz);
  }

  @Override
  public void start(Handler<AsyncResult<Void>> completionHandler) {
    eventBus.start(completionHandler);
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    eventBus.close(completionHandler);
  }

  @Override
  public <T> EventBus addOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    return eventBus.addOutboundInterceptor(interceptor);
  }

  @Override
  public <T> EventBus removeOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    return eventBus.removeOutboundInterceptor(interceptor);
  }

  @Override
  public <T> EventBus addInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    return eventBus.addInboundInterceptor(interceptor);
  }

  @Override
  public <T> EventBus removeInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    return eventBus.removeInboundInterceptor(interceptor);
  }
}
