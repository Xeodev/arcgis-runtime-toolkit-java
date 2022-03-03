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

package com.esri.arcgisruntime.toolkit.skins;

import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedListener;
import com.esri.arcgisruntime.toolkit.Scalebar;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Base class for the skins that visualize the scalebar.
 *
 * @since 100.2.1
 */
public abstract class ScalebarSkin extends SkinBase<Scalebar> {

  static final double HEIGHT = 10.0;
  final static double SHADOW_OFFSET = 1.5;
  final static double STROKE_WIDTH = 3.0;

  final static Color LINE_COLOR = Color.WHITE;
  final static Color FILL_COLOR = Color.rgb(0xB7, 0xCB, 0xD3);
  final static Color ALTERNATE_FILL_COLOR = Color.BLACK;
  final static Color SHADOW_COLOR = Color.rgb(0x6E, 0x84, 0x8D);
  final static Color TEXT_COLOR = Color.BLACK;

  private boolean invalid = true;
  private final VBox vBox = new VBox();

  private UnitSystem unitSystem;
  private LinearUnit baseUnit;
  private HPos alignment = HPos.CENTER;

  private final ViewpointChangedListener viewpointChangedListener = v -> invalidated();

  private final ChangeListener<UnitSystem> unitsChangedListener = (observable, oldValue, newValue) -> {
    updateUnits(newValue);
    invalidated();
  };

  private final ChangeListener<HPos> alignmentChangedListener = (observable, oldValue, newValue) -> {
    alignment = newValue;
    invalidated();
  };

  /**
   * Constructs a skin.
   *
   * @param control the control this skin represents
   * @since 100.2.1
   */
  ScalebarSkin(Scalebar control) {
    super(control);

    // add listeners for things that cause the scalebar to change
    control.widthProperty().addListener(this::invalidated);
    control.heightProperty().addListener(this::invalidated);
    control.mapViewProperty().get().addViewpointChangedListener(viewpointChangedListener);
    control.mapViewProperty().get().widthProperty().addListener(this::invalidated);
    control.mapViewProperty().get().heightProperty().addListener(this::invalidated);
    control.unitSystemProperty().addListener(unitsChangedListener);
    control.alignmentProperty().addListener(alignmentChangedListener);

    updateUnits(control.getUnitSystem());
    alignment = control.getAlignment();

    // Subclasses will add their nodes into this VBox. A VBox is used since each scalebar type consists of vertically
    // arranged elements e.g. a line with a distance label below.
    vBox.setAlignment(Pos.CENTER);
    getChildren().add(vBox);
  }

  @Override
  public void dispose() {
    // remove listeners when this skin is being disposed
    getSkinnable().widthProperty().removeListener(this::invalidated);
    getSkinnable().heightProperty().removeListener(this::invalidated);
    getSkinnable().mapViewProperty().get().removeViewpointChangedListener(viewpointChangedListener);
    getSkinnable().mapViewProperty().get().widthProperty().removeListener(this::invalidated);
    getSkinnable().mapViewProperty().get().heightProperty().removeListener(this::invalidated);
    getSkinnable().unitSystemProperty().removeListener(unitsChangedListener);
    getSkinnable().alignmentProperty().removeListener(alignmentChangedListener);

    vBox.getChildren().clear();
  }

  /**
   * Called during layout when the control needs to be redrawn e.g. the size has changed or the units have been changed.
   *
   * @param width the width
   * @param height the height
   * @since 100.2.1
   */
  protected abstract void update(double width, double height);

  @Override
  protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
    if (invalid) {
      update(contentWidth, contentHeight);
      invalid = false;
    }
    getChildren().forEach(c -> layoutInArea(c, contentX, contentY, contentWidth, contentHeight, -1, HPos.CENTER, VPos.CENTER));
  }

  /**
   * Returns the width that can be used for the scalebar e.g. some scalebars have labels at the end so they can't be
   * as long as a scalebar with the label underneath.
   *
   * @param width the total width available
   * @return the width that the scalebar line/bar can occupy
   * @since 100.2.1
   */
  protected abstract double calculateAvailableWidth(double width);

    /**
   * Returns the VBox that is used to contain all the scalebar nodes.
     *
   * @return the VBox
     * @since 100.2.1
   */
  protected VBox getVBox() {
    return vBox;
  }

  /**
   * Returns the base unit of the scalebar which is normally either meters or feet.
   *
   * @return the base unit
   * @since 100.2.1
   */
  LinearUnit getBaseUnit() {
    return baseUnit;
  }

  /**
   * Returns the unit system of the scalebar which is normally metric or imperial.
   *
   * @return the unit system
   * @since 100.2.1
   */
  UnitSystem getUnitSystem() {
    return unitSystem;
  }

  /**
   * Returns the horizontal alignment of the scalebar.
   *
   * @return the horizontal alignment
   * @since 100.2.1
   */
  protected HPos getAlignment() {
    return alignment;
  }

  /**
   * Calculates the actual size of a region. Used when we need to know the size before layout.
   *
   * @param region the region
   * @return a region with the true size
   * @since 100.2.1
   */
  Region calculateRegion(Region region) {
    Group root = new Group();
    Scene placeholderScene = new Scene(root);
    root.getChildren().add(region);
    root.applyCss();
    root.layout();

    return region;
  }

  /**
   * Calculates a distance on the map view based on the maximum possible scalebar width.
   *
   * @param width the width
   * @return the distance
   * @since 100.2.1
   */
  double calculateDistance(MapView mapView, LinearUnit unit, double width) {
    double distance = 0.0;

    Polygon visibleArea = mapView.getVisibleArea();
    if (visibleArea != null) {
      Point mapCenter = visibleArea.getExtent().getCenter();
      if (!mapCenter.isEmpty()) {
        double maxPlanarWidth = mapView.getUnitsPerDensityIndependentPixel() * width;
        Point point1 = new Point(mapCenter.getX() - (maxPlanarWidth / 2.0), mapCenter.getY());
        Point point2 = new Point(mapCenter.getX() + (maxPlanarWidth / 2.0), mapCenter.getY());

        PolylineBuilder polylineBuilder = new PolylineBuilder(mapView.getSpatialReference());
        polylineBuilder.addPoint(point1);
        polylineBuilder.addPoint(mapCenter);
        polylineBuilder.addPoint(point2);

        distance = GeometryEngine.lengthGeodetic(polylineBuilder.toGeometry(), unit, GeodeticCurveType.GEODESIC);
      }
    }

    return distance;
  }

  /**
   * Returns the width to draw the scalebar.
   *
   * @param displayDistance the distance that the scalebar will actually be
   * @param maximumDistance the distance the width of the control represents
   * @param availableWidth the width actually available for the scalebar
   * @return the final width
   * @since 100.2.1
   */
  double calculateDisplayWidth(double displayDistance, double  maximumDistance, double availableWidth) {
    return displayDistance / maximumDistance * availableWidth;
  }

  /**
   * Calculates the X translation required to move a scalebar to the correct alignment - left or right. No translation
   * is required for center alignment.
   *
   * @param width the width of the area containing the scalebar
   * @param actualWidth the actual width of the scalebar
   * @return the X translation required
   * @since 100.2.1
   */
  double calculateAlignmentTranslationX(double width, double actualWidth) {
    double translate = 0.0;
    switch (getAlignment()) {
      case LEFT:
        translate = -((width - actualWidth - STROKE_WIDTH) / 2.0);
        break;
      case RIGHT:
        translate = (width - actualWidth - STROKE_WIDTH - SHADOW_OFFSET) / 2.0;
        break;
    }
    return translate;
  }

  /**
   * Requests layout when the control's layout has been invalidated.
   *
   * @param observable the observable
   * @since 100.2.1
   */
  private void invalidated(Observable observable) {
    invalidated();
  }

  /**
   * Requests layout when the control's layout has been invalidated.
   *
   * @since 100.2.1
   */
  private void invalidated() {
    invalid = true;
    getSkinnable().requestLayout();
  }

  /**
   * Updates the unit system when the control has its unit system changed.
   *
   * @param unitSystem the new unit system
   * @since 100.2.1
   */
  private void updateUnits(UnitSystem unitSystem) {
    this.unitSystem = unitSystem;
    switch (unitSystem) {
      case METRIC:
        baseUnit = new LinearUnit(LinearUnitId.METERS);
        break;
      case IMPERIAL:
        baseUnit = new LinearUnit(LinearUnitId.FEET);
        break;
    }
  }

  @Override
  protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return getSkinnable().getMapView().getWidth() / 4.0;
  }

  @Override
  protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
  }
}
