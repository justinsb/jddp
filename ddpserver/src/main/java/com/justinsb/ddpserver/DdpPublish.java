package com.justinsb.ddpserver;

import java.io.IOException;

import com.google.gson.JsonArray;

public interface DdpPublish {

  DdpSubscription subscribe(DdpPublishContext context, String name, JsonArray params) throws Exception;

}
