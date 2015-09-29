package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.LinePoint;
import org.zakariya.photodoodle.geom.InputPoint;
import org.zakariya.photodoodle.geom.PointFUtil;
import org.zakariya.photodoodle.geom.RectFUtil;
import org.zakariya.photodoodle.util.Accumulator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	private static final float VelocityScaling = 0.05f;

	class InputLine {
		private ArrayList<InputPoint> points = new ArrayList<>();

		public ArrayList<InputPoint> getPoints() {
			return points;
		}

		public int size() {
			return points.size();
		}

		public void add(float x, float y) {
			InputPoint p = new InputPoint(x,y);
			points.add(p);

			int count = points.size();
			if (count == 2) {
				InputPoint a = points.get(0);
				InputPoint b = points.get(1);
				a.tangent = PointFUtil.dir(a.position,b.position).first;
			}

			if (count > 2) {
				InputPoint a = points.get(count-3);
				InputPoint b = points.get(count-2);
				InputPoint c = points.get(count-1);

				Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
				PointF abPrime = PointFUtil.rotateCCW(abDir.first);

				Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
				PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

				PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
				if (PointFUtil.length2(half) > 1e-4) {
					b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;;
				} else {
					b.tangent = bcPrime;
				}
			}
		}

		public void finish() {
			int count = points.size();
			if (count > 1) {
				InputPoint a = points.get(count-2);
				InputPoint b = points.get(count-1);
				a.tangent = PointFUtil.dir(a.position,b.position).first;
			}
		}

		/**
		 * Analyze the line and compute tangent vectors for each vertex
		 */
		public void computeTangents() {
			if (points.size() < 3) {
				return;
			}

			for (int i = 0, N = points.size(); i < N; i++) {
				if (i == 0) {
					InputPoint a = points.get(i);
					InputPoint b = points.get(i + 1);
					Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

					a.tangent = dir.first;
				} else if (i == N - 1) {
					InputPoint b = points.get(i);
					InputPoint a = points.get(i - 1);
					Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

					b.tangent = dir.first;
				} else {
					InputPoint a = points.get(i - 1);
					InputPoint b = points.get(i);
					InputPoint c = points.get(i + 1);

					Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
					PointF abPrime = PointFUtil.rotateCCW(abDir.first);

					Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
					PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

					PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
					if (PointFUtil.length2(half) > 1e-4) {
						b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;;
					} else {
						b.tangent = bcPrime;
					}
				}
			}
		}
	}

	class ControlPointLine {
		ArrayList<LinePoint> linePoints = new ArrayList<>();
		RectF boundingRect;

		ControlPointLine() {
		}

		// compute a ControlPointLine for an InputLine
		ControlPointLine(InputLine inputLine) {
			if (inputLine.size() < 2) {
				return;
			}

			Accumulator accumulator = new Accumulator(16, 0);
			ArrayList<InputPoint> points = inputLine.getPoints();

			// a is the previous point, b is current point, c is next point
			InputPoint a = null, b = points.get(0), c = points.get(1);

			// create bounding rect which isn't empty, so we can expand it in loop below
			boundingRect = new RectF(b.position.x - 1, b.position.y - 1, b.position.x + 1, b.position.y + 1);

			for (int i = 0, N = points.size(); i < N; i++) {
				LinePoint cp = null;
				if (a == null) {
					if (b != null && c != null) { // b & c should always be nonnull, just silencing compiler warnings
						cp = new LinePoint(b.position, 0);
					}
				} else if (c == null) {
					if (b != null) { // b should always be nonnull, just silencing compiler warnings
						// this is last point in sequence
						cp = new LinePoint(b.position, 0);
					}
				} else {
					if (b != null) { // b should always be nonnull, just silencing compiler warnings
						// this is a point in the sequence middle
						float dist = PointFUtil.distance(a.position,b.position);

						long millis = b.timestamp - a.timestamp;
						float dpPerSecond = dist / (millis / 1000f);
						float size = dpPerSecond * VelocityScaling;

						// smooth the size
						size = accumulator.add(size);

						cp = new LinePoint(b.position, size / 2);
					}
				}

				if (cp != null) {
					linePoints.add(cp);
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

		if (currentInputLine != null) {
			Log.d(TAG, "draw - Will draw currentInputLine with " + currentInputLine.points.size() + " points");
			Path p = new Path();
			ArrayList<InputPoint> points = currentInputLine.getPoints();
			InputPoint firstPoint = points.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = points.size(); i < N; i++) {
				PointF point = points.get(i).position;
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
			LinePoint firstPoint = line.linePoints.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = line.linePoints.size(); i < N; i++) {
				PointF point = line.linePoints.get(i).position;
				p.lineTo(point.x, point.y);
			}

			linePaint.setColor(0xFF000000);
			canvas.drawPath(p, linePaint);

			// now draw tangents and sizes
			linePaint.setColor(0xFFFF0000);
			p = new Path();
			for (LinePoint point : line.linePoints) {
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
			for (InputPoint inputPoint : currentInputLine.points) {
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
			for (LinePoint linePoint : controlPointLine.linePoints) {
				if (boundingRect.isEmpty()) {
					boundingRect.set(linePoint.position.x - linePoint.halfSize,
							linePoint.position.y - linePoint.halfSize,
							linePoint.position.x + linePoint.halfSize,
							linePoint.position.y + linePoint.halfSize);
				} else {
					boundingRect.union(linePoint.position.x - linePoint.halfSize,
							linePoint.position.y - linePoint.halfSize,
							linePoint.position.x + linePoint.halfSize,
							linePoint.position.y + linePoint.halfSize);
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
		currentInputLine.add(event.getX(), event.getY());
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputPoint lastPoint = currentInputLine.points.get(currentInputLine.points.size() - 1);
		currentInputLine.add(event.getX(),event.getY());
		InputPoint currentPoint = currentInputLine.points.get(currentInputLine.points.size() - 1);

		// invalidate the region containing the last point plotted and the current one
		RectF dirtyRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
		getInvalidationDelegate().invalidate(dirtyRect);
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		currentInputLine.finish();
		ControlPointLine line = new ControlPointLine(currentInputLine);
		controlPointLines.add(line);
		currentInputLine = null;

		Log.d(TAG, "onTouchEventEnd added control point line with " + line.linePoints.size() + " points, invalidating: " + line.getBoundingRect());

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

