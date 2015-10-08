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
import org.zakariya.photodoodle.geom.CircleLine;
import org.zakariya.photodoodle.geom.CircleLineTessellator;
import org.zakariya.photodoodle.geom.InputPoint;
import org.zakariya.photodoodle.geom.InputPointLine;
import org.zakariya.photodoodle.geom.RectFUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	private static final float MinStrokeThickness = 1;
	private static final float MaxStrokeThickness = 10;
	private static final float MaxStrokeVel = 700;

	private InputPointLine currentInputPointLine = null;
	private CircleLine currentCircleLine = null;
	private RectF boundingRect = null;
	private InvalidationDelegate invalidationDelegate;
	private Paint inputLinePaint, strokePaint;
	private RectF invalidationRect;

	public LineDoodle() {
		inputLinePaint = new Paint();
		inputLinePaint.setAntiAlias(true);
		inputLinePaint.setColor(0xFF000000);
		inputLinePaint.setStrokeWidth(1);
		inputLinePaint.setStyle(Paint.Style.STROKE);

		strokePaint = new Paint();
		strokePaint.setAntiAlias(true);
		strokePaint.setColor(0xFF000000);
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

		if (currentCircleLine != null) {
			canvas.drawPath(currentCircleLine.getPath(), strokePaint);
		} else if (currentInputPointLine != null) {

			Path p = new Path();
			ArrayList<InputPoint> points = currentInputPointLine.getPoints();
			InputPoint firstPoint = points.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = points.size(); i < N; i++) {
				PointF point = points.get(i).position;
				p.lineTo(point.x, point.y);
			}

			canvas.drawPath(p, inputLinePaint);
		}

		invalidationRect = null;
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
		currentInputPointLine = new InputPointLine();
		currentInputPointLine.add(event.getX(), event.getY());
		currentCircleLine = null;
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputPoint lastPoint = currentInputPointLine.lastPoint();
		currentInputPointLine.add(event.getX(), event.getY());
		InputPoint currentPoint = currentInputPointLine.lastPoint();

		if (lastPoint != null && currentPoint != null) {
			// invalidate the region containing the last point plotted and the current one
			invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
			getInvalidationDelegate().invalidate(invalidationRect);
		}
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		currentInputPointLine.finish();
		currentCircleLine = CircleLine.smoothedCircleLine(currentInputPointLine, MinStrokeThickness, MaxStrokeThickness, MaxStrokeVel);
		currentInputPointLine = null;

		// invalidate region containing the line we just generated
		invalidationRect = currentCircleLine.getBoundingRect();
		getInvalidationDelegate().invalidate(invalidationRect);
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

