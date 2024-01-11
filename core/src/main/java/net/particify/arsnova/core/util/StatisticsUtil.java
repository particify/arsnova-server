/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.particify.arsnova.core.util;

import java.util.List;

public class StatisticsUtil {
  public static double calculateMean(final List<Double> numbers) {
    return numbers.stream()
      .mapToDouble(Double::doubleValue)
      .average()
      .orElse(0);
  }

  public static double calculateMedian(final List<Double> numbers) {
    final List<Double> sortedValues = numbers.stream()
        .sorted()
        .toList();
    final int size = sortedValues.size();
    if (size == 0) {
      return 0;
    }
    if (size % 2 == 0) {
      final int mid = size / 2;
      return (sortedValues.get(mid - 1) + sortedValues.get(mid)) / 2.0;
    } else {
      return sortedValues.get(size / 2);
    }
  }

  public static double calculateVariance(final List<Double> numbers) {
    final double mean = calculateMean(numbers);
    return numbers.stream()
      .mapToDouble(number -> Math.pow(number - mean, 2))
      .average()
      .orElse(0);
  }

  public static double calculateStandardDeviation(final List<Double> numbers) {
    return Math.sqrt(calculateVariance(numbers));
  }

  public static double findMinimum(final List<Double> numbers) {
    return numbers.stream()
      .mapToDouble(Double::doubleValue)
      .min()
      .orElse(0);
  }

  public static double findMaximum(final List<Double> numbers) {
    return numbers.stream()
      .mapToDouble(Double::doubleValue)
      .max()
      .orElse(0);
  }
}
