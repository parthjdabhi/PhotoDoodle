package org.zakariya.photodoodle.model;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;

import org.zakariya.photodoodle.DoodleView;

/**
 * Created by shamyl on 8/11/15.
 */
public abstract class Doodle {

	public interface InvalidationDelegate {
		/**
		 * Queue a complete redraw of the doodle
		 */
		void invalidate();

		/**
		 * Queue a redraw of a sub rect of the doodle
		 * @param rect the region which needs to be redrawn
		 */
		void invalidate(RectF rect);
	}

	public abstract DoodleView.InputDelegate inputDelegate();
	public abstract RectF getBoundingRect();
	public abstract void clear();
	public abstract void draw(Canvas canvas);
	public abstract void setInvalidationDelegate(InvalidationDelegate invalidationDelegate);
	public abstract InvalidationDelegate getInvalidationDelegate();

	public abstract void onSaveInstanceState(Bundle outState);
	public abstract void onCreate(Bundle savedInstanceState);

}
