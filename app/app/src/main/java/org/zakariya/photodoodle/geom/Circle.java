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

	public PointF position = new PointF(); // the position of the point
	public float radius; // the half thickness of the line at this point

	Circle() {
	}

	public Circle(PointF position, float radius) {
		this.position.x = position.x;
		this.position.y = position.y;
		this.radius = radius;
	}

	public RectF getBoundingRect() {
		return new RectF(position.x - radius, position.y - radius, position.x + radius, position.y + radius);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeFloat(position.x);
		out.writeFloat(position.y);
		out.writeFloat(radius);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		position.x = in.readFloat();
		position.y = in.readFloat();
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
		position.x = in.readFloat();
		position.y = in.readFloat();
		radius = in.readFloat();
	}
}
