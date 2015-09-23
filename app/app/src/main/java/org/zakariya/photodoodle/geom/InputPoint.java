package org.zakariya.photodoodle.geom;

import android.graphics.PointF;

/**
 * Represents user input. As user drags across screen, each location is recorded along with its timestamp.
 * The timestamps can be compared across an array of ControlPoint to determine the velocity of the touch,
 * which will be used to determine line thickness.
 */
public class InputPoint {
	public PointF position;
	public long timestamp;

	InputPoint() {
	}

	public InputPoint(float x, float y) {
		position = new PointF(x, y);
		timestamp = System.currentTimeMillis();
	}
}
