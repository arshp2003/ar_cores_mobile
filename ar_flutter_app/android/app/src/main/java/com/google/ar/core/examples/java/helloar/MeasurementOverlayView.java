package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple overlay that renders measurement lines and distance labels between screen-projected
 * anchor positions.
 */
public class MeasurementOverlayView extends View {

  private final Paint linePaint = new Paint();
  private final Paint textPaint = new Paint();
  private final Path linePath = new Path();

  private final List<PointF> anchorPoints = new ArrayList<>();
  private final List<Float> segmentDistances = new ArrayList<>();

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

    linePaint.setAntiAlias(true);
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setStrokeWidth(6f);
    linePaint.setColor(0xFFFFC107); // Amber accent

    textPaint.setAntiAlias(true);
    textPaint.setColor(0xFFFFFFFF);
    textPaint.setTextSize(42f);
    textPaint.setShadowLayer(6f, 0f, 0f, 0xFF000000);
  }

  public void clear() {
    anchorPoints.clear();
    segmentDistances.clear();
    invalidate();
  }

  public void setAnchors(List<PointF> points, List<Float> distances) {
    anchorPoints.clear();
    segmentDistances.clear();
    if (points != null) {
      anchorPoints.addAll(points);
    }
    if (distances != null) {
      segmentDistances.addAll(distances);
    }
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (anchorPoints.size() < 2) {
      return;
    }

    linePath.reset();

    PointF previous = anchorPoints.get(0);
    for (int i = 1; i < anchorPoints.size(); ++i) {
      PointF point = anchorPoints.get(i);
      linePath.moveTo(previous.x, previous.y);
      linePath.lineTo(point.x, point.y);
      previous = point;
    }

    canvas.drawPath(linePath, linePaint);

    previous = anchorPoints.get(0);
    for (int i = 1; i < anchorPoints.size(); ++i) {
      PointF point = anchorPoints.get(i);

      float distanceMeters = i - 1 < segmentDistances.size() ? segmentDistances.get(i - 1) : Float.NaN;
      if (!Float.isNaN(distanceMeters)) {
        float midX = (previous.x + point.x) / 2f;
        float midY = (previous.y + point.y) / 2f;
        canvas.drawText(String.format("%.2f m", distanceMeters), midX, midY, textPaint);
      }

      previous = point;
    }
  }
}
