package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.Pair;

/**
 * Created by shamyl on 9/24/15.
 */
public class Lines {

	/**
	 * Compute distance of point `point to line defined by `a and `b
	 * @param a start of line
	 * @param b end of line
	 * @param point point to test
	 * @return distance of test point from line
	 */
	public static float distance(PointF a, PointF b, PointF point) {

		Pair<PointF,Float> dir = PointFUtil.dir(a,b);
		PointF toPoint = PointFUtil.subtract(point, a);
		float projectedLength = PointFUtil.dot(toPoint,dir.first);

		// early exit condition, we'll have to get distance from endpoints
		if ( projectedLength < 0 )
		{
			return PointFUtil.distance(point, a);
		}
		else if ( projectedLength > dir.second )
		{
			return PointFUtil.distance(point,b);
		}

		// compute distance from point to the closest projection point on the line segment
		PointF projectedOnSegment = PointFUtil.add(a, PointFUtil.scale(dir.first,projectedLength));
		return PointFUtil.distance(point, projectedOnSegment);
	}

	/**
	 * Compute intersection of two line segments
	 *
	 * @param a0 start point of segment A
	 * @param a1 end point of segment A
	 * @param b0 start point of segment B
	 * @param b1 end point of segment B
	 * @return intersection point, iff the two segments intersect. Null otherwise.
	 */
	@Nullable
	public static PointF lineSegmentIntersection(PointF a0, PointF a1, PointF b0, PointF b1) {
		return lineSegmentIntersection(a0.x, a0.y, a1.x, a1.y, b0.x, b0.y, b1.x, b1.y);
	}

	/**
	 * Compute intersection of two line segments
	 *
	 * @param a0x x coord of start of segment A
	 * @param a0y y coord of start of segment A
	 * @param a1x x coord of end of segment A
	 * @param a1y y coord of end of segment A
	 * @param b0x x coord of start of segment B
	 * @param b0y y coord of start of segment B
	 * @param b1x x coord of end of segment B
	 * @param b1y y coord of end of segment B
	 * @return intersection point, iff the two segments intersect. Null otherwise.
	 */
	@Nullable
	public static PointF lineSegmentIntersection(float a0x, float a0y, float a1x, float a1y, float b0x, float b0y, float b1x, float b1y) {
		// cribbed from: http://wiki.processing.org/w/Line-Line_intersection
		float adx = a1x - a0x;
		float ady = a1y - a0y;
		float bdx = b1x - b0x;
		float bdy = b1y - b0y;
		float ad_dot_bd = adx * bdy - ady * bdx;

		if (Math.abs(ad_dot_bd) < 1e-4) {
			return null;
		}

		float abx = b0x - a0x;
		float aby = b0y - a0y;
		float t = (abx * bdy - aby * bdx) / ad_dot_bd;

		if (t < 1e-4 || t > 1 - 1e-4) {
			return null;
		}

		float u = (abx * ady - aby * adx) / ad_dot_bd;
		if (u < 1e-4 || u > 1 - 1e-4) {
			return null;
		}

		return new PointF(a0x + t * adx, a0y + t * ady);
	}

	/**
	 * Compute intersection of two infinite-length lines
	 *
	 * @param a    coordinate of a point on line A
	 * @param aDir direction of line A
	 * @param b    coordinate of a point on line B
	 * @param bDir direction of line B
	 * @return intersection point iff lines aren't parallel. null if they are parallel
	 */
	@Nullable
	public static PointF infiniteLineIntersection(PointF a, PointF aDir, PointF b, PointF bDir) {
		return infiniteLineIntersection(a.x, a.y, aDir.x, aDir.y, b.x, b.y, bDir.x, bDir.y);
	}

	/**
	 * Compute intersection of two infinite-length lines
	 *
	 * @param ax  x coord of a point on line A
	 * @param ay  y coord of a point on line A
	 * @param adx x direction of line A
	 * @param ady y direction of line A
	 * @param bx  x coord of a point on line B
	 * @param by  y coord of a point on line B
	 * @param bdx x direction of line B
	 * @param bdy y direction of line B
	 * @return intersection point iff lines aren't parallel. null if they are parallel
	 */
	@Nullable
	public static PointF infiniteLineIntersection(float ax, float ay, float adx, float ady, float bx, float by, float bdx, float bdy) {
		// cribbed from: http://wiki.processing.org/w/Line-Line_intersection

		float dot = adx * bdy - ady * bdx;
		if (Math.abs(dot) < 1e-4) {
			return null;
		}

		float abx = bx - ax;
		float aby = by - ay;
		float t = (abx * bdy - aby * bdx) / dot;

		return new PointF(ax + t * adx, ay + t * ady);
	}

}
