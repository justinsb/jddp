package com.justinsb.ddpserver;

import com.google.gson.JsonElement;

public interface Jsonable {

  JsonElement toJsonElement();

  public static Jsonable fromJson(final JsonElement jsonElement) {
    return new Jsonable() {

      @Override
      public JsonElement toJsonElement() {
        return jsonElement;
      }
    };
  }

}
