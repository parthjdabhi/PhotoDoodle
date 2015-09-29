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
import android.util.Pair;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.LinePoint;
import org.zakariya.photodoodle.geom.LineTessellator;
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
	private ArrayList<LinePoint> points = new ArrayList<>();
	private Paint linePaint, handlePaint, vectorPaint;
	private LinePoint draggingPoint = null;
	private Context context;
	private LineTessellator tessellator;

	public LineTessellationDoodle(Context context) {
		this.context = context;
		this.tessellator = new LineTessellator();

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setColor(0xFF000000);
		linePaint.setStrokeWidth(1);
		linePaint.setStyle(Paint.Style.STROKE);

		vectorPaint= new Paint();
		vectorPaint.setAntiAlias(true);
		vectorPaint.setColor(0xFFFF0000);
		vectorPaint.setStrokeWidth(2);
		vectorPaint.setStyle(Paint.Style.STROKE);

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0x5533ffff);
		handlePaint.setStyle(Paint.Style.FILL);

		if (!loadPoints()) {
			points.add(new LinePoint(new PointF(50, 100), 10));
			points.add(new LinePoint(new PointF(350, 100), 20));
			points.add(new LinePoint(new PointF(350, 300), 30));
			points.add(new LinePoint(new PointF(50, 300), 20));
			points.add(new LinePoint(new PointF(50, 400), 10));
			computeTangents();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		LinePoint[] linePoints = new LinePoint[points.size()];
		outState.putParcelableArray("points", (LinePoint[]) points.toArray(linePoints));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		LinePoint[] ps = (LinePoint[]) savedInstanceState.getParcelableArray("points");
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
			LinePoint p = points.get(0);
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
			LinePoint p = points.get(0);
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

			path = new Path();
			for (int i = 0; i < points.size(); i++ ) {
				p = points.get(i);
				path.moveTo(p.position.x, p.position.y);
				path.lineTo(p.position.x + p.tangent.x * p.halfSize,p.position.y + p.tangent.y * p.halfSize);
			}

			canvas.drawPath(path, vectorPaint);
		}

		tessellator.tessellate(points, null, canvas);
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
			LinePoint cp = points.get(i);
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
			computeTangents();
		}
		getInvalidationDelegate().invalidate(getBoundingRect());
	}

	void onTouchEventEnd(@NonNull MotionEvent event) {
		draggingPoint = null;
		getInvalidationDelegate().invalidate(getBoundingRect());
		save();
	}

	private void computeTangents() {
		if (points.size() < 3) {
			return;
		}

		for (int i = 0, N = points.size(); i < N; i++) {
			if (i == 0) {
				LinePoint a = points.get(i);
				LinePoint b = points.get(i + 1);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				a.tangent = dir.first;
			} else if (i == N - 1) {
				LinePoint b = points.get(i);
				LinePoint a = points.get(i - 1);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				b.tangent = dir.first;
			} else {
				LinePoint a = points.get(i - 1);
				LinePoint b = points.get(i);
				LinePoint c = points.get(i + 1);

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

	private void save() {
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
					LinePoint cp = (LinePoint) ois.readObject();
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
