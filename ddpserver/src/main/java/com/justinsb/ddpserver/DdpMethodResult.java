package com.justinsb.ddpserver;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;

public class DdpMethodResult {
  final JsonElement result;
  final ListenableFuture<Boolean> complete;

  DdpMethodResult(JsonElement result, ListenableFuture<Boolean> complete) {
    this.result = result;
    this.complete = complete;
  }

  public static DdpMethodResult complete(JsonElement result) {
    return new DdpMethodResult(result, Futures.immediateFuture(true));
  }

  public JsonElement getResult() {
    return result;
  }

  public ListenableFuture<Boolean> getCompletion() {
    return complete;
  }

}
