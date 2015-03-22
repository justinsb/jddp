package com.justinsb.ddpserver.triggeredpoll;

import com.google.common.util.concurrent.ListenableFuture;

public interface InvalidationSystem {

  public abstract ListenableFuture<Long> waitForPosition(String key, long minPosition);

  public abstract long notifyChange(String key);

  public abstract long getPosition(String key);

}