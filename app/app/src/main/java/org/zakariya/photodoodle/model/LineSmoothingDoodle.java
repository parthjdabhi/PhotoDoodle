package org.zakariya.photodoodle.model;

import android.content.Context;
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
import org.zakariya.photodoodle.geom.Circle;
import org.zakariya.photodoodle.geom.CircleLine;
import org.zakariya.photodoodle.geom.CircleLineTessellator;
import org.zakariya.photodoodle.geom.CubicBezierInterpolator;
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.PointFUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by shamyl on 10/4/15.
 */
public class LineSmoothingDoodle extends Doodle {
	private static final String TAG = "LineSmoothingDoodle";
	private static final String FILE = "LineSmoothingDoodle.dat";
	private static final float HandleRadius = 10f;
	private static boolean DrawBezierControlPoints = false;

	private InputStroke inputStroke = new InputStroke();
	private CircleLine renderedCircleLine = null;
	private CircleLineTessellator renderedCircleLineTessellator = new CircleLineTessellator();
	private InvalidationDelegate invalidationDelegate;
	private Paint handlePaint, controlPointPaint, smoothedLinePaint, renderedCircleLinePaint;
	private InputStroke.Point draggingPoint;
	private Context context;

	public LineSmoothingDoodle(Context context) {
		this.context = context;

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0xFF00FFFF);
		handlePaint.setStyle(Paint.Style.FILL);

		controlPointPaint = new Paint();
		controlPointPaint.setAntiAlias(true);
		controlPointPaint.setStyle(Paint.Style.STROKE);

		smoothedLinePaint = new Paint();
		smoothedLinePaint.setAntiAlias(true);
		smoothedLinePaint.setColor(0xFF000000);
		smoothedLinePaint.setStrokeWidth(1);
		smoothedLinePaint.setStyle(Paint.Style.STROKE);

		renderedCircleLinePaint = new Paint();
		renderedCircleLinePaint.setAntiAlias(true);
		renderedCircleLinePaint.setColor(0xFFFF0000);
		renderedCircleLinePaint.setStrokeWidth(1);
		renderedCircleLinePaint.setStyle(Paint.Style.STROKE);

		if (!loadPoints()) {
			long timestamp = 0;
			long deltaTimestamp = 500;

			inputStroke.add(50, 100, timestamp);
			timestamp += deltaTimestamp;
			inputStroke.add(350, 100, timestamp);
			timestamp += deltaTimestamp;
			inputStroke.add(350, 300, timestamp);
			timestamp += deltaTimestamp;
			inputStroke.add(50, 300, timestamp);
			timestamp += deltaTimestamp;
			inputStroke.add(50, 400, timestamp);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
	}

	@Override
	public RectF getBoundingRect() {
		return inputStroke.getBoundingRect();
	}

	@Override
	public void clear() {

	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);

		if (inputStroke != null) {

			ArrayList<InputStroke.Point> points = inputStroke.getPoints();
			CubicBezierInterpolator cbi = new CubicBezierInterpolator();
			for (int i = 0; i < points.size() - 1; i++) {
				InputStroke.Point a = points.get(i);
				InputStroke.Point b = points.get(i + 1);

				float length = PointFUtil.distance(a.position, b.position) / 4;
				PointF controlPointA = PointFUtil.add(a.position, PointFUtil.scale(a.tangent, length));
				PointF controlPointB = PointFUtil.add(b.position, PointFUtil.scale(b.tangent, -length));

				if (DrawBezierControlPoints) {
					controlPointPaint.setColor(0x9900FF00);
					canvas.drawCircle(controlPointA.x, controlPointA.y, HandleRadius * 1.5f, controlPointPaint);

					controlPointPaint.setColor(0x990000FF);
					canvas.drawCircle(controlPointB.x, controlPointB.y, HandleRadius * 1.5f, controlPointPaint);
				}

				// now tessellate
				PointF bp = new PointF();
				Path path = new Path();
				cbi.set(a.position, controlPointA, controlPointB, b.position);
				int subdivisions = cbi.getRecommendedSubdivisions(1);
				if (subdivisions > 1) {

					path.moveTo(a.position.x, a.position.y);
					float dt = 1f / subdivisions;
					float t = dt;
					for (int j = 0; j < subdivisions; j++, t += dt) {
						cbi.getBezierPoint(t, bp);
						path.lineTo(bp.x, bp.y);
					}

					canvas.drawPath(path, smoothedLinePaint);

				} else {
					canvas.drawLine(a.position.x, a.position.y, b.position.x, b.position.y, smoothedLinePaint);
				}
			}

			// draw dots representing the input points
			for (InputStroke.Point point : points) {

				canvas.drawCircle(point.position.x, point.position.y, HandleRadius, handlePaint);

				PointF t = new PointF(point.position.x + point.tangent.x * 2 * HandleRadius, point.position.y + point.tangent.y * 2 * HandleRadius);

				canvas.drawLine(point.position.x, point.position.y, t.x, t.y, handlePaint);
			}

		} else {
			Log.d(TAG, "draw - inputStroke is null");
		}

		if (renderedCircleLine != null) {

//			Path path = new Path();
//			renderedCircleLinePaint.setColor(0xFFFF0000);
//			renderedCircleLineTessellator.tessellate(renderedCircleLine, path);
//			canvas.drawPath(path, renderedCircleLinePaint);

//			renderedCircleLinePaint.setColor(0x66FF0000);
			for (Circle circle : renderedCircleLine.getCircles()) {
				canvas.drawCircle(circle.position.x, circle.position.y, circle.radius, renderedCircleLinePaint);
			}
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

	void onTouchEventBegin(@NonNull MotionEvent event) {

		PointF point = new PointF(event.getX(), event.getY());
		float minDist2 = Float.MAX_VALUE;
		for (int i = 0; i < inputStroke.size(); i++) {
			InputStroke.Point p = inputStroke.get(i);
			float d2 = PointFUtil.distance2(point, p.position);
			if (d2 < minDist2) {
				minDist2 = d2;
				draggingPoint = p;
			}
		}

		// now, confirm closest match was close enough
		if (minDist2 > HandleRadius * HandleRadius) {
			draggingPoint = null;
		}
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		if (draggingPoint != null) {

			// update line
			draggingPoint.position.x = event.getX();
			draggingPoint.position.y = event.getY();
			inputStroke.invalidate();
			updateCircleLine();
		}

		// trigger redraw
		getInvalidationDelegate().invalidate(getBoundingRect());
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		draggingPoint = null;
		getInvalidationDelegate().invalidate(getBoundingRect());
		save();
	}

	private void save() {
		try {
			context.deleteFile(FILE);
			FileOutputStream fos = context.openFileOutput(FILE, Context.MODE_PRIVATE);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			oos.writeObject(inputStroke);
			oos.close();

			Log.i(TAG, "Saved to " + FILE);
		} catch (Exception e) {
			Log.e(TAG, "Unable to write to file: " + FILE + " e: " + e);
		}
	}

	private boolean loadPoints() {
		try {
			FileInputStream fis = context.openFileInput(FILE);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);

			inputStroke = (InputStroke) ois.readObject();
			updateCircleLine();
			Log.i(TAG, "Loaded " + inputStroke.size() + " points from " + FILE);
			return inputStroke.size() > 0;
		} catch (Exception e) {
			Log.e(TAG, "Error opening file: " + FILE + " e: " + e);
			return false;
		}
	}

	private void updateCircleLine() {
		renderedCircleLine = CircleLine.smoothedCircleLine(inputStroke, 5, 100, 600);
		//renderedCircleLine = new CircleLine(inputStroke,5,100,600);
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<LineSmoothingDoodle> weakLineDoodle;

		public InputDelegate(LineSmoothingDoodle lineDoodle) {
			this.weakLineDoodle = new WeakReference<>(lineDoodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			LineSmoothingDoodle lineDoodle = weakLineDoodle.get();
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
