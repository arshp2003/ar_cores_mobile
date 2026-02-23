package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Measurement overlay view that renders anchors and measurement lines while handling user
 * interactions such as pinch scaling, measurement creation, and element removal.
 */
public class MeasurementOverlayView extends View {

  private static final float DEFAULT_ANCHOR_RADIUS = 50f;
  private static final String ANCHOR_EMOJI = "\uD83D\uDCCD";
  private static final float ANCHOR_DELETE_RADIUS = 18f;
  private static final float ANCHOR_DELETE_TOUCH_EXTRA = 12f;
  private static final float MIN_SCALE = 0.5f;
  private static final float MAX_SCALE = 3.0f;
  private static final float TOUCH_THRESHOLD = 64f;
  private static final float PREVIEW_LINE_WIDTH = 6f;
  private static final float LABEL_VERTICAL_OFFSET = 40f;
  private static final float LABEL_PADDING = 18f;
  private static final float LABEL_CORNER_RADIUS = 18f;
  private static final float LABEL_DELETE_RADIUS = 18f;
  private static final float LABEL_DELETE_MARGIN = 16f;
  private static final float LABEL_DELETE_TOUCH_EXTRA = 12f;
  private static final Locale LABEL_LOCALE = Locale.US;

  private final Paint anchorPaint = new Paint();
  private final Paint deletePaint = new Paint();
  private final Paint deleteLinePaint = new Paint();
  private final Paint linePaint = new Paint();
  private final Paint previewLinePaint = new Paint();
  private final Paint textPaint = new Paint();
  private final Paint labelBackgroundPaint = new Paint();

  private final List<PointF> anchorPoints = new ArrayList<>();
  private final List<Integer> anchorIds = new ArrayList<>();
  private final Map<Integer, Float> anchorScales = new HashMap<>();

  private final List<MeasurementLine> measurementLines = new ArrayList<>();
  private final Map<Integer, CircleArea> measurementDeleteAreas = new HashMap<>();

  private Integer dragStartIndex = null;
  private Integer dragStartAnchorId = null;
  private Integer dragTargetAnchorId = null;
  private PointF dragCurrentPoint = null;
  private float previewDistanceMeters = Float.NaN;

  private ScaleGestureDetector scaleDetector;
  private Integer scalingOverlayIndex = null;

  private OnAnchorDeleteListener anchorDeleteListener;
  private OnPassthroughTouchListener passthroughTouchListener;
  private OnMeasurementInteractionListener measurementListener;

  public MeasurementOverlayView(Context context) {
    this(context, null);
  }

  public MeasurementOverlayView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public MeasurementOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setWillNotDraw(false);

    anchorPaint.setAntiAlias(true);
    anchorPaint.setTextAlign(Paint.Align.CENTER);
    anchorPaint.setTextSize(DEFAULT_ANCHOR_RADIUS * 2f);

    deletePaint.setAntiAlias(true);
    deletePaint.setStyle(Paint.Style.FILL);
    deletePaint.setColor(0xFFFF5252);

    deleteLinePaint.setAntiAlias(true);
    deleteLinePaint.setStyle(Paint.Style.STROKE);
    deleteLinePaint.setStrokeWidth(3f);
    deleteLinePaint.setColor(0xFFFFFFFF);

    linePaint.setAntiAlias(true);
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setStrokeWidth(6f);
    linePaint.setColor(0xFFFFC107);

    previewLinePaint.setAntiAlias(true);
    previewLinePaint.setStyle(Paint.Style.STROKE);
    previewLinePaint.setStrokeWidth(PREVIEW_LINE_WIDTH);
    previewLinePaint.setColor(0xFFFFFFFF);
    previewLinePaint.setAlpha(220);

    textPaint.setAntiAlias(true);
    textPaint.setColor(0xFFFFFFFF);
    textPaint.setTextSize(42f);
    textPaint.setShadowLayer(6f, 0f, 0f, 0xCC000000);

    labelBackgroundPaint.setAntiAlias(true);
    labelBackgroundPaint.setStyle(Paint.Style.FILL);
    labelBackgroundPaint.setColor(0xAA000000);

    scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
  }

  public interface OnAnchorDeleteListener {
    void onAnchorDelete(int anchorId);
  }
  public interface OnPassthroughTouchListener {
    void onUnhandledTouch(MotionEvent event);
  }

  public interface OnMeasurementInteractionListener {
    float requestDistanceMeters(int startAnchorId, int endAnchorId);

    void onMeasurementCreated(int startAnchorId, int endAnchorId);

    void onMeasurementDeleted(int measurementId);
  }

  public static class MeasurementLine {
    public final int id;
    public final int startAnchorId;
    public final int endAnchorId;
    public final float distanceMeters;

    public MeasurementLine(int id, int startAnchorId, int endAnchorId, float distanceMeters) {
      this.id = id;
      this.startAnchorId = startAnchorId;
      this.endAnchorId = endAnchorId;
      this.distanceMeters = distanceMeters;
    }
  }

  private static class CircleArea {
    final float centerX;
    final float centerY;
    final float radius;

    CircleArea(float centerX, float centerY, float radius) {
      this.centerX = centerX;
      this.centerY = centerY;
      this.radius = radius;
    }
  }

  public void setOnAnchorDeleteListener(OnAnchorDeleteListener listener) {
    this.anchorDeleteListener = listener;
  }

  public void setOnPassthroughTouchListener(OnPassthroughTouchListener listener) {
    this.passthroughTouchListener = listener;
  }

  public void setOnMeasurementInteractionListener(OnMeasurementInteractionListener listener) {
    this.measurementListener = listener;
  }

  public void clear() {
    anchorPoints.clear();
    anchorIds.clear();
    anchorScales.clear();
    measurementLines.clear();
    measurementDeleteAreas.clear();
    dragStartIndex = null;
    dragStartAnchorId = null;
    dragTargetAnchorId = null;
    dragCurrentPoint = null;
    previewDistanceMeters = Float.NaN;
    invalidate();
  }

  public void setAnchors(List<PointF> points, List<Integer> ids) {
    anchorPoints.clear();
    anchorIds.clear();
    if (points != null) {
      anchorPoints.addAll(points);
    }
    if (ids != null) {
      anchorIds.addAll(ids);
    }

    Set<Integer> activeIds = new HashSet<>(anchorIds);
    anchorScales.keySet().retainAll(activeIds);
    for (Integer id : anchorIds) {
      anchorScales.putIfAbsent(id, 1.0f);
    }

    invalidate();
  }

  public void setMeasurementLines(List<MeasurementLine> lines) {
    measurementLines.clear();
    if (lines != null) {
      measurementLines.addAll(lines);
    }
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean handled = false;

    scaleDetector.onTouchEvent(event);
    if (scaleDetector.isInProgress()) {
      return true;
    }

    float x = event.getX();
    float y = event.getY();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        Integer measurementId = findMeasurementDeleteAtPoint(x, y);
        if (measurementId != null) {
          if (measurementListener != null) {
            measurementListener.onMeasurementDeleted(measurementId);
          }
          handled = true;
          break;
        }

        Integer deleteIndex = findAnchorDeleteAtPoint(x, y);
        if (deleteIndex != null) {
          if (anchorDeleteListener != null) {
            Integer anchorId = overlayIndexToAnchorId(deleteIndex);
            if (anchorId != null) {
              anchorDeleteListener.onAnchorDelete(anchorId);
            }
          }
          handled = true;
          break;
        }

        Integer anchorIndex = findAnchorAtPoint(x, y);
        if (anchorIndex != null) {
          dragStartIndex = anchorIndex;
          dragStartAnchorId = overlayIndexToAnchorId(anchorIndex);
          dragCurrentPoint = new PointF(x, y);
          dragTargetAnchorId = null;
          previewDistanceMeters = Float.NaN;
          handled = true;
          invalidate();
        }
        break;

      case MotionEvent.ACTION_MOVE:
        if (dragStartIndex != null) {
          Integer targetIndex = findAnchorAtPoint(x, y);
          if (targetIndex != null && !targetIndex.equals(dragStartIndex)) {
            dragTargetAnchorId = overlayIndexToAnchorId(targetIndex);
            PointF targetPoint = anchorPoints.get(targetIndex);
            dragCurrentPoint = new PointF(targetPoint.x, targetPoint.y);
            if (measurementListener != null
                && dragStartAnchorId != null
                && dragTargetAnchorId != null) {
              previewDistanceMeters =
                  measurementListener.requestDistanceMeters(dragStartAnchorId, dragTargetAnchorId);
            }
          } else {
            dragTargetAnchorId = null;
            dragCurrentPoint = new PointF(x, y);
            previewDistanceMeters = Float.NaN;
          }
          handled = true;
          invalidate();
        }
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (dragStartAnchorId != null
            && dragTargetAnchorId != null
            && measurementListener != null) {
          measurementListener.onMeasurementCreated(dragStartAnchorId, dragTargetAnchorId);
        }
        handled = dragStartIndex != null || dragCurrentPoint != null;
        dragStartIndex = null;
        dragStartAnchorId = null;
        dragTargetAnchorId = null;
        dragCurrentPoint = null;
        previewDistanceMeters = Float.NaN;
        invalidate();
        break;
    }

    if (!handled && passthroughTouchListener != null) {
      MotionEvent copy = MotionEvent.obtain(event);
      passthroughTouchListener.onUnhandledTouch(copy);
      copy.recycle();
    }

    return handled || super.onTouchEvent(event);
  }

  private Integer findMeasurementDeleteAtPoint(float x, float y) {
    for (Map.Entry<Integer, CircleArea> entry : measurementDeleteAreas.entrySet()) {
      CircleArea area = entry.getValue();
      float dx = x - area.centerX;
      float dy = y - area.centerY;
      if (Math.sqrt(dx * dx + dy * dy) <= area.radius + LABEL_DELETE_TOUCH_EXTRA) {
        return entry.getKey();
      }
    }
    return null;
  }

  private Integer findAnchorAtPoint(float x, float y) {
    for (int i = 0; i < anchorPoints.size(); i++) {
      PointF point = anchorPoints.get(i);
      Integer anchorId = overlayIndexToAnchorId(i);
      if (anchorId == null) {
        continue;
      }
      float scale = anchorScales.getOrDefault(anchorId, 1.0f);
      float radius = DEFAULT_ANCHOR_RADIUS * scale;
      float dx = x - point.x;
      float dy = y - point.y;
      if (Math.sqrt(dx * dx + dy * dy) <= radius + TOUCH_THRESHOLD) {
        return i;
      }
    }
    return null;
  }

  private Integer findAnchorDeleteAtPoint(float x, float y) {
    for (int i = 0; i < anchorPoints.size(); i++) {
      PointF point = anchorPoints.get(i);
      Integer anchorId = overlayIndexToAnchorId(i);
      if (anchorId == null) {
        continue;
      }
      float scale = anchorScales.getOrDefault(anchorId, 1.0f);
      float anchorRadius = DEFAULT_ANCHOR_RADIUS * scale;
      float deleteX = point.x + anchorRadius * 0.7f;
      float deleteY = point.y - anchorRadius * 0.7f;
      float dx = x - deleteX;
      float dy = y - deleteY;
      if (Math.sqrt(dx * dx + dy * dy) <= ANCHOR_DELETE_RADIUS + ANCHOR_DELETE_TOUCH_EXTRA) {
        return i;
      }
    }
    return null;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    measurementDeleteAreas.clear();

    drawMeasurementLines(canvas);
    drawPreviewLine(canvas);
    drawAnchors(canvas);
  }

  private void drawMeasurementLines(Canvas canvas) {
    for (MeasurementLine line : measurementLines) {
      PointF start = getAnchorPoint(line.startAnchorId);
      PointF end = getAnchorPoint(line.endAnchorId);
      if (start == null || end == null) {
        continue;
      }
      canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);

      float midX = (start.x + end.x) / 2f;
      float midY = (start.y + end.y) / 2f;
      drawMeasurementLabel(canvas, midX, midY, line.distanceMeters, line.id, true);
    }
  }

  private void drawPreviewLine(Canvas canvas) {
    if (dragStartIndex == null || dragCurrentPoint == null) {
      return;
    }

    PointF start = anchorPoints.get(dragStartIndex);
    canvas.drawLine(start.x, start.y, dragCurrentPoint.x, dragCurrentPoint.y, previewLinePaint);

    if (!Float.isNaN(previewDistanceMeters)
        && dragStartAnchorId != null
        && dragTargetAnchorId != null) {
      float midX = (start.x + dragCurrentPoint.x) / 2f;
      float midY = (start.y + dragCurrentPoint.y) / 2f;
      drawMeasurementLabel(canvas, midX, midY, previewDistanceMeters, /*measurementId=*/ -1, false);
    }
  }

  private void drawAnchors(Canvas canvas) {
    for (int i = 0; i < anchorPoints.size(); i++) {
      PointF point = anchorPoints.get(i);
      Integer anchorId = overlayIndexToAnchorId(i);
      if (anchorId == null) {
        continue;
      }
      float scale = anchorScales.getOrDefault(anchorId, 1.0f);
      float radius = DEFAULT_ANCHOR_RADIUS * scale;

      Paint.FontMetrics metrics = anchorPaint.getFontMetrics();
      float textBaseline = point.y - (metrics.ascent + metrics.descent) / 2f;
      float textSize = DEFAULT_ANCHOR_RADIUS * 2f * scale;
      anchorPaint.setTextSize(textSize);
      canvas.drawText(ANCHOR_EMOJI, point.x, textBaseline, anchorPaint);

      String idLabel = String.format(Locale.US, "%d", anchorId);
      float idTextSize = textSize * 0.45f;
      anchorPaint.setTextSize(idTextSize);
      float idBaseline = textBaseline + radius * 0.55f;

      anchorPaint.setStyle(Paint.Style.STROKE);
      anchorPaint.setStrokeWidth(Math.max(2f, idTextSize * 0.1f));
      canvas.drawText(idLabel, point.x, idBaseline, anchorPaint);
      anchorPaint.setStyle(Paint.Style.FILL);
      anchorPaint.setStrokeWidth(0f);
      canvas.drawText(idLabel, point.x, idBaseline, anchorPaint);

      float deleteX = point.x + radius * 0.7f;
      float deleteY = point.y - radius * 0.7f;
      canvas.drawCircle(deleteX, deleteY, ANCHOR_DELETE_RADIUS, deletePaint);

      float cross = ANCHOR_DELETE_RADIUS * 0.5f;
      canvas.drawLine(deleteX - cross, deleteY - cross, deleteX + cross, deleteY + cross, deleteLinePaint);
      canvas.drawLine(deleteX - cross, deleteY + cross, deleteX + cross, deleteY - cross, deleteLinePaint);
    }
  }

  private void drawMeasurementLabel(
      Canvas canvas, float midX, float midY, float distanceMeters, int measurementId, boolean withDelete) {
    String label = String.format(LABEL_LOCALE, "%.2f m", distanceMeters);
    float textWidth = textPaint.measureText(label);
    Paint.FontMetrics metrics = textPaint.getFontMetrics();
    float textHeight = metrics.bottom - metrics.top;

    float rectHeight = textHeight + LABEL_PADDING * 2f;
    float rectWidth = textWidth + LABEL_PADDING * 2f;
    float deleteSpace = 0f;
    if (withDelete) {
      deleteSpace = LABEL_DELETE_MARGIN + LABEL_DELETE_RADIUS * 2f;
      rectWidth += deleteSpace;
    }

    float centerY = midY - LABEL_VERTICAL_OFFSET;
    RectF rect =
        new RectF(
            midX - rectWidth / 2f,
            centerY - rectHeight / 2f,
            midX + rectWidth / 2f,
            centerY + rectHeight / 2f);

    canvas.drawRoundRect(rect, LABEL_CORNER_RADIUS, LABEL_CORNER_RADIUS, labelBackgroundPaint);

    float textBaseY = centerY - (metrics.ascent + metrics.descent) / 2f;
    float textStartX = rect.left + LABEL_PADDING;
    canvas.drawText(label, textStartX, textBaseY, textPaint);

    if (withDelete && measurementId >= 0) {
      float deleteCenterX = rect.right - LABEL_PADDING - LABEL_DELETE_RADIUS;
      float deleteCenterY = centerY;
      canvas.drawCircle(deleteCenterX, deleteCenterY, LABEL_DELETE_RADIUS, deletePaint);

      float cross = LABEL_DELETE_RADIUS * 0.5f;
      canvas.drawLine(
          deleteCenterX - cross, deleteCenterY - cross, deleteCenterX + cross, deleteCenterY + cross,
          deleteLinePaint);
      canvas.drawLine(
          deleteCenterX - cross, deleteCenterY + cross, deleteCenterX + cross, deleteCenterY - cross,
          deleteLinePaint);

      measurementDeleteAreas.put(measurementId, new CircleArea(deleteCenterX, deleteCenterY, LABEL_DELETE_RADIUS));
    }
  }

  private PointF getAnchorPoint(int anchorId) {
    for (int i = 0; i < anchorIds.size(); i++) {
      if (anchorIds.get(i) == anchorId) {
        return anchorPoints.get(i);
      }
    }
    return null;
  }

  private Integer overlayIndexToAnchorId(int overlayIndex) {
    if (overlayIndex < 0 || overlayIndex >= anchorIds.size()) {
      return null;
    }
    return anchorIds.get(overlayIndex);
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      float focusX = detector.getFocusX();
      float focusY = detector.getFocusY();
      scalingOverlayIndex = findAnchorAtPoint(focusX, focusY);
      return scalingOverlayIndex != null;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      if (scalingOverlayIndex != null) {
        Integer anchorId = overlayIndexToAnchorId(scalingOverlayIndex);
        if (anchorId != null) {
          float currentScale = anchorScales.getOrDefault(anchorId, 1.0f);
          float newScale = currentScale * detector.getScaleFactor();
          newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
          anchorScales.put(anchorId, newScale);
          invalidate();
          return true;
        }
      }
      return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      scalingOverlayIndex = null;
    }
  }
}
