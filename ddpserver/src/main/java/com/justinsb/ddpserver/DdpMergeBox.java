package com.justinsb.ddpserver;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Implements a merge-box, which optimizes delivery of items on subscriptions.
 * 
 * The current implementation is very simplistic, and resends objects unnecessarily.
 *
 */
public class DdpMergeBox {
  final DdpSession ddpSession;

  public DdpMergeBox(DdpSession ddpSession) {
    this.ddpSession = ddpSession;
  }

  class ClientCollectionState {
    final String collectionName;
    final Multiset<String> ids = HashMultiset.create();

    final HashMultimap<String, String> subscriptionObjects = HashMultimap.create();

    public ClientCollectionState(String collectionName) {
      super();
      this.collectionName = collectionName;
    }

    public void replaceAll(String subscriptionId, Iterable<Entry<String, Jsonable>> objects) throws IOException {
      synchronized (this) {
        Set<String> oldIds = subscriptionObjects.get(subscriptionId);
        Set<String> newIds = Sets.newConcurrentHashSet();

        for (Entry<String, Jsonable> entry : objects) {
          JsonObject msg = new JsonObject();
          String id = entry.getKey();

          if (ids.contains(id)) {
            // TODO: Check if actually changed?
            msg.addProperty("msg", "changed");
          } else {
            msg.addProperty("msg", "added");
          }
          msg.addProperty("collection", collectionName);
          msg.addProperty("id", id);
          msg.add("fields", entry.getValue().toJsonElement());

          ddpSession.sendMessage(msg);

          if (!oldIds.contains(id)) {
            ids.add(id);
          }
          newIds.add(id);
        }

        for (String id : oldIds) {
          if (!newIds.contains(id)) {
            int preCount = ids.remove(id, 1);
            if (preCount <= 1) {
              assert preCount == 1;

              JsonObject msg = new JsonObject();
              msg.addProperty("msg", "removed");
              msg.addProperty("collection", collectionName);
              msg.addProperty("id", id);
              ddpSession.sendMessage(msg);
            }
          }
        }

        subscriptionObjects.replaceValues(subscriptionId, newIds);
      }
    }

    public void unsubscribe(String subscriptionId) throws IOException {
      synchronized (this) {
        Set<String> subscriptionIds = subscriptionObjects.removeAll(subscriptionId);
        for (String id : subscriptionIds) {
          int preCount = ids.remove(id, 1);
          if (preCount <= 1) {
            assert preCount == 1;

            JsonObject msg = new JsonObject();
            msg.addProperty("msg", "removed");
            msg.addProperty("collection", collectionName);
            msg.addProperty("id", id);
            ddpSession.sendMessage(msg);
          }
        }
      }
    }

  }

  final Map<String, ClientCollectionState> clientCollectionStates = Maps.newHashMap();

  public void replaceAll(String subscriptionId, String collectionName, Iterable<Entry<String, Jsonable>> objects)
      throws IOException {
    ClientCollectionState clientCollectionState = getClientCollectionState(collectionName);

    clientCollectionState.replaceAll(subscriptionId, objects);
  }

  private ClientCollectionState getClientCollectionState(String collectionName) {
    synchronized (clientCollectionStates) {
      ClientCollectionState clientCollectionState = clientCollectionStates.get(collectionName);
      if (clientCollectionState == null) {
        clientCollectionState = new ClientCollectionState(collectionName);
        clientCollectionStates.put(collectionName, clientCollectionState);
      }
      return clientCollectionState;
    }
  }

  public void sendReady(String subscriptionId) throws IOException {
    JsonObject ready = new JsonObject();
    ready.addProperty("msg", "ready");
    JsonArray readySubs = new JsonArray();
    readySubs.add(new JsonPrimitive(subscriptionId));
    ready.add("subs", readySubs);
    ddpSession.sendMessage(ready);
  }

  public void unsubscribe(String subscriptionId, String collectionName) throws IOException {
    ClientCollectionState clientCollectionState = getClientCollectionState(collectionName);

    clientCollectionState.unsubscribe(subscriptionId);

    {
      JsonObject nosub = new JsonObject();
      nosub.addProperty("msg", "nosub");
      nosub.addProperty("id", subscriptionId);
      ddpSession.sendMessage(nosub);
    }
  }

  public void disconnected() {
    synchronized (clientCollectionStates) {
      clientCollectionStates.clear();
    }
  }

}
