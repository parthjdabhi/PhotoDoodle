package org.zakariya.photodoodle;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleView extends View {

	/**
	 * DoodleView forwards touch events to a delegate which translates them into appropriate drawing commands for the current drawing operation.
	 */
	public interface InputDelegate {
		boolean onTouchEvent(@NonNull MotionEvent event);
	}

	public interface DrawDelegate {
		void draw(Canvas canvas);
	}

	private static final String TAG = "DoodleView";
	private InputDelegate inputDelegate;
	private DrawDelegate drawDelegate;

	public DoodleView(Context context) {
		super(context);
	}

	public DoodleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (drawDelegate != null) {
			drawDelegate.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		return inputDelegate != null && inputDelegate.onTouchEvent(event);
	}

	public InputDelegate getInputDelegate() {
		return inputDelegate;
	}

	public void setInputDelegate(InputDelegate inputDelegate) {
		this.inputDelegate = inputDelegate;
	}

	public DrawDelegate getDrawDelegate() {
		return drawDelegate;
	}

	public void setDrawDelegate(DrawDelegate drawDelegate) {
		this.drawDelegate = drawDelegate;
	}
}
