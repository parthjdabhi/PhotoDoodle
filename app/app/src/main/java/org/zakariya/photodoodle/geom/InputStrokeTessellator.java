package org.zakariya.photodoodle.geom;

import android.graphics.Path;
import android.graphics.PointF;

import org.zakariya.photodoodle.util.FloatBuffer;

import java.util.ArrayList;

/**
 * Created by shamyl on 10/18/15.
 */
public class InputStrokeTessellator {

	private InputStroke inputStroke;
	private float minWidth, maxWidth, maxVelDPps, minRadius, deltaRadius;
	private Path path;
	private FloatBuffer leftCoordinates = new FloatBuffer();
	private FloatBuffer rightCoordinates = new FloatBuffer();
	private CubicBezierInterpolator cbi = new CubicBezierInterpolator();

	public InputStrokeTessellator(InputStroke inputStroke, float minWidth, float maxWidth, float maxVelDPps) {
		this.inputStroke = inputStroke;
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
		this.maxVelDPps = maxVelDPps;
		minRadius = minWidth / 2;
		float maxRadius = maxWidth / 2;
		deltaRadius = maxRadius - minRadius;
	}

	public InputStroke getInputStroke() {
		return inputStroke;
	}

	public float getMinWidth() {
		return minWidth;
	}

	public float getMaxWidth() {
		return maxWidth;
	}

	public float getMaxVelDPps() {
		return maxVelDPps;
	}

	public Path getPath() {
		return path;
	}

	public Path tessellate(int startIndex, int endIndex) {
		path = new Path();

		if (inputStroke.size() < 2) {
			return path;
		}

		ArrayList<InputStroke.Point> points = inputStroke.getPoints();
		PointF aLeftControlPoint = new PointF();
		PointF aRightControlPoint = new PointF();
		PointF bLeftControlPoint = new PointF();
		PointF bRightControlPoint = new PointF();
		PointF previousSegmentDir = null;
		PointF currentSegmentDir = inputStroke.getSegmentDirection(startIndex);
		PointF nextSegmentDir = null;
		PointF bp = new PointF();

		for (int i = startIndex; i < endIndex; i++) {
			final InputStroke.Point a = points.get(i);
			final InputStroke.Point b = points.get(i + 1);
			final PointF aTangent = inputStroke.getTangent(i);
			final PointF bTangent = inputStroke.getTangent(i + 1);
			final float aRadius = getRadiusForInputStrokePoint(i);
			final float bRadius = getRadiusForInputStrokePoint(i + 1);

			// aLeft and aRight are the start points of the two bezier curves
			PointF aLeft = PointFUtil.scale(PointFUtil.rotateCCW(aTangent), aRadius);
			PointF aRight = PointFUtil.scale(aLeft, -1);
			aLeft.x += a.position.x;
			aLeft.y += a.position.y;
			aRight.x += a.position.x;
			aRight.y += a.position.y;

			// bLeft and bRight are the end points of the two bezier curves
			PointF bLeft = PointFUtil.scale(PointFUtil.rotateCCW(bTangent), bRadius);
			PointF bRight = PointFUtil.scale(bLeft, -1);
			bLeft.x += b.position.x;
			bLeft.y += b.position.y;
			bRight.x += b.position.x;
			bRight.y += b.position.y;

			// now compute the bezier control points
			float leftControlPointLength = PointFUtil.distance(aLeft, bLeft) / 4;
			float rightControlPointLength = PointFUtil.distance(aRight, bRight) / 4;
			float aLeftControlPointLength = leftControlPointLength;
			float bLeftControlPointLength = leftControlPointLength;
			float aRightControlPointLength = rightControlPointLength;
			float bRightControlPointLength = rightControlPointLength;

			// scale down start bezier control points by acuteness of angle between current and previous segments
			if (previousSegmentDir != null) {
				float dot = PointFUtil.dot(previousSegmentDir,currentSegmentDir);
				float acuteness = -1 * Math.min(dot, 0); // clamp dot from [-1,0] and invert so we have an acuteness from 0 to 1
				float controlPointScale = 1-acuteness;
				aLeftControlPointLength *= controlPointScale;
				aRightControlPointLength *= controlPointScale;
			}

			aLeftControlPoint.x = aLeft.x + aTangent.x * aLeftControlPointLength;
			aLeftControlPoint.y = aLeft.y + aTangent.y * aLeftControlPointLength;
			aRightControlPoint.x = aRight.x + aTangent.x * aRightControlPointLength;
			aRightControlPoint.y = aRight.y + aTangent.y * aRightControlPointLength;

			nextSegmentDir = inputStroke.getSegmentDirection(i+1);
			// scale down end bezier control points by acuteness of angle between current and next segments
			if (nextSegmentDir != null) {
				float dot = PointFUtil.dot(nextSegmentDir, currentSegmentDir);
				float acuteness = -1 * Math.min(dot,0); // clamp dot from [-1,0] and invert so we have an acuteness from 0 to 1
				float controlPointScale = 1-acuteness;
				bLeftControlPointLength *= controlPointScale;
				bRightControlPointLength *= controlPointScale;
			}

			bLeftControlPoint.x = bLeft.x + bTangent.x * -bLeftControlPointLength;
			bLeftControlPoint.y = bLeft.y + bTangent.y * -bLeftControlPointLength;
			bRightControlPoint.x = bRight.x + bTangent.x * -bRightControlPointLength;
			bRightControlPoint.y = bRight.y + bTangent.y * -bRightControlPointLength;

			// perform bezier interpolation of left side from aLeft up to but not including bLeft since next step will add bLeft
			cbi.set(aLeft,aLeftControlPoint,bLeftControlPoint,bLeft);
			int subdivisions = cbi.getRecommendedSubdivisions(1);
			leftCoordinates.add(aLeft.x);
			leftCoordinates.add(aLeft.y);
			if (subdivisions > 1) {
				// time interpolator
				final float dt = 1f / subdivisions;
				float t = dt;
				for (int j = 0; j < subdivisions; j++, t += dt) {
					cbi.getBezierPoint(t, bp);
					leftCoordinates.add(bp.x);
					leftCoordinates.add(bp.y);
				}
			}

			// if we're at last step add last vertex
			if (i == endIndex) {
				leftCoordinates.add(bLeft.x);
				leftCoordinates.add(bLeft.y);
			}

			// perform bezier interpolation of right side from aRight up to but not including bRight since next step will add bRight
			cbi.set(aRight,aRightControlPoint,bRightControlPoint,bRight);
			subdivisions = cbi.getRecommendedSubdivisions(1);
			rightCoordinates.add(aRight.x);
			rightCoordinates.add(aRight.y);
			if (subdivisions > 1) {
				// time interpolator
				final float dt = 1f / subdivisions;
				float t = dt;
				for (int j = 0; j < subdivisions; j++, t += dt) {
					cbi.getBezierPoint(t, bp);
					rightCoordinates.add(bp.x);
					rightCoordinates.add(bp.y);
				}
			}

			// if we're at last step add last vertex
			if (i == endIndex) {
				rightCoordinates.add(bRight.x);
				rightCoordinates.add(bRight.y);
			}

			// update segment directions for angle acuteness testing
			previousSegmentDir = currentSegmentDir;
			currentSegmentDir = nextSegmentDir;
		}

		// now that we've populated the left and right coord buffers, stitch a path together
		// TODO: This indexing is incompatible with incremental tessellation, it assumes complete path generation from complete buffers
		float x = leftCoordinates.get(0);
		float y = leftCoordinates.get(1);
		path.moveTo(x, y);

		for (int i = 2, N = leftCoordinates.size(); i < N; i += 2) {
			x = leftCoordinates.get(i);
			y = leftCoordinates.get(i + 1);
			path.lineTo(x, y);
		}

		// right coords must be iterated backwards from end to start
		for (int i = rightCoordinates.size() - 2; i >= 0; i -= 2) {
			x = rightCoordinates.get(i);
			y = rightCoordinates.get(i + 1);
			path.lineTo(x, y);
		}

		path.close();

		return path;
	}

	public Path tessellate() {
		leftCoordinates.clear();
		rightCoordinates.clear();
		return tessellate(0, inputStroke.size() - 1);
	}

	public float getRadiusForInputStrokePoint(int index) {
		// TODO: Implement meaningful radius measure
		return maxWidth/2;
//		// TODO: Apply gaussian smoothing by weighted-average of neighbor points?
//		final float vel = inputStroke.getDpPerSecond(index);
//		final float velScale = Math.min(vel / maxVelDPps, 1f);
//		return minRadius + (velScale * velScale * deltaRadius);
	}

}
