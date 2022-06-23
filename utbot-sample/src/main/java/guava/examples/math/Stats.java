/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package guava.examples.math;

import java.io.Serializable;
import java.util.Iterator;


public final class Stats implements Serializable {

  private final long count;
  private final double mean;
  private final double sumOfSquaresOfDeltas;
  private final double min;
  private final double max;

  /**
   * Internal constructor. Users should use {@link #ofIterable} or {@link StatsAccumulator#snapshot}.
   *
   * <p>To ensure that the created instance obeys its contract, the parameters should satisfy the
   * following constraints. This is the callers responsibility and is not enforced here.
   *
   * <ul>
   *   <li>If {@code count} is 0, {@code mean} may have any finite value (its only usage will be to
   *       get multiplied by 0 to calculate the sum), and the other parameters may have any values
   *       (they will not be used).
   *   <li>If {@code count} is 1, {@code sumOfSquaresOfDeltas} must be exactly 0.0 or {@link
   *       Double#NaN}.
   * </ul>
   */
  Stats(long count, double mean, double sumOfSquaresOfDeltas, double min, double max) {
    this.count = count;
    this.mean = mean;
    this.sumOfSquaresOfDeltas = sumOfSquaresOfDeltas;
    this.min = min;
    this.max = max;
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public static Stats ofIterable(Iterable<? extends Number> values) {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(values);
    return accumulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public static Stats ofIterator(Iterator<? extends Number> values) {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.addAll(values);
    return accumulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values
   */
  public static Stats ofDoubles(double... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values
   */
  public static Stats ofInts(int... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /**
   * Returns statistics over a dataset containing the given values.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
   */
  public static Stats ofLongs(long... values) {
    StatsAccumulator acummulator = new StatsAccumulator();
    acummulator.addAll(values);
    return acummulator.snapshot();
  }

  /** Returns the number of values. */
  public long count() {
    return count;
  }


  private static final long serialVersionUID = 0;
}
