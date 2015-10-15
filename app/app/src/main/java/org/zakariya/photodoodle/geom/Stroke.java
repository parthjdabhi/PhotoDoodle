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
public class Stroke implements Serializable, Parcelable {

	private static final String TAG = "Stroke";

	private static final float SNAP_DIST_SQUARED = 1;
	private ArrayList<Point> points = new ArrayList<>();
	private RectF boundingRect;
	private Path path = new Path();
	private boolean needsTessellation = true;
	private StrokeTessellator tessellator = new StrokeTessellator();
	private Accumulator accumulator;
	private CubicBezierInterpolator cbi;

	public Stroke() {
	}

	public void appendAndSmooth(InputStroke inputStroke, int startIndex, int endIndex, float minDiameter, float maxDiameter, float maxVel) {
		if (inputStroke.size() < 2) {
			return;
		}

		if (accumulator == null) {
			accumulator = new Accumulator(16,0);
		}

		if (cbi == null) {
			cbi = new CubicBezierInterpolator();
		}

		ArrayList<InputStroke.Point> points = inputStroke.getPoints();
		PointF controlPointA = new PointF();
		PointF controlPointB = new PointF();
		PointF bp = new PointF();

		final float minRadius = minDiameter * 0.5f;
		final float maxRadius = maxDiameter * 0.5f;
		final float deltaRadius = maxRadius - minRadius;

		for (int i = startIndex; i < endIndex; i++) {
			InputStroke.Point a = points.get(i);
			InputStroke.Point b = points.get(i + 1);
			PointF aTangent = inputStroke.getTangent(i);
			PointF bTangent = inputStroke.getTangent(i+1);

			float length = PointFUtil.distance(a.position, b.position) / 4;
			controlPointA.x = a.position.x + aTangent.x * length;
			controlPointA.y = a.position.y + aTangent.y * length;

			controlPointB.x = b.position.x + bTangent.x * -length;
			controlPointB.y = b.position.y + bTangent.y * -length;

			// compute radius of point for point A
			final float aDpPerSecond = inputStroke.getDpPerSecond(i);
			final float aVelScale = Math.min(aDpPerSecond / maxVel, 1f);
			final float aRadius = accumulator.add(minRadius + aVelScale * aVelScale * deltaRadius);

			//Log.i(TAG, "i: " + i + " aDpPerSecond: " + aDpPerSecond + " aVelScale: " + aVelScale + " aRadius: " + aRadius);

			// prime the bezier interpolator
			cbi.set(a.position, controlPointA, controlPointB, b.position);
			int subdivisions = cbi.getRecommendedSubdivisions(1);

			if (subdivisions > 1) {

				add(new Point(a.position, aRadius));

				// compute radius of point for point B
				final float bDpPerSecond = inputStroke.getDpPerSecond(i + 1);
				final float bVelScale = Math.min(bDpPerSecond / maxVel, 1f);
				final float bRadius = accumulator.add(minRadius + bVelScale * bVelScale * deltaRadius);

				//Log.i(TAG, "i: " + i + " bDpPerSecond: " + bDpPerSecond + " bVelScale: " + bVelScale + " bRadius: " + bRadius);

				// time interpolator
				final float dt = 1f / subdivisions;
				float t = dt;

				// radius interpolator
				final float dr = (bRadius - aRadius) / subdivisions;
				float r = aRadius + dr;

				for (int j = 0; j < subdivisions; j++, t += dt, r += dr) {
					cbi.getBezierPoint(t, bp);
					add(new Point(bp, r));
				}
			} else {
				add(new Point(a.position, aRadius));
			}
		}

		invalidate();
	}

	@Nullable
	public static Stroke smoothedStroke(InputStroke inputStroke, float minDiameter, float maxDiameter, float maxVel) {
		Stroke stroke = new Stroke();
		stroke.appendAndSmooth(inputStroke, 0, inputStroke.size() - 1, minDiameter, maxDiameter, maxVel);
		return stroke;
	}

	/**
	 * Initialize start Stroke from an inputStroke, scaling the points' radii by velocityScaling.
	 *
	 * @param inputStroke line describing drawing input
	 * @param minDiameter the min diameter of points added for slow moving line segments
	 * @param maxDiameter the max diameter of points added for fast moving line segments
	 * @param maxVel      the max velocity of line segments to produce maxDiameter points
	 */
	public Stroke(InputStroke inputStroke, float minDiameter, float maxDiameter, float maxVel) {
		if (inputStroke.size() < 2) {
			return;
		}

		Accumulator accumulator = new Accumulator(16, 0);
		ArrayList<InputStroke.Point> points = inputStroke.getPoints();

		// start is the previous point, b is current point, c is next point
		InputStroke.Point a = null, b = points.get(0), c = points.get(1);
		final float thicknessDelta = maxDiameter - minDiameter;

		for (int i = 0, N = points.size(); i < N; i++) {
			Point cp = null;
			if (a == null) {
				if (b != null && c != null) { // b & c should always be nonnull, just silencing compiler warnings
					cp = new Point(b.position, 0);
				}
			} else if (c == null) {
				if (b != null) { // b should always be nonnull, just silencing compiler warnings
					// this is last point in sequence
					cp = new Point(b.position, 0);
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

					// appendAndSmooth the size
					diameter = accumulator.add(diameter);

					cp = new Point(b.position, diameter * 0.5f);
				}
			}

			if (cp != null) {
				this.points.add(cp);
			}

			a = b;
			b = c;
			c = (i + 2 < N) ? points.get(i + 2) : null;
		}

		invalidate();
	}

	/**
	 * Get the array of points. You shouldn't modify this array.
	 *
	 * @return ArrayList of points making up this line
	 */
	public ArrayList<Point> getPoints() {
		return points;
	}

	/**
	 * Get the point at index i. If i < 0, index from end of list
	 *
	 * @param i index
	 * @return point at index, or point at size+index if index is negative
	 */
	public Point get(int i) {
		if (i < 0) {
			return points.get(points.size() + i);
		}
		return points.get(i);
	}

	/**
	 * Get number of points making up list
	 *
	 * @return
	 */
	public int size() {
		return points.size();
	}

	/**
	 * Check if list is empty
	 *
	 * @return true if list is empty
	 */
	public boolean isEmpty() {
		return points.isEmpty();
	}

	/**
	 * Adds start new point to this line. If the new point is close to the last point in the line, the two will be averaged.
	 *
	 * @param point
	 */
	public void add(Point point) {
		Point lastPoint = this.lastCircle();
		if (lastPoint != null && PointFUtil.distance2(point.position, lastPoint.position) < SNAP_DIST_SQUARED) {
			// average positions & radius of lastPoint and incoming point
			lastPoint.position.x = (lastPoint.position.x + point.position.x) * 0.5f;
			lastPoint.position.y = (lastPoint.position.y + point.position.y) * 0.5f;
			lastPoint.radius = (lastPoint.radius + point.radius) * 0.5f;
		} else {
			points.add(point);
		}

		invalidate();
	}

	public void addNoCheck(Point point) {
		points.add(point);
		invalidate();
	}

	/**
	 * @return The first point in the line, or null if empty
	 */
	@Nullable
	public Point firstCircle() {
		return points.isEmpty() ? null : points.get(0);
	}

	/**
	 * @return The last point in the line, or null if empty
	 */
	@Nullable
	public Point lastCircle() {
		return points.isEmpty() ? null : points.get(points.size() - 1);
	}

	public void invalidate() {
		boundingRect = null;
		needsTessellation = true;
	}

	/**
	 * Get the tessellated path circumscribing this circleLine
	 *
	 * @return Path representing this Stroke
	 */
	public Path getPath() {
		if (needsTessellation) {
			path.reset();
			tessellator.tessellate(this, path);
		}

		return path;
	}

	/**
	 * @return The rect bounding this Stroke
	 */
	public RectF getBoundingRect() {
		if (boundingRect == null) {
			if (isEmpty()) {
				// empty
				boundingRect = new RectF();
			} else {
				Point c = get(0);
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
			out.writeObject(points.get(i));
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		points = new ArrayList<>();
		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			Point cp = (Point) in.readObject();
			points.add(cp);
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
		for (Point point : points) {
			dest.writeParcelable(point, 0);
		}
	}

	public static final Parcelable.Creator<Stroke> CREATOR = new Parcelable.Creator<Stroke>() {
		public Stroke createFromParcel(Parcel in) {
			return new Stroke(in);
		}

		public Stroke[] newArray(int size) {
			return new Stroke[size];
		}
	};

	private Stroke(Parcel in) {
		int size = in.readInt();
		points = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Point point = in.readParcelable(null);
			points.add(point);
		}

		invalidate();
	}

	/**
	 * Created by shamyl on 9/22/15.
	 */
	public static class Point implements Serializable, Parcelable {

		private static final long serialVersionUID = 0L;

		public PointF position; // the position of the point
		public float radius; // the half thickness of the line at this point

		Point() {
			position = new PointF(0, 0);
		}

		public Point(PointF position, float radius) {
			this.position = new PointF(position.x, position.y);
			this.radius = radius;
		}

		public RectF getBoundingRect() {
			return new RectF(position.x - radius, position.y - radius, position.x + radius, position.y + radius);
		}

		/**
		 * Check if this point is bigger than another point, and completely contains it.
		 * Note that start.contains(b) != b.contains(start)
		 *
		 * @param other the point to check if its inside this point
		 * @return true if other is completely inside this point
		 */
		public boolean contains(Point other) {
			if (other.radius < radius) {
				float minDist = radius - other.radius;
				float minDist2 = minDist * minDist;
				float dist2 = PointFUtil.distance2(position, other.position);
				return dist2 < minDist2;
			} else if (other.radius == radius) {
				return PointFUtil.distance2(position, other.position) < 1e-3;
			} else {
				return false;
			}
		}

		/**
		 * Check if this point intersects another point
		 *
		 * @param other the point to test if it intersects with this point
		 * @return true if the two points intersect
		 */
		public boolean intersects(Point other) {
			float minDist2 = radius * radius;
			float dist2 = PointFUtil.distance2(position, other.position);
			return dist2 < minDist2;
		}

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.writeFloat(position.x);
			out.writeFloat(position.y);
			out.writeFloat(radius);
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			position = new PointF(in.readFloat(), in.readFloat());
			radius = in.readFloat();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeFloat(position.x);
			dest.writeFloat(position.y);
			dest.writeFloat(radius);
		}

		public static final Creator<Point> CREATOR = new Creator<Point>() {
			public Point createFromParcel(Parcel in) {
				return new Point(in);
			}

			public Point[] newArray(int size) {
				return new Point[size];
			}
		};

		private Point(Parcel in) {
			position = new PointF(in.readFloat(), in.readFloat());
			radius = in.readFloat();
		}
	}
}
