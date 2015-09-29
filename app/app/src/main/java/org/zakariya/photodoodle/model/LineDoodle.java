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
import org.zakariya.photodoodle.geom.InputPointLine;
import org.zakariya.photodoodle.geom.Circle;
import org.zakariya.photodoodle.geom.InputPoint;
import org.zakariya.photodoodle.geom.RectFUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	private static final float VelocityScaling = 0.05f;

	private InputPointLine currentInputPointLine = null;
	private ArrayList<CircleLine> circleLines = new ArrayList<>();
	private RectF boundingRect = null;
	private InvalidationDelegate invalidationDelegate;

	public LineDoodle() {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this,savedInstanceState);
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

		Paint linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setColor(0xFF000000);
		linePaint.setStrokeWidth(1);
		linePaint.setStyle(Paint.Style.STROKE);

		if (currentInputPointLine != null) {
			Log.d(TAG, "draw - Will draw currentInputPointLine with " + currentInputPointLine.size() + " points");
			Path p = new Path();
			ArrayList<InputPoint> points = currentInputPointLine.getPoints();
			InputPoint firstPoint = points.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = points.size(); i < N; i++) {
				PointF point = points.get(i).position;
				p.lineTo(point.x, point.y);
			}

			canvas.drawPath(p, linePaint);
		} else {
			Log.d(TAG, "draw - currentInputPointLine is null");
		}

		Log.d(TAG, "draw - will draw + " + circleLines.size() + " control point lines");

		// now draw our control points
		for (CircleLine line : circleLines) {
			Path p = new Path();
			Circle firstPoint = line.firstCircle();
			p.moveTo(firstPoint.position.x, firstPoint.position.y);


			for (int i = 1, N = line.size(); i < N; i++) {
				PointF point = line.get(i).position;
				p.lineTo(point.x, point.y);
			}

			linePaint.setColor(0xFF000000);
			canvas.drawPath(p, linePaint);

			// now draw tangents and sizes
			linePaint.setColor(0xFFFF0000);
			p = new Path();
			for (Circle point : line.getCircles()) {
				p.addCircle(point.position.x, point.position.y, point.radius, Path.Direction.CW);
			}

			canvas.drawPath(p, linePaint);
		}
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

	/**
	 * Compute the boundingRect that contains the entire contents of the drawing
	 */
	void computeBoundingRect() {
		boundingRect = new RectF();

		if (currentInputPointLine != null) {
			for (InputPoint inputPoint : currentInputPointLine.getPoints()) {
				if (boundingRect.isEmpty()) {
					boundingRect.left = inputPoint.position.x;
					boundingRect.right = inputPoint.position.x;
					boundingRect.bottom = inputPoint.position.y;
					boundingRect.top = inputPoint.position.y;
				} else {
					boundingRect.union(inputPoint.position.x, inputPoint.position.y);
				}
			}
		}

		for (CircleLine circleLine : circleLines) {
			for (Circle circle : circleLine.getCircles()) {
				if (boundingRect.isEmpty()) {
					boundingRect.set(circle.position.x - circle.radius,
							circle.position.y - circle.radius,
							circle.position.x + circle.radius,
							circle.position.y + circle.radius);
				} else {
					boundingRect.union(circle.position.x - circle.radius,
							circle.position.y - circle.radius,
							circle.position.x + circle.radius,
							circle.position.y + circle.radius);
				}
			}
		}
	}

	/**
	 * Expand the bounding rect to contain dirtyRect
	 *
	 * @param dirtyRect the rect to union with the current bounding rect
	 */
	void updateBoundingRect(RectF dirtyRect) {
		if (boundingRect == null || boundingRect.isEmpty()) {
			boundingRect = dirtyRect;
		} else {
			boundingRect.union(dirtyRect);
		}
	}

	void onTouchEventBegin(@NonNull MotionEvent event) {
		currentInputPointLine = new InputPointLine();
		currentInputPointLine.add(event.getX(), event.getY());
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputPoint lastPoint = currentInputPointLine.lastPoint();
		currentInputPointLine.add(event.getX(),event.getY());
		InputPoint currentPoint = currentInputPointLine.lastPoint();

		if (lastPoint != null && currentPoint != null) {
			// invalidate the region containing the last point plotted and the current one
			RectF dirtyRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
			getInvalidationDelegate().invalidate(dirtyRect);
		}
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		currentInputPointLine.finish();
		CircleLine line = new CircleLine(currentInputPointLine, VelocityScaling);
		circleLines.add(line);
		currentInputPointLine = null;

		Log.d(TAG, "onTouchEventEnd added control point line with " + line.size() + " points, invalidating: " + line.getBoundingRect());

		// invalidate region containing the line we just generated
		getInvalidationDelegate().invalidate(line.getBoundingRect());
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

