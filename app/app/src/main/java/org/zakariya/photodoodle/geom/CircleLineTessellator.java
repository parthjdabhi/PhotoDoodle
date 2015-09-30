package org.zakariya.photodoodle.geom;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;

import org.zakariya.photodoodle.util.FloatBuffer;

/**
 * Created by shamyl on 9/24/15.
 */
public class CircleLineTessellator {

	private FloatBuffer leftCoordinates;
	private FloatBuffer rightCoordinates;

	public CircleLineTessellator(){}

	/**
	 * Tessellate circles into a Path object suitable for rendering
	 * @param circles list of Circle instances, in order
	 * @param path destination Path into which tessellated closed path will be added, if non-null
	 */
	public void tessellate(CircleLine circles, @Nullable Path path) {

		// set up our buffers
		if (leftCoordinates == null) {
			leftCoordinates = new FloatBuffer((int)(circles.size() * 1.5));
		} else {
			leftCoordinates.clear();
		}

		if (rightCoordinates == null) {
			rightCoordinates = new FloatBuffer((int)(circles.size() * 1.5));
		} else {
			rightCoordinates.clear();
		}

		// tessellate!
		int count = circles.size();
		for (int i = 0; i < count-1; i++) {
			tessellateSegment(circles.get(i), circles.get(i+1));
		}

		// and we're done - add to Path
		if (path != null) {
			addToPath(path);
		}
	}

	private void tessellateSegment(Circle a, Circle b) {
		PointF dir = PointFUtil.dir(a.position,b.position).first;
		PointF aLeftAttachDir = PointFUtil.scale(PointFUtil.rotateCCW(dir),a.radius);
		PointF aLeftAttachPoint = PointFUtil.add(a.position, aLeftAttachDir);
		PointF aRightAttachPoint = PointFUtil.add(a.position, PointFUtil.invert(aLeftAttachDir));

		PointF bLeftAttachDir = PointFUtil.scale(PointFUtil.rotateCCW(dir),b.radius);
		PointF bLeftAttachPoint = PointFUtil.add(b.position, bLeftAttachDir);
		PointF bRightAttachPoint = PointFUtil.add(b.position, PointFUtil.invert(bLeftAttachDir));

		if (leftCoordinates.size() >= 4) {
			float previousSegStartX = leftCoordinates.get(-4);
			float previousSegStartY = leftCoordinates.get(-3);
			float previousSegEndX = leftCoordinates.get(-2);
			float previousSegEndY = leftCoordinates.get(-1);

			// if aLeftAttachPoint->bLeftAttachPoint intersects line defined by previousSeg,
			// update leftCoordinates[-2,-1] to that intersection, and only add bLeftAttachPoint

			PointF intersection = LineIntersection.intersect(previousSegStartX,previousSegStartY,previousSegEndX,previousSegEndY,aLeftAttachPoint.x,aLeftAttachPoint.y,bLeftAttachPoint.x,bLeftAttachPoint.y);

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

			PointF intersection = LineIntersection.intersect(previousSegStartX,previousSegStartY,previousSegEndX,previousSegEndY,aRightAttachPoint.x,aRightAttachPoint.y,bRightAttachPoint.x,bRightAttachPoint.y);

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
			y = rightCoordinates.get(i+1);
			path.lineTo(x,y);
		}

		path.close();
	}

}
