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
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.InputStrokeTessellator;
import org.zakariya.photodoodle.geom.PointFUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

/**
 * Created by shamyl on 10/18/15.
 */
public class InputStrokeTessellationDoodle extends Doodle {

	private static final String TAG = "InputStrokeTessDoodle";
	private static final String FILE = "InputStrokeTessellationDoodle.dat";

	private InputStroke inputStroke = new InputStroke();
	private InputStrokeTessellator inputStrokeTessellator;
	private Path inputStrokeTessellatedPath;
	private InvalidationDelegate invalidationDelegate;
	private Paint handlePaint, tessellatedInputStrokePathFillPaint, tessellatedInputStrokePathStrokePaint;
	private InputStroke.Point draggingPoint;
	private Context context;

	public InputStrokeTessellationDoodle(Context context) {
		this.context = context;

		handlePaint = new Paint();
		handlePaint.setAntiAlias(true);
		handlePaint.setColor(0xFF00FFFF);
		handlePaint.setStyle(Paint.Style.STROKE);

		tessellatedInputStrokePathFillPaint = new Paint();
		tessellatedInputStrokePathFillPaint.setAntiAlias(true);
		tessellatedInputStrokePathFillPaint.setColor(0xFFAAAAAA);
		tessellatedInputStrokePathFillPaint.setStrokeWidth(1);
		tessellatedInputStrokePathFillPaint.setStyle(Paint.Style.FILL);

		tessellatedInputStrokePathStrokePaint = new Paint();
		tessellatedInputStrokePathStrokePaint.setAntiAlias(true);
		tessellatedInputStrokePathStrokePaint.setColor(0xFF000000);
		tessellatedInputStrokePathStrokePaint.setStrokeWidth(1);
		tessellatedInputStrokePathStrokePaint.setStyle(Paint.Style.STROKE);


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

			tessellate();
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

		if (inputStrokeTessellatedPath != null) {
			canvas.drawPath(inputStrokeTessellatedPath, tessellatedInputStrokePathFillPaint);
			canvas.drawPath(inputStrokeTessellatedPath, tessellatedInputStrokePathStrokePaint);
		}

		if (inputStroke != null) {
			// draw dots representing the input points
			for (int i = 0, N = inputStroke.size(); i < N; i++) {
				InputStroke.Point point = inputStroke.get(i);
				PointF tangent = inputStroke.getTangent(i);
				float radius = inputStrokeTessellator.getRadiusForInputStrokePoint(i);
				canvas.drawCircle(point.position.x, point.position.y, radius, handlePaint);

				PointF t = new PointF(point.position.x + tangent.x * 2 * radius, point.position.y + tangent.y * 2 * radius);
				canvas.drawLine(point.position.x, point.position.y, t.x, t.y, handlePaint);
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
		int index = -1;
		for (int i = 0; i < inputStroke.size(); i++) {
			InputStroke.Point p = inputStroke.get(i);
			float d2 = PointFUtil.distance2(point, p.position);
			if (d2 < minDist2) {
				minDist2 = d2;
				draggingPoint = p;
				index = i;
			}
		}

		// now, confirm closest match was close enough
		if (index >= 0 ){
			float radius = inputStrokeTessellator.getRadiusForInputStrokePoint(index);
			if (minDist2 > (radius*radius)) {
				draggingPoint = null;
			}
		}
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		if (draggingPoint != null) {

			// update line
			draggingPoint.position.x = event.getX();
			draggingPoint.position.y = event.getY();
			inputStroke.invalidate();
			tessellate();
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
			tessellate();
			Log.i(TAG, "Loaded " + inputStroke.size() + " points from " + FILE);
			return inputStroke.size() > 0;
		} catch (Exception e) {
			Log.e(TAG, "Error opening file: " + FILE + " e: " + e);
			return false;
		}
	}

	private void tessellate() {
		inputStrokeTessellator = new InputStrokeTessellator(inputStroke,4,60,200);
		inputStrokeTessellatedPath = inputStrokeTessellator.tessellate();
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<InputStrokeTessellationDoodle> weakDoodle;

		public InputDelegate(InputStrokeTessellationDoodle lineDoodle) {
			this.weakDoodle = new WeakReference<>(lineDoodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			InputStrokeTessellationDoodle doodle = weakDoodle.get();
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