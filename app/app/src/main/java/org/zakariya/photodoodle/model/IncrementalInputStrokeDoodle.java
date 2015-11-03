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
import android.util.Log;
import android.view.MotionEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.zakariya.photodoodle.geom.IncrementalInputStrokeTessellator;
import org.zakariya.photodoodle.geom.InputStroke;
import org.zakariya.photodoodle.geom.InputStrokeTessellator;

import java.io.FileNotFoundException;
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
		invalidate();
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
			RectF r = invalidationRect != null ? invalidationRect : new RectF(0,0,getWidth(),getHeight());
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
			loadedDrawingSteps = kryo.readObject(input,ArrayList.class);
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

		incrementalInputStrokeTessellator.add(canvasPoint[0], canvasPoint[1]);
	}


	protected void onTouchEventEnd(@NonNull MotionEvent event) {
		incrementalInputStrokeTessellator.finish();
		if (!incrementalInputStrokeTessellator.getStaticPaths().isEmpty()) {
			drawingSteps.add(new IntermediateDrawingStep(getBrush().copy(), incrementalInputStrokeTessellator.getInputStrokes()));
		}
	}

	private Matrix computeScreenToCanvasMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(), getHeight()) * 0.5f;

		Matrix matrix = new Matrix();
		matrix.preScale(CANVAS_SIZE / maxHalfDim, CANVAS_SIZE / maxHalfDim);
		matrix.preTranslate(-midX, -midY);

		return matrix;
	}

	private Matrix computeCanvasToScreenMatrix() {
		final float midX = getWidth() * 0.5f;
		final float midY = getHeight() * 0.5f;
		final float maxHalfDim = Math.max(getWidth(), getHeight()) * 0.5f;

		Matrix matrix = new Matrix();
		matrix.preTranslate(midX, midY);
		matrix.preScale(maxHalfDim / CANVAS_SIZE, maxHalfDim / CANVAS_SIZE);

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
			switch(serializationVersion) {
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
