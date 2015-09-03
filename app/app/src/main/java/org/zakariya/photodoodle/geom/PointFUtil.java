package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.util.Pair;

/**
 * Created by shamyl on 9/2/15.
 */
public class PointFUtil {

	static float distance(PointF a, PointF b) {
		float dx = b.x - a.x;
		float dy = b.y - a.y;
		return (float) Math.sqrt((dx * dx) + (dy * dy));
	}

	static PointF scale(PointF p, float scale) {
		return new PointF(p.x * scale, p.y * scale);
	}

	static PointF add(PointF a, PointF b) {
		return new PointF(a.x + b.x, a.y + b.y);
	}

	static PointF subtract(PointF a, PointF b) {
		return new PointF(a.x - b.x, a.y - b.y);
	}

	static Pair<PointF,Float> normalize(PointF p) {
		float length = p.length() + 0.001f; // a tiny fudge to prevent div by zero
		float rLength = 1/length;
		return new Pair<>(new PointF(p.x * rLength, p.y * rLength), length);
	}
}
