package org.zakariya.photodoodle.geom;

import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by shamyl on 10/18/15.
 */
public class IncrementalInputStrokeTessellator {

	private static final String TAG = "IIST";
	private static final int MIN_PARTITION_SIZE = 32;

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
		 * Called by IncrementalInputStrokeTessellator.add to notify that the "live" livePath is updated.
		 * This is the dynamic livePath that's being recomputed as the pen moves.
		 *
		 * @param path the rendered livePath
		 * @param rect the rect containing the livePath
		 */
		void onLivePathModified(Path path, RectF rect);

		/**
		 * As the live livePath is drawn and optimized, older parts of the livePath may be frozen and do
		 * not need to be recomputed with each input stroke modification. As these chunks freeze,
		 * they will be passed to the listener as new static paths.
		 * @param path the new chunk of static livePath that's available
		 * @param rect the rect containing the new chun of static livePath
		 */
		void onNewStaticPathAvailable(Path path, RectF rect);

		/**
		 * @return optimization threshold for the input stroke. If > 0, the stroke will be optimized periodically
		 */
		float getInputStrokeOptimizationThreshold();

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
	private float optimizationThreshold;
	private Path livePath;
	private ArrayList<Path> staticPaths = new ArrayList<>();
	private RectF livePathBounds = new RectF();
	private RectF staticPathBounds = new RectF();

	/**
	 * Create new IncrementalStrokeTessellator
	 * NOTE: listener is held weakly
	 * @param listener listener which will be notified as renderable paths are generated
	 */
	public IncrementalInputStrokeTessellator(Listener listener) {
		listenerWeakReference = new WeakReference<>(listener);
		optimizationThreshold = listener.getInputStrokeOptimizationThreshold();
		inputStroke = new InputStroke(optimizationThreshold);
		inputStrokeTessellator = new InputStrokeTessellator(inputStroke,listener.getStrokeMinWidth(),listener.getStrokeMaxWidth(),listener.getStrokeMaxVelDPps());
	}

	public InputStroke getInputStroke() {
		return inputStroke;
	}

	public void add(float x, float y) {
		InputStroke.Point lastPoint = inputStroke.lastPoint();
		boolean shouldPartition = inputStroke.add(x, y);
		InputStroke.Point currentPoint = inputStroke.lastPoint();

		if (!shouldPartition && inputStroke.size() > MIN_PARTITION_SIZE) {
			Log.i(TAG, "partitioning...");
			shouldPartition = true;
		}

		Listener listener = listenerWeakReference.get();
		if (listener != null) {
			if (lastPoint != null && currentPoint != null) {
				RectF invalidationRect = RectFUtil.containing(lastPoint.position, currentPoint.position);
				listener.onInputStrokeModified(inputStroke, inputStroke.size() - 2, inputStroke.size() - 1, invalidationRect);
			}

			int firstPoint = staticPaths.isEmpty() ? 0 : 1;

			if (shouldPartition) {
				// adding the point triggered an optimization pass. tessellate to path
				Path newStaticPathChunk = inputStrokeTessellator.tessellate(firstPoint);
				staticPaths.add(newStaticPathChunk);

				// grab last two points in path, freeze their velocity, clear stroke and re-add.
				// we need them to compute velocity of next point, and we freeze their velocity for stability
				InputStroke.Point a = inputStroke.get(-2);
				InputStroke.Point b = inputStroke.get(-1);
				a.freezeVelocity = true;
				b.freezeVelocity = true;
				inputStroke.clear();
				inputStroke.getPoints().add(a);
				inputStroke.getPoints().add(b);

				if (!newStaticPathChunk.isEmpty()) {
					newStaticPathChunk.computeBounds(staticPathBounds, true);
					listener.onNewStaticPathAvailable(newStaticPathChunk,staticPathBounds);
				}

			} else {
				livePath = inputStrokeTessellator.tessellate(firstPoint);
				if (!livePath.isEmpty()) {
					livePath.computeBounds(livePathBounds, true);
					listener.onLivePathModified(livePath, livePathBounds);
				}
			}
		}
	}

	public Path getLivePath() {
		return livePath;
	}

	public ArrayList<Path> getStaticPaths() {
		return staticPaths;
	}

	public RectF getBoundingRect() {
		if (!livePathBounds.isEmpty()) {
			return livePathBounds;
		} else {
			return inputStroke.getBoundingRect();
		}
	}

	public void finish() {
		inputStroke.finish();
	}
}
