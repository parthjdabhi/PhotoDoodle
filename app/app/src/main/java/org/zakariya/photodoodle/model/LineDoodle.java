package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.RectFUtil;
import org.zakariya.photodoodle.geom.Stroke;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	private static final float OptimizationThreshold = 4;
	private static final float MinStrokeThickness = 2;
	private static final float MaxStrokeThickness = 16;
	private static final float MaxStrokeVel = 700;

	private InputStroke currentInputStroke = null;
	private InputStroke optimizedCurrentInputStroke = null;
	private Stroke currentStroke = null;
	private RectF boundingRect = null;
	private InvalidationDelegate invalidationDelegate;
	private Paint inputStrokePaint, strokePaint;
	private RectF invalidationRect;

	public LineDoodle() {
		inputStrokePaint = new Paint();
		inputStrokePaint.setAntiAlias(true);
		inputStrokePaint.setColor(0xFFFF0000);
		inputStrokePaint.setStrokeWidth(1);
		inputStrokePaint.setStyle(Paint.Style.STROKE);

		strokePaint = new Paint();
		strokePaint.setAntiAlias(true);
		strokePaint.setColor(0xFF999999);
		strokePaint.setStrokeWidth(1);
		strokePaint.setStyle(Paint.Style.FILL);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public RectF getBoundingRect() {
		return boundingRect;
	}

	@Override
	public void clear() {
		boundingRect = null;
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);

		if (invalidationRect == null) {
			invalidationRect = getInvalidationDelegate().getBounds();
		}

		if (currentStroke != null) {
			canvas.drawPath(currentStroke.getPath(), strokePaint);
		} else {

			if (currentInputStroke != null) {
				drawInputStroke(canvas, currentInputStroke, 0xFFFF0000, 8);
			}

			if (optimizedCurrentInputStroke != null) {
				drawInputStroke(canvas, optimizedCurrentInputStroke, 0xaa00FF00, 4);
			}
		}
		
		if (invalidationRect != null) {
			canvas.drawRect(invalidationRect, inputStrokePaint);
		}

		invalidationRect = null;
	}

	private void drawInputStroke(Canvas canvas, InputStroke stroke, int color, float radius) {
		inputStrokePaint.setColor(color);
		inputStrokePaint.setStyle(Paint.Style.FILL);

		Path p = new Path();
		ArrayList<InputStroke.Point> points = stroke.getPoints();
		InputStroke.Point firstPoint = points.get(0);
		p.moveTo(firstPoint.position.x, firstPoint.position.y);
		canvas.drawCircle(firstPoint.position.x, firstPoint.position.y,radius,inputStrokePaint);

		for (int i = 1, N = points.size(); i < N; i++) {
			PointF point = points.get(i).position;
			p.lineTo(point.x, point.y);

			canvas.drawCircle(point.x, point.y,radius,inputStrokePaint);
		}

		inputStrokePaint.setStyle(Paint.Style.STROKE);
		canvas.drawPath(p, inputStrokePaint);
	}

	@Override
	public DoodleView.InputDelegate inputDelegate() {
		return new InputDelegate(this);
	}

	@Override
	public void setInvalidationDelegate(InvalidationDelegate invalidationDelegate) {
		this.invalidationDelegate = invalidationDelegate;
	}

	@Override
	public InvalidationDelegate getInvalidationDelegate() {
		return invalidationDelegate;
	}

	void onTouchEventBegin(@NonNull MotionEvent event) {
		optimizedCurrentInputStroke = null;
		currentInputStroke = new InputStroke();
		currentInputStroke.add(event.getX(), event.getY());
		currentStroke = null;
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputStroke.Point lastPoint = currentInputStroke.lastPoint();
		currentInputStroke.add(event.getX(), event.getY());
		InputStroke.Point currentPoint = currentInputStroke.lastPoint();

		if (lastPoint != null && currentPoint != null) {
			// invalidate the region containing the last point plotted and the current one
			invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
			getInvalidationDelegate().invalidate(invalidationRect);
		}
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		optimizedCurrentInputStroke = currentInputStroke.optimize(OptimizationThreshold);
		currentStroke = Stroke.smoothedStroke(optimizedCurrentInputStroke, MinStrokeThickness, MaxStrokeThickness, MaxStrokeVel);

		// invalidate region containing the line we just generated
		invalidationRect = currentStroke.getBoundingRect();
		if (invalidationRect != null) {
			getInvalidationDelegate().invalidate(invalidationRect);
		} else {
			getInvalidationDelegate().invalidate();
		}
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<LineDoodle> weakLineDoodle;

		public InputDelegate(LineDoodle lineDoodle) {
			this.weakLineDoodle = new WeakReference<>(lineDoodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			LineDoodle lineDoodle = weakLineDoodle.get();
			if (lineDoodle != null) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						lineDoodle.onTouchEventBegin(event);
						return true;
					case MotionEvent.ACTION_UP:
						lineDoodle.onTouchEventEnd(event);
						return true;
					case MotionEvent.ACTION_MOVE:
						lineDoodle.onTouchEventMove(event);
						return true;
				}
			}

			return false;
		}
	}
}

