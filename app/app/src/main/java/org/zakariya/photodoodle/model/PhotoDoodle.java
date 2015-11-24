package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;

import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 11/23/15.
 */
public class PhotoDoodle extends IncrementalInputStrokeDoodle {

	private static final String TAG = "PhotoDoodle";

	@State
	Bitmap photo;

	private Paint photoPaint;

	public PhotoDoodle(Context context) {
		super(context);

		photoPaint = new Paint();
		photoPaint.setAntiAlias(true);
		photoPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void clear() {
		setPhoto(null);
		super.clear();
	}

	@Override
	public void draw(Canvas canvas) {

		if (photo != null) {
			// draw the photo, centered
			int photoLeft = (int) Math.round(getWidth() / 2 - photo.getWidth() / 2);
			int photoTop = (int) Math.round(getHeight() / 2 - photo.getHeight() / 2);
			canvas.drawBitmap(photo, photoLeft, photoTop, photoPaint);
		}

		super.draw(canvas);
	}

	public Bitmap getPhoto() {
		return photo;
	}

	public void setPhoto(Bitmap photo) {
		this.photo = photo;
		setBackgroundColor(photo != null ? 0x0 : 0xFFFFFFFF);
		invalidate();
	}
}
