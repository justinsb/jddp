package com.justinsb.ddpserver.triggeredpoll;

import java.io.IOException;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justinsb.ddpserver.DdpPublishContext;
import com.justinsb.ddpserver.DdpSubscription;
import com.justinsb.ddpserver.Jsonable;

public abstract class SimpleDdpSubscription extends DdpSubscription {

  private static final Logger log = LoggerFactory.getLogger(SimpleDdpSubscription.class);

  public SimpleDdpSubscription(DdpPublishContext context) {
    super(context);
  }

  public void begin() throws Exception {
    String collectionName = getCollectionName();

    Iterable<Entry<String, Jsonable>> items = getInitialItems();
    mergeBox.replaceAll(subscriptionId, collectionName, items);
    sendReady();
  }


  @Override
  public void recalculate() throws Exception {
    begin();
  }
  
  
  protected abstract Iterable<Entry<String, Jsonable>> getInitialItems() throws Exception;

}
