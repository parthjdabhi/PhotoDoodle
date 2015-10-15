package org.zakariya.photodoodle.geom;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;

import org.zakariya.photodoodle.util.FloatBuffer;

/**
 * Created by shamyl on 9/24/15.
 */
public class StrokeTessellator {

	private FloatBuffer leftCoordinates;
	private FloatBuffer rightCoordinates;

	public StrokeTessellator() {
	}

	/**
	 * Tessellate stroke into Path object suitable for rendering
	 *
	 * @param stroke list of Point instances, in order
	 * @param path   destination Path into which tessellated closed path will be added, if non-null
	 */
	public void tessellate(Stroke stroke, @Nullable Path path) {

		// set up our buffers
		if (leftCoordinates == null) {
			leftCoordinates = new FloatBuffer((int) (stroke.size() * 1.5));
		} else {
			leftCoordinates.clear();
		}

		if (rightCoordinates == null) {
			rightCoordinates = new FloatBuffer((int) (stroke.size() * 1.5));
		} else {
			rightCoordinates.clear();
		}

		stroke = sanitize(stroke);

		// tessellate!
		int count = stroke.size();
		for (int i = 0; i < count - 1; i++) {
			tessellateSegment(stroke.get(i), stroke.get(i + 1));
		}

		// and we're done - add to Path
		if (path != null) {
			addToPath(path);
		}
	}

	private Stroke sanitize(Stroke stroke) {

		// check if sanitizing is needed, breaking loop early if that's the case
		boolean needsSanitizing = false;
		final int N = stroke.size();
		for (int i = 0; i < N - 1; i++) {
			Stroke.Point a = stroke.get(i);
			Stroke.Point b = stroke.get(i + 1);

			// check for containment
			if (a.contains(b) || b.contains(a)) {
				needsSanitizing = true;
				break;
			}
		}

		if (needsSanitizing) {
			if (stroke.size() < 3) {
				return stroke;
			}

			Stroke sanitized = new Stroke();

			Stroke.Point a, b, c;

			// handle first point manually since loop handles the rest. only add first point if
			// it's not contained by the second point
			a = stroke.get(0);
			b = stroke.get(1);
			if (!b.contains(a)) {
				sanitized.addNoCheck(a);
			}

			for (int i = 1; i < N - 1; i++) {
				// note: 'b' is the current point
				a = stroke.get(i - 1);
				b = stroke.get(i);
				c = stroke.get(i + 1);

				if (!a.contains(b) && !c.contains(b)) {
					sanitized.addNoCheck(b);
				}
			}

			// handle last point
			a = stroke.get(-2);
			b = stroke.get(-1);
			if (!a.contains(b)) {
				sanitized.addNoCheck(b);
			}

			// recurse until stroke is clean
			return sanitize(sanitized);
		} else {
			return stroke;
		}
	}

	private void tessellateSegment(Stroke.Point a, Stroke.Point b) {
		PointF dir = PointFUtil.dir(a.position, b.position).first;
		PointF aLeftAttachDir = PointFUtil.scale(PointFUtil.rotateCCW(dir), a.radius);
		PointF aLeftAttachPoint = PointFUtil.add(a.position, aLeftAttachDir);
		PointF aRightAttachPoint = PointFUtil.add(a.position, PointFUtil.invert(aLeftAttachDir));

		PointF bLeftAttachDir = PointFUtil.scale(PointFUtil.rotateCCW(dir), b.radius);
		PointF bLeftAttachPoint = PointFUtil.add(b.position, bLeftAttachDir);
		PointF bRightAttachPoint = PointFUtil.add(b.position, PointFUtil.invert(bLeftAttachDir));

		if (leftCoordinates.size() >= 4) {
			float previousSegStartX = leftCoordinates.get(-4);
			float previousSegStartY = leftCoordinates.get(-3);
			float previousSegEndX = leftCoordinates.get(-2);
			float previousSegEndY = leftCoordinates.get(-1);

			// if aLeftAttachPoint->bLeftAttachPoint intersects line defined by previousSeg,
			// update leftCoordinates[-2,-1] to that intersection, and only add bLeftAttachPoint

			PointF intersection = Lines.lineSegmentIntersection(previousSegStartX, previousSegStartY, previousSegEndX, previousSegEndY, aLeftAttachPoint.x, aLeftAttachPoint.y, bLeftAttachPoint.x, bLeftAttachPoint.y);

			if (intersection != null) {
				leftCoordinates.set(-2, intersection.x);
				leftCoordinates.set(-1, intersection.y);
				leftCoordinates.add(bLeftAttachPoint.x);
				leftCoordinates.add(bLeftAttachPoint.y);
			} else {
				leftCoordinates.add(aLeftAttachPoint.x);
				leftCoordinates.add(aLeftAttachPoint.y);
				leftCoordinates.add(bLeftAttachPoint.x);
				leftCoordinates.add(bLeftAttachPoint.y);
			}
		} else {
			leftCoordinates.add(aLeftAttachPoint.x);
			leftCoordinates.add(aLeftAttachPoint.y);
			leftCoordinates.add(bLeftAttachPoint.x);
			leftCoordinates.add(bLeftAttachPoint.y);
		}

		if (rightCoordinates.size() >= 4) {
			float previousSegStartX = rightCoordinates.get(-4);
			float previousSegStartY = rightCoordinates.get(-3);
			float previousSegEndX = rightCoordinates.get(-2);
			float previousSegEndY = rightCoordinates.get(-1);

			// if aRightAttachPoint->bRightAttachPoint intersects line defined by previousSeg,
			// update rightCoordinates[-2,-1] to that intersection, and only add bLeftAttachPoint

			PointF intersection = Lines.lineSegmentIntersection(previousSegStartX, previousSegStartY, previousSegEndX, previousSegEndY, aRightAttachPoint.x, aRightAttachPoint.y, bRightAttachPoint.x, bRightAttachPoint.y);

			if (intersection != null) {
				rightCoordinates.set(-2, intersection.x);
				rightCoordinates.set(-1, intersection.y);
				rightCoordinates.add(bRightAttachPoint.x);
				rightCoordinates.add(bRightAttachPoint.y);
			} else {
				rightCoordinates.add(aRightAttachPoint.x);
				rightCoordinates.add(aRightAttachPoint.y);
				rightCoordinates.add(bRightAttachPoint.x);
				rightCoordinates.add(bRightAttachPoint.y);
			}
		} else {
			rightCoordinates.add(aRightAttachPoint.x);
			rightCoordinates.add(aRightAttachPoint.y);
			rightCoordinates.add(bRightAttachPoint.x);
			rightCoordinates.add(bRightAttachPoint.y);
		}
	}

	private void addToPath(Path path) {
		float x = leftCoordinates.get(0);
		float y = leftCoordinates.get(1);
		path.moveTo(x, y);

		for (int i = 2, N = leftCoordinates.size(); i < N; i += 2) {
			x = leftCoordinates.get(i);
			y = leftCoordinates.get(i + 1);
			path.lineTo(x, y);
		}

		for (int i = rightCoordinates.size() - 2; i >= 0; i -= 2) {
			x = rightCoordinates.get(i);
			y = rightCoordinates.get(i + 1);
			path.lineTo(x, y);
		}

		path.close();
	}

}
