package org.zakariya.photodoodle;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleView extends View {

	private static final String TAG = "DoodleView";

	public DoodleView(Context context) {
		super(context);
	}

	public DoodleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		PointF point = new PointF(event.getX(),event.getY());
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.i(TAG, "ACTION_DOWN @ " + point);
				break;
			case MotionEvent.ACTION_MOVE:
				Log.i(TAG, "ACTION_MOVE @ " + point);
				break;
			case MotionEvent.ACTION_UP:
				Log.i(TAG, "ACTION_UP @ " + point);
				break;
			case MotionEvent.ACTION_CANCEL:
				Log.i(TAG, "ACTION_CANCEL @ " + point);
				break;
		}

		return true;
	}
}
