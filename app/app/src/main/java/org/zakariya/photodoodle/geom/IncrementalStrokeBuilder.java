package org.zakariya.photodoodle.geom;

import android.graphics.RectF;

import java.lang.ref.WeakReference;

/**
 * Created by shamyl on 10/9/15.
 */
public class IncrementalStrokeBuilder {

	public interface StrokeConsumer {
		/**
		 * Called by IncrementalStrokeBuilder.add to notify that the inputStroke has been modified and a redraw may be warranted.
		 *
		 * @param inputStroke the input stroke which was modified
		 * @param startIndex  the start index of newly added input stroke
		 * @param endIndex    the end index of newly added input stroke
		 * @param rect        the region containing the modified stroke
		 */
		void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect);

		/**
		 * Called by IncrementalStrokeBuilder.add to notify that the stroke has been modified and a redraw may be warranted.
		 *
		 * @param stroke the rendered smoothed stroke
		 * @param rect   the rect containing the updated stroke
		 */
		void onStrokeModified(Stroke stroke, RectF rect);
	}

	private InputStroke inputStroke;
	private Stroke stroke;
	private WeakReference<StrokeConsumer> strokeConsumerWeakReference;
	private int finishedStrokeStartIndex = -1;

	public IncrementalStrokeBuilder(StrokeConsumer strokeConsumer) {
		inputStroke = new InputStroke();
		stroke = new Stroke();
		strokeConsumerWeakReference = new WeakReference<>(strokeConsumer);
	}

	public InputStroke getInputStroke() {
		return inputStroke;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void add(float x, float y) {
		InputStroke.Point lastPoint = inputStroke.lastPoint();
		inputStroke.add(x, y);
		InputStroke.Point currentPoint = inputStroke.lastPoint();

		// we can tessellate up to size - 2
		if (inputStroke.size() > 4) {
			

		}

		StrokeConsumer strokeConsumer = strokeConsumerWeakReference.get();
		if (strokeConsumer != null) {
			if (lastPoint != null && currentPoint != null) {
				RectF invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
				strokeConsumer.onInputStrokeModified(inputStroke, inputStroke.size() - 2, inputStroke.size() - 1, invalidationRect);
			}


		}
	}

	public void finish() {

	}
}
