package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.photodoodle.geom.InputStroke;

import java.lang.ref.WeakReference;

import icepick.Icepick;

/**
 * Created by shamyl on 10/14/15.
 */
public class IncrementalInputStrokeDoodle extends Doodle implements IncrementalInputStrokeTessellator.Listener {
	private static final String TAG = "RawInputStrokeDoodle";

	private static boolean DRAW_INVALIDATION_RECT = false;

	private Paint invalidationRectPaint, bitmapPaint;
	private RectF invalidationRect;
	private IncrementalInputStrokeTessellator incrementalInputStrokeTessellator;
	private Context context;
	private Bitmap bitmap;
	private Canvas bitmapCanvas;

	public IncrementalInputStrokeDoodle(Context context) {
		this.context = context;

		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		bitmap = Bitmap.createBitmap(1024,1024,config);
		bitmap.eraseColor(0x0);
		bitmapCanvas = new Canvas(bitmap);

		invalidationRectPaint = new Paint();
		invalidationRectPaint.setAntiAlias(true);
		invalidationRectPaint.setColor(0xFFFF0000);
		invalidationRectPaint.setStrokeWidth(1);
		invalidationRectPaint.setStyle(Paint.Style.STROKE);

		bitmapPaint = new Paint();
		bitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

		setBrush(new Brush(0xFF000000, 1, 1, 100, false));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public RectF getBoundingRect() {
		if (incrementalInputStrokeTessellator != null) {
			return incrementalInputStrokeTessellator.getBoundingRect();
		}
		return null;
	}

	@Override
	public void clear() {
		incrementalInputStrokeTessellator = null;
		bitmap.eraseColor(0x0);
		getInvalidationDelegate().invalidate();
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFddddFF);
		canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);


		if (incrementalInputStrokeTessellator != null && !getBrush().isEraser()) {
			Path path = incrementalInputStrokeTessellator.getLivePath();
			if (path != null && !path.isEmpty()) {
				canvas.drawPath(path, getBrush().getPaint());
			}
		}

		// draw the invalidation rect
		if (DRAW_INVALIDATION_RECT) {
			canvas.drawRect(invalidationRect != null ? invalidationRect : getInvalidationDelegate().getBounds(), invalidationRectPaint);
		}

		invalidationRect = null;
	}

	@Override
	public DoodleView.InputDelegate inputDelegate() {
		return new InputDelegate(this);
	}

	@Override
	public void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect) {
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public void onLivePathModified(Path path, RectF rect) {
		if (getBrush().isEraser()) {
			bitmapCanvas.drawPath(path,getBrush().getPaint());
		}

		invalidationRect = rect;
		//getInvalidationDelegate().invalidate(rect);
		getInvalidationDelegate().invalidate();
	}

	@Override
	public void onNewStaticPathAvailable(Path path, RectF rect) {

		// draw path into bitmapCanvas
		bitmapCanvas.drawPath(path,getBrush().getPaint());

		invalidationRect = rect;
		//getInvalidationDelegate().invalidate(rect);
		getInvalidationDelegate().invalidate();
	}

	@Override
	public float getInputStrokeOptimizationThreshold() {
		return 7;
	}

	@Override
	public float getStrokeMinWidth() {
		return getBrush().getMinWidth();
	}

	@Override
	public float getStrokeMaxWidth() {
		return getBrush().getMaxWidth();
	}

	@Override
	public float getStrokeMaxVelDPps() {
		return getBrush().getMaxWidthDpPs();
	}

	private void onTouchEventBegin(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator = new IncrementalInputStrokeTessellator(this);
		incrementalInputStrokeTessellator.add(event.getX(), event.getY());
	}

	private void onTouchEventMove(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator.add(event.getX(), event.getY());
	}

	private void onTouchEventEnd() {
		incrementalInputStrokeTessellator.finish();
	}

	private static class InputDelegate implements DoodleView.InputDelegate {

		private WeakReference<IncrementalInputStrokeDoodle> weakDoodle;

		public InputDelegate(IncrementalInputStrokeDoodle doodle) {
			this.weakDoodle = new WeakReference<>(doodle);
		}

		@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
			IncrementalInputStrokeDoodle doodle = weakDoodle.get();
			if (doodle != null) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						doodle.onTouchEventBegin(event);
						return true;
					case MotionEvent.ACTION_UP:
						doodle.onTouchEventEnd();
						return true;
					case MotionEvent.ACTION_MOVE:
						doodle.onTouchEventMove(event);
						return true;
				}
			}

			return false;
		}
	}
}
