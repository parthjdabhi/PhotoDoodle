package org.zakariya.photodoodle.geom;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import org.zakariya.photodoodle.util.Accumulator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class CircleLine implements Serializable, Parcelable {

	private static final String TAG = "CircleLine";

	private static final float SNAP_DIST_SQUARED = 1;
	private ArrayList<Circle> circles = new ArrayList<>();
	private RectF boundingRect;
	private Path path = new Path();
	private boolean needsTessellation = true;
	private CircleLineTessellator tessellator = new CircleLineTessellator();

	public CircleLine() {
	}

	@Nullable
	public static CircleLine smoothedCircleLine(InputPointLine inputPointLine, float minDiameter, float maxDiameter, float maxVel) {
		if (inputPointLine.size() < 2) {
			return null;
		}

		CircleLine circleLine = new CircleLine();

		ArrayList<InputPoint> inputPoints = inputPointLine.getPoints();
		Accumulator accumulator = new Accumulator(16, 0);
		CubicBezierInterpolator cbi = new CubicBezierInterpolator();

		PointF controlPointA = new PointF();
		PointF controlPointB = new PointF();
		PointF bp = new PointF();

		final float minRadius = minDiameter * 0.5f;
		final float maxRadius = maxDiameter * 0.5f;
		final float deltaRadius = maxRadius - minRadius;

		for (int i = 0; i < inputPoints.size() - 1; i++) {
			InputPoint a = inputPoints.get(i);
			InputPoint b = inputPoints.get(i + 1);

			float length = PointFUtil.distance(a.position, b.position) / 4;
			controlPointA.x = a.position.x + a.tangent.x * length;
			controlPointA.y = a.position.y + a.tangent.y * length;

			controlPointB.x = b.position.x + b.tangent.x * -length;
			controlPointB.y = b.position.y + b.tangent.y * -length;

			// compute radius of circle for point A
			final float aDpPerSecond = inputPointLine.getDpPerSecond(i);
			final float aVelScale = Math.min(aDpPerSecond / maxVel, 1f);
			final float aRadius = accumulator.add(minRadius + aVelScale * aVelScale * deltaRadius);

			// prime the bezier interpolator
			cbi.set(a.position, controlPointA, controlPointB, b.position);
			int subdivisions = cbi.getRecommendedSubdivisions(1);

			if (subdivisions > 1) {

				circleLine.add(new Circle(a.position, aRadius));

				// compute radius of circle for point B
				final float bDpPerSecond = inputPointLine.getDpPerSecond(i+1);
				final float bVelScale = Math.min(bDpPerSecond / maxVel, 1f);
				final float bRadius = accumulator.add(minRadius + bVelScale * bVelScale * deltaRadius);

				// time interpolator
				final float dt = 1f / subdivisions;
				float t = dt;

				// radius interpolator
				final float dr = (bRadius - aRadius) / subdivisions;
				float r = aRadius + dr;

				for (int j = 0; j < subdivisions; j++, t += dt, r += dr) {
					cbi.getBezierPoint(t, bp);
					circleLine.add(new Circle(bp, r));
				}
			} else {
				circleLine.add(new Circle(a.position, aRadius));
			}
		}

		circleLine.invalidate();
		return circleLine;
	}

	/**
	 * Initialize start CircleLine from an inputPointLine, scaling the circles' radii by velocityScaling.
	 *
	 * @param inputPointLine line describing drawing input
	 * @param minDiameter    the min diameter of circles added for slow moving line segments
	 * @param maxDiameter    the max diameter of circles added for fast moving line segments
	 * @param maxVel         the max velocity of line segments to produce maxDiameter circles
	 */
	public CircleLine(InputPointLine inputPointLine, float minDiameter, float maxDiameter, float maxVel) {
		if (inputPointLine.size() < 2) {
			return;
		}

		Accumulator accumulator = new Accumulator(16, 0);
		ArrayList<InputPoint> points = inputPointLine.getPoints();

		// start is the previous point, b is current point, c is next point
		InputPoint a = null, b = points.get(0), c = points.get(1);
		final float thicknessDelta = maxDiameter - minDiameter;

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


					final float dist = PointFUtil.distance(a.position, b.position);
					final long millis = b.timestamp - a.timestamp;
					final float dpPerSecond = dist / (millis / 1000f);
					float velScale = dpPerSecond / maxVel;
					if (velScale > 1) {
						velScale = 1;
					}

					velScale *= velScale;

					float diameter = minDiameter + velScale * thicknessDelta;

					Log.i(TAG, "dpPerSecond: " + dpPerSecond + " velScale: " + velScale + " diameter: " + diameter);

					// smooth the size
					diameter = accumulator.add(diameter);

					cp = new Circle(b.position, diameter * 0.5f);
				}
			}

			if (cp != null) {
				this.circles.add(cp);
			}

			a = b;
			b = c;
			c = (i + 2 < N) ? points.get(i + 2) : null;
		}

		invalidate();
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
	 *
	 * @return
	 */
	public int size() {
		return circles.size();
	}

	/**
	 * Check if list is empty
	 *
	 * @return true if list is empty
	 */
	public boolean isEmpty() {
		return circles.isEmpty();
	}

	/**
	 * Adds start new circle to this line. If the new circle is close to the last circle in the line, the two will be averaged.
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

		invalidate();
	}

	public void addNoCheck(Circle circle) {
		circles.add(circle);
		invalidate();
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

	public void invalidate() {
		boundingRect = null;
		path.reset();
		needsTessellation = true;
	}

	/**
	 * Get the tessellated path circumscribing this circleLine
	 *
	 * @return Path representing this CircleLine
	 */
	public Path getPath() {
		if (needsTessellation) {
			tessellator.tessellate(this, path);
		}

		return path;
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

		invalidate();
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

		invalidate();
	}
}
