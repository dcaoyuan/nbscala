/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a simple statistics collector.
 *
 * @author Robert Grimm
 * @version $Revision: 1.13 $
 */
public class Statistics {

  /** The list of numbers. */
  private List<Double> numbers;

  /** Create a new statistics collector. */
  public Statistics() {
    numbers = new ArrayList<Double>();
  }

  /** Reset this statistics collector. */
  public void reset() {
    numbers.clear();
  }

  /**
   * Add the specified number.
   *
   * @param d The number.
   */
  public void add(final double d) {
    numbers.add(d);
  }

  /**
   * Get the size of this collection.
   *
   * @return The size.
   */
  public int size() {
    return numbers.size();
  }

  /**
   * Get the specified number.
   *
   * @param idx The index.
   * @return The corresponding number.
   * @throws IndexOutOfBoundsException
   *   Signals that the index is out of range.
   */
  public double get(final int idx) {
    return numbers.get(idx);
  }

  /**
   * Calculate the sum.
   *
   * @return The sum.
   */
  public double sum() {
    double sum = 0;

    final int size = numbers.size();
    for (int i=0; i<size; i++) {
      sum += numbers.get(i);
    }

    return sum;
  }

  /**
   * Calculate the mean.
   *
   * @return The mean.
   */
  public double mean() {
    double mean = 0;

    final int size = numbers.size();
    for (int i=0; i<size; i++) {
      mean += (numbers.get(i) - mean) / (i + 1);
    }

    return mean;
  }

  /**
   * Calculate the median.  Note that this method does not change the
   * order of numbers in this collection, i.e. it sorts a copy.
   *
   * @return The median.
   */
  public double median() {
    if (0 == size()) {
      return 0;

    } else {
      List<Double> sorted = new ArrayList<Double>(numbers);
      Collections.sort(sorted);
      return sorted.get(size()/2);
    }
  }

  /**
   * Calculate the standard deviation.
   *
   * @return The standard deviation.
   */
  public double stdev() {
    final double mean = mean();
    double variance   = 0;

    final int size = size();
    for (int i=0; i<size; i++) {
      final double diff  = numbers.get(i) - mean;
      variance          += (diff * diff - variance) / (i + 1);
    }

    return Math.sqrt(variance);
  }

  /**
   * Round the specified number to two digits after the decimal point.
   *
   * @param d The number.
   * @return The rounded number.
   */
  public static double round(final double d) {
    return (Math.floor((d * 100) + 0.5)/100);
  }

  /**
   * Compute the least squares fit.  This method computes the least
   * squares fit to a straight line model without a constant term for
   * the specified collections of numbers: <code>y = mx</code>.
   *
   * @param x The collection of x coordinates.
   * @param y The collection of y coordiantes.
   * @return The slope of the corresponding line model.
   * @throws IllegalArgumentException Signals that the two collections
   *   are empty or not of the same size.
   */
  public static double fitSlope(Statistics x, Statistics y) {
    if (x.size() != y.size()) {
      throw new IllegalArgumentException("Inconsistent collection sizes");
    } else if (0 == x.size()) {
      throw new IllegalArgumentException("Empty collections");
    }

    double xMean    = 0;
    double yMean    = 0;
    double dx2Mean  = 0;
    double dxdyMean = 0;
    final int size  = x.size();

    for (int i=0; i<size; i++) {
      xMean += (x.get(i) - xMean) / (i + 1);
      yMean += (y.get(i) - yMean) / (i + 1);
    }

    for (int i=0; i<size; i++) {
      final double dx = x.get(i) - xMean;
      final double dy = y.get(i) - yMean;

      dx2Mean  += (dx * dx - dx2Mean)  / (i + 1);
      dxdyMean += (dx * dy - dxdyMean) / (i + 1);
    }

    return (xMean * yMean + dxdyMean) / (xMean * xMean + dx2Mean);
  }

}
