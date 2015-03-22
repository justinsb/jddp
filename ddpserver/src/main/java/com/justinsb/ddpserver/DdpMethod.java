package com.justinsb.ddpserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public interface DdpMethod {

  DdpMethodResult executeMethod(DdpMethodContext context, JsonArray params) throws Exception;

}
