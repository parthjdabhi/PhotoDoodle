package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;

/**
 * Created by shamyl on 9/28/15.
 */
public class InputPointLine {
	private ArrayList<InputPoint> points = new ArrayList<>();

	public ArrayList<InputPoint> getPoints() {
		return points;
	}

	public int size() {
		return points.size();
	}

	@Nullable
	public InputPoint firstPoint() {
		return points.isEmpty() ? null : points.get(0);
	}

	@Nullable
	public InputPoint lastPoint() {
		return points.isEmpty() ? null : points.get(points.size()-1);
	}

	public void add(float x, float y) {
		InputPoint p = new InputPoint(x, y);
		points.add(p);

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
}
