package com.justinsb.ddpserver;

import java.io.IOException;

/**
 * Abstract DDP subscription
 *
 */
public abstract class DdpSubscription {
  protected final DdpMergeBox mergeBox;
  protected final String subscriptionId;
  protected final DdpPublishContext context;

  public DdpSubscription(DdpPublishContext context) {
    this.context = context;
    this.mergeBox = context.getMergeBox();
    this.subscriptionId = context.getSubscriptionId();
  }

  protected void sendReady() throws IOException {
    mergeBox.sendReady(subscriptionId);
  }

  protected String getCollectionName() {
    return context.getCollectionName();
  }

  public abstract void begin() throws Exception;

  public void end() throws IOException {
    String collectionName = getCollectionName();

    mergeBox.unsubscribe(subscriptionId, collectionName);
  }

  public void disconnected() {

  }

  public abstract void recalculate() throws Exception;
}
