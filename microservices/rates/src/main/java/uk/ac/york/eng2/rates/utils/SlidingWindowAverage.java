package uk.ac.york.eng2.rates.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes sliding window averages by using a circular buffer.
 */
public class SlidingWindowAverage<Key extends Comparable<Key> > {
  private final List<Key> keys;
  private final double[] values;
  private int nValues = 0, iNext = 0;

  public SlidingWindowAverage(int windowSize) {
    this.values = new double[windowSize];
    this.keys = new ArrayList<Key>(windowSize);
  }

  public void add(Key key, double value) {
    this.values[iNext] = value;
    if (isFull()) {
      keys.set(iNext, key);
    } else {
      keys.add(key);
    }

    // Increase position in circular buffer
    iNext = (iNext + 1) % values.length;

    // Count one more value (if we haven't filled the window already)
    nValues = Math.min(values.length, nValues + 1);
  }

  public boolean isFull() {
    return nValues == values.length;
  }

  public double getAverage() {
    if (!isFull()) {
      throw new IllegalStateException(String.format(
          "Window is not full yet: currently have %d out of %d values",
          nValues, values.length));
    }

    double total = 0;
    for (double value : values) {
      total += value;
    }
    return total / values.length;
  }

  public Key getMinimumKey() {
    Key minimum = null;
    for (Key key : keys) {
      if (minimum == null || key.compareTo(minimum) < 0) {
        minimum = key;
      }
    }
    return minimum;
  }

  public Key getMaximumKey() {
    Key maximum = null;
    for (Key key : keys) {
      if (maximum == null || key.compareTo(maximum) > 0) {
        maximum = key;
      }
    }
    return maximum;
  }

}
