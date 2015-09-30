package org.zakariya.photodoodle.geom;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Created by shamyl on 9/3/15.
 */
public class RectFUtil {

	public static RectF containing(PointF a, PointF b) {
		return new RectF(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y));
	}
}
