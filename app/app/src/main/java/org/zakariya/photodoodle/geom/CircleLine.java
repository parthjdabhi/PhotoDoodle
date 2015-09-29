package org.zakariya.photodoodle.geom;

import android.graphics.RectF;
import android.support.annotation.Nullable;

import org.zakariya.photodoodle.util.Accumulator;

import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class CircleLine {
	private ArrayList<Circle> circles = new ArrayList<>();
	private RectF boundingRect;

	public CircleLine() {
	}

	public ArrayList<Circle> getCircles() {
		return circles;
	}

	public Circle get(int i) {
		return circles.get(i);
	}

	public int size() {
		return circles.size();
	}

	@Nullable
	public Circle firstCircle() {
		return circles.isEmpty() ? null : circles.get(0);
	}

	@Nullable
	public Circle lastCircle() {
		return circles.isEmpty() ? null : circles.get(circles.size()-1);
	}

	// compute a CircleLine for an InputPointLine
	public CircleLine(InputPointLine inputPointLine, float velocityScaling) {
		if (inputPointLine.size() < 2) {
			return;
		}

		Accumulator accumulator = new Accumulator(16, 0);
		ArrayList<InputPoint> points = inputPointLine.getPoints();

		// a is the previous point, b is current point, c is next point
		InputPoint a = null, b = points.get(0), c = points.get(1);

		// create bounding rect which isn't empty, so we can expand it in loop below
		boundingRect = new RectF(b.position.x - 1, b.position.y - 1, b.position.x + 1, b.position.y + 1);

		for (int i = 0, N = points.size(); i < N; i++) {
			Circle cp = null;
			if (a == null) {
				if (b != null && c != null) { // b & c should always be nonnull, just silencing compiler warnings
					cp = new Circle(b.position, 0);
				}
			} else if (c == null) {
				if (b != null) { // b should always be nonnull, just silencing compiler warnings
					// this is last point in sequence
					cp = new Circle(b.position, 0);
				}
			} else {
				if (b != null) { // b should always be nonnull, just silencing compiler warnings
					// this is a point in the sequence middle
					float dist = PointFUtil.distance(a.position, b.position);

					long millis = b.timestamp - a.timestamp;
					float dpPerSecond = dist / (millis / 1000f);
					float size = dpPerSecond * velocityScaling;

					// smooth the size
					size = accumulator.add(size);

					cp = new Circle(b.position, size / 2);
				}
			}

			if (cp != null) {
				this.circles.add(cp);
				boundingRect.union(cp.position.x - cp.radius,
						cp.position.y - cp.radius,
						cp.position.x + cp.radius,
						cp.position.y + cp.radius);
			}

			a = b;
			b = c;
			c = (i + 2 < N) ? points.get(i + 2) : null;
		}
	}

	public RectF getBoundingRect() {
		return boundingRect;
	}
}
