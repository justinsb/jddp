package com.meteor.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Tracker {
  static final Logger log = LoggerFactory.getLogger(Tracker.class);

  // //////////////////////////////////////////////////
  // //Package docs at http://docs.meteor.com/#tracker //
  // //////////////////////////////////////////////////
  //
  // /**
  // * @namespace Tracker
  // * @summary The namespace for Tracker-related methods.
  // */
  // Tracker = {};
  //
  // http://docs.meteor.com/#tracker_active

  /**
   * @summary True if there is a current computation, meaning that dependencies on reactive data sources will be tracked
   *          and potentially cause the current computation to be rerun.
   * @locus Client
   * @type {Boolean}
   */
  static boolean active = false;

  // http://docs.meteor.com/#tracker_currentcomputation

  /**
   * @summary The current computation, or `null` if there isn't one. The current computation is the
   *          [`Tracker.Computation`](#tracker_computation) object created by the innermost active call to
   *          `Tracker.autorun`, and it's the computation that gains dependencies when reactive data sources are
   *          accessed.
   * @locus Client
   * @type {Tracker.Computation}
   */
  // Tracker.currentComputation = null;
  static Computation currentComputation = null;

  static void setCurrentComputation(Computation c) {
    Tracker.currentComputation = c;
    Tracker.active = c != null;
  }

  // var _debugFunc = function () {
  // // We want this code to work without Meteor, and also without
  // // "console" (which is technically non-standard and may be missing
  // // on some browser we come across, like it was on IE 7).
  // //
  // // Lazy evaluation because `Meteor` does not exist right away.(??)
  // return (typeof Meteor !== "undefined" ? Meteor._debug :
  // ((typeof console !== "undefined") && console.log ?
  // function () { console.log.apply(console, arguments); } :
  // function () {}));
  // };
  //
  static <E extends Throwable> void _throwOrLog(Object from, E e) throws E {
    if (throwFirstError) {
      throw e;
    } else {
      log.warn("Exception from Tracker {} function:", e);
    }
    // var messageAndStack;
    // if (e.stack && e.message) {
    // var idx = e.stack.indexOf(e.message);
    // if (idx >= 0 && idx <= 10) // allow for "Error: " (at least 7)
    // messageAndStack = e.stack; // message is part of e.stack, as in Chrome
    // else
    // messageAndStack = e.message +
    // (e.stack.charAt(0) === '\n' ? '' : '\n') + e.stack; // e.g. Safari
    // } else {
    // messageAndStack = e.stack || e.message;
    // }
    // _debugFunc()("Exception from Tracker " + from + " function:",
    // messageAndStack);
    // }
  }

  // Takes a function `f`, and wraps it in a `Meteor._noYieldsAllowed`
  // block if we are running on the server. On the client, returns the
  // original function (since `Meteor._noYieldsAllowed` is a
  // no-op). This has the benefit of not adding an unnecessary stack
  // frame on the client.
  static <T, R> Function<T, R> withNoYieldsAllowed(Function<T, R> f) {
    return f;
    // var withNoYieldsAllowed = function (f) {
    // if ((typeof Meteor === 'undefined') || Meteor.isClient) {
    // return f;
    // } else {
    // return function () {
    // var args = arguments;
    // Meteor._noYieldsAllowed(function () {
    // f.apply(null, args);
    // });
    // };
    // }
    // };
  }

  static <V> Consumer<V> withNoYieldsAllowed(Consumer<V> f) {
    return f;
  }

  static AtomicLong nextId = new AtomicLong(1);
  // computations whose callbacks we should call at flush time
  static Queue<Computation> pendingComputations = Lists.newLinkedList();

  // `true` if a Tracker.flush is scheduled, or if we are in Tracker.flush now
  static boolean willFlush = false;
  // `true` if we are in Tracker.flush now
  static boolean inFlush = false;
  // `true` if we are computing a computation now, either first time
  // or recompute. This matches Tracker.active unless we are inside
  // Tracker.nonreactive, which nullfies currentComputation even though
  // an enclosing computation may still be running.
  static boolean inCompute = false;
  // `true` if the `_throwFirstError` option was passed in to the call
  // to Tracker.flush that we are in. When set, throw rather than log the
  // first error encountered while flushing. Before throwing the error,
  // finish flushing (from a finally block), logging any subsequent
  // errors.
  static boolean throwFirstError = false;

  static Queue<Function<Void, Void>> afterFlushCallbacks = Lists.newLinkedList();

  static ExecutorService executor = Executors.newSingleThreadExecutor();

  public static void requireFlush() {
    if (!willFlush) {
      executor.submit(() -> {
        Tracker.flush();
      });
      willFlush = true;
    }
  }

  // http://docs.meteor.com/#tracker_flush

  public static class FlushOptions {
    boolean _throwFirstError;
  }

  public static void flush() {
    flush(null);
  }

  /**
   * @summary Process all reactive updates immediately and ensure that all invalidated computations are rerun.
   * @locus Client
   */
  public static void flush(FlushOptions _opts) {
    // XXX What part of the comment below is still true? (We no longer
    // have Spark)
    //
    // Nested flush could plausibly happen if, say, a flush causes
    // DOM mutation, which causes a "blur" event, which runs an
    // app event handler that calls Tracker.flush. At the moment
    // Spark blocks event handlers during DOM mutation anyway,
    // because the LiveRange tree isn't valid. And we don't have
    // any useful notion of a nested flush.
    //
    // https://app.asana.com/0/159908330244/385138233856
    if (inFlush)
      throw new Error("Can't call Tracker.flush while flushing");

    if (inCompute)
      throw new Error("Can't flush inside Tracker.autorun");

    inFlush = true;
    willFlush = true;
    throwFirstError = ((_opts != null) && _opts._throwFirstError);

    boolean finishedTry = false;
    try {
      while (!pendingComputations.isEmpty() || !afterFlushCallbacks.isEmpty()) {

        // recompute all pending computations
        while (!pendingComputations.isEmpty()) {
          Computation comp = pendingComputations.remove();
          comp._recompute();
        }

        if (!afterFlushCallbacks.isEmpty()) {
          // call one afterFlush callback, which may
          // invalidate more computations
          Function<Void, Void> func = afterFlushCallbacks.remove();
          try {
            func.apply(null);
          } catch (Throwable e) {
            try {
              _throwOrLog("afterFlush", e);
            } catch (Throwable e1) {
              throw Throwables.propagate(e1);
            }
          }
        }
      }
      finishedTry = true;
    } finally {
      if (!finishedTry) {
        // we're erroring
        inFlush = false; // needed before calling `Tracker.flush()` again
        FlushOptions flushOptions = new FlushOptions();
        flushOptions._throwFirstError = false;
        Tracker.flush(flushOptions); // finish flushing
      }
      willFlush = false;
      inFlush = false;
    }
  }

  // http://docs.meteor.com/#tracker_autorun
  //
  // Run f(). Record its dependencies. Rerun it whenever the
  // dependencies change.
  //
  // Returns a new Computation, which is also passed to f.
  //
  // Links the computation to the current computation
  // so that it is stopped if the current computation is invalidated.

  /**
   * @summary Run a function now and rerun it later whenever its dependencies change. Returns a Computation object that
   *          can be used to stop or observe the rerunning.
   * @locus Client
   * @param {Function} runFunc The function to run. It receives one argument: the Computation object that will be
   *        returned.
   * @returns {Tracker.Computation}
   */
  public static Computation autorun(Function<Computation, Void> f) {
    // constructingComputation = true;
    Computation c = new Computation(f, Tracker.currentComputation);

    if (Tracker.active)
      Tracker.onInvalidate((Computation _c) -> {
        c.stop();
      });

    return c;
  }

  // http://docs.meteor.com/#tracker_nonreactive
  //
  // Run `f` with no current computation, returning the return value
  // of `f`. Used to turn off reactivity for the duration of `f`,
  // so that reactive data sources accessed by `f` will not result in any
  // computations being invalidated.

  /**
   * @summary Run a function without tracking dependencies.
   * @locus Client
   * @param {Function} func A function to call immediately.
   */
  public static void nonreactive(Consumer<Void> f) {
    Computation previous = Tracker.currentComputation;
    Tracker.setCurrentComputation(null);
    try {
      f.accept(null);
    } finally {
      Tracker.setCurrentComputation(previous);
    }
  }

  // http://docs.meteor.com/#tracker_oninvalidate

  /**
   * @summary Registers a new [`onInvalidate`](#computation_oninvalidate) callback on the current computation (which
   *          must exist), to be called immediately when the current computation is invalidated or stopped.
   * @locus Client
   * @param {Function} callback A callback function that will be invoked as `func(c)`, where `c` is the computation on
   *        which the callback is registered.
   */
  public static void onInvalidate(Consumer<Computation> f) {
    if (!Tracker.active)
      throw new Error("Tracker.onInvalidate requires a currentComputation");

    Tracker.currentComputation.onInvalidate(f);
  }

  // http://docs.meteor.com/#tracker_afterflush

  /**
   * @summary Schedules a function to be called during the next flush, or later in the current flush if one is in
   *          progress, after all invalidated computations have been rerun. The function will be run once and not on
   *          subsequent flushes unless `afterFlush` is called again.
   * @locus Client
   * @param {Function} callback A function to call at flush time.
   */
  public void afterFlush(Function<Void, Void> f) {
    afterFlushCallbacks.add(f);
    requireFlush();
  }

}