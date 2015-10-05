package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.InputPoint;
import org.zakariya.photodoodle.geom.InputPointLine;
import org.zakariya.photodoodle.geom.LineIntersection;
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

	private InputPointLine inputPointLine = new InputPointLine();
	private InvalidationDelegate invalidationDelegate;
	private Paint handlePaint, controlPointPaint, smoothedLinePaint;
	private InputPoint draggingPoint;
	private Context context;

	public LineSmoothingDoodle(Context context) {
		this.context = context;

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0x99FF0000);
		handlePaint.setStyle(Paint.Style.FILL);

		controlPointPaint = new Paint();
		controlPointPaint.setAntiAlias(true);
		controlPointPaint.setColor(0x9900FF00);
		controlPointPaint.setStyle(Paint.Style.STROKE);

		smoothedLinePaint = new Paint();
		smoothedLinePaint.setAntiAlias(true);
		smoothedLinePaint.setColor(0xFF000000);
		smoothedLinePaint.setStrokeWidth(1);
		smoothedLinePaint.setStyle(Paint.Style.STROKE);

		if (!loadPoints()) {
			inputPointLine.add(50, 100);
			inputPointLine.add(350, 100);
			inputPointLine.add(350, 300);
			inputPointLine.add(50, 300);
			inputPointLine.add(50, 400);
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
		return inputPointLine.getBoundingRect();
	}

	@Override
	public void clear() {

	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);

		if (inputPointLine != null) {

			ArrayList<InputPoint> points = inputPointLine.getPoints();
			for (int i = 0; i < points.size() - 1; i++) {
				InputPoint a = points.get(i);
				InputPoint b = points.get(i + 1);
				PointF controlPoint = LineIntersection.infiniteLineIntersection(a.position, a.tangent, b.position, b.tangent);
				if (controlPoint != null) {
					canvas.drawCircle(controlPoint.x, controlPoint.y, HandleRadius*1.5f, controlPointPaint);
				}
			}

			// draw dots representing the input points
			for (InputPoint point : points) {
				canvas.drawCircle(point.position.x, point.position.y, HandleRadius, handlePaint);

				PointF a = new PointF(point.position.x - point.tangent.x * 2 * HandleRadius, point.position.y - point.tangent.y * 2 * HandleRadius);
				PointF b = new PointF(point.position.x + point.tangent.x * 2 * HandleRadius, point.position.y + point.tangent.y * 2 * HandleRadius);

				canvas.drawLine(a.x,a.y,b.x,b.y, handlePaint);
			}

		} else {
			Log.d(TAG, "draw - inputPointLine is null");
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
		for (int i = 0; i < inputPointLine.size(); i++) {
			InputPoint p = inputPointLine.get(i);
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
			inputPointLine.invalidate();
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

			oos.writeObject(inputPointLine);
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

			inputPointLine = (InputPointLine) ois.readObject();
			Log.i(TAG, "Loaded " + inputPointLine.size() + " points from " + FILE);
			return inputPointLine.size() > 0;
		} catch (Exception e) {
			Log.e(TAG, "Error opening file: " + FILE + " e: " + e);
			return false;
		}
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
