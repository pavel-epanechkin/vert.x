/*
 * Copyright (c) 2011-2017 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.eventbus;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

/**
 * Represents a stream of message that can be written to.
 * <p>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MessageProducer<T> extends WriteStream<T> {

  int DEFAULT_WRITE_QUEUE_MAX_SIZE = 1000;

  /**
   * This method actually sends a message using the send semantic regardless this producer
   * is a sender or a publisher.
   *
   * @param message the message to send
   * @return  reference to this for fluency
   */
  MessageProducer<T> send(T message);

  /**
   * Like {@link #send(Object)} but specifying a {@code replyHandler} that will be called if the recipient
   * subsequently replies to the message.
   *
   * @param message the message to send
   * @param replyHandler reply handler will be called when any reply from the recipient is received, may be {@code null}
   * @return  reference to this for fluency
   */
  <R> MessageProducer<T> send(T message, Handler<AsyncResult<Message<R>>> replyHandler);

  //TODO: add javadoc
  MessageProducer<T> send(T message, DebuggingOptions debuggingOptions);

  //TODO: add javadoc
  <R> MessageProducer<T> send(T message, DebuggingOptions debuggingOptions, Handler<AsyncResult<Message<R>>> replyHandler);

  //TODO: add javadoc
  MessageProducer withDebuggingLabel(String label);

  //TODO: add javadoc
  MessageProducer withContextMessage(Message contextMessage);

  @Override
  MessageProducer<T> exceptionHandler(Handler<Throwable> handler);

  @Override
  MessageProducer<T> write(T data);

  @Override
  MessageProducer<T> setWriteQueueMaxSize(int maxSize);

  @Override
  MessageProducer<T> drainHandler(Handler<Void> handler);

  /**
   * Update the delivery options of this producer.
   *
   * @param options the new options
   * @return this producer object
   */
  @Fluent
  MessageProducer<T> deliveryOptions(DeliveryOptions options);

  /**
   * @return The address to which the producer produces messages.
   */
  String address();

  /**
   * Closes the producer, calls {@link #close()}
   */
  @Override
  void end();

  /**
   * Closes the producer, this method should be called when the message producer is not used anymore.
   */
  void close();
}
