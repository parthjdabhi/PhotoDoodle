package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.photodoodle.geom.InputStroke;

import java.lang.ref.WeakReference;

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

			Path path = incrementalInputStrokeTessellator.getPath();
			if (path != null) {
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
		canvas.drawRect(invalidationRect != null ? invalidationRect : getInvalidationDelegate().getBounds(), invalidationRectPaint);
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
	public void onPathAvailable(Path path, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public float getInputStrokeAutoOptimizationThreshold() {
		return 2;
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
