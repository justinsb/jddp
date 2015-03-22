package com.justinsb.ddpserver.examples.inmem;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.justinsb.ddpserver.DdpSession;
import com.justinsb.ddpserver.Jsonable;
import com.justinsb.ddpserver.Storage;

public class InMemoryStorage implements Storage {

  private static final Logger log = LoggerFactory.getLogger(InMemoryStorage.class);

  final Map<String, InMemoryCollection> collections = Maps.newHashMap();

  protected InMemoryCollection getCollection(String name) {
    synchronized (collections) {
      InMemoryCollection collection = collections.get(name);
      if (collection == null) {
        collection = new InMemoryCollection(name);
        collections.put(name, collection);
      }
      return collection;
    }
  }

  @Override
  public Set<Entry<String, Jsonable>> query(String collectionName) {
    InMemoryCollection collection = getCollection(collectionName);
    return collection.getItems();
  }

  @Override
  public JsonElement executeCollectionMethod(DdpSession session, String methodId, String collectionName,
      String collectionMethod, JsonArray params) {
    JsonElement result;

    // {"msg":"method","method":"/lists/insert","params":[{"name":"Meteor Principles","incompleteCount":7}],"id":"1"}
    InMemoryCollection collection = getCollection(collectionName);
    if (collectionMethod.equals("insert")) {
      if (params.size() == 1) {
        result = collection.insert(params.get(0).getAsJsonObject());
      } else {
        throw new IllegalArgumentException();
      }
    } else if (collectionMethod.equals("update")) {
      result = collection.update(params);
    } else if (collectionMethod.equals("remove")) {
      result = collection.remove(params);
    } else {
      log.warn("Unknown collection method: {}", collectionMethod);
      throw new IllegalArgumentException();
    }

    return result;
  }

}
