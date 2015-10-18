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

		/**
		 * @return auto optimization threshold for the input stroke. If > 0, the stroke will be optimized as it's drawn.
		 */
		float getInputStrokeAutoOptimizationThreshold();

		/**
		 * @return Min-width for generated stroke
		 */
		float getStrokeMinWidth();

		/**
		 * @return Max-width for generated stroke
		 */
		float getStrokeMaxWidth();

		/**
		 * @return Max input velocity which generates max width stroke. Input velocity lower than this trends towards min width stroke.
		 */
		float getStrokeMaxVelDPps();
	}

	private InputStroke inputStroke;
	private Stroke stroke;
	private WeakReference<StrokeConsumer> strokeConsumerWeakReference;
	private int finishedStrokeStartIndex = -1;
	private float autoOptimizationThreshold, strokeMinWidth, strokeMaxWidth, strokeMaxVelDPps;

	public IncrementalStrokeBuilder(StrokeConsumer strokeConsumer) {
		autoOptimizationThreshold = strokeConsumer.getInputStrokeAutoOptimizationThreshold();
		strokeMinWidth = strokeConsumer.getStrokeMinWidth();
		strokeMaxWidth = strokeConsumer.getStrokeMaxWidth();
		strokeMaxVelDPps = strokeConsumer.getStrokeMaxVelDPps();

		strokeConsumerWeakReference = new WeakReference<>(strokeConsumer);
		inputStroke = new InputStroke(autoOptimizationThreshold);
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

		StrokeConsumer strokeConsumer = strokeConsumerWeakReference.get();


		// we can tessellate up to size - 2
		if (inputStroke.size() > 4) {
			if (autoOptimizationThreshold > 0) {
				inputStroke.optimize(autoOptimizationThreshold);
			}

			stroke = Stroke.smoothedStroke(inputStroke,strokeMinWidth,strokeMaxWidth,strokeMaxVelDPps);
		}

		if (strokeConsumer != null) {
			if (lastPoint != null && currentPoint != null) {
				RectF invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
				strokeConsumer.onInputStrokeModified(inputStroke, inputStroke.size() - 2, inputStroke.size() - 1, invalidationRect);
			}

			if (stroke != null) {
				strokeConsumer.onStrokeModified(stroke, stroke.getBoundingRect());
			}
		}
	}

	public void finish() {
		inputStroke.finish();
	}
}
