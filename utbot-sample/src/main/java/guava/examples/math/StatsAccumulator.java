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

import java.util.Iterator;

import static java.lang.Double.*;

public final class StatsAccumulator {

  // These fields must satisfy the requirements of Stats' constructor as well as those of the stat
  // methods of this class.
  private long count = 0;
  private double mean = 0.0; // any finite value will do, we only use it to multiply by zero for sum
  private double sumOfSquaresOfDeltas = 0.0;
  private double min = NaN; // any value will do
  private double max = NaN; // any value will do

  /** Adds the given value to the dataset. */
  public void add(double value) {
    if (count == 0) {
      count = 1;
      mean = value;
      min = value;
      max = value;
      if (!isFinite(value)) {
        sumOfSquaresOfDeltas = NaN;
      }
    } else {
      count++;
      if (isFinite(value) && isFinite(mean)) {
        // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15) and (16)
        double delta = value - mean;
        mean += delta / count;
        sumOfSquaresOfDeltas += delta * (value - mean);
      } else {
        mean = calculateNewMeanNonFinite(mean, value);
        sumOfSquaresOfDeltas = NaN;
      }
      min = Math.min(min, value);
      max = Math.max(max, value);
    }
  }

  /**
   * Adds the given values to the dataset.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public void addAll(Iterable<? extends Number> values) {
    for (Number value : values) {
      add(value.doubleValue());
    }
  }

  /**
   * Adds the given values to the dataset.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision)
   */
  public void addAll(Iterator<? extends Number> values) {
    while (values.hasNext()) {
      add(values.next().doubleValue());
    }
  }

  /**
   * Adds the given values to the dataset.
   *
   * @param values a series of values
   */
  public void addAll(double... values) {
    for (double value : values) {
      add(value);
    }
  }

  /**
   * Adds the given values to the dataset.
   *
   * @param values a series of values
   */
  public void addAll(int... values) {
    for (int value : values) {
      add(value);
    }
  }

  /**
   * Adds the given values to the dataset.
   *
   * @param values a series of values, which will be converted to {@code double} values (this may
   *     cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
   */
  public void addAll(long... values) {
    for (long value : values) {
      add(value);
    }
  }

  /** Returns an immutable snapshot of the current statistics. */
  public Stats snapshot() {
    return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);
  }

  /** Returns the number of values. */
  public long count() {
    return count;
  }

  /**
   * Calculates the new value for the accumulated mean when a value is added, in the case where at
   * least one of the previous mean and the value is non-finite.
   */
  static double calculateNewMeanNonFinite(double previousMean, double value) {
    /*
     * Desired behaviour is to match the results of applying the naive mean formula. In particular,
     * the update formula can subtract infinities in cases where the naive formula would add them.
     *
     * Consequently:
     * 1. If the previous mean is finite and the new value is non-finite then the new mean is that
     *    value (whether it is NaN or infinity).
     * 2. If the new value is finite and the previous mean is non-finite then the mean is unchanged
     *    (whether it is NaN or infinity).
     * 3. If both the previous mean and the new value are non-finite and...
     * 3a. ...either or both is NaN (so mean != value) then the new mean is NaN.
     * 3b. ...they are both the same infinities (so mean == value) then the mean is unchanged.
     * 3c. ...they are different infinities (so mean != value) then the new mean is NaN.
     */
    if (isFinite(previousMean)) {
      // This is case 1.
      return value;
    } else if (isFinite(value) || previousMean == value) {
      // This is case 2. or 3b.
      return previousMean;
    } else {
      // This is case 3a. or 3c.
      return NaN;
    }
  }

  public static boolean isFinite(double value) {
    return NEGATIVE_INFINITY < value && value < POSITIVE_INFINITY;
  }
}
