package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;

import org.zakariya.photodoodle.DoodleView;

/**
 * Created by shamyl on 8/11/15.
 */
public abstract class Doodle {

	private Brush brush;
	private InvalidationDelegate invalidationDelegate;

	public interface InvalidationDelegate {
		/**
		 * Queue a complete redraw of the doodle
		 */
		void invalidate();

		/**
		 * Queue a redraw of a sub rect of the doodle
		 *
		 * @param rect the region which needs to be redrawn
		 */
		void invalidate(RectF rect);

		/**
		 * Get the full bounds of the surface which will be invalidated
		 *
		 * @return RectF describing full bounds of view/surface
		 */
		RectF getBounds();
	}

	public abstract DoodleView.InputDelegate inputDelegate();

	public abstract RectF getBoundingRect();

	public abstract void clear();

	public abstract void draw(Canvas canvas);

	public abstract void resize(int newWidth, int newHeight);

	public void setInvalidationDelegate(InvalidationDelegate invalidationDelegate) {
		this.invalidationDelegate = invalidationDelegate;
	}

	public InvalidationDelegate getInvalidationDelegate() {
		return this.invalidationDelegate;
	}

	public void setBrush(Brush brush) {
		this.brush = brush;
	}

	public Brush getBrush() {
		return brush;
	}

	public void onSaveInstanceState(Bundle outState){}
	public void onCreate(Bundle savedInstanceState){}

}
