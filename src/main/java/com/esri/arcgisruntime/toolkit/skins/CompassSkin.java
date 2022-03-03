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

import com.esri.arcgisruntime.toolkit.Compass;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.VLineTo;
import javafx.util.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements a skin for the {@link Compass} control.
 *
 * @since 100.2.1
 */
public final class CompassSkin extends SkinBase<Compass> {

  private static final double PREF_SIZE = 100.0;

  // how close to north the compass has to be to auto-hide
  private static final double HEADING_TOLERANCE = 0.25;

  private boolean invalid = true;
  private final StackPane stackPane = new StackPane();

  // duration used for fading the compass
  private static final long TIMER_DURATION = 500;

  // property that will be true when the compass is hidden
  private final SimpleBooleanProperty hiddenProperty = new SimpleBooleanProperty(true);

  // a scheduled service used to perform a delayed fade in/out if auto-hide is enabled
  private final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(0);

  /**
   * Creates an instance of the skin.
   *
   * @param control the {@link Compass} control this skin represents
   * @since 100.2.1
   */
  public CompassSkin(com.esri.arcgisruntime.toolkit.Compass control) {
    super(control);

    control.widthProperty().addListener(observable -> invalid = true);
    control.heightProperty().addListener(observable -> invalid = true);
    control.insetsProperty().addListener(observable -> invalid = true);

    // bind to the control's heading property
    stackPane.rotateProperty().bind(control.headingProperty().negate());

    // hide the compass when the heading is close to north if the auto hide property is enabled
    SimpleDoubleProperty controlHeadingProperty = control.headingProperty();
    hiddenProperty.bind(control.autoHideProperty()
      .and(controlHeadingProperty.isEqualTo(0.0, HEADING_TOLERANCE)
      .or(controlHeadingProperty.isEqualTo(360.0, HEADING_TOLERANCE))));
    hiddenProperty.addListener(observable -> {
      // when the hidden property changes schedule to perform a fade in/out - having a delay prevents the compass from
      // starting to fade if it momentarily passes through north
      scheduledService.schedule(() -> Platform.runLater(() -> {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(TIMER_DURATION), stackPane);
        if (hiddenProperty.get()) {
          fadeTransition.setToValue(0.0);
        } else {
          fadeTransition.setToValue(1.0);
        }
        fadeTransition.play();
      }), TIMER_DURATION, TimeUnit.MILLISECONDS);
    });

    // initial opacity based on the auto-hide property
    if (control.isAutoHide()) {
      stackPane.setOpacity(0.0);
    } else {
      stackPane.setOpacity(1.0);
    }

    getChildren().add(stackPane);
  }

  @Override
  protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
    if (invalid) {
      update(contentWidth, contentHeight);
      invalid = false;
    }
    layoutInArea(stackPane, contentX, contentY, contentWidth, contentHeight, -1, HPos.CENTER, VPos.CENTER);
  }

  /**
   * Updates the visual representation of the compass e.g. when the size is changed.
   *
   * @param width the width of the control
   * @param height the height of the control
   * @since 100.2.1
   */
  private void update(double width, double height) {
    stackPane.getChildren().clear();

    double radius = Math.min(height, width) / 2.0;
    double triangleHeight = radius * (3.0 / 5.0);
    double triangleWidth = triangleHeight / 3.0;

    PathElement northEastTrianglePath[] =
      {new MoveTo(0.0, 0.0), new VLineTo(triangleHeight), new HLineTo(triangleWidth), new ClosePath()};
    PathElement northWestTrianglePath[] =
      {new MoveTo(0.0, 0.0), new VLineTo(triangleHeight), new HLineTo(-triangleWidth), new ClosePath()};

    Path northEastTriangle = new Path(northEastTrianglePath);
    northEastTriangle.setStroke(null);
    northEastTriangle.setFill(Color.rgb(0x88, 0x00, 0x00));
    northEastTriangle.setTranslateY(-triangleHeight / 2.0);
    northEastTriangle.setTranslateX(triangleWidth / 2.0);

    Path southWestTriangle = new Path(northEastTrianglePath);
    southWestTriangle.setStroke(null);
    southWestTriangle.setFill(Color.rgb(0xA9, 0xA9, 0xA9));
    southWestTriangle.setTranslateY(triangleHeight / 2.0);
    southWestTriangle.setTranslateX(-triangleWidth / 2.0);
    southWestTriangle.setRotate(180.0);

    Path northWestTriangle = new Path(northWestTrianglePath);
    northWestTriangle.setStroke(null);
    northWestTriangle.setFill(Color.rgb(0xFF, 0x00, 0x00));
    northWestTriangle.setTranslateY(-triangleHeight / 2.0);
    northWestTriangle.setTranslateX(-triangleWidth / 2.0);

    Path southEastTriangle = new Path(northWestTrianglePath);
    southEastTriangle.setStroke(null);
    southEastTriangle.setFill(Color.rgb(0x80, 0x80, 0x80));
    southEastTriangle.setTranslateY(triangleHeight / 2.0);
    southEastTriangle.setTranslateX(triangleWidth / 2.0);
    southEastTriangle.setRotate(180.0);

    Circle pivot = new Circle();
    pivot.setRadius(triangleWidth / 3.0);
    pivot.setFill(Color.rgb(0xFF, 0xA5, 0x00));

    Circle circle = new Circle();
    circle.setRadius(radius);
    circle.setFill(Color.rgb(0xE1, 0xF1, 0xF5, 0.25));
    circle.setStroke(Color.rgb(0x80, 0x80, 0x80));
    circle.setStrokeType(StrokeType.INSIDE);
    circle.setStrokeWidth(0.1 * radius);

    stackPane.getChildren().addAll(circle, northEastTriangle, northWestTriangle,
      southEastTriangle, southWestTriangle, pivot);

    // fire action event if any of the compass elements are clicked
    stackPane.getChildren().forEach(c -> c.setOnMouseClicked(e -> getSkinnable().fireEvent(new ActionEvent())));
  }

  @Override
  protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
  }

  @Override
  protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return PREF_SIZE;
  }

  @Override
  protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double
    leftInset) {
    return PREF_SIZE;
  }
}
