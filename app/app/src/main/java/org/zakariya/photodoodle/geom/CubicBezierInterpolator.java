package org.zakariya.photodoodle.geom;

import android.graphics.PointF;

import static org.zakariya.photodoodle.geom.PointFUtil.add;
import static org.zakariya.photodoodle.geom.PointFUtil.distance;
import static org.zakariya.photodoodle.geom.PointFUtil.scale;

/**
 * Created by shamyl on 9/2/15.
 */
class CubicBezierInterpolator {
	CubicBezierPoint start, end;

	public CubicBezierInterpolator() {
	}

	public CubicBezierInterpolator(CubicBezierPoint start, CubicBezierPoint end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Get the point on the bezier line defined by start->end distance `t along that line, where `t varies from 0->1
	 *
	 * @param t distance along the bezier line, varying from 0->1
	 * @return the bezier point value of the curve defined by start->end
	 */
	PointF getBezierPoint(float t) {
		// from http://ciechanowski.me/blog/2014/02/18/drawing-bezier-curves/

		float nt = 1 - t;
		float A = nt * nt * nt;
		float B = 3 * nt * nt * t;
		float C = 3 * nt * t * t;
		float D = t * t * t;

		PointF p = scale(start.position, A);
		p = add(p, scale(start.control, B));
		p = add(p, scale(end.control, C));
		p = add(p, scale(end.position, D));

		return p;
	}

	/**
	 * @param scale the scale at which the line is being rendered
	 * @return An estimated number of subdivisions to divide this bezier curve into to represent start visually smooth curve.
	 */
	int getRecommendedSubdivisions(float scale) {
		// from http://ciechanowski.me/blog/2014/02/18/drawing-bezier-curves/

		float l0 = distance(start.position, start.control);
		float l1 = distance(start.control, end.control);
		float l2 = distance(end.control, end.position);
		float approximateLength = l0 + l1 + l2;
		float min = 10;
		float segments = approximateLength / 30;
		float slope = 0.6f;

		return (int) Math.ceil(Math.sqrt(segments * segments) * slope + (min * min) * scale);
	}
}
