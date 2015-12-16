package org.zakariya.doodle.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import org.zakariya.doodle.model.Doodle;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleView extends View {

	private static final String TAG = "DoodleView";
	private Doodle doodle;

	public DoodleView(Context context) {
		super(context);
	}

	public DoodleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Doodle getDoodle() {
		return doodle;
	}

	public void setDoodle(Doodle doodle) {
		this.doodle = doodle;
		doodle.setDoodleView(this);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				doodle.resize(getWidth(), getHeight());
			}
		});
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (doodle != null) {
			doodle.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		return doodle != null && doodle.onTouchEvent(event);
	}

}
