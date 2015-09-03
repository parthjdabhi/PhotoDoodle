package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.RectFUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by shamyl on 8/9/15.
 */
public class LineDoodle extends Doodle {

	private static final String TAG = "LineDoodle";

	/**
	 * Represents user input. As user drags across screen, each location is recorded along with its timestamp.
	 * The timestamps can be compared across an array of ControlPoint to determine the velocity of the touch,
	 * which will be used to determine line thickness.
	 */
	class InputPoint {
		PointF position;
		long timestamp;

		InputPoint(){}
		InputPoint(float x, float y) {
			position = new PointF(x,y);
			timestamp = System.currentTimeMillis();
		}
	}

	class ControlPoint {
		PointF position; // the position of the point
		PointF tangent; // the normalized tangent vector of the line at this point - points "forward"
		float halfSize; // the half thickness of the line at this point
	}

	class InputLine {
		ArrayList<InputPoint> inputPoints = new ArrayList<>();
	}

	class ControlPointLine {
		ArrayList<ControlPoint> controlPoints = new ArrayList<>();

		ControlPointLine() {
		}

		// compute a ControlPointLine for an InputLine
		ControlPointLine(InputLine inputLine) {

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
		if (currentInputLine != null) {
			Log.d(TAG, "draw - Will draw currentInputLine with " + currentInputLine.inputPoints.size() + " points");
			Path p = new Path();
			InputPoint firstPoint = currentInputLine.inputPoints.get(0);
			p.moveTo(firstPoint.position.x, firstPoint.position.y);

			for (int i = 1, N = currentInputLine.inputPoints.size(); i < N; i++) {
				PointF point = currentInputLine.inputPoints.get(i).position;
				p.lineTo(point.x, point.y);
			}

			canvas.drawColor(0xFFCCFFFF);

			Paint linePaint = new Paint();
			linePaint.setAntiAlias(true);
			linePaint.setColor(0xFF000000);
			linePaint.setStrokeWidth(1);
			linePaint.setStyle(Paint.Style.STROKE);

			canvas.drawPath(p,linePaint);
		} else {
			Log.d(TAG, "draw - currentInputLine is null");
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
		currentInputLine.inputPoints.add(new InputPoint(event.getX(),event.getY()));
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		InputPoint lastPoint = currentInputLine.inputPoints.get(currentInputLine.inputPoints.size()-1);
		currentInputLine.inputPoints.add(new InputPoint(event.getX(),event.getY()));
		InputPoint currentPoint = currentInputLine.inputPoints.get(currentInputLine.inputPoints.size()-1);

		// invalidate the region containing the last point plotted and the current one
		RectF dirtyRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
		getInvalidationDelegate().invalidate(dirtyRect);
		Log.d(TAG,"onTouchEventMove invalidating " + dirtyRect);
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		controlPointLines.add(new ControlPointLine(currentInputLine));
		currentInputLine = null;
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

