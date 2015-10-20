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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class RawInputStrokeDoodle extends Doodle {

	private static final String TAG = "RawInputStrokeDoodle";

	private static final float OptimizationThreshold = 2;
	private static final float MinStrokeThickness = 2;
	private static final float MaxStrokeThickness = 16;
	private static final float MaxStrokeVel = 700;

	private InputStroke optimizingInputStroke = null;
	private InputStroke rawInputStroke = null;
	private RectF boundingRect = null;
	private InvalidationDelegate invalidationDelegate;
	private Paint inputStrokePaint, strokePaint;
	private RectF invalidationRect;

	public RawInputStrokeDoodle() {
		inputStrokePaint = new Paint();
		inputStrokePaint.setAntiAlias(true);
		inputStrokePaint.setColor(0xFFFF0000);
		inputStrokePaint.setStrokeWidth(1);
		inputStrokePaint.setStyle(Paint.Style.STROKE);
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

		if (rawInputStroke != null) {
			drawInputStroke(canvas, rawInputStroke, 0xFF0000FF, 1);
		}

		if (optimizingInputStroke != null) {
			drawInputStroke(canvas, optimizingInputStroke, 0xFFFF9900, 1);
		}

		if (invalidationRect != null) {
			canvas.drawRect(invalidationRect, inputStrokePaint);
		}

		invalidationRect = null;
	}

	private float getRadiusForInputStrokePoint(InputStroke inputStroke, int index) {
		final float vel = inputStroke.get(index).velocity;
		final float velScale = Math.min(vel / MAX_VEL_DP_PS, 1f);

		Log.i(TAG, "i: " + index + " vel: " + vel + " velScale: " + velScale);

		return MIN_RADIUS + (velScale * (MAX_RADIUS - MIN_RADIUS));
	}

	private static final float MIN_RADIUS = 2;
	private static final float MAX_RADIUS = 20;
	private static final float MAX_VEL_DP_PS = 500;

	private void drawInputStroke(Canvas canvas, InputStroke stroke, int color, float width) {
		inputStrokePaint.setColor(color);
		inputStrokePaint.setStyle(Paint.Style.FILL);
		inputStrokePaint.setStrokeWidth(width);

		Path p = new Path();
		ArrayList<InputStroke.Point> points = stroke.getPoints();
		InputStroke.Point firstPoint = points.get(0);
		p.moveTo(firstPoint.position.x, firstPoint.position.y);

		float radius = getRadiusForInputStrokePoint(stroke, 0);
		canvas.drawCircle(firstPoint.position.x, firstPoint.position.y, radius, inputStrokePaint);

		for (int i = 1, N = points.size(); i < N; i++) {
			PointF point = points.get(i).position;
			p.lineTo(point.x, point.y);
			radius = getRadiusForInputStrokePoint(stroke, i);

			if (radius > 0) {
				canvas.drawCircle(point.x, point.y, radius, inputStrokePaint);
			}
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
		rawInputStroke = new InputStroke();
		rawInputStroke.add(event.getX(), event.getY());

//		optimizingInputStroke = new InputStroke(OptimizationThreshold);
//		optimizingInputStroke.add(event.getX(), event.getY());
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputStroke.Point lastPoint = rawInputStroke.lastPoint();
		rawInputStroke.add(event.getX(), event.getY());
		InputStroke.Point currentPoint = rawInputStroke.lastPoint();

		if (optimizingInputStroke != null) {
			optimizingInputStroke.add(event.getX(), event.getY());
		}

		if (lastPoint != null && currentPoint != null) {
			// invalidate the region containing the last point plotted and the current one
			invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
			getInvalidationDelegate().invalidate(invalidationRect);
		}
	}

	void onTouchEventEnd() {
		rawInputStroke.finish();
		invalidationRect = rawInputStroke.getBoundingRect();

		if (optimizingInputStroke != null) {
			optimizingInputStroke.finish();
			invalidationRect.union(optimizingInputStroke.getBoundingRect());
		}

		if (invalidationRect != null) {
			getInvalidationDelegate().invalidate(invalidationRect);
		} else {
			getInvalidationDelegate().invalidate();
		}
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<RawInputStrokeDoodle> weakDoodle;

		public InputDelegate(RawInputStrokeDoodle rawInputStrokeDoodle) {
			this.weakDoodle = new WeakReference<>(rawInputStrokeDoodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			RawInputStrokeDoodle rawInputStrokeDoodle = weakDoodle.get();
			if (rawInputStrokeDoodle != null) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						rawInputStrokeDoodle.onTouchEventBegin(event);
						return true;
					case MotionEvent.ACTION_UP:
						rawInputStrokeDoodle.onTouchEventEnd();
						return true;
					case MotionEvent.ACTION_MOVE:
						rawInputStrokeDoodle.onTouchEventMove(event);
						return true;
				}
			}

			return false;
		}
	}
}

