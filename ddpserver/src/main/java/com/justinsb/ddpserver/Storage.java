package com.justinsb.ddpserver;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Abstract queryable data store
 *
 */
public interface Storage {

  Iterable<Entry<String, Jsonable>> query(String collectionName);

  JsonElement executeCollectionMethod(DdpSession session, String methodId, String collectionName,
      String collectionMethod, JsonArray params);

}
