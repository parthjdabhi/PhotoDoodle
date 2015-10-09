package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class InputStroke implements Serializable, Parcelable {
	private ArrayList<Point> points = new ArrayList<>();
	RectF boundingRect = new RectF();

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

		int count = points.size();
		if (count == 2) {
			Point a = points.get(0);
			Point b = points.get(1);
			a.tangent = PointFUtil.dir(a.position, b.position).first;
		}

		if (count > 2) {
			Point a = points.get(count - 3);
			Point b = points.get(count - 2);
			Point c = points.get(count - 1);

			Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
			PointF abPrime = PointFUtil.rotateCCW(abDir.first);

			Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
			PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

			PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
			if (PointFUtil.length2(half) > 1e-4) {
				b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;
			} else {
				b.tangent = bcPrime;
			}
		}
	}

	public void add(float x, float y) {
		add(x,y,System.currentTimeMillis());
	}

	public void finish() {
		int count = points.size();
		if (count > 1) {
			Point a = points.get(count - 2);
			Point b = points.get(count - 1);
			a.tangent = PointFUtil.dir(a.position, b.position).first;
		}
	}

	public void invalidate() {
		computeBoundingRect();
		computeTangents();
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

	/**
	 * Analyze the line and compute tangent vectors for each vertex
	 */
	public void computeTangents() {
		if (points.size() < 3) {
			return;
		}

		for (int i = 0, N = points.size(); i < N; i++) {
			if (i == 0) {
				Point a = points.get(i);
				Point b = points.get(i + 1);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				a.tangent = dir.first;
			} else if (i == N - 1) {
				Point a = points.get(i - 1);
				Point b = points.get(i);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				b.tangent = dir.first;
			} else {
				Point a = points.get(i - 1);
				Point b = points.get(i);
				Point c = points.get(i + 1);

				Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
				PointF abPrime = PointFUtil.rotateCCW(abDir.first);

				Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
				PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

				PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
				if (PointFUtil.length2(half) > 1e-4) {
					b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;
				} else {
					b.tangent = bcPrime;
				}
			}
		}
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
	 * The timestamps can be compared across an array of Circle to determine the velocity of the touch,
	 * which will be used to determine line thickness.
	 */
	public static class Point implements Serializable, Parcelable {
		public PointF position = new PointF();
		public PointF tangent = new PointF();
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

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			out.writeFloat(position.x);
			out.writeFloat(position.y);
			out.writeFloat(tangent.x);
			out.writeFloat(tangent.y);
			out.writeLong(timestamp);
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			position = new PointF(in.readFloat(), in.readFloat());
			tangent = new PointF(in.readFloat(), in.readFloat());
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
			dest.writeFloat(tangent.x);
			dest.writeFloat(tangent.y);
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
			tangent = new PointF(in.readFloat(), in.readFloat());
			timestamp = in.readLong();
		}
	}
}
