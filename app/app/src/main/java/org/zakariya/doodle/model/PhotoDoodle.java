package org.zakariya.doodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.OutputStream;
import java.util.ArrayList;

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

	static final String TAG = "PhotoDoodle";
	static final int COOKIE = 0xD00D;

	@State
	byte[] photoJpegData;

	@State
	int interactionMode = InteractionMode.DRAW.ordinal();

	@State
	PointF userTranslationOnCanvas = new PointF();

	// transient data
	Bitmap photo;
	Matrix photoMatrix;
	Paint photoPaint;
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
		setPhotoJpegData(photoJpegData);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
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
				touchStartPosition = new PointF(
						event.getX() - (userTranslationOnCanvas.x * canvasToScreenScale),
						event.getY() - (userTranslationOnCanvas.y * canvasToScreenScale));
				break;
			case DRAW:
				super.onTouchEventBegin(event);
		}
	}

	public void serialize(OutputStream out) {
		Output output = new Output(out);

		Kryo kryo = new Kryo();
		kryo.writeObject(output, COOKIE);
		kryo.writeObject(output, drawingSteps);

		if (photoJpegData != null && photoJpegData.length > 0) {
			kryo.writeObject(output, true);
			kryo.writeObject(output, userTranslationOnCanvas);
			kryo.writeObject(output, photoJpegData);
		} else {
			// mark that we have no photo
			kryo.writeObject(output, false);
		}

		output.close();
	}

	public void inflate(InputStream in) throws InvalidObjectException {
		Input input = new Input(in);
		Kryo kryo = new Kryo();

		int cookie = kryo.readObject(input, Integer.class);
		if (cookie == COOKIE) {
			//noinspection unchecked
			drawingSteps = kryo.readObject(input, ArrayList.class);

			boolean hasPhoto = kryo.readObject(input, Boolean.class);
			if (hasPhoto) {
				userTranslationOnCanvas = kryo.readObject(input, PointF.class);
				setPhotoJpegData(kryo.readObject(input, byte[].class));
			}

			renderDrawingSteps();

		} else {
			throw new InvalidObjectException("Missing COOKIE header (0x" + Integer.toString(COOKIE, 16) + ")");
		}
	}

	protected void onTouchEventMove(@NonNull MotionEvent event) {
		switch (getInteractionMode()) {
			case PHOTO:
				userTranslationOnCanvas.x = (event.getX() - touchStartPosition.x) / canvasToScreenScale;
				userTranslationOnCanvas.y = (event.getY() - touchStartPosition.y) / canvasToScreenScale;
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
		setPhotoJpegData(null);
	}

	public void setPhotoJpegData(@Nullable byte[] photoData) {
		this.photoJpegData = photoData;
		if (this.photoJpegData != null && this.photoJpegData.length > 0) {
			photo = BitmapFactory.decodeByteArray(this.photoJpegData, 0, this.photoJpegData.length);
			setBackgroundColor(0x0);
			updatePhotoMatrix();
		} else {
			photo = null;
			photoJpegData = null;
			setBackgroundColor(0xFFFFFFFF);
		}

		invalidate();
	}

	public byte[] getPhotoJpegData() {
		return photoJpegData;
	}

	public Bitmap getPhoto() {
		return photo;
	}

	public void setPhoto(Bitmap photo) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		photo.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
		setPhotoJpegData(byteArrayOutputStream.toByteArray());
	}

	public InteractionMode getInteractionMode() {
		return InteractionMode.values()[interactionMode];
	}

	public void setInteractionMode(InteractionMode interactionMode) {
		this.interactionMode = interactionMode.ordinal();
	}

	void updatePhotoMatrix() {

		photoMatrix.reset();

		// this can happen during rotations before scaling matrices are configured
		if (canvasToScreenScale < 1e-3) {
			return;
		}

		if (photo != null) {
			float minPhotoSize = Math.min(photo.getWidth(), photo.getHeight());
			float maxPhotoSize = Math.max(photo.getWidth(), photo.getHeight());
			float photoScale = 1;
			switch (getScaleMode()) {
				case FIT:
					photoScale = CANVAS_SIZE * 2 / maxPhotoSize;
					break;
				case FILL:
					photoScale = CANVAS_SIZE * 2 / minPhotoSize;
					break;
			}

			photoMatrix.preTranslate(userTranslationOnCanvas.x, userTranslationOnCanvas.y);
			photoMatrix.preScale(photoScale, photoScale);
			photoMatrix.preTranslate(-photo.getWidth() / 2, -photo.getHeight() / 2);
		}

	}
}
