/*
 * Copyright (c) 2011-2018 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.eventbus;

import com.sun.xml.internal.messaging.saaj.soap.MessageImpl;
import io.vertx.codegen.annotations.DataObject;

@DataObject
public class DebuggingOptions {

  private String debuggingContextLabel;
  private MessageImpl contextMessage;

  public DebuggingOptions(String debuggingContextLabel, MessageImpl contextMessage) {
    this.debuggingContextLabel = debuggingContextLabel;
    this.contextMessage = contextMessage;
  }

  public DebuggingOptions() {
    this.debuggingContextLabel = getDefaultLabel();
  }

  private String getDefaultLabel() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    return stackTrace.toString();
  }

  public String getDebuggingContextLabel() {
    return debuggingContextLabel;
  }

  public void setDebuggingContextLabel(String debuggingContextLabel) {
    this.debuggingContextLabel = debuggingContextLabel;
  }

  public MessageImpl getContextMessage() {
    return contextMessage;
  }

  public void setContextMessage(MessageImpl contextMessage) {
    this.contextMessage = contextMessage;
  }
}
