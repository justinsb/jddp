package com.justinsb.ddpserver.triggeredpoll;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Helper class that implements a value that can be set & watched (with a ListenableFuture)
 *
 */
public class WatchableValue {
  static class Watcher {
    final long minValue;
    final SettableFuture<Long> future;

    public Watcher(long minValue) {
      this.minValue = minValue;
      this.future = SettableFuture.create();
    }
  }

  final List<Watcher> watchers = Lists.newArrayList();

  long value;

  public WatchableValue(long value) {
    super();
    this.value = value;
  }

  public void setValue(long newValue) {
    synchronized (this) {
      this.value = newValue;
      Iterator<Watcher> it = watchers.iterator();
      while (it.hasNext()) {
        Watcher watcher = it.next();
        if (watcher.minValue <= newValue) {
          watcher.future.set(newValue);
          it.remove();
        }
      }
    }
  }

  public ListenableFuture<Long> waitForMin(long minValue) {
    synchronized (this) {
      Watcher watcher = new Watcher(minValue);
      if (watcher.minValue <= value) {
        watcher.future.set(value);
      } else {
        watchers.add(watcher);
      }
      return watcher.future;
    }
  }

  public long getValue() {
    synchronized (this) {
      return value;
    }
  }

  @Override
  public String toString() {
    return "WatchableValue [=" + value + "]";
  }

}
