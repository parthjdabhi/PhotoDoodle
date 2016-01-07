package org.zakariya.doodle.model;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import org.zakariya.doodle.view.DoodleView;

import java.lang.ref.WeakReference;

import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 8/11/15.
 */
public abstract class Doodle {

	@State
	boolean dirty = false;

	private Brush brush;
	private int width, height;
	private int backgroundColor = 0xFFFFFFFF;
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

	public void clear() {
		markDirty();
	}

	public abstract void draw(Canvas canvas);

	public void draw(Canvas canvas, int width, int height) {
		int oldWidth = getWidth();
		int oldHeight = getHeight();
		if (oldWidth != width || oldHeight != height) {
			resize(width, height);
			draw(canvas);
			if (oldWidth > 0 && oldHeight > 0) {
				resize(oldWidth, oldHeight);
			}
		} else {
			draw(canvas);
		}
	}

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

	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
	}

	public boolean onTouchEvent(@NonNull MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				markDirty();
				onTouchEventBegin(event);
				return true;
			case MotionEvent.ACTION_UP:
				markDirty();
				onTouchEventEnd(event);
				return true;
			case MotionEvent.ACTION_MOVE:
				markDirty();
				onTouchEventMove(event);
				return true;
		}

		return false;
	}

	protected abstract void onTouchEventBegin(@NonNull MotionEvent event);

	protected abstract void onTouchEventMove(@NonNull MotionEvent event);

	protected abstract void onTouchEventEnd(@NonNull MotionEvent event);

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	/**
	 * Mark that this Doodle was modified in some way (drawing, etc)
	 */
	public void markDirty() {
		dirty = true;
	}

	/**
	 * Set whether this Doodle has been modified
	 * @param dirty if true, mark that this Doodle has been modified since it was loaded
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Check if this doodle was modified
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}
}
