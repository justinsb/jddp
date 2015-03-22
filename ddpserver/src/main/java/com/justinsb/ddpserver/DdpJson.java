package com.justinsb.ddpserver;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DdpJson {

  public static JsonObject buildError(Exception e) {
    if (e instanceof MeteorError) {
      MeteorError meteorError = (MeteorError) e;
      int code = meteorError.getCode();
      String reason = meteorError.getReason();
      return buildMeteorError(code, reason);
    } else {
      JsonObject error = new JsonObject();
      // error.addProperty("error", error);
      error.addProperty("reason", e.toString());
      // error.addProperty("details", e.toString());
      return error;
    }
  }

  public static JsonObject buildMeteorError(int code, String reason) {
    JsonObject error = new JsonObject();

    error.addProperty("error", code);
    error.addProperty("reason", reason);
    error.addProperty("message", reason + " [" + code + "]");
    error.addProperty("errorType", "Meteor.Error");

    return error;
  }

  public static String optionalString(JsonArray params, int i) {
    if (params.size() <= i) {
      return null;
    }

    JsonElement jsonElement = params.get(i);
    if (jsonElement.isJsonNull())
      return null;
    return jsonElement.getAsString();
  }

  public static String requiredString(JsonArray params, int i) {
    String v = optionalString(params, i);
    if (Strings.isNullOrEmpty(v)) {
      throw new IllegalArgumentException();
    }
    return v;
  }

}
