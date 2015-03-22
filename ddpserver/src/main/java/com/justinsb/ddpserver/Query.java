package com.justinsb.ddpserver;

import java.util.Map.Entry;

/**
 * Abstraction over a data-query.
 *
 * Note that we assume that the data store is dumb, and that we do invalidations separately.
 *
 */
public interface Query {

  String getCollectionName();

  Iterable<Entry<String, Jsonable>> getItems();

}
