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

import io.vertx.test.core.VertxDebuggingTestBase;
import org.junit.Test;

import java.util.concurrent.TimeUnit;


public class EventBusDebuggingTest extends VertxDebuggingTestBase {

  @Test
  public void testFlowControl() {

    EventBus eb = vertx.eventBus();

    MessageProducer producer = eb.publisher("some-address").withDebuggingLabel("Initial message");

    MessageProducer producer2 = eb.publisher("some-address").withDebuggingLabel("Initial message2");

    eb.consumer("some-address", msg -> {
      for (int i = 0; i < 100; i++) {
        DeliveryOptions options = new DeliveryOptions();
        options.addHeader("some-header1", "some-value1");
        options.addHeader("some-header2", "some-value2");
        options.addHeader("some-header3", "some-value3");
        options.addHeader("some-header4", "some-value4");
        options.addHeader("some-header5", "some-value5");
        options.addHeader("some-header6", "some-veeeeeeeeeeeeery-long-value");

        eb.send("some-address1", "Send test1", options,  new DebuggingOptions("Sequence1-Action1", msg));
        eb.send("some-address2", "Send test2", new DebuggingOptions("Sequence2-Action1", msg), reply -> {
          String message = reply.result().body().toString();
        });
      }
    });

    eb.consumer("some-address1", msg -> {
      eb.publish("some-address3", "Publish test", new DebuggingOptions("Sequence1-Action2", msg));
    });

    eb.consumer("some-address2", msg -> {
      msg.reply("Reply test1");
    });

    eb.consumer("some-address3", msg -> {
      msg.reply("Reply test2");
    });

    for (int i = 0; i < 125; i++) {
      producer.send("Test publisher send");
      producer2.send("Another test message");
    }


    await(5, TimeUnit.SECONDS);
  }


  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
}
