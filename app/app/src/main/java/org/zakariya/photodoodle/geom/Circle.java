package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by shamyl on 9/22/15.
 */
public class Circle implements Serializable, Parcelable {

	private static final long serialVersionUID = 0L;

	public PointF position; // the position of the point
	public float radius; // the half thickness of the line at this point

	Circle() {
		position = new PointF(0, 0);
	}

	public Circle(PointF position, float radius) {
		this.position = new PointF(position.x, position.y);
		this.radius = radius;
	}

	public RectF getBoundingRect() {
		return new RectF(position.x - radius, position.y - radius, position.x + radius, position.y + radius);
	}

	/**
	 * Check if this circle is bigger than another circle, and completely contains it.
	 * Note that a.contains(b) != b.contains(a)
	 *
	 * @param other the circle to check if its inside this circle
	 * @return true if other is completely inside this circle
	 */
	public boolean contains(Circle other) {
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
	 * Check if this circle intersects another circle
	 *
	 * @param other the circle to test if it intersects with this circle
	 * @return true if the two circles intersect
	 */
	public boolean intersects(Circle other) {
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

	public static final Parcelable.Creator<Circle> CREATOR = new Parcelable.Creator<Circle>() {
		public Circle createFromParcel(Parcel in) {
			return new Circle(in);
		}

		public Circle[] newArray(int size) {
			return new Circle[size];
		}
	};

	private Circle(Parcel in) {
		position = new PointF(in.readFloat(), in.readFloat());
		radius = in.readFloat();
	}
}
