package com.justinsb.ddpserver.triggeredpoll;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

public class InMemoryInvalidationSystem implements InvalidationSystem {

  private static final Logger log = LoggerFactory.getLogger(InMemoryInvalidationSystem.class);

  final Map<String, WatchableValue> positions = Maps.newHashMap();

  WatchableValue getWatchable(String key) {
    synchronized (positions) {
      WatchableValue position = positions.get(key);
      if (position == null) {
        position = new WatchableValue(0);
        positions.put(key, position);
      }
      return position;
    }
  }

  /* (non-Javadoc)
   * @see com.justinsb.ddpserver.triggeredpoll.InvalidationSystem#waitForPosition(java.lang.String, long)
   */
  @Override
  public ListenableFuture<Long> waitForPosition(String key, long minPosition) {
    synchronized (this) {
      WatchableValue position = getWatchable(key);
      return position.waitForMin(minPosition);
    }
  }

  /* (non-Javadoc)
   * @see com.justinsb.ddpserver.triggeredpoll.InvalidationSystem#notifyChange(java.lang.String)
   */
  @Override
  public long notifyChange(String key) {
    synchronized (this) {
      WatchableValue position = getWatchable(key);
      long current = position.getValue();
      long next = current + 1;
      log.debug("Notify change: {}={}", key, next);
      position.setValue(next);

      return next;
    }
  }

  /* (non-Javadoc)
   * @see com.justinsb.ddpserver.triggeredpoll.InvalidationSystem#getPosition(java.lang.String)
   */
  @Override
  public long getPosition(String key) {
    synchronized (this) {
      WatchableValue position = getWatchable(key);
      return position.getValue();
    }
  }

}
