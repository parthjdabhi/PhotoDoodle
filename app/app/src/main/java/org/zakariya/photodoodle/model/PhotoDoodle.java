package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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

	;

	private static final String TAG = "PhotoDoodle";
	private static final String STATE_BITMAP = "bitmap";

	Bitmap photo;

	private Matrix photoMatrix;
	private Paint photoPaint;
	private Paint debugPaint;

	@State
	boolean drawDebugPositioningOverlay = false;

	@State
	int interactionMode = InteractionMode.DRAW.ordinal();

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
		setPhoto(null);
		super.clear();
	}

	@Override
	public void draw(Canvas canvas) {

		if (photo != null) {
			canvas.save();
			canvas.concat(canvasToScreenMatrix);
			canvas.concat(photoMatrix);
			canvas.drawBitmap(photo, 0, 0, photoPaint);

			if (drawDebugPositioningOverlay) {
				Paint dp = getDebugPaint();
				dp.setColor(0xFF00FFFF); // cyan
				dp.setStrokeWidth(8);
				canvas.drawRect(0, 0, photo.getWidth(), photo.getHeight(), dp);
				canvas.drawLine(0, 0, photo.getWidth(), photo.getHeight(), dp);
				canvas.drawLine(photo.getWidth(), 0, 0, photo.getHeight(), dp);
			}

			canvas.restore();
		}

		super.draw(canvas);

		if (drawDebugPositioningOverlay) {
			canvas.save();
			canvas.concat(canvasToScreenMatrix);

			Paint dp = getDebugPaint();
			dp.setColor(0xFFFF9900); // yellow
			dp.setStrokeWidth(4);
			canvas.drawRect(-CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, CANVAS_SIZE, dp);
			canvas.drawLine(-CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, CANVAS_SIZE, dp);
			canvas.drawLine(CANVAS_SIZE, -CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, dp);

			canvas.restore();
		}
	}

	@Override
	protected void onTouchEventBegin(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
				break;
			case DRAW:
				super.onTouchEventBegin(event);
		}
	}


	protected void onTouchEventMove(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
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

	public boolean isDrawDebugPositioningOverlay() {
		return drawDebugPositioningOverlay;
	}

	public void setDrawDebugPositioningOverlay(boolean drawDebugPositioningOverlay) {
		this.drawDebugPositioningOverlay = drawDebugPositioningOverlay;
	}

	public InteractionMode getInteractionMode() {
		return InteractionMode.values()[interactionMode];
	}

	public void setInteractionMode(InteractionMode interactionMode) {
		this.interactionMode = interactionMode.ordinal();
	}

	private Paint getDebugPaint() {
		if (debugPaint == null) {
			debugPaint = new Paint();
			debugPaint.setStyle(Paint.Style.STROKE);
			debugPaint.setStrokeWidth(8);
			debugPaint.setAntiAlias(true);
			debugPaint.setColor(0xFF00FFFF);
		}
		return debugPaint;
	}

	private void updatePhotoMatrix() {

		float minPhotoSize = Math.min(photo.getWidth(), photo.getHeight());
		float scale = CANVAS_SIZE * 2 / minPhotoSize;

		photoMatrix = new Matrix();
		photoMatrix.preScale(scale, scale);
		photoMatrix.preTranslate(-photo.getWidth() / 2, -photo.getHeight() / 2);
	}
}
