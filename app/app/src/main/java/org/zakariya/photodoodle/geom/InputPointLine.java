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
public class InputPointLine implements Serializable, Parcelable {
	private ArrayList<InputPoint> points = new ArrayList<>();
	RectF boundingRect = new RectF();

	public InputPointLine() {
	}

	public ArrayList<InputPoint> getPoints() {
		return points;
	}

	public int size() {
		return points.size();
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	public InputPoint get(int i) {
		if (i < 0) {
			return get(points.size() + i);
		} else {
			return points.get(i);
		}
	}

	@Nullable
	public InputPoint firstPoint() {
		return points.isEmpty() ? null : points.get(0);
	}

	@Nullable
	public InputPoint lastPoint() {
		return points.isEmpty() ? null : points.get(points.size() - 1);
	}

	public void add(float x, float y) {
		InputPoint p = new InputPoint(x, y);
		points.add(p);

		if (points.size() == 1) {
			// give it a little space since a point has no area
			boundingRect.set(x - 0.5f, y - 0.5f, x + 0.5f, y + 0.5f);
		} else {
			boundingRect.union(x, y);
		}

		int count = points.size();
		if (count == 2) {
			InputPoint a = points.get(0);
			InputPoint b = points.get(1);
			a.tangent = PointFUtil.dir(a.position, b.position).first;
		}

		if (count > 2) {
			InputPoint a = points.get(count - 3);
			InputPoint b = points.get(count - 2);
			InputPoint c = points.get(count - 1);

			Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
			PointF abPrime = PointFUtil.rotateCCW(abDir.first);

			Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
			PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

			PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
			if (PointFUtil.length2(half) > 1e-4) {
				b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;
				;
			} else {
				b.tangent = bcPrime;
			}
		}
	}

	public void finish() {
		int count = points.size();
		if (count > 1) {
			InputPoint a = points.get(count - 2);
			InputPoint b = points.get(count - 1);
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
			InputPoint p = get(0);
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
	 * Analyze the line and compute tangent vectors for each vertex
	 */
	public void computeTangents() {
		if (points.size() < 3) {
			return;
		}

		for (int i = 0, N = points.size(); i < N; i++) {
			if (i == 0) {
				InputPoint a = points.get(i);
				InputPoint b = points.get(i + 1);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				a.tangent = dir.first;
			} else if (i == N - 1) {
				InputPoint b = points.get(i);
				InputPoint a = points.get(i - 1);
				Pair<PointF, Float> dir = PointFUtil.dir(a.position, b.position);

				b.tangent = dir.first;
			} else {
				InputPoint a = points.get(i - 1);
				InputPoint b = points.get(i);
				InputPoint c = points.get(i + 1);

				Pair<PointF, Float> abDir = PointFUtil.dir(a.position, b.position);
				PointF abPrime = PointFUtil.rotateCCW(abDir.first);

				Pair<PointF, Float> bcDir = PointFUtil.dir(b.position, c.position);
				PointF bcPrime = PointFUtil.rotateCCW(bcDir.first);

				PointF half = new PointF(abPrime.x + bcPrime.x, abPrime.y + bcPrime.y);
				if (PointFUtil.length2(half) > 1e-4) {
					b.tangent = PointFUtil.normalize(PointFUtil.rotateCW(half)).first;
					;
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
			InputPoint p = (InputPoint) in.readObject();
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
		for (InputPoint point : points) {
			dest.writeParcelable(point, 0);
		}
	}

	public static final Parcelable.Creator<InputPointLine> CREATOR = new Parcelable.Creator<InputPointLine>() {
		public InputPointLine createFromParcel(Parcel in) {
			return new InputPointLine(in);
		}

		public InputPointLine[] newArray(int size) {
			return new InputPointLine[size];
		}
	};

	private InputPointLine(Parcel in) {
		points = new ArrayList<>();
		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			InputPoint p = in.readParcelable(null);
			points.add(p);
		}

		invalidate();
	}
}
