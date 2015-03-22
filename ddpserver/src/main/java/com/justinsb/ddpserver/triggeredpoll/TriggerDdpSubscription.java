package com.justinsb.ddpserver.triggeredpoll;

import java.io.IOException;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.justinsb.ddpserver.DdpPublishContext;
import com.justinsb.ddpserver.DdpSubscription;
import com.justinsb.ddpserver.Jsonable;
import com.justinsb.ddpserver.Query;

public class TriggerDdpSubscription extends DdpSubscription {

  private static final Logger log = LoggerFactory.getLogger(TriggerDdpSubscription.class);

  final Query query;

  boolean stop = false;

  final InvalidationSystem invalidationSystem;
  final WatchableValue sentPosition = new WatchableValue(0);
  final String invalidationKey;

  public TriggerDdpSubscription(DdpPublishContext context, InvalidationSystem invalidationSystem,
      String invalidationKey, Query query) {
    super(context);
    this.invalidationSystem = invalidationSystem;
    this.query = query;
    this.invalidationKey = invalidationKey;
  }

  @Override
  public String toString() {
    return "TriggerDdpSubscription [query=" + query + ", invalidationKey=" + invalidationKey + ", sentPosition="
        + sentPosition + ", stop=" + stop + "]";
  }

  @Override
  public void begin() throws IOException {
    String collectionName = query.getCollectionName();

    long position = invalidationSystem.getPosition(invalidationKey);
    Iterable<Entry<String, Jsonable>> items = query.getItems();
    mergeBox.replaceAll(subscriptionId, collectionName, items);
    sendReady();

    sentPosition.setValue(position);

    watchChanges(position + 1);
  }

  private void watchChanges(long minPosition) {
    ListenableFuture<Long> changeHandle = invalidationSystem.waitForPosition(invalidationKey, minPosition);
    Futures.addCallback(changeHandle, new FutureCallback<Long>() {

      @Override
      public void onFailure(Throwable e) {
        log.error("Error in subscription", e);
      }

      @Override
      public void onSuccess(Long newPosition) {
        try {
          if (stop) {
            // We set the position to avoid any races
            sentPosition.setValue(newPosition);
            return;
          }

          log.debug("Got change notification: {}={}", invalidationKey, newPosition);

          String collectionName = query.getCollectionName();
          Iterable<Entry<String, Jsonable>> items = query.getItems();
          mergeBox.replaceAll(subscriptionId, collectionName, items);
          log.debug("Setting sentPosition={}", newPosition);
          sentPosition.setValue(newPosition);

          watchChanges(newPosition + 1);
        } catch (Exception e) {
          log.error("Error in subscription refresh", e);
        }
      }
    }, DdpExecutors.DEFAULT_EXECUTOR);
  }

  @Override
  public void end() throws IOException {
    String collectionName = query.getCollectionName();

    mergeBox.unsubscribe(subscriptionId, collectionName);

    // TODO: Actively stop
    this.stop = true;
  }

  public boolean isWatching(String invalidationKey) {
    if (stop) {
      return false;
    }
    return this.invalidationKey.equals(invalidationKey);
  }

  public boolean hasSent(long minPosition) {
    return sentPosition.getValue() >= minPosition;
  }

  public ListenableFuture<Long> waitForSent(long newPosition) {
    return sentPosition.waitForMin(newPosition);
  }

  @Override
  public void disconnected() {
    stop = true;

    super.disconnected();
  }

  @Override
  public void recalculate() throws IOException {
    begin();
  }

}
