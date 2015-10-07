package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents user input. As user drags across screen, each location is recorded along with its timestamp.
 * The timestamps can be compared across an array of Circle to determine the velocity of the touch,
 * which will be used to determine line thickness.
 */
public class InputPoint implements Serializable, Parcelable {
	public PointF position = new PointF();
	public PointF tangent = new PointF();
	public long timestamp;

	InputPoint() {
	}

	public InputPoint(float x, float y) {
		position.x = x;
		position.y = y;
		timestamp = System.currentTimeMillis();
	}

	public InputPoint(float x, float y, long timestamp) {
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

	public static final Parcelable.Creator<InputPoint> CREATOR = new Parcelable.Creator<InputPoint>() {
		public InputPoint createFromParcel(Parcel in) {
			return new InputPoint(in);
		}

		public InputPoint[] newArray(int size) {
			return new InputPoint[size];
		}
	};

	private InputPoint(Parcel in) {
		position = new PointF(in.readFloat(), in.readFloat());
		tangent = new PointF(in.readFloat(), in.readFloat());
		timestamp = in.readLong();
	}
}
