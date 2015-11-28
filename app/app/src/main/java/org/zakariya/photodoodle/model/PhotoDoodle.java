package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 11/23/15.
 */
public class PhotoDoodle extends IncrementalInputStrokeDoodle {

	public enum InteractionMode {
		PHOTO,
		DRAW
	}

	private static final String TAG = "PhotoDoodle";
	private static final String STATE_BITMAP = "bitmap";

	Bitmap photo;

	private Matrix photoMatrix;
	private Paint photoPaint;

	@State
	int interactionMode = InteractionMode.DRAW.ordinal();

	@State
	PointF userTranslation = new PointF();

	PointF touchStartPosition;

	public PhotoDoodle(Context context) {
		super(context);

		photoMatrix = new Matrix();

		photoPaint = new Paint();
		photoPaint.setAntiAlias(true);
		photoPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		setPhoto((Bitmap) savedInstanceState.getParcelable(STATE_BITMAP));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);

		if (photo != null) {
			outState.putParcelable(STATE_BITMAP, photo);
		}
	}

	@Override
	public void resize(int newWidth, int newHeight) {
		super.resize(newWidth, newHeight);
		if (photo != null) {
			updatePhotoMatrix();
		}
	}

	@Override
	public void clear() {
		clearPhoto();
		super.clear();
	}

	@Override
	public void draw(Canvas canvas) {

		if (photo != null) {
			canvas.save();
			canvas.concat(canvasToScreenMatrix);
			canvas.concat(photoMatrix);
			canvas.drawBitmap(photo, 0, 0, photoPaint);

			// draw the photo bounds
			if (isDrawDebugPositioningOverlay()) {
				Paint dp = getDebugPositioningPaint();
				dp.setColor(0xFF00FFFF); // cyan
				dp.setStrokeWidth(8);
				canvas.drawRect(0, 0, photo.getWidth(), photo.getHeight(), dp);
				canvas.drawLine(0, 0, photo.getWidth(), photo.getHeight(), dp);
				canvas.drawLine(photo.getWidth(), 0, 0, photo.getHeight(), dp);
			}

			canvas.restore();
		}

		super.draw(canvas);
	}

	@Override
	protected void onTouchEventBegin(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
				touchStartPosition = new PointF(event.getX() - userTranslation.x, event.getY() - userTranslation.y);
				break;
			case DRAW:
				super.onTouchEventBegin(event);
		}
	}


	protected void onTouchEventMove(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
				userTranslation.x = event.getX() - touchStartPosition.x;
				userTranslation.y = event.getY() - touchStartPosition.y;
				updatePhotoMatrix();
				invalidate();
				break;
			case DRAW:
				super.onTouchEventMove(event);
		}
	}


	protected void onTouchEventEnd(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
				break;
			case DRAW:
				super.onTouchEventEnd(event);
		}
	}

	public void clearPhoto() {
		setPhoto(null);
	}

	public Bitmap getPhoto() {
		return photo;
	}

	public void setPhoto(Bitmap photo) {
		this.photo = photo;
		setBackgroundColor(photo != null ? 0x0 : 0xFFFFFFFF);
		if (photo != null) {
			updatePhotoMatrix();
		}
		invalidate();
	}

	public InteractionMode getInteractionMode() {
		return InteractionMode.values()[interactionMode];
	}

	public void setInteractionMode(InteractionMode interactionMode) {
		this.interactionMode = interactionMode.ordinal();
	}


	private void updatePhotoMatrix() {

		float minPhotoSize = Math.min(photo.getWidth(), photo.getHeight());
		float photoScale = CANVAS_SIZE * 2 / minPhotoSize;
		float adjustX = userTranslation.x / canvasToScreenScale;
		float adjustY = userTranslation.y / canvasToScreenScale;

		photoMatrix.reset();
		photoMatrix.preTranslate(adjustX,adjustY);
		photoMatrix.preScale(photoScale, photoScale);
		photoMatrix.preTranslate(-photo.getWidth() / 2, -photo.getHeight() / 2);
	}
}
