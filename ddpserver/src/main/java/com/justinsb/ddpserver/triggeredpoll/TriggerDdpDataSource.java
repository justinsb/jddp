package com.justinsb.ddpserver.triggeredpoll;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.justinsb.ddpserver.DdpDataSource;
import com.justinsb.ddpserver.DdpMergeBox;
import com.justinsb.ddpserver.DdpMethodResult;
import com.justinsb.ddpserver.DdpPublish;
import com.justinsb.ddpserver.DdpPublishContext;
import com.justinsb.ddpserver.DdpSession;
import com.justinsb.ddpserver.DdpSubscription;
import com.justinsb.ddpserver.Jsonable;
import com.justinsb.ddpserver.Query;
import com.justinsb.ddpserver.Storage;

/**
 * Backend for DDP, which uses a simple data store with a separate invalidation system
 */
public class TriggerDdpDataSource implements DdpDataSource {

  private static final Logger log = LoggerFactory.getLogger(TriggerDdpDataSource.class);

  final List<TriggerDdpSubscription> subscriptions = Lists.newArrayList();

  final Storage storage;
  final InvalidationSystem invalidationSystem;

  public TriggerDdpDataSource(Storage storage, InvalidationSystem invalidationSystem) {
    this.storage = storage;
    this.invalidationSystem = invalidationSystem;
  }

  @Override
  public DdpPublish getPublishFunction(DdpSession session, final String collectionName, JsonArray params) {

    String invalidationKey = collectionName;
    Query query = new Query() {

      @Override
      public String getCollectionName() {
        return collectionName;
      }

      @Override
      public Iterable<Entry<String, Jsonable>> getItems() {
        Iterable<Entry<String, Jsonable>> items = storage.query(collectionName);
        return items;
      }

      public String toString() {
        return "Query:" + collectionName;
      }

    };
    return new DdpPublish() {
      @Override
      public DdpSubscription subscribe(DdpPublishContext context, String name, JsonArray params) throws Exception {
        TriggerDdpSubscription subscription = new TriggerDdpSubscription(context, invalidationSystem, invalidationKey,
            query);
        synchronized (this) {
          subscriptions.add(subscription);
        }

        return subscription;
      }
    };
  }

  @Override
  public DdpMethodResult executeMethod(final DdpSession session, final String methodId, String method, JsonArray params) {
    List<String> tokens = Splitter.on('/').splitToList(method);
    if (tokens.size() == 3) {
      if (tokens.get(0).isEmpty()) {
        String collectionName = tokens.get(1);
        String collectionMethod = tokens.get(2);

        JsonElement result = storage.executeCollectionMethod(session, methodId, collectionName, collectionMethod,
            params);

        String invalidationKey = collectionName;
        long newPosition = invalidationSystem.notifyChange(invalidationKey);

        ListenableFuture<Long> sentChanges = waitForSubscriptionPosition(invalidationKey, newPosition);
        Futures.addCallback(sentChanges, new FutureCallback<Long>() {

          @Override
          public void onFailure(Throwable t) {
            session.unexpectedError(t);
          }

          @Override
          public void onSuccess(Long pos) {
            try {
              JsonObject updated = new JsonObject();
              updated.addProperty("msg", "updated");
              JsonArray methodIds = new JsonArray();
              methodIds.add(new JsonPrimitive(methodId));
              updated.add("methods", methodIds);
              session.sendMessage(updated);
            } catch (Exception e) {
              session.unexpectedError(e);
            }
          }

        });
        return DdpMethodResult.complete(result);
      }
    }

    throw new IllegalArgumentException();
  }

  private ListenableFuture<Long> waitForSubscriptionPosition(String invalidationKey, long newPosition) {
    SettableFuture<Long> future = SettableFuture.create();

    waitForSubscriptionPosition0(invalidationKey, newPosition, future);
    return future;
  }

  private void waitForSubscriptionPosition0(final String invalidationKey, final long newPosition,
      final SettableFuture<Long> future) {
    synchronized (subscriptions) {
      TriggerDdpSubscription notReady = null;
      for (TriggerDdpSubscription subscription : subscriptions) {
        if (!subscription.isWatching(invalidationKey)) {
          continue;
        }
        if (!subscription.hasSent(newPosition)) {
          notReady = subscription;
          break;
        }
      }

      if (notReady == null) {
        future.set(newPosition);
        return;
      }

      log.debug("Waiting for subscription to hit {}: {}", newPosition, notReady);

      ListenableFuture<Long> notification = notReady.waitForSent(newPosition);
      Futures.addCallback(notification, new FutureCallback<Long>() {

        @Override
        public void onFailure(Throwable t) {
          future.setException(t);
        }

        @Override
        public void onSuccess(Long p) {
          waitForSubscriptionPosition0(invalidationKey, newPosition, future);
        }
      });
    }
  }

}
