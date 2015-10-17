package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class InputStroke implements Serializable, Parcelable {

	private static final String TAG = "InputStroke";

	private ArrayList<Point> points = new ArrayList<>();
	private RectF boundingRect = new RectF();

	public InputStroke() {
	}

	public ArrayList<Point> getPoints() {
		return points;
	}

	public int size() {
		return points.size();
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	public Point get(int i) {
		if (i < 0) {
			return get(points.size() + i);
		} else {
			return points.get(i);
		}
	}

	/**
	 * Get the tangent of the line at a given point
	 *
	 * @param i the index of the point of interest
	 * @return the tangent vector, computed as the bisecting unit vector between the preceeding line segment and the outgoing line segment, pointing "forward"
	 */
	public PointF getTangent(int i) {
		final int count = points.size();
		if (count < 2) {
			return new PointF(0, 0);
		}

		if (i == 0) {
			return PointFUtil.dir(get(0).position, get(1).position).first;
		} else if (i == count - 1) {
			return PointFUtil.dir(get(i - 1).position, get(i).position).first;
		} else {
			PointF a = get(i - 1).position;
			PointF b = get(i).position;
			PointF c = get(i + 1).position;

			Pair<PointF, Float> abDir = PointFUtil.dir(a, b);
			PointF abPrime = PointFUtil.rotateCCW(abDir.first);

			Pair<PointF, Float> bcDir = PointFUtil.dir(b, c);
			PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

			PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
			if (PointFUtil.length2(half) > 1e-4) {
				return PointFUtil.normalize(PointFUtil.rotateCW(half)).first;
			} else {
				return bcPrime;
			}
		}
	}

	/**
	 * Get rough estimate of the velocity, in dp-per-second, of the user's finger when drawing the point at index `i
	 *
	 * @param i index of point to query dp-per-second
	 * @return rough dp-per-second of input point at requested index
	 */
	public float getDpPerSecond(int i) {
		if (i == 0) {
			return 0;
		} else if (i < points.size()) {
			final Point a = points.get(i - 1);
			final Point b = points.get(i);
			final float length = PointFUtil.distance(a.position, b.position);
			final long millis = b.timestamp - a.timestamp;
			return length / (millis / 1000f);
		} else if (i < 0) {
			return getDpPerSecond(points.size() + i);
		} else {
			return 0;
		}
	}

	@Nullable
	public Point firstPoint() {
		return points.isEmpty() ? null : points.get(0);
	}

	@Nullable
	public Point lastPoint() {
		return points.isEmpty() ? null : points.get(points.size() - 1);
	}

	public void add(float x, float y, long timestamp) {
		Point p = new Point(x, y, timestamp);
		points.add(p);

		if (points.size() == 1) {
			// give it a little space since a point has no area
			boundingRect.set(x - 0.5f, y - 0.5f, x + 0.5f, y + 0.5f);
		} else {
			boundingRect.union(x, y);
		}
	}

	public void add(float x, float y) {
		add(x, y, System.currentTimeMillis());
	}

	public void invalidate() {
		computeBoundingRect();
	}

	public RectF getBoundingRect() {
		return boundingRect;
	}

	public RectF computeBoundingRect() {
		if (!isEmpty()) {
			Point p = get(0);
			boundingRect = new RectF(p.position.x - 0.5f, p.position.y - 0.5f, p.position.x + 0.5f, p.position.y + 0.5f);
			for (int i = 1, N = size(); i < N; i++) {
				p = get(i);
				boundingRect.union(p.position.x, p.position.y);
			}
		} else {
			boundingRect = new RectF();
		}

		return boundingRect;
	}

	@Override
	public String toString() {
		return TextUtils.join(",", points);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return copy();
	}

	public InputStroke copy() {
		InputStroke c = new InputStroke();
		int size = points.size();
		c.points.ensureCapacity(size);
		for (int i = 0; i < size; i++) {
			c.points.add(points.get(i).copy());
		}

		return c;
	}

	/**
	 * Return an optimized InputStroke where vertices which deviate less than `threshold from the line defined by their neighbors are excised
	 *
	 * @param threshold the minimum linear deviation for a vertex to be included in final stroke
	 * @return optimized InputStroke with fewer points
	 */
	public InputStroke optimize(float threshold) {
		InputStroke optimized = optimize(this, threshold, 0);
		optimized.computeBoundingRect();

		Log.i(TAG, "optimize this.size: " + size() + " optimized.size: " + optimized.size());

		return optimized;
	}

	private static InputStroke optimize(InputStroke in, float thresholdSquared, int depth) {

		if (in.size() <= 2) {
			return in;
		}

		//
		//	Find the vertex farthest from the line defined by the start and and of the path
		//

		float maxDistSquared = 0;
		int maxDistSquaredIndex = 0;
		final int size = in.size();
		final Point first = in.get(0);
		final Point last = in.get(size - 1);
		final LineSegment line = new LineSegment(first.position, last.position);

		for (int i = 1; i < size - 1; i++) {
			float dist = line.distanceSquared(in.get(i).position, true);
			if (dist > maxDistSquared) {
				maxDistSquared = dist;
				maxDistSquaredIndex = i;
			}
		}

		//
		//	If the farthest vertex is greater than our thresholdSquared, we need to
		//	partition and optimize left and right separately
		//

		if (maxDistSquared > thresholdSquared) {


			//
			//	Partition 'in' into left and right sub vectors, optimize them and join
			//

			InputStroke left = slice(in, 0, maxDistSquaredIndex + 1);
			InputStroke right = slice(in, maxDistSquaredIndex, size);
			InputStroke leftSimplified = optimize(left, thresholdSquared, depth + 1);
			InputStroke rightSimplified = optimize(right, thresholdSquared, depth + 1);

			InputStroke joined = new InputStroke();
			joined.points.ensureCapacity(leftSimplified.size() + rightSimplified.size() - 1);

			for (int i = 0; i < leftSimplified.size() - 1; i++) {
				joined.points.add(leftSimplified.points.get(i));
			}

			// skip first point of right since it's same as last of left
			for (int i = 0; i < rightSimplified.size(); i++) {
				joined.points.add(rightSimplified.points.get(i));
			}

			return joined;
		} else {

			//
			//  The line's straight enough that we don't need to keep the middle points
			//

			InputStroke optimized = new InputStroke();
			optimized.points.ensureCapacity(2);
			optimized.points.add(first);
			optimized.points.add(last);

			return optimized;
		}
	}

	/**
	 * Cut out a slice of an InputStroke starting at index start, up to but not including element at end index
	 *
	 * @param stroke the stroke to slice
	 * @param start index of first element to copy
	 * @param end   end of slice, not included in output
	 * @return sub slice of this InputStroke
	 * NOTE: Does not update bounds of slice, you must call computeBoundingRect if you need the bounds updated.
	 */
	private static InputStroke slice(InputStroke stroke, int start, int end) {
		InputStroke s = new InputStroke();
		s.points.ensureCapacity(end - start);

		for (int i = start; i < end; i++) {
			s.points.add(stroke.points.get(i));
		}

		return s;
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
			Point p = (Point) in.readObject();
			points.add(p);
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

	public static final Parcelable.Creator<InputStroke> CREATOR = new Parcelable.Creator<InputStroke>() {
		public InputStroke createFromParcel(Parcel in) {
			return new InputStroke(in);
		}

		public InputStroke[] newArray(int size) {
			return new InputStroke[size];
		}
	};

	private InputStroke(Parcel in) {
		points = new ArrayList<>();
		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			Point p = in.readParcelable(null);
			points.add(p);
		}

		invalidate();
	}

	/**
	 * Represents user input. As user drags across screen, each location is recorded along with its timestamp.
	 * The timestamps can be compared across an array of Point to determine the velocity of the touch,
	 * which will be used to determine line thickness.
	 */
	public static class Point implements Serializable, Parcelable {
		public PointF position = new PointF();
		public long timestamp;

		Point() {
		}

		public Point(float x, float y) {
			position.x = x;
			position.y = y;
			timestamp = System.currentTimeMillis();
		}

		public Point(float x, float y, long timestamp) {
			position.x = x;
			position.y = y;
			this.timestamp = timestamp;
		}

		@Override
		public String toString() {
			return "(" + position.x + "," + position.y + ")";
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return copy();
		}

		public Point copy() {
			return new Point(position.x, position.y, timestamp);
		}

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.writeFloat(position.x);
			out.writeFloat(position.y);
			out.writeLong(timestamp);
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			position = new PointF(in.readFloat(), in.readFloat());
			timestamp = in.readLong();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeFloat(position.x);
			dest.writeFloat(position.y);
			dest.writeLong(timestamp);
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
			timestamp = in.readLong();
		}
	}
}
