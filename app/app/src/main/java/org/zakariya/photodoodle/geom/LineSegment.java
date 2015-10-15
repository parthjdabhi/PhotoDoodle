package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.util.Pair;

/**
 * Created by shamyl on 10/15/15.
 */
public class LineSegment {
	private PointF a;
	private PointF b;
	private PointF dir;
	float length;

	public LineSegment(PointF a, PointF b) {
		this.a = a;
		this.b = b;
		Pair<PointF,Float> dir = PointFUtil.dir(a,b);
		this.dir = dir.first;
		this.length = dir.second;
	}

	public float distance(PointF point) {

		PointF toPoint = PointFUtil.subtract(point, a);
		float projectedLength = PointFUtil.dot(toPoint,dir);

		// early exit condition, we'll have to get distance from endpoints
		if ( projectedLength < 0 )
		{
			return PointFUtil.distance(point, a);
		}
		else if ( projectedLength > length )
		{
			return PointFUtil.distance(point,b);
		}

		// compute distance from point to the closest projection point on the line segment
		PointF projectedOnSegment = PointFUtil.add(a, PointFUtil.scale(dir, projectedLength));
		return PointFUtil.distance(point, projectedOnSegment);
	}
}
