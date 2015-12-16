package org.zakariya.doodle.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.util.Log;
import android.view.MotionEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.zakariya.doodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.doodle.geom.InputStroke;
import org.zakariya.doodle.geom.InputStrokeTessellator;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 10/14/15.
 */
public class IncrementalInputStrokeDoodle extends Doodle implements IncrementalInputStrokeTessellator.Listener {
	private static final String TAG = "IncInptStrokeDoodle";

	public static final float CANVAS_SIZE = 1024f;

	public enum ScaleMode {
		FIT,
		FILL
	}

	protected Matrix screenToCanvasMatrix;
	protected Matrix canvasToScreenMatrix;
	protected float screenToCanvasScale;
	protected float canvasToScreenScale;

	private Paint invalidationRectPaint, debugPositioningPaint, bitmapPaint;
	private RectF invalidationRect;
	private IncrementalInputStrokeTessellator incrementalInputStrokeTessellator;
	private Context context;
	private Canvas bitmapCanvas;
	private Bitmap bitmap;

	@State
	int scaleMode = ScaleMode.FILL.ordinal();

	@State
	boolean drawInvalidationRect = false;

	@State
	boolean drawDebugPositioningOverlay = false;

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
		clearDrawing();
	}

	@Override
	public void draw(Canvas canvas) {

		if (Color.alpha(getBackgroundColor()) > 0) {
			canvas.drawColor(getBackgroundColor());
		}

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

		// draw the canvas bounds
		if (isDrawDebugPositioningOverlay()) {
			canvas.save();
			canvas.concat(canvasToScreenMatrix);

			Paint dp = getDebugPositioningPaint();
			dp.setColor(0xFFFF9900); // yellow
			dp.setStrokeWidth(4);
			canvas.drawRect(-CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, CANVAS_SIZE, dp);
			canvas.drawLine(-CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, CANVAS_SIZE, dp);
			canvas.drawLine(CANVAS_SIZE, -CANVAS_SIZE, -CANVAS_SIZE, CANVAS_SIZE, dp);

			canvas.restore();
		}

		// draw the invalidation rect
		if (isDrawInvalidationRect()) {
			RectF r = invalidationRect != null ? invalidationRect : new RectF(0, 0, getWidth(), getHeight());
			canvas.drawRect(r, invalidationRectPaint);
		}

		invalidationRect = null;
	}

	@Override
	public void resize(int newWidth, int newHeight) {
		super.resize(newWidth, newHeight);

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
	public void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect) {
		canvasToScreenMatrix.mapRect(rect);
		invalidationRect = rect;
		invalidate(rect);
	}

	@Override
	public void onLivePathModified(Path path, RectF rect) {
		if (getBrush().isEraser()) {
			onNewStaticPathAvailable(path, rect);
		} else {
			canvasToScreenMatrix.mapRect(rect);
			invalidationRect = rect;
			invalidate(rect);
		}
	}

	@Override
	public void onNewStaticPathAvailable(Path path, RectF rect) {
		// draw path into bitmapCanvas
		bitmapCanvas.drawPath(path, getBrush().getPaint());

		canvasToScreenMatrix.mapRect(rect);
		invalidationRect = rect;
		invalidate(rect);
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

	public boolean isDrawInvalidationRect() {
		return drawInvalidationRect;
	}

	public void setDrawInvalidationRect(boolean drawInvalidationRect) {
		this.drawInvalidationRect = drawInvalidationRect;
	}

	public void clearDrawing() {
		incrementalInputStrokeTessellator = null;
		drawingSteps.clear();
		bitmap.eraseColor(0x0);
		invalidate();
	}

	public void undo() {
		if (!drawingSteps.isEmpty()) {
			drawingSteps.remove(drawingSteps.size() - 1);
		}

		renderDrawingSteps();
	}

	public Context getContext() {
		return context;
	}

	public ScaleMode getScaleMode() {
		return ScaleMode.values()[scaleMode];
	}

	public void setScaleMode(ScaleMode scaleMode) {
		this.scaleMode = scaleMode.ordinal();
		invalidate();
	}


	public boolean isDrawDebugPositioningOverlay() {
		return drawDebugPositioningOverlay;
	}

	public void setDrawDebugPositioningOverlay(boolean drawDebugPositioningOverlay) {
		this.drawDebugPositioningOverlay = drawDebugPositioningOverlay;
	}


	private static final String TEST_KRYO_SERIALIZATION_FILE = "KryoTest.bin";

	@SuppressWarnings("unchecked")
	public void TEST_saveAndReload() {
		Log.i(TAG, "TEST_saveAndReload...");

		// first, save to file
		try (Output output = new Output(context.openFileOutput(TEST_KRYO_SERIALIZATION_FILE, Context.MODE_PRIVATE))) {
			Kryo kryo = new Kryo();
			kryo.writeObject(output, drawingSteps);
		} catch (FileNotFoundException ex) {
			Log.e(TAG, "Unable to open file for writing: " + ex);
		}

		Log.i(TAG, "TEST_saveAndReload - save complete. Loading...");
		ArrayList<IntermediateDrawingStep> loadedDrawingSteps = null;

		// now, re-open and load
		try (Input input = new Input(context.openFileInput(TEST_KRYO_SERIALIZATION_FILE))) {
			Kryo kryo = new Kryo();
			loadedDrawingSteps = kryo.readObject(input, ArrayList.class);
		} catch (FileNotFoundException ex) {
			Log.e(TAG, "Unable to open file for reading: " + ex);
		}

		if (loadedDrawingSteps != null) {
			Log.i(TAG, "loading drawing steps. original.size: " + drawingSteps.size() + " loaded.size: " + loadedDrawingSteps.size());
			drawingSteps = loadedDrawingSteps;
			renderDrawingSteps();
		} else {
			Log.e(TAG, "Unable to load drawing steps");
		}

	}

	@Override
	protected void onTouchEventBegin(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator = new IncrementalInputStrokeTessellator(this);

		float[] canvasPoint = {event.getX(), event.getY()};
		screenToCanvasMatrix.mapPoints(canvasPoint);

		incrementalInputStrokeTessellator.add(canvasPoint[0], canvasPoint[1]);
	}


	protected void onTouchEventMove(@NonNull MotionEvent event) {
		float[] canvasPoint = {event.getX(), event.getY()};
		screenToCanvasMatrix.mapPoints(canvasPoint);

		//Log.i(TAG, "touch move canvas coord: " + canvasPoint[0] + ", " + canvasPoint[1]);
		incrementalInputStrokeTessellator.add(canvasPoint[0], canvasPoint[1]);
	}


	protected void onTouchEventEnd(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator.finish();
		if (!incrementalInputStrokeTessellator.getStaticPaths().isEmpty()) {
			drawingSteps.add(new IntermediateDrawingStep(getBrush().copy(), incrementalInputStrokeTessellator.getInputStrokes()));
		}
	}

	protected Paint getDebugPositioningPaint() {
		if (debugPositioningPaint == null) {
			debugPositioningPaint = new Paint();
			debugPositioningPaint.setStyle(Paint.Style.STROKE);
			debugPositioningPaint.setStrokeWidth(8);
			debugPositioningPaint.setAntiAlias(true);
			debugPositioningPaint.setColor(0xFF00FFFF);
		}
		return debugPositioningPaint;
	}

	protected Matrix computeScreenToCanvasMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(), getHeight()) * 0.5f;
		final float minHalfDim = Math.min(getWidth(), getHeight()) * 0.5f;

		switch(getScaleMode()){
			case FIT:
				screenToCanvasScale = CANVAS_SIZE / minHalfDim;
				break;
			case FILL:
				screenToCanvasScale = CANVAS_SIZE / maxHalfDim;
		}

		Matrix matrix = new Matrix();
		matrix.preScale(screenToCanvasScale, screenToCanvasScale);
		matrix.preTranslate(-midX, -midY);

		return matrix;
	}

	protected Matrix computeCanvasToScreenMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(), getHeight()) * 0.5f;
		final float minHalfDim = Math.min(getWidth(), getHeight()) * 0.5f;

		switch(getScaleMode()){
			case FIT:
				canvasToScreenScale = minHalfDim / CANVAS_SIZE;
				break;
			case FILL:
				canvasToScreenScale = maxHalfDim / CANVAS_SIZE;
		}

		Matrix matrix = new Matrix();
		matrix.preTranslate(midX, midY);
		matrix.preScale(canvasToScreenScale,canvasToScreenScale);

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
			invalidate();
		}
	}

	private static final class IntermediateDrawingStep implements Parcelable, KryoSerializable {
		Brush brush;
		ArrayList<InputStroke> inputStrokes;

		public IntermediateDrawingStep() {
		}

		public IntermediateDrawingStep(Brush brush, ArrayList<InputStroke> inputStrokes) {
			this.brush = brush;
			this.inputStrokes = inputStrokes;
		}

		// Parcelable

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeParcelable(brush, 0);
			dest.writeInt(inputStrokes.size());
			for (InputStroke stroke : inputStrokes) {
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

		// KryoSerializable

		static final int SERIALIZATION_VERSION = 0;

		@Override
		public void write(Kryo kryo, Output output) {
			output.writeInt(SERIALIZATION_VERSION);
			kryo.writeObject(output, brush);
			kryo.writeObject(output, inputStrokes);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void read(Kryo kryo, Input input) {
			int serializationVersion = input.readInt();
			switch (serializationVersion) {
				case 0:
					brush = kryo.readObject(input, Brush.class);
					inputStrokes = kryo.readObject(input, ArrayList.class);
					break;

				default:
					throw new IllegalArgumentException("Unsupported " + this.getClass().getName() + " serialization version: " + serializationVersion);
			}
		}
	}
}
