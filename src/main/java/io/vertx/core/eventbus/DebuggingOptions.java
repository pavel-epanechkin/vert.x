package io.vertx.core.eventbus;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.eventbus.impl.MessageImpl;

@DataObject
public class DebuggingOptions {

  private String debuggingContextLabel;
  private Message contextMessage;

  public DebuggingOptions(String debuggingContextLabel, Message contextMessage) {
    this.debuggingContextLabel = debuggingContextLabel;
    this.contextMessage = contextMessage;
  }

  public DebuggingOptions(String debuggingContextLabel) {
    this.debuggingContextLabel = debuggingContextLabel;
  }

  public DebuggingOptions(Message contextMessage) {
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

  public Message getContextMessage() {
    return contextMessage;
  }

  public void setContextMessage(Message contextMessage) {
    this.contextMessage = contextMessage;
  }
}
