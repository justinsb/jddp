package com.meteor.tracker;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Maps;

//
//http://docs.meteor.com/#tracker_dependency

/**
 * @summary A Dependency represents an atomic unit of reactive data that a computation might depend on. Reactive data
 *          sources such as Session or Minimongo internally create different Dependency objects for different pieces of
 *          data, each of which may be depended on by multiple computations. When the data changes, the computations are
 *          invalidated.
 * @class
 * @instanceName dependency
 */
class Dependency {
  Map<Long, Computation> _dependentsById;

  public Dependency() {
    this._dependentsById = Maps.newHashMap();
  }

  // http://docs.meteor.com/#dependency_depend
  //
  // Adds `computation` to this set if it is not already
  // present. Returns true if `computation` is a new member of the set.
  // If no argument, defaults to currentComputation, or does nothing
  // if there is no currentComputation.

  /**
   * @summary Declares that the current computation (or `fromComputation` if given) depends on `dependency`. The
   *          computation will be invalidated the next time `dependency` changes. If there is no current computation and
   *          `depend()` is called with no arguments, it does nothing and returns false. Returns true if the computation
   *          is a new dependent of `dependency` rather than an existing one.
   * @locus Client
   * @param {Tracker.Computation} [fromComputation] An optional computation declared to depend on `dependency` instead
   *        of the current computation.
   * @return
   * @returns {Boolean}
   */
  boolean depend(Computation computation) {
    if (computation == null) {
      if (!Tracker.active)
        return false;

      computation = Tracker.currentComputation;
    }
    long id = computation._id;
    if (!_dependentsById.containsKey(id)) {
      this._dependentsById.put(id, computation);
      computation.onInvalidate((Computation c) -> { _dependentsById.remove(id); });
      return true;
    }
    return false;
  };

  // http://docs.meteor.com/#dependency_changed

  /**
   * @summary Invalidate all dependent computations immediately and remove them as dependents.
   * @locus Client
   */
  public void changed() {
    for (Computation dep : _dependentsById.values()) {
      dep.invalidate();
    }
  }

  // http://docs.meteor.com/#dependency_hasdependents

  /**
   * @summary True if this Dependency has one or more dependent Computations, which would be invalidated if this
   *          Dependency were to change.
   * @locus Client
   * @returns {Boolean}
   */
  public boolean hasDependents() {
    return !_dependentsById.isEmpty();
  }
}
