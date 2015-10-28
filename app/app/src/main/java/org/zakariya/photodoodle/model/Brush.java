package org.zakariya.photodoodle.model;

import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * Created by shamyl on 10/28/15.
 */
public class Brush {

	private int color;
	private float minWidth;
	private float maxWidth;
	private float maxWidthDpPs;
	private boolean eraser;
	private Paint paint;

	public Brush(int color, float minWidth, float maxWidth, float maxWidthDpPs, boolean eraser) {
		this.color = color;
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
		this.maxWidthDpPs = maxWidthDpPs;
		this.eraser = eraser;

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
		paint.setXfermode(new PorterDuffXfermode(eraser ? PorterDuff.Mode.CLEAR : PorterDuff.Mode.SRC_OVER));
	}

	public int getColor() {
		return color;
	}

	public float getMinWidth() {
		return minWidth;
	}

	public float getMaxWidth() {
		return maxWidth;
	}

	public float getMaxWidthDpPs() {
		return maxWidthDpPs;
	}

	public boolean isEraser() {
		return eraser;
	}

	public Paint getPaint() {
		return paint;
	}
}
