package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.IncrementalStrokeBuilder;
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.Stroke;

import java.lang.ref.WeakReference;

import icepick.Icepick;

/**
 * Created by shamyl on 10/14/15.
 */
public class IncrementalStrokeDoodle extends Doodle implements IncrementalStrokeBuilder.StrokeConsumer {
	private static final String TAG = "LineDoodle";

	private static final float MinStrokeThickness = 2;
	private static final float MaxStrokeThickness = 16;
	private static final float MaxStrokeVel = 700;

	private InvalidationDelegate invalidationDelegate;
	private Paint invalidationRectPaint, inputStrokePaint, strokePaint;
	private RectF invalidationRect;
	private IncrementalStrokeBuilder strokeBuilder;
	private Path inputStrokePath;

	public IncrementalStrokeDoodle() {
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
		strokePaint.setColor(0xFF999999);
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
		if (strokeBuilder != null) {
			return strokeBuilder.getStroke().getBoundingRect();
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

		if (inputStrokePath != null) {
			canvas.drawPath(inputStrokePath, inputStrokePaint);
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

		for (int i = startIndex; i <= endIndex; i++) {
			PointF p = inputStroke.getPoints().get(i).position;
			if (inputStrokePath.isEmpty()) {
				inputStrokePath.moveTo(p.x, p.y);
			} else {
				inputStrokePath.lineTo(p.x, p.y);
			}
		}
	}

	@Override
	public void onStrokeModified(Stroke stroke, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	void onTouchEventBegin(@NonNull MotionEvent event) {
		inputStrokePath = new Path();
		strokeBuilder = new IncrementalStrokeBuilder(this);
		strokeBuilder.add(event.getX(), event.getY());
	}

	void onTouchEventMove(@NonNull MotionEvent event) {
		strokeBuilder.add(event.getX(), event.getY());
	}

	void onTouchEventEnd() {
		strokeBuilder.finish();
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<IncrementalStrokeDoodle> weakDoodle;

		public InputDelegate(IncrementalStrokeDoodle doodle) {
			this.weakDoodle = new WeakReference<>(doodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			IncrementalStrokeDoodle doodle = weakDoodle.get();
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
