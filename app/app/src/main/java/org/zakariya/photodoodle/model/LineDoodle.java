package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.PointFUtil;
import org.zakariya.photodoodle.geom.RectFUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	private static final float VelocityScaling = 0.05f;

	class Accumulator {
		float[] values;
		float current;
		float accumulator;
		int size = 0;
		int index = 0;

		Accumulator(int size, float current) {
			values = new float[size];
			this.size = size;
			this.index = 0;
			this.accumulator = 0;
			this.current = current;
		}

		float add(float v) {

			if (size == 0) {

				//
				// for zero-size, just pass through
				//

				current = v;
				return v;
			} else if (index < size) {

				//
				//  We're still filling the buffer
				//

				values[index++] = v;
				accumulator += v;
			} else {

				//
				//  Buffer is full - round-robin and average
				//  Subtract oldest value from accumulator
				//  Replace oldest with newest
				//  add newest to accumulator
				//

				accumulator -= values[index % size];
				values[index++ % size] = v;
				accumulator += v;
			}
			current = accumulator / (float) index;
			return current;
		}

		float getCurrent() {
			return current;
		}

		boolean isPrimed() {
			return size <= 0 || index >= size;
		}
	}

	/**
	 * Represents user input. As user drags across screen, each location is recorded along with its timestamp.
	 * The timestamps can be compared across an array of ControlPoint to determine the velocity of the touch,
	 * which will be used to determine line thickness.
	 */
	class InputPoint {
		PointF position;
		long timestamp;

		InputPoint() {
		}

		InputPoint(float x, float y) {
			position = new PointF(x, y);
			timestamp = System.currentTimeMillis();
		}
	}

	class ControlPoint {
		PointF position; // the position of the point
		PointF tangent; // the normalized tangent vector of the line at this point - points "forward"
		float halfSize; // the half thickness of the line at this point

		ControlPoint() {
		}

		ControlPoint(PointF position, PointF tangent, float halfSize) {
			this.position = position;
			this.tangent = tangent;
			this.halfSize = halfSize;
		}
	}

	class InputLine {
		ArrayList<InputPoint> inputPoints = new ArrayList<>();
	}

	class ControlPointLine {
		ArrayList<ControlPoint> controlPoints = new ArrayList<>();
		RectF boundingRect;

		ControlPointLine() {
		}

		// compute a ControlPointLine for an InputLine
		ControlPointLine(InputLine inputLine) {
			if (inputLine.inputPoints.size() < 2) {
				return;
			}

			Accumulator accumulator = new Accumulator(16,0);
			ArrayList<InputPoint> points = inputLine.inputPoints;

			// a is the previous point, b is current point, c is next point
			InputPoint a = null, b = points.get(0), c = points.get(1);

			// create bounding rect which isn't empty, so we can expand it in loop below
			boundingRect = new RectF(b.position.x - 1, b.position.y - 1, b.position.x + 1, b.position.y + 1);

			for (int i = 0, N = points.size(); i < N; i++) {
				ControlPoint cp = null;
				if (a == null) {
					if (b != null && c != null) { // b & c should always be nonnull, just silencing compiler warnings
						// this is first point in sequence
						Pair<PointF, Float> dir = PointFUtil.dir(b.position, c.position);
						cp = new ControlPoint(b.position, dir.first, 0);
					}
				} else if (c == null) {
					if (b != null) { // b should always be nonnull, just silencing compiler warnings
						// this is last point in sequence
						Pair<PointF, Float> dir = PointFUtil.dir(b.position, a.position);
						cp = new ControlPoint(b.position, dir.first, 0);
					}
				} else {
					if (b != null) { // b should always be nonnull, just silencing compiler warnings
						// this is a point in the sequence middle
						Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
						Pair<PointF, Float> cbDir = PointFUtil.dir(c.position, b.position);
						PointF half = new PointF(abDir.first.x + cbDir.first.x, abDir.first.y + cbDir.first.y);

						// rotate half and normalize, that's our tangent
						PointF tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;

						long millis = b.timestamp - a.timestamp;
						float dpPerSecond = abDir.second / (millis / 1000f);
						float size = dpPerSecond * VelocityScaling;

						// smooth the size
						size = accumulator.add(size);

						cp = new ControlPoint(b.position, tangent, size / 2);
					}
				}

				if (cp != null) {
					controlPoints.add(cp);
					boundingRect.union(cp.position.x - cp.halfSize,
							cp.position.y - cp.halfSize,
							cp.position.x + cp.halfSize,
							cp.position.y + cp.halfSize);
				}

				a = b;
				b = c;
				c = (i + 2 < N) ? points.get(i + 2) : null;
			}
		}

		RectF getBoundingRect() {
			return boundingRect;
		}
	}

	private InputLine currentInputLine = null;
	private ArrayList<ControlPointLine> controlPointLines = new ArrayList<>();
	private RectF boundingRect = null;
	private InvalidationDelegate invalidationDelegate;

	public LineDoodle() {
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

		if (currentInputLine != null) {
			Log.d(TAG, "draw - Will draw currentInputLine with " + currentInputLine.inputPoints.size() + " points");
			Path p = new Path();
			InputPoint firstPoint = currentInputLine.inputPoints.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = currentInputLine.inputPoints.size(); i < N; i++) {
				PointF point = currentInputLine.inputPoints.get(i).position;
				p.lineTo(point.x, point.y);
			}

			canvas.drawPath(p, linePaint);
		} else {
			Log.d(TAG, "draw - currentInputLine is null");
		}

		Log.d(TAG, "draw - will draw + " + controlPointLines.size() + " control point lines");

		// now draw our control points
		for (ControlPointLine line : controlPointLines) {
			Path p = new Path();
			ControlPoint firstPoint = line.controlPoints.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = line.controlPoints.size(); i < N; i++) {
				PointF point = line.controlPoints.get(i).position;
				p.lineTo(point.x, point.y);
			}

			linePaint.setColor(0xFF000000);
			canvas.drawPath(p, linePaint);

			// now draw tangents and sizes
			linePaint.setColor(0xFFFF0000);
			p = new Path();
			for (ControlPoint point : line.controlPoints) {
				p.moveTo(point.position.x, point.position.y);
				p.lineTo(point.position.x + point.tangent.x * point.halfSize, point.position.y + point.tangent.y * point.halfSize);
				p.addCircle(point.position.x, point.position.y, point.halfSize, Path.Direction.CW);
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

		if (currentInputLine != null) {
			for (InputPoint inputPoint : currentInputLine.inputPoints) {
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

		for (ControlPointLine controlPointLine : controlPointLines) {
			for (ControlPoint controlPoint : controlPointLine.controlPoints) {
				if (boundingRect.isEmpty()) {
					boundingRect.set(controlPoint.position.x - controlPoint.halfSize,
							controlPoint.position.y - controlPoint.halfSize,
							controlPoint.position.x + controlPoint.halfSize,
							controlPoint.position.y + controlPoint.halfSize);
				} else {
					boundingRect.union(controlPoint.position.x - controlPoint.halfSize,
							controlPoint.position.y - controlPoint.halfSize,
							controlPoint.position.x + controlPoint.halfSize,
							controlPoint.position.y + controlPoint.halfSize);
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
		currentInputLine = new InputLine();
		currentInputLine.inputPoints.add(new InputPoint(event.getX(), event.getY()));
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputPoint lastPoint = currentInputLine.inputPoints.get(currentInputLine.inputPoints.size() - 1);
		currentInputLine.inputPoints.add(new InputPoint(event.getX(), event.getY()));
		InputPoint currentPoint = currentInputLine.inputPoints.get(currentInputLine.inputPoints.size() - 1);

		// invalidate the region containing the last point plotted and the current one
		RectF dirtyRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
		getInvalidationDelegate().invalidate(dirtyRect);
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		ControlPointLine line = new ControlPointLine(currentInputLine);
		controlPointLines.add(line);
		currentInputLine = null;

		Log.d(TAG, "onTouchEventEnd added control point line with " + line.controlPoints.size() + " points, invalidating: " + line.getBoundingRect());

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

