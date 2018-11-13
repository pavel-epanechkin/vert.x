package io.vertx.core.debugging;

import io.vertx.core.json.JsonObject;

public class DebuggingHeader {

  private String contextLabel;

  public DebuggingHeader() {
  }

  public DebuggingHeader(String contextLabel) {
    this.contextLabel = contextLabel;
  }

  public String getContextLabel() {
    return contextLabel;
  }

  public void setContextLabel(String contextLabel) {
    this.contextLabel = contextLabel;
  }

  public DebuggingHeader fromJsonString(String jsonString) {
    return new JsonObject(jsonString).mapTo(DebuggingHeader.class);
  }

  public String toJsonString() {
    return JsonObject.mapFrom(this).toString();
  }

}
