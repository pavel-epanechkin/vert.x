package io.vertx.core.debugging;

import io.vertx.core.json.JsonObject;

import java.security.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DebuggingHeader {

  private String messageId;
  private ArrayList<String> prevMessageIds;
  private String debuggingLabel;
  private Date messageOccurrenceTime;

  private final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghigklmnopqrstuvwxyz1234567890";
  private final int ID_LENGTH = 16;

  public DebuggingHeader(ArrayList<String> prevMessageIds, String debuggingLabel) {
    this.messageId = generateMessageId();
    this.prevMessageIds = prevMessageIds;
    this.debuggingLabel = debuggingLabel;
  }

  public static DebuggingHeader fromJsonString(String jsonString) {
    return new JsonObject(jsonString).mapTo(DebuggingHeader.class);
  }

  public String toJsonString() {
    return JsonObject.mapFrom(this).toString();
  }

  private String generateMessageId() {
    StringBuilder code = new StringBuilder();
    Random rand = new Random();
    while (code.length() < ID_LENGTH) {
      int index = (int) (rand.nextFloat() * CHARS.length());
      code.append(CHARS.charAt(index));
    }

    return code.toString();
  }

  public String getMessageId() {
    return messageId;
  }


  public ArrayList<String> getPrevMessageIds() {
    return prevMessageIds;
  }

  public String getDebuggingLabel() {
    return debuggingLabel;
  }

  public Date getMessageOccurrenceTime() {
    return messageOccurrenceTime;
  }

  public void setMessageOccurrenceTime(Date messageOccurrenceTime) {
    this.messageOccurrenceTime = messageOccurrenceTime;
  }
}
