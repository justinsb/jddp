package com.justinsb.ddpserver;

import com.google.gson.JsonArray;

public interface DdpMethod {

  DdpMethodResult executeMethod(DdpMethodContext context, JsonArray params) throws Exception;

}
