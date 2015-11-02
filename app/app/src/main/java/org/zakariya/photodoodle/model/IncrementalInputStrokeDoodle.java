package org.zakariya.photodoodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
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

	private static final float CANVAS_SIZE = 1024f;

	private Paint invalidationRectPaint, bitmapPaint;
	private RectF invalidationRect;
	private IncrementalInputStrokeTessellator incrementalInputStrokeTessellator;
	private Context context;
	private Canvas bitmapCanvas;
	private Matrix screenToCanvasMatrix;
	private Matrix canvasToScreenMatrix;
	private boolean renderInvalidationRect = false;

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

		// render backing store
		canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);

		if (incrementalInputStrokeTessellator != null && !getBrush().isEraser()) {
			Path path = incrementalInputStrokeTessellator.getLivePath();
			if (path != null && !path.isEmpty()) {
				canvas.save();
				canvas.concat(canvasToScreenMatrix);
				canvas.drawPath(path, getBrush().getPaint());
				canvas.restore();
			}
		}

		// draw the invalidation rect
		if (renderInvalidationRect) {
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

		bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(0x0);
		bitmapCanvas = new Canvas(bitmap);

		// apply canvas to screen matrix
		screenToCanvasMatrix = computeScreenToCanvasMatrix();
		canvasToScreenMatrix = computeCanvasToScreenMatrix();
		bitmapCanvas.setMatrix(canvasToScreenMatrix);

		// redraw our drawing into the new bitmap
		renderDrawingSteps();
	}

	@Override
	public DoodleView.InputDelegate inputDelegate() {
		return new InputDelegate(this);
	}

	@Override
	public void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect) {
		canvasToScreenMatrix.mapRect(rect);
		invalidationRect = rect;
		getInvalidationDelegate().invalidate(rect);
	}

	@Override
	public void onLivePathModified(Path path, RectF rect) {
		if (getBrush().isEraser()) {
			onNewStaticPathAvailable(path, rect);
		} else {
			canvasToScreenMatrix.mapRect(rect);
			invalidationRect = rect;
			getInvalidationDelegate().invalidate(rect);
		}
	}

	@Override
	public void onNewStaticPathAvailable(Path path, RectF rect) {
		// draw path into bitmapCanvas
		bitmapCanvas.drawPath(path, getBrush().getPaint());

		canvasToScreenMatrix.mapRect(rect);
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

	public boolean isRenderInvalidationRect() {
		return renderInvalidationRect;
	}

	public void setRenderInvalidationRect(boolean renderInvalidationRect) {
		this.renderInvalidationRect = renderInvalidationRect;
	}

	public void undo() {
		if (!drawingSteps.isEmpty()) {
			drawingSteps.remove(drawingSteps.size() - 1);
		}

		renderDrawingSteps();
	}

	private void onTouchEventBegin(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator = new IncrementalInputStrokeTessellator(this);

		float []canvasPoint = { event.getX(), event.getY() };
		screenToCanvasMatrix.mapPoints(canvasPoint);

		incrementalInputStrokeTessellator.add(canvasPoint[0], canvasPoint[1]);
	}

	private void onTouchEventMove(@NonNull MotionEvent event) {
		float []canvasPoint = { event.getX(), event.getY() };
		screenToCanvasMatrix.mapPoints(canvasPoint);

		incrementalInputStrokeTessellator.add(canvasPoint[0], canvasPoint[1]);
	}

	private void onTouchEventEnd() {
		incrementalInputStrokeTessellator.finish();
		if (!incrementalInputStrokeTessellator.getStaticPaths().isEmpty()) {
			drawingSteps.add(new IntermediateDrawingStep(getBrush().copy(), incrementalInputStrokeTessellator.getInputStrokes()));
		}
	}

	private Matrix computeScreenToCanvasMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(),getHeight()) * 0.5f;

		Matrix matrix = new Matrix();
		matrix.preScale(CANVAS_SIZE/maxHalfDim, CANVAS_SIZE/maxHalfDim);
		matrix.preTranslate(-midX, -midY);

		return matrix;
	}

	private Matrix computeCanvasToScreenMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(),getHeight()) * 0.5f;

		Matrix matrix = new Matrix();
		matrix.preTranslate(midX, midY);
		matrix.preScale(maxHalfDim / CANVAS_SIZE, maxHalfDim/CANVAS_SIZE);

		return matrix;
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
			getInvalidationDelegate().invalidate();
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
