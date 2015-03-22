package com.justinsb.ddpserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The logic implementation that the DDP connection exposes
 *
 */
public interface DdpDataSource {

  DdpPublish getPublishFunction(DdpSession session, String name, JsonArray params) throws Exception;

  DdpMethodResult executeMethod(DdpSession session, String methodId, String method, JsonArray params) throws Exception;

}
