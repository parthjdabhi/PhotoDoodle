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
public class ControlPoint implements Serializable, Parcelable {

	private static final long serialVersionUID = 0L;

	public PointF position; // the position of the point
	public PointF tangent; // the normalized tangent vector of the line at this point - points "forward"
	public float halfSize; // the half thickness of the line at this point

	ControlPoint() {
	}

	public ControlPoint(PointF position, float halfSize) {
		this.position = position;
		this.halfSize = halfSize;
		this.tangent = new PointF(0, 0);
	}

	public ControlPoint(PointF position, PointF tangent, float halfSize) {
		this.position = position;
		this.tangent = tangent;
		this.halfSize = halfSize;
	}

	public RectF getBoundingRect() {
		return new RectF(position.x - halfSize, position.y - halfSize, position.x + halfSize, position.y + halfSize);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeFloat(position.x);
		out.writeFloat(position.y);
		out.writeFloat(tangent.x);
		out.writeFloat(tangent.y);
		out.writeFloat(halfSize);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		position = new PointF(in.readFloat(),in.readFloat());
		tangent = new PointF(in.readFloat(),in.readFloat());
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
		dest.writeFloat(tangent.x);
		dest.writeFloat(tangent.y);
		dest.writeFloat(halfSize);
	}

	public static final Parcelable.Creator<ControlPoint> CREATOR = new Parcelable.Creator<ControlPoint>() {
		public ControlPoint createFromParcel(Parcel in) {
			return new ControlPoint(in);
		}

		public ControlPoint[] newArray(int size) {
			return new ControlPoint[size];
		}
	};

	private ControlPoint(Parcel in) {
		position = new PointF(in.readFloat(),in.readFloat());
		tangent = new PointF(in.readFloat(),in.readFloat());
		halfSize = in.readFloat();
	}
}
