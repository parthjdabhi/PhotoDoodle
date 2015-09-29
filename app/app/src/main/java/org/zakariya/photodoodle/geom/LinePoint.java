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
public class LinePoint implements Serializable, Parcelable {

	private static final long serialVersionUID = 0L;

	public PointF position = new PointF(); // the position of the point
	public float halfSize; // the half thickness of the line at this point

	LinePoint() {
	}

	public LinePoint(PointF position, float halfSize) {
		this.position.x = position.x;
		this.position.y = position.y;
		this.halfSize = halfSize;
	}

	public RectF getBoundingRect() {
		return new RectF(position.x - halfSize, position.y - halfSize, position.x + halfSize, position.y + halfSize);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeFloat(position.x);
		out.writeFloat(position.y);
		out.writeFloat(halfSize);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		position.x = in.readFloat();
		position.y = in.readFloat();
		halfSize = in.readFloat();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloat(position.x);
		dest.writeFloat(position.y);
		dest.writeFloat(halfSize);
	}

	public static final Parcelable.Creator<LinePoint> CREATOR = new Parcelable.Creator<LinePoint>() {
		public LinePoint createFromParcel(Parcel in) {
			return new LinePoint(in);
		}
		public LinePoint[] newArray(int size) {
			return new LinePoint[size];
		}
	};

	private LinePoint(Parcel in) {
		position.x = in.readFloat();
		position.y = in.readFloat();
		halfSize = in.readFloat();
	}
}
