package io.vertx.core.debugging;

import java.io.Serializable;


public class MessageInfo implements Serializable {

  private Integer recordId;

  private String messageId;

  private String label;

  private String type;

  private long timestamp;

  private String prevMessageId;

  private String targetAddress;

  private String replyAddress;

  private String headers;

  private String body;

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getPrevMessageId() {
    return prevMessageId;
  }

  public void setPrevMessageId(String prevMessageId) {
    this.prevMessageId = prevMessageId;
  }

  public String getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(String targetAddress) {
    this.targetAddress = targetAddress;
  }

  public String getReplyAddress() {
    return replyAddress;
  }

  public void setReplyAddress(String replyAddress) {
    this.replyAddress = replyAddress;
  }

  public String getHeaders() {
    return headers;
  }

  public void setHeaders(String headers) {
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer recordId) {
    this.recordId = recordId;
  }
}
