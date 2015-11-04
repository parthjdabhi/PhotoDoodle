package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import org.zakariya.photodoodle.view.DoodleView;

import java.lang.ref.WeakReference;

/**
 * Created by shamyl on 8/11/15.
 */
public abstract class Doodle {

	private Brush brush;
	private int width, height;
	WeakReference<DoodleView> doodleViewWeakReference;

	public void setDoodleView(DoodleView doodleView) {
		doodleViewWeakReference = new WeakReference<>(doodleView);
	}

	@Nullable
	public DoodleView getDoodleView() {
		if (doodleViewWeakReference != null) {
			return doodleViewWeakReference.get();
		}

		return null;
	}

	public void invalidate() {
		DoodleView dv = getDoodleView();
		if (dv != null) {
			dv.invalidate();
		}
	}

	public void invalidate(RectF rect) {
		DoodleView dv = getDoodleView();
		if (dv != null) {
			dv.invalidate((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
		}
	}

	public abstract RectF getBoundingRect();

	public abstract void clear();

	public abstract void draw(Canvas canvas);

	public void resize(int newWidth, int newHeight) {
		width = newWidth;
		height = newHeight;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setBrush(Brush brush) {
		this.brush = brush;
	}

	public Brush getBrush() {
		return brush;
	}

	public void onSaveInstanceState(Bundle outState) {
	}

	public void onCreate(Bundle savedInstanceState) {
	}

	public boolean onTouchEvent(@NonNull MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				onTouchEventBegin(event);
				return true;
			case MotionEvent.ACTION_UP:
				onTouchEventEnd(event);
				return true;
			case MotionEvent.ACTION_MOVE:
				onTouchEventMove(event);
				return true;
		}

		return false;
	}

	protected abstract void onTouchEventBegin(@NonNull MotionEvent event);

	protected abstract void onTouchEventMove(@NonNull MotionEvent event);

	protected abstract void onTouchEventEnd(@NonNull MotionEvent event);

}
