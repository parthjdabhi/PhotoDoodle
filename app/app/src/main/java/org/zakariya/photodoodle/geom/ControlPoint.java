package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Created by shamyl on 9/22/15.
 */
public class ControlPoint {
	public PointF position; // the position of the point
	public PointF tangent; // the normalized tangent vector of the line at this point - points "forward"
	public float halfSize; // the half thickness of the line at this point

	ControlPoint() {
	}

	public ControlPoint(PointF position, float halfSize) {
		this.position = position;
		this.halfSize = halfSize;
		this.tangent = null;
	}

	public ControlPoint(PointF position, PointF tangent, float halfSize) {
		this.position = position;
		this.tangent = tangent;
		this.halfSize = halfSize;
	}

	public RectF getBoundingRect() {
		return new RectF(position.x - halfSize, position.y - halfSize, position.x + halfSize, position.y + halfSize);
	}
}
