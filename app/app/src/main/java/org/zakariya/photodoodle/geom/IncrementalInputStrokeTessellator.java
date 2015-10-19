package org.zakariya.photodoodle.geom;

import android.graphics.Path;
import android.graphics.RectF;

import java.lang.ref.WeakReference;

/**
 * Created by shamyl on 10/18/15.
 */
public class IncrementalInputStrokeTessellator {

	public interface Listener {
		/**
		 * Called by IncrementalInputStrokeTessellator.add to notify that the inputStroke has been modified and a redraw may be warranted.
		 *
		 * @param inputStroke the input stroke which was modified
		 * @param startIndex  the start index of newly added input stroke
		 * @param endIndex    the end index of newly added input stroke
		 * @param rect        the region containing the modified stroke
		 */
		void onInputStrokeModified(InputStroke inputStroke, int startIndex, int endIndex, RectF rect);

		/**
		 * Called by IncrementalInputStrokeTessellator.add to notify that a drawable Path is available
		 *
		 * @param path the rendered path
		 * @param rect the rect containing the path
		 */
		void onPathAvailable(Path path, RectF rect);

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
	private InputStrokeTessellator inputStrokeTessellator;
	private WeakReference<Listener> listenerWeakReference;
	private float autoOptimizationThreshold;
	private Path path;
	private RectF pathBounds = new RectF();


	public IncrementalInputStrokeTessellator(Listener listener) {
		listenerWeakReference = new WeakReference<>(listener);
		autoOptimizationThreshold = listener.getInputStrokeAutoOptimizationThreshold();
		inputStroke = new InputStroke(autoOptimizationThreshold);
		inputStrokeTessellator = new InputStrokeTessellator(inputStroke,listener.getStrokeMinWidth(),listener.getStrokeMaxWidth(),listener.getStrokeMaxVelDPps());
	}

	public InputStroke getInputStroke() {
		return inputStroke;
	}

	public void add(float x, float y) {
		InputStroke.Point lastPoint = inputStroke.lastPoint();
		inputStroke.add(x, y);
		InputStroke.Point currentPoint = inputStroke.lastPoint();


		// we can tessellate up to size - 2
		boolean shouldTessellate = false;
		if (inputStroke.size() > 4) {
			if (autoOptimizationThreshold > 0) {
				inputStroke.optimize(autoOptimizationThreshold);
			}

			shouldTessellate = true;
		}

		Listener listener = listenerWeakReference.get();
		if (listener != null) {
			if (lastPoint != null && currentPoint != null) {
				RectF invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
				listener.onInputStrokeModified(inputStroke, inputStroke.size() - 2, inputStroke.size() - 1, invalidationRect);
			}

			if (shouldTessellate) {
				path = inputStrokeTessellator.tessellate();
				if (!path.isEmpty()) {
					path.computeBounds(pathBounds, true);
					listener.onPathAvailable(path, pathBounds);
				}
			}
		}
	}

	public Path getPath() {
		return path;
	}

	public RectF getBoundingRect() {
		if (!pathBounds.isEmpty()) {
			return pathBounds;
		} else {
			return inputStroke.getBoundingRect();
		}
	}

	public void finish() {
		inputStroke.finish();
	}
}
