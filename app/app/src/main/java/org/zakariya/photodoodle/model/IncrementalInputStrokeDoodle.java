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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import org.zakariya.photodoodle.DoodleView;
import org.zakariya.photodoodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.InputStrokeTessellator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 10/14/15.
 */
public class IncrementalInputStrokeDoodle extends Doodle implements IncrementalInputStrokeTessellator.Listener {
	private static final String TAG = "IncInptStrokeDoodle";

	private static boolean DRAW_INVALIDATION_RECT = true;

	private Paint invalidationRectPaint, bitmapPaint;
	private RectF invalidationRect;
	private IncrementalInputStrokeTessellator incrementalInputStrokeTessellator;
	private Context context;
	private Canvas bitmapCanvas;

	Bitmap bitmap;

	@State
	ArrayList<IntermediateDrawingStep> drawingSteps = new ArrayList<>();

	public IncrementalInputStrokeDoodle(Context context) {
		this.context = context;

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
	public void onCreate(Bundle savedInstanceState) {
		Icepick.restoreInstanceState(this, savedInstanceState);

		if (drawingSteps != null) {
			Log.i(TAG, "onCreate - drawingSteps.size: " + drawingSteps.size());
		} else {
			Log.i(TAG, "onCreate - drawingSteps is NULL");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
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
		drawingSteps.clear();
		bitmap.eraseColor(0x0);
		getInvalidationDelegate().invalidate();
	}

	@Override
	public void draw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(0xFFddddFF);

		// draw bitmap centered
		int left = getWidth()/2 - bitmap.getWidth()/2;
		int top = getHeight()/2 - bitmap.getHeight()/2;
		canvas.drawBitmap(bitmap, left, top, bitmapPaint);


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
	public void resize(int newWidth, int newHeight) {
		super.resize(newWidth,newHeight);

		if (bitmap != null && newWidth == bitmap.getWidth() && newHeight == bitmap.getHeight()) {
			return;
		}

		Log.i(TAG, "resize w: " + newWidth + " h: " + newHeight);

		bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(0x0);
		bitmapCanvas = new Canvas(bitmap);

		// now set up a transform for incoming paths
		int left = getWidth()/2 - bitmap.getWidth()/2;
		int top = getHeight()/2 - bitmap.getHeight()/2;
		bitmapCanvas.translate(-left, -top);

		// redraw our drawing into the new bitmap
		renderDrawingSteps();
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
			onNewStaticPathAvailable(path, rect);
		}

		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public void onNewStaticPathAvailable(Path path, RectF rect) {
		// draw path into bitmapCanvas
		bitmapCanvas.drawPath(path, getBrush().getPaint());

		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
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

	public void undo() {
		if (!drawingSteps.isEmpty()) {
			drawingSteps.remove(drawingSteps.size() - 1);
			renderDrawingSteps();
			getInvalidationDelegate().invalidate();
		}
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
		if (!incrementalInputStrokeTessellator.getStaticPaths().isEmpty()) {
			drawingSteps.add(new IntermediateDrawingStep(getBrush().copy(), incrementalInputStrokeTessellator.getInputStrokes()));
		}
	}

	private void renderDrawingSteps() {
		if (bitmap != null) {
			bitmap.eraseColor(0x0);

			InputStrokeTessellator tess = new InputStrokeTessellator();
			for (IntermediateDrawingStep step : drawingSteps) {
				tess.setMinWidth(step.brush.getMinWidth());
				tess.setMaxWidth(step.brush.getMaxWidth());
				tess.setMaxVelDPps(step.brush.getMaxWidthDpPs());

				for (InputStroke stroke : step.inputStrokes) {
					tess.setInputStroke(stroke);
					Path path = tess.tessellate(false, true, true);
					bitmapCanvas.drawPath(path, step.brush.getPaint());
				}
			}
		}
	}

	private static final class IntermediateDrawingStep implements Parcelable {
		Brush brush;
		ArrayList<InputStroke> inputStrokes;

		public IntermediateDrawingStep(Brush brush, ArrayList<InputStroke> inputStrokes) {
			this.brush = brush;
			this.inputStrokes = inputStrokes;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeParcelable(brush, 0);
			dest.writeInt(inputStrokes.size());
			for (InputStroke stroke: inputStrokes) {
				dest.writeParcelable(stroke, 0);
			}
		}

		public static final Parcelable.Creator<IntermediateDrawingStep> CREATOR = new Parcelable.Creator<IntermediateDrawingStep>() {
			public IntermediateDrawingStep createFromParcel(Parcel in) {
				return new IntermediateDrawingStep(in);
			}

			public IntermediateDrawingStep[] newArray(int size) {
				return new IntermediateDrawingStep[size];
			}
		};

		private IntermediateDrawingStep(Parcel in) {
			brush = in.readParcelable(null);
			int count = in.readInt();
			inputStrokes = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				InputStroke s = in.readParcelable(null);
				inputStrokes.add(s);
			}
		}
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
