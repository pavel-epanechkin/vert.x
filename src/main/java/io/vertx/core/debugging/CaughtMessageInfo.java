package io.vertx.core.debugging;

import io.vertx.core.eventbus.Message;

import java.util.Date;

public class CaughtMessageInfo {

  private Message message;
  private MessageType messageType;
  private Date messageOccuranceTime;

  public CaughtMessageInfo(Message message, MessageType messageType) {
    this.message = message;
    this.messageType = messageType;
    this.messageOccuranceTime = new Date(System.currentTimeMillis());
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public void setMessageType(MessageType messageType) {
    this.messageType = messageType;
  }

  public Date getMessageOccuranceTime() {
    return messageOccuranceTime;
  }

  public void setMessageOccuranceTime(Date messageOccuranceTime) {
    this.messageOccuranceTime = messageOccuranceTime;
  }
}
