package org.zakariya.photodoodle.geom;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.zakariya.photodoodle.util.Accumulator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class CircleLine implements Serializable, Parcelable {
	private static final float SNAP_DIST_SQUARED = 1;
	private ArrayList<Circle> circles = new ArrayList<>();
	private RectF boundingRect;

	public CircleLine() {
	}

	/**
	 * Initialize a CircleLine from an inputPointLine, scaling the circles' radii by velocityScaling.
	 * @param inputPointLine line describing drawing input
	 * @param velocityScaling circles' radii are computed by multiplying velocityScaling by the dp-per-second the line was being drawn at a given point.
	 */
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

	/**
	 * Get the array of circles. You shouldn't modify this array.
	 *
	 * @return ArrayList of circles making up this line
	 */
	public ArrayList<Circle> getCircles() {
		return circles;
	}

	/**
	 * Get the circle at index i. If i < 0, index from end of list
	 *
	 * @param i index
	 * @return circle at index, or circle at size+index if index is negative
	 */
	public Circle get(int i) {
		if (i < 0) {
			return circles.get(circles.size() + i);
		}
		return circles.get(i);
	}

	/**
	 * Get number of circles making up list
	 * @return
	 */
	public int size() {
		return circles.size();
	}

	/**
	 * Check if list is empty
	 * @return true if list is empty
	 */
	public boolean isEmpty() {
		return circles.isEmpty();
	}

	/**
	 * Adds a new circle to this line. If the new circle is close to the last circle in the line, the two will be averaged.
	 *
	 * @param circle
	 */
	public void add(Circle circle) {
		Circle lastCircle = this.lastCircle();
		if (lastCircle != null && PointFUtil.distance2(circle.position, lastCircle.position) < SNAP_DIST_SQUARED) {
			// average positions & radius of lastCircle and incoming circle
			lastCircle.position.x = (lastCircle.position.x + circle.position.x) * 0.5f;
			lastCircle.position.y = (lastCircle.position.y + circle.position.y) * 0.5f;
			lastCircle.radius = (lastCircle.radius + circle.radius) * 0.5f;
		} else {
			circles.add(circle);
		}

		boundingRect = null;
	}

	/**
	 * @return The first circle in the line, or null if empty
	 */
	@Nullable
	public Circle firstCircle() {
		return circles.isEmpty() ? null : circles.get(0);
	}

	/**
	 * @return The last circle in the line, or null if empty
	 */
	@Nullable
	public Circle lastCircle() {
		return circles.isEmpty() ? null : circles.get(circles.size() - 1);
	}

	/**
	 * @return The rect bounding this CircleLine
	 */
	public RectF getBoundingRect() {
		if (boundingRect == null) {
			if (isEmpty()) {
				// empty
				boundingRect = new RectF();
			} else {
				Circle c = get(0);
				boundingRect = new RectF(c.position.x - c.radius, c.position.y - c.radius, c.position.x + c.radius, c.position.y + c.radius);
				for (int i = 1, N = size(); i < N; i++) {
					c = get(i);
					boundingRect.union(c.position.x - c.radius, c.position.y - c.radius, c.position.x + c.radius, c.position.y + c.radius);
				}
			}
		}

		return boundingRect;
	}

	// Serializable

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		int count = size();
		out.writeInt(count);
		for (int i = 0; i < count; i++) {
			out.writeObject(circles.get(i));
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		circles = new ArrayList<>();
		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			Circle cp = (Circle) in.readObject();
			circles.add(cp);
		}
	}

	// Parcelable

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(size());
		for (Circle circle : circles) {
			dest.writeParcelable(circle, 0);
		}
	}

	public static final Parcelable.Creator<CircleLine> CREATOR = new Parcelable.Creator<CircleLine>() {
		public CircleLine createFromParcel(Parcel in) {
			return new CircleLine(in);
		}

		public CircleLine[] newArray(int size) {
			return new CircleLine[size];
		}
	};

	private CircleLine(Parcel in) {
		int size = in.readInt();
		circles = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Circle circle = in.readParcelable(null);
			circles.add(circle);
		}
	}
}
