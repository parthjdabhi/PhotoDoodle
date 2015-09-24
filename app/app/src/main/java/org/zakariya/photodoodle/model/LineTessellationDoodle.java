package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.ControlPoint;
import org.zakariya.photodoodle.geom.PointFUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by shamyl on 9/22/15.
 */
public class LineTessellationDoodle extends Doodle {

	private static final String TAG = "LineTessellationDoodle";
	private static final String FILE = "LineTessellationDoodle.dat";

	private InvalidationDelegate invalidationDelegate;
	private ArrayList<ControlPoint> points = new ArrayList<>();
	private Paint linePaint;
	private Paint handlePaint;
	private ControlPoint draggingPoint = null;
	private Context context;
	private Handler debouncedSaveHandler;
	private Runnable debouncedSaveRunnable;

	public LineTessellationDoodle(Context context) {
		this.context = context;

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setColor(0xFF000000);
		linePaint.setStrokeWidth(1);
		linePaint.setStyle(Paint.Style.STROKE);

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0x5533ffff);
		handlePaint.setStyle(Paint.Style.FILL);

		if (!loadPoints()) {
			points.add(new ControlPoint(new PointF(50, 100), 10));
			points.add(new ControlPoint(new PointF(350, 100), 20));
			points.add(new ControlPoint(new PointF(350, 300), 30));
			points.add(new ControlPoint(new PointF(50, 300), 20));
			points.add(new ControlPoint(new PointF(50, 400), 10));
		}

		debouncedSaveHandler = new Handler(Looper.getMainLooper());

		final WeakReference<LineTessellationDoodle> weakThis = new WeakReference<>(this);
		debouncedSaveRunnable = new Runnable() {
			@Override
			public void run() {
				LineTessellationDoodle strongThis = weakThis.get();
				if (strongThis != null) {
					strongThis.savePoints();
				}
			}
		};
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		ControlPoint[] controlPoints = new ControlPoint[points.size()];
		outState.putParcelableArray("points", (ControlPoint[]) points.toArray(controlPoints));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		ControlPoint[] ps = (ControlPoint[]) savedInstanceState.getParcelableArray("points");
		if (ps != null) {
			points.clear();
			points.addAll(Arrays.asList(ps));
		}
	}

	@Override
	public DoodleView.InputDelegate inputDelegate() {
		return new InputDelegate(this);
	}

	@Override
	public RectF getBoundingRect() {
		if (!points.isEmpty()) {
			ControlPoint p = points.get(0);
			RectF boundingRect = p.getBoundingRect();

			for (int i = 1; i < points.size(); i++) {
				boundingRect.union(points.get(i).getBoundingRect());
			}

			return boundingRect;
		} else {
			return new RectF();
		}
	}

	@Override
	public void clear() {
		// nothing here
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);


		// draw lines connecting control points
		if (!points.isEmpty()) {
			ControlPoint p = points.get(0);
			Path path = new Path();
			path.moveTo(p.position.x, p.position.y);
			for (int i = 1; i < points.size(); i++) {
				p = points.get(i);
				path.lineTo(p.position.x, p.position.y);
			}

			canvas.drawPath(path, linePaint);

			path = new Path();
			for (int i = 0; i < points.size(); i++) {
				p = points.get(i);
				path.addOval(p.getBoundingRect(), Path.Direction.CW);
			}

			canvas.drawPath(path, handlePaint);
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
		for (int i = 0; i < points.size(); i++) {
			ControlPoint cp = points.get(i);
			float d2 = PointFUtil.distance2(point, cp.position);
			if (d2 < minDist2) {
				minDist2 = d2;
				draggingPoint = cp;
			}
		}

		// now, confirm closest match was inside handle
		if (PointFUtil.distance2(point, draggingPoint.position) > draggingPoint.halfSize * draggingPoint.halfSize) {
			draggingPoint = null;
		}
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		if (draggingPoint != null) {
			draggingPoint.position.x = event.getX();
			draggingPoint.position.y = event.getY();
		}
		getInvalidationDelegate().invalidate(getBoundingRect());
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		draggingPoint = null;
		getInvalidationDelegate().invalidate(getBoundingRect());
		save();
	}

	private void save() {
		debouncedSaveHandler.removeCallbacks(debouncedSaveRunnable);
		debouncedSaveHandler.postDelayed(debouncedSaveRunnable, 500);
	}

	private void savePoints() {
		try {
			context.deleteFile(FILE);
			FileOutputStream fos = context.openFileOutput(FILE, Context.MODE_PRIVATE);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			int count = points.size();
			oos.writeInt(count);
			for (int i = 0; i < count; i++) {
				oos.writeObject(points.get(i));
			}
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

			int count = ois.readInt();
			if (count > 0) {
				points.clear();

				for (int i = 0; i < count; i++) {
					ControlPoint cp = (ControlPoint) ois.readObject();
					points.add(cp);
				}

				if (getInvalidationDelegate() != null) {
					getInvalidationDelegate().invalidate(getBoundingRect());
				}

				Log.i(TAG, "Loaded " + count + " points from " + FILE);
				return true;
			} else {
				return false;
			}
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
