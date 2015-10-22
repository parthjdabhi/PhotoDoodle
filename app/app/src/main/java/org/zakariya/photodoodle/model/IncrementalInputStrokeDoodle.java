package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.photodoodle.geom.InputStroke;

import java.lang.ref.WeakReference;
import java.util.Random;

import icepick.Icepick;

/**
 * Created by shamyl on 10/14/15.
 */
public class IncrementalInputStrokeDoodle extends Doodle implements IncrementalInputStrokeTessellator.Listener {
	private static final String TAG = "RawInputStrokeDoodle";

	private InvalidationDelegate invalidationDelegate;
	private Paint invalidationRectPaint, inputStrokePaint, strokePaint;
	private RectF invalidationRect;
	private IncrementalInputStrokeTessellator incrementalInputStrokeTessellator;

	public IncrementalInputStrokeDoodle() {
		invalidationRectPaint = new Paint();
		invalidationRectPaint.setAntiAlias(true);
		invalidationRectPaint.setColor(0xFFFF0000);
		invalidationRectPaint.setStrokeWidth(1);
		invalidationRectPaint.setStyle(Paint.Style.STROKE);

		inputStrokePaint = new Paint();
		inputStrokePaint.setAntiAlias(true);
		inputStrokePaint.setColor(0xFF00FF00);
		inputStrokePaint.setStrokeWidth(1);
		inputStrokePaint.setStyle(Paint.Style.STROKE);

		strokePaint = new Paint();
		strokePaint.setAntiAlias(true);
		strokePaint.setColor(0xFF666666);
		strokePaint.setStrokeWidth(1);
		strokePaint.setStyle(Paint.Style.FILL);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public RectF getBoundingRect() {
		if (incrementalInputStrokeTessellator != null) {
			return incrementalInputStrokeTessellator.getBoundingRect();
		}
		return null;
	}

	@Override
	public void clear() {
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFFFFFFF);


		if (incrementalInputStrokeTessellator != null) {

			// draw static paths in a repeatable random color sequence
			Random colorGenerator = new Random(12345);
			for (Path path : incrementalInputStrokeTessellator.getStaticPaths()) {
				int color = Color.argb(255,64 + colorGenerator.nextInt(128),64 + colorGenerator.nextInt(128),64 + colorGenerator.nextInt(128));
				strokePaint.setColor(color);
				canvas.drawPath(path,strokePaint);
			}


			Path path = incrementalInputStrokeTessellator.getLivePath();
			if (path != null && !path.isEmpty()) {

				strokePaint.setColor(0xFF0000FF);
				canvas.drawPath(path, strokePaint);

			} else {
				InputStroke inputStroke = incrementalInputStrokeTessellator.getInputStroke();
				if (inputStroke != null) {
					path = new Path();
					InputStroke.Point p = inputStroke.get(0);
					path.moveTo(p.position.x, p.position.y);
					for (int i = 1, N = inputStroke.size(); i < N; i++) {
						p = inputStroke.get(i);
						path.lineTo(p.position.x, p.position.y);
					}

					canvas.drawPath(path, inputStrokePaint);
				}
			}
		}

		// draw the invalidation rect
//		canvas.drawRect(invalidationRect != null ? invalidationRect : getInvalidationDelegate().getBounds(), invalidationRectPaint);
		invalidationRect = null;
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

	@Override
	public void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public void onLivePathModified(Path path, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public void onNewStaticPathAvailable(Path path, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public float getInputStrokeOptimizationThreshold() {
		return 3;
	}

	@Override
	public float getStrokeMinWidth() {
		return 1;
	}

	@Override
	public float getStrokeMaxWidth() {
		return 16;
	}

	@Override
	public float getStrokeMaxVelDPps() {
		return 700;
	}

	void renderTestStroke() {
		Log.i(TAG, "renderTestStroke");
		incrementalInputStrokeTessellator = new IncrementalInputStrokeTessellator(this);
		float x = 10, y = 50;
		for (; x < 310; x += 15, y += 10) {
			incrementalInputStrokeTessellator.add(x,y);
		}

		for (; x > 10; x -= 15, y += 10) {
			incrementalInputStrokeTessellator.add(x,y);
		}

		for (; x < 310; x += 15, y += 10) {
			incrementalInputStrokeTessellator.add(x,y);
		}
	}

	void onTouchEventBegin(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator = new IncrementalInputStrokeTessellator(this);
		incrementalInputStrokeTessellator.add(event.getX(), event.getY());
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator.add(event.getX(), event.getY());
	}

	void onTouchEventEnd() {
		incrementalInputStrokeTessellator.finish();
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<IncrementalInputStrokeDoodle> weakDoodle;

		public InputDelegate(IncrementalInputStrokeDoodle doodle) {
			this.weakDoodle = new WeakReference<>(doodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			IncrementalInputStrokeDoodle doodle = weakDoodle.get();
			if (doodle != null) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						doodle.onTouchEventBegin(event);
						return true;
					case MotionEvent.ACTION_UP:
						doodle.onTouchEventEnd();
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
