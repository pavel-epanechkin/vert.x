package io.vertx.core.debugging;

import io.netty.util.internal.ThreadLocalRandom;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DebuggingHeader {

  private String messageId;
  private String prevMessageId;
  private String debuggingLabel;

  public static String DEBUGGING_HEADER_NAME = "VERTX-DEBUG";

  private final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghigklmnopqrstuvwxyz1234567890";
  private final int ID_LENGTH = 16;

  public DebuggingHeader(String prevMessageId, String debuggingLabel) {
    this.messageId = generateMessageId();
    this.prevMessageId = prevMessageId;
    this.debuggingLabel = debuggingLabel;
  }

  public DebuggingHeader() {}

  public static DebuggingHeader fromJsonString(String jsonString) {
    return new JsonObject(jsonString).mapTo(DebuggingHeader.class);
  }

  public String toJsonString() {
    return JsonObject.mapFrom(this).toString();
  }

  private String generateMessageId() {
    StringBuilder code = new StringBuilder();
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    while (code.length() < ID_LENGTH) {
      int index = (int) (rand.nextFloat() * CHARS.length());
      code.append(CHARS.charAt(index));
    }

    return code.toString();
  }

  public String getMessageId() {
    return messageId;
  }

  public String getPrevMessageId() {
    return prevMessageId;
  }

  public String getDebuggingLabel() {
    return debuggingLabel;
  }

}
