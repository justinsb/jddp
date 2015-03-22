package com.meteor.tracker;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

//Tracker.Computation constructor is visible but private
//(throws an error if you try to call it)
//var constructingComputation = false;

//http://docs.meteor.com/#tracker_computation

/**
 * @summary A Computation object represents code that is repeatedly rerun in response to reactive data changes.
 *          Computations don't have return values; they just perform actions, such as rerendering a template on the
 *          screen. Computations are created using Tracker.autorun. Use stop to prevent further rerunning of a
 *          computation.
 * @instancename computation
 */
public class Computation {
  private boolean stopped;
  private boolean invalidated;
  private boolean firstRun;
  private Computation _parent;
  private Function<Computation, Void> _func;
  private boolean _recomputing;
  private List<Consumer<Computation>> _onInvalidateCallbacks;
  final long _id;

  public Computation(Function<Computation, Void> f, Computation parent) {
    // if (! constructingComputation)
    // throw new Error(
    // "Tracker.Computation constructor is private; use Tracker.autorun");
    // constructingComputation = false;

    // http://docs.meteor.com/#computation_stopped

    /**
     * @summary True if this computation has been stopped.
     * @locus Client
     * @memberOf Tracker.Computation
     * @instance
     * @name stopped
     */
    this.stopped = false;

    // http://docs.meteor.com/#computation_invalidated

    /**
     * @summary True if this computation has been invalidated (and not yet rerun), or if it has been stopped.
     * @locus Client
     * @memberOf Tracker.Computation
     * @instance
     * @name invalidated
     * @type {Boolean}
     */
    this.invalidated = false;

    // http://docs.meteor.com/#computation_firstrun

    /**
     * @summary True during the initial run of the computation at the time `Tracker.autorun` is called, and false on
     *          subsequent reruns and at other times.
     * @locus Client
     * @memberOf Tracker.Computation
     * @instance
     * @name firstRun
     * @type {Boolean}
     */
    this.firstRun = true;

    this._id = Tracker.nextId.getAndIncrement();
    this._onInvalidateCallbacks = Lists.newArrayList();
    // the plan is at some point to use the parent relation
    // to constrain the order that computations are processed
    this._parent = parent;
    this._func = f;
    this._recomputing = false;

    boolean errored = true;
    try {
      this._compute();
      errored = false;
    } finally {
      this.firstRun = false;
      if (errored)
        this.stop();
    }
  }

  // http://docs.meteor.com/#computation_oninvalidate

  /**
   * @summary Registers `callback` to run when this computation is next invalidated, or runs it immediately if the
   *          computation is already invalidated. The callback is run exactly once and not upon future invalidations
   *          unless `onInvalidate` is called again after the computation becomes valid again.
   * @locus Client
   * @param {Function} callback Function to be called on invalidation. Receives one argument, the computation that was
   *        invalidated.
   */
  public void onInvalidate(Consumer<Computation> f) {
    if (this.invalidated) {
      Tracker.nonreactive((Void v) -> { Tracker.withNoYieldsAllowed(f).accept(this);  });
    } else {
      this._onInvalidateCallbacks.add(f);
    }
  }

  // http://docs.meteor.com/#computation_invalidate

  /**
   * @summary Invalidates this computation so that it will be rerun.
   * @locus Client
   */
  public void invalidate() {
    if (!this.invalidated) {
      // if we're currently in _recompute(), don't enqueue
      // ourselves, since we'll rerun immediately anyway.
      if (!this._recomputing && !this.stopped) {
        Tracker.requireFlush();
        Tracker.pendingComputations.add(this);
      }

      this.invalidated = true;

      // callbacks can't add callbacks, because
      // self.invalidated === true.
      for (Consumer<Computation> f : this._onInvalidateCallbacks) {
        Tracker.nonreactive((Void v) -> { Tracker.withNoYieldsAllowed(f).accept(this); });
      }
      this._onInvalidateCallbacks = Lists.newArrayList();
    }
  }

  // http://docs.meteor.com/#computation_stop

  /**
   * @summary Prevents this computation from rerunning.
   * @locus Client
   */
  public void stop() {
    if (!this.stopped) {
      this.stopped = true;
      this.invalidate();
    }
  }

  void _compute() {
    this.invalidated = false;

    Computation previous = Tracker.currentComputation;
    Tracker.setCurrentComputation(this);
    boolean previousInCompute = Tracker.inCompute;
    Tracker.inCompute = true;
    try {
      Tracker.withNoYieldsAllowed(this._func).apply(this);
    } finally {
      Tracker.setCurrentComputation(previous);
      Tracker.inCompute = previousInCompute;
    }
  }

  void _recompute() {
    this._recomputing = true;
    try {
      while (this.invalidated && !this.stopped) {
        try {
          this._compute();
        } catch (Throwable e) {
          try {
            Tracker._throwOrLog("recompute", e);
          } catch (Throwable e1) {
            throw Throwables.propagate(e1);
          }
        }
        // If _compute() invalidated us, we run again immediately.
        // A computation that invalidates itself indefinitely is an
        // infinite loop, of course.
        //
        // We could put an iteration counter here and catch run-away
        // loops.
      }
    } finally {
      this._recomputing = false;
    }
  }
}
