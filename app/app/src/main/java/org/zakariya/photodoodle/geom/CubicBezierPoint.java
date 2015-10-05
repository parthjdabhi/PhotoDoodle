package org.zakariya.photodoodle.geom;

import android.graphics.PointF;

/**
 * Created by shamyl on 9/2/15.
 */
class CubicBezierPoint {
	PointF position;
	PointF control;

	public CubicBezierPoint(PointF position, PointF control) {
		this.position = position;
		this.control = control;
	}

	public PointF getPosition() {
		return position;
	}

	public void setPosition(PointF position) {
		this.position = position;
	}

	public PointF getControl() {
		return control;
	}

	public void setControl(PointF control) {
		this.control = control;
	}
}
