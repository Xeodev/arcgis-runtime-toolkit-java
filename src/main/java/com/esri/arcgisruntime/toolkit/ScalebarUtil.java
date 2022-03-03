/*
 * Copyright 2018 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgisruntime.toolkit;

import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility methods used by Scalebar.
 *
 * @since 100.2.1
 */
public class ScalebarUtil {

  private static final LinearUnit METERS = new LinearUnit(LinearUnitId.METERS);
  private static final LinearUnit FEET = new LinearUnit(LinearUnitId.FEET);
  private static final LinearUnit KILOMETERS = new LinearUnit(LinearUnitId.KILOMETERS);
  private static final LinearUnit MILES = new LinearUnit(LinearUnitId.MILES);

  // Array containing the multipliers that may be used for a scalebar and arrays of segment options appropriate for each
  // multiplier
  private static final MultiplierData[] MULTIPLIER_DATA_ARRAY = {
    new MultiplierData(1, new int[] {1, 2, 4, 5}),
    new MultiplierData(1.2, new int[] {1, 2, 3, 4}),
    new MultiplierData(1.25, new int[] {1, 2}),
    new MultiplierData(1.5, new int[] {1, 2, 3, 5}),
    new MultiplierData(1.75, new int[] {1, 2}),
    new MultiplierData(2.0, new int[] {1, 2, 4, 5}),
    new MultiplierData(2.4, new int[] {1, 2, 3}),
    new MultiplierData(2.5, new int[] {1, 2, 5}),
    new MultiplierData(3, new int[] {1, 2, 3}),
    new MultiplierData(3.75, new int[] {1, 3}),
    new MultiplierData(4, new int[] {1, 2, 4}),
    new MultiplierData(5, new int[] {1, 2, 5}),
    new MultiplierData(6, new int[] {1, 2, 3}),
    new MultiplierData(7.5, new int[] {1, 2}),
    new MultiplierData(8.0, new int[] {1, 2, 4}),
    new MultiplierData(9.0, new int[] {1, 2, 3}),
    new MultiplierData(10.0, new int[] {1, 2, 5}),
  };

  /**
   * Calculates the best length for the scalebar to fit within a given maximum length.
   *
   * @param maxLength the maximum length
   * @param unit indicates the unit of length being used: meters or feet
   * @param isSegmented true if the scalebar is segmented
   * @return the "best length", the highest "nice" number less than or equal to maxLength
   * @since 100.2.1
   */
  public static double calculateBestScalebarLength(double maxLength, LinearUnit unit, boolean isSegmented) {
    double magnitude = calculateMagnitude(maxLength);
    double multiplier = selectMultiplierData(maxLength, magnitude).getMultiplier();

    double bestLength = multiplier * magnitude;

    // If using imperial units, check if the number of feet is greater than the threshold for using feet
    if (unit.getLinearUnitId() == LinearUnitId.FEET) {
      LinearUnit displayUnits = selectLinearUnit(bestLength, UnitSystem.IMPERIAL);
      if (unit.getLinearUnitId() != displayUnits.getLinearUnitId()) {
        // Recalculate the best length in miles
        bestLength = calculateBestScalebarLength(unit.convertTo(displayUnits, maxLength), displayUnits, isSegmented);
        // But convert that back to feet because the caller is using feet
        return displayUnits.convertTo(unit, bestLength);
      }
    }
    return bestLength;
  }

  /**
   * Calculates the optimal number of segments in the scalebar when the distance represented by the whole scalebar has
   * a particular value. This is optimized so that the labels on the segments are all "nice" numbers.
   *
   * @param distance the distance represented by the whole scalebar, that is the value to be displayed at the end of the
   *                 scalebar
   * @param maxNumSegments the maximum number of segments to avoid the labels of the segments overwriting each other
   *                       (this is passed in by the caller to allow this method to be platform independent)
   * @return the optimal number of segments in the scalebar
   * @since 100.2.1
   */
  public static int calculateOptimalNumberOfSegments(double distance, int maxNumSegments) {
    // Create an ordered array of options for the specified distance
    int[] options = segmentOptionsForDistance(distance);

    // Select the largest option that's <= maxNumSegments
    int ret = 1;
    for (int i=0; i < options.length; i++) {
      if (options[i] > maxNumSegments) {
        break;
      }
      ret = options[i];
    }
    return ret;
  }

  /**
   * Selects the appropriate LinearUnit to use when the distance represented by the whole scalebar has a particular
   * value.
   *
   * @param distance the distance represented by the whole scalebar, that is the value to be displayed at the end of the
   *                 scalebar; in feet if unitSystem is IMPERIAL or meters if unitSystem is METRIC
   * @param unitSystem the UnitSystem being used
   * @return the LinearUnit
   * @throws NullPointerException if unitSystem is null
   * @since 100.2.1
   */
  public static LinearUnit selectLinearUnit(double distance, UnitSystem unitSystem) {
    Objects.requireNonNull(unitSystem, "unitSystem cannot be null");
    switch (unitSystem) {
      case IMPERIAL:
        // use MILES if at least half a mile
        if (distance >= 2640) {
          return MILES;
        }
        return FEET;

      case METRIC:
      default:
        // use KILOMETERS if at least one kilometer
        if (distance >= 1000) {
          return KILOMETERS;
        }
        return METERS;
    }
  }

  /**
   * Calculates the distance value to display in the correct units.
   *
   * @param distance the distance in the base unit
   * @param baseUnit the base unit
   * @param displayUnit the display unit
   * @return the distance to display
   * @throws NullPointerException if baseUnit is null
   * @throws NullPointerException if displayUnit is null
   * @since 100.2.1
   */
  public static double calculateDistanceInDisplayUnits(double distance, LinearUnit baseUnit, LinearUnit displayUnit) {
    Objects.requireNonNull(baseUnit, "baseUnit cannot be null");
    Objects.requireNonNull(displayUnit, "displayUnit cannot be null");
    double displayDistance = distance;
    if (displayUnit != baseUnit) {
      displayDistance = baseUnit.convertTo(displayUnit, displayDistance);
    }
    return displayDistance;
  }

  /**
   * Creates a string to display as a scalebar label corresponding to a given distance.
   *
   * @param distance the distance
   * @return the label string
   * @since 100.2.1
   */
  public static String labelString(double distance) {
    // Format with 2 decimal places
    String label = String.format("%.2f", distance);

    // Strip off both decimal places if they're 0s
    if (label.endsWith(".00")) {
      return label.substring(0, label.length() - 3);
    }

    // Otherwise, strip off last decimal place if it's 0
    if (label.endsWith("0")) {
      return label.substring(0, label.length() - 1);
    }
    return label;
  }

  /**
   * Calculates the "magnitude" used when calculating the length of a scalebar or the number of segments. This is the
   * largest power of 10 that's less than or equal to a given distance.
   *
   * @param distance the distance represented by the scalebar
   * @return the magnitude, a power of 10
   * @since 100.2.1
   */
  private static double calculateMagnitude(double distance) {
    return Math.pow(10, Math.floor(Math.log10(distance)));
  }

  /**
   * Selects the "multiplier" used when calculating the length of a scalebar or the number of segments in the
   * scalebar. This is chosen to give "nice" numbers for all the labels on the scalebar.
   *
   * @param distance the distance represented by the scalebar
   * @param magnitude the "magnitude" used when calculating the length of a scalebar or the number of segments
   * @return a MultiplierData object containing the multiplier, which will give the scalebar length when multiplied by
   * the magnitude
   * @since 100.2.1
   */
  private static MultiplierData selectMultiplierData(double distance, double magnitude) {
    double residual = distance / magnitude;

    // Select the largest multiplier that's <= residual
    List<MultiplierData> multipliers = Arrays.stream(MULTIPLIER_DATA_ARRAY).filter(
      m -> m.getMultiplier() <= residual).collect(Collectors.toList());
    if (multipliers.isEmpty()) {
      return MULTIPLIER_DATA_ARRAY[0];
    } else {
      return multipliers.get(multipliers.size() - 1);
    }
  }

  /**
   * Returns the segment options that are appropriate when a scalebar represents a given distance.
   *
   * @param distance the distance represented by the scalebar
   * @return the segment options; these are ints representing number of segments in the scalebar
   * @since 100.2.1
   */
  private static int[] segmentOptionsForDistance(double distance) {
    return selectMultiplierData(distance, calculateMagnitude(distance)).getSegmentOptions();
  }

  /**
   * Container for a "multiplier" and the array of segment options appropriate for that multiplier. The multiplier is
   * used when calculating the length of a scalebar or the number of segments in the scalebar.
   *
   * @since 100.2.1
   */
  private static class MultiplierData {
    private final double multiplier;

    private final int[] segmentOptions;

    /**
     * Constructs a MultiplierData.
     *
     * @param multiplier the multiplier
     * @param segmentOptions the array of segment options appropriate for the multiplier; these are ints representing
     *                       number of segments in the scalebar; it's important that they are in ascending order
     * @since 100.2.1
     */
    public MultiplierData(double multiplier, int[] segmentOptions) {
      this.multiplier = multiplier;
      this.segmentOptions = segmentOptions;
    }

    /**
     * Gets the multiplier.
     *
     * @return the multiplier
     * @since 100.2.1
     */
    public double getMultiplier() {
      return multiplier;
    }

    /**
     * Gets the segment options.
     *
     * @return the segment options; these are ints representing number of segments in the scalebar
     * @since 100.2.1
     */
    public int[] getSegmentOptions() {
      return segmentOptions;
    }
  }
}
