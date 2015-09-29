package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.support.annotation.Nullable;

/**
 * Created by shamyl on 9/24/15.
 */
public class LineIntersection {

	/**
	 * Compute intersection, if any, of two line segments
	 *
	 * @param x1 x coordinate of start of first line segment
	 * @param y1 y coordinate of start of first line segment
	 * @param x2 x coordinate of end of first line segment
	 * @param y2 y coordinate of end of first line segment
	 * @param x3 x coordinate of start of second line segment
	 * @param y3 y coordinate of start of second line segment
	 * @param x4 x coordinate of end of second line segment
	 * @param y4 y coordinate of end of second line segment
	 * @return intersection PointF, if any intersection. Null if lines are collinear or do not intersect
	 */
	@Nullable
	public PointF intersect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		// http://processingjs.org/learning/custom/intersect/

		float a1, a2, b1, b2, c1, c2;
		float r1, r2, r3, r4;
		float denom, offset, num;

		// Compute a1, b1, c1, where line joining points 1 and 2
		// is "a1 x + b1 y + c1 = 0".
		a1 = y2 - y1;
		b1 = x1 - x2;
		c1 = (x2 * y1) - (x1 * y2);

		// Compute r3 and r4.
		r3 = ((a1 * x3) + (b1 * y3) + c1);
		r4 = ((a1 * x4) + (b1 * y4) + c1);

		// Check signs of r3 and r4. If both point 3 and point 4 lie on
		// same side of line 1, the line segments do not intersect.
		if ((r3 != 0) && (r4 != 0) && sameSign(r3, r4)) {
			return null;
		}

		// Compute a2, b2, c2
		a2 = y4 - y3;
		b2 = x3 - x4;
		c2 = (x4 * y3) - (x3 * y4);

		// Compute r1 and r2
		r1 = (a2 * x1) + (b2 * y1) + c2;
		r2 = (a2 * x2) + (b2 * y2) + c2;

		// Check signs of r1 and r2. If both point 1 and point 2 lie
		// on same side of second line segment, the line segments do
		// not intersect.
		if ((r1 != 0) && (r2 != 0) && (sameSign(r1, r2))) {
			return null;
		}

		//Line segments intersect: compute intersection point.
		denom = (a1 * b2) - (a2 * b1);

		if (denom == 0) {
			return null;
		}

		if (denom < 0) {
			offset = -denom / 2;
		} else {
			offset = denom / 2;
		}

		float x, y;

		// The denom/2 is to get rounding instead of truncating. It
		// is added or subtracted to the numerator, depending upon the
		// sign of the numerator.
		num = (b1 * c2) - (b2 * c1);
		if (num < 0) {
			x = (num - offset) / denom;
		} else {
			x = (num + offset) / denom;
		}

		num = (a2 * c1) - (a1 * c2);
		if (num < 0) {
			y = (num - offset) / denom;
		} else {
			y = (num + offset) / denom;
		}

		// lines_intersect
		return new PointF(x, y);
	}

	@Nullable
	public PointF intersect(PointF a, PointF b, PointF c, PointF d) {
		return intersect(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y);
	}

	boolean sameSign(float a, float b) {
		return ((a * b) >= 0);
	}

}
