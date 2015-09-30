package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
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
import org.zakariya.photodoodle.geom.PointFUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

/**
 * Created by shamyl on 9/22/15.
 */
public class LineTessellationDoodle extends Doodle {

	private static final String TAG = "LineTessellationDoodle";
	private static final String FILE = "LineTessellationDoodle.dat";

	private InvalidationDelegate invalidationDelegate;
	private CircleLine circleLine = new CircleLine();
	private Paint linePaint, handlePaint, pathPaint;
	private Circle draggingPoint = null;
	private Context context;
	private CircleLineTessellator tessellator;
	private Path path = new Path();

	public LineTessellationDoodle(Context context) {
		this.context = context;
		this.tessellator = new CircleLineTessellator();

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setColor(0xFF00FF00);
		linePaint.setStrokeWidth(1);
		linePaint.setStyle(Paint.Style.STROKE);

		float[] dashes = {2, 2};
		linePaint.setPathEffect(new DashPathEffect(dashes, 0));

		pathPaint = new Paint();
		pathPaint.setAntiAlias(true);
		pathPaint.setColor(0xFF000000);
		pathPaint.setStrokeWidth(1);
		pathPaint.setStyle(Paint.Style.STROKE);

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0x5533ffff);
		handlePaint.setStyle(Paint.Style.FILL);

		if (!loadPoints()) {
			circleLine.add(new Circle(new PointF(50, 100), 10));
			circleLine.add(new Circle(new PointF(350, 100), 20));
			circleLine.add(new Circle(new PointF(350, 300), 30));
			circleLine.add(new Circle(new PointF(50, 300), 20));
			circleLine.add(new Circle(new PointF(50, 400), 10));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		//outState.putParcelable("circleLine", circleLine);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		//circleLine = savedInstanceState.getParcelable("circleLine");
	}

	@Override
	public DoodleView.InputDelegate inputDelegate() {
		return new InputDelegate(this);
	}

	@Override
	public RectF getBoundingRect() {
		return circleLine.getBoundingRect();
	}

	@Override
	public void clear() {
		// nothing here
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);


		// draw lines connecting circles
		if (!circleLine.isEmpty()) {
			Circle circle = circleLine.get(0);
			Path path = new Path();
			path.moveTo(circle.position.x, circle.position.y);
			for (int i = 1; i < circleLine.size(); i++) {
				circle = circleLine.get(i);
				path.lineTo(circle.position.x, circle.position.y);
			}

			canvas.drawPath(path, linePaint);

			path = new Path();
			for (int i = 0; i < circleLine.size(); i++) {
				circle = circleLine.get(i);
				path.addOval(circle.getBoundingRect(), Path.Direction.CW);
			}

			canvas.drawPath(path, handlePaint);
		}

		if (path != null) {
			canvas.drawPath(path, pathPaint);
		}
	}

	@Override
	public void setInvalidationDelegate(InvalidationDelegate invalidationDelegate) {
		this.invalidationDelegate = invalidationDelegate;
	}

	@Override
	public InvalidationDelegate getInvalidationDelegate() {
		return this.invalidationDelegate;
	}

	void onTouchEventBegin(@NonNull MotionEvent event) {
		PointF point = new PointF(event.getX(), event.getY());
		float minDist2 = Float.MAX_VALUE;
		for (int i = 0; i < circleLine.size(); i++) {
			Circle circle = circleLine.get(i);
			float d2 = PointFUtil.distance2(point, circle.position);
			if (d2 < minDist2) {
				minDist2 = d2;
				draggingPoint = circle;
			}
		}

		// now, confirm closest match was inside handle
		if (PointFUtil.distance2(point, draggingPoint.position) > draggingPoint.radius * draggingPoint.radius) {
			draggingPoint = null;
		}
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		if (draggingPoint != null) {

			// update line
			draggingPoint.position.x = event.getX();
			draggingPoint.position.y = event.getY();
			circleLine.invalidateBoundingRect();

			// update tessellation
			path.reset();
			tessellator.tessellate(circleLine, path);
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

			oos.writeObject(circleLine);
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

			circleLine = (CircleLine) ois.readObject();
			Log.i(TAG, "Loaded " + circleLine.size() + " points from " + FILE);
			return circleLine.size() > 0;
		} catch (Exception e) {
			Log.e(TAG, "Error opening file: " + FILE + " e: " + e);
			return false;
		}
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<LineTessellationDoodle> weakDoodle;

		public InputDelegate(LineTessellationDoodle doodle) {
			this.weakDoodle = new WeakReference<>(doodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			LineTessellationDoodle doodle = weakDoodle.get();
			if (doodle != null) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						doodle.onTouchEventBegin(event);
						return true;
					case MotionEvent.ACTION_UP:
						doodle.onTouchEventEnd(event);
						return true;
					case MotionEvent.ACTION_MOVE:
						doodle.onTouchEventMove(event);
						return true;
				}
			}

			return false;
		}
	}

}
