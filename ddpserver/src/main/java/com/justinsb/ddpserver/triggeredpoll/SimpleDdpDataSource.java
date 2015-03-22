package com.justinsb.ddpserver.triggeredpoll;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.justinsb.ddpserver.DdpDataSource;
import com.justinsb.ddpserver.DdpMethod;
import com.justinsb.ddpserver.DdpMethodContext;
import com.justinsb.ddpserver.DdpMethodResult;
import com.justinsb.ddpserver.DdpPublish;
import com.justinsb.ddpserver.DdpSession;

/**
 * Backend for DDP
 */
public class SimpleDdpDataSource implements DdpDataSource {

  private static final Logger log = LoggerFactory.getLogger(SimpleDdpDataSource.class);

  final Map<String, DdpMethod> ddpMethods = Maps.newHashMap();
  final Map<String, DdpPublish> ddpPublishes = Maps.newHashMap();

  public void addMethod(String methodName, DdpMethod method) {
    Preconditions.checkNotNull(method);
    ddpMethods.put(methodName, method);
  }

  public void addPublish(String name, DdpPublish publish) {
    Preconditions.checkNotNull(publish);
    ddpPublishes.put(name, publish);
  }

  @Override
  public DdpPublish getPublishFunction(DdpSession session, final String collectionName, JsonArray params)
      throws Exception {

    DdpPublish publish = ddpPublishes.get(collectionName);
    if (publish != null) {
      return publish;
    }

    log.warn("Unknown subscription: {}", collectionName);
    return null;
  }

  @Override
  public DdpMethodResult executeMethod(final DdpSession session, final String methodId, String method, JsonArray params)
      throws Exception {
    DdpMethod ddpMethod = ddpMethods.get(method);
    if (ddpMethod != null) {
      DdpMethodContext context = new DdpMethodContext(session, methodId, method);
      DdpMethodResult result = ddpMethod.executeMethod(context, params);
      return result;
    }

    log.warn("Unknown method: {}", method);
    throw new IllegalArgumentException();
  }

}
