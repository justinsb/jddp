package com.justinsb.ddpserver;

public class DdpPublishContext {

  final DdpMergeBox mergeBox;
  final String subscriptionId;
  final String collectionName;
  final DdpSession session;

  public DdpPublishContext(DdpSession session, String subscriptionId, String collectionName) {
    this.session = session;
    this.mergeBox = session.mergeBox;
    this.subscriptionId = subscriptionId;
    this.collectionName = collectionName;
  }

  public DdpMergeBox getMergeBox() {
    return mergeBox;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public DdpSession getSession() {
    return session;
  }

}
