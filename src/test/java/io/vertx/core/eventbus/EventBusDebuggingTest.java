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


public class EventBusDebuggingTest extends VertxDebuggingTestBase {

  protected EventBus eb;

  @Test
  public void testFlowControl() {

    MessageProducer<String> prod = eb.sender("some-address");

    eb.consumer("some-address", msg -> {
      eb.send("some-address1", "Test1", new DebuggingOptions("Usecase1-Action2", msg));
    });

    eb.consumer("some-address1", msg -> {
//      eb.send("some-address1", "Test1", new DebuggingOptions("Usecase1-Action2", msg));
    });

    eb.send("some-address", "Test", new DebuggingOptions("Usecase1-Action1", null));

    await();
  }


  @Override
  public void setUp() throws Exception {
    super.setUp();
    eb = vertx.eventBus();
  }
}
