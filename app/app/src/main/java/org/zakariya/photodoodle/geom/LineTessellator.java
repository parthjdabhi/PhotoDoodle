package org.zakariya.photodoodle.geom;

import android.graphics.Canvas;
import android.graphics.Path;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shamyl on 9/24/15.
 */
public class LineTessellator {

	class FloatBuffer {
		private float buffer[] = null;
		private int size = 0;

		FloatBuffer() {
			buffer = new float[256];
			this.size = 0;
		}

		FloatBuffer(int initialBufferSize) {
			buffer = new float[initialBufferSize];
			this.size = 0;
		}

		void add(float v) {
			if (size == buffer.length) {
				buffer = Arrays.copyOf(buffer, buffer.length * 2);
			}

			buffer[size++] = v;
		}

		float get(int i) {
			return buffer[i];
		}

		int size() {
			return size;
		}

		void clear() {
			size = 0;
		}
	}

	private FloatBuffer leftCoordinates;
	private FloatBuffer rightCoordinates;

	public LineTessellator(){}

	/**
	 * Tessellate linePoints into a Path object suitable for rendering
	 * @param linePoints list of LinePoint instances, in order
	 * @param path destination Path into which tessellated closed path will be added, if non-null
	 * @param debugDrawCanvas if non-null, tessellation debug line rendering will be drawn immediately
	 */
	public void tessellate(List<LinePoint> linePoints, @Nullable Path path, @Nullable Canvas debugDrawCanvas) {

		// set up our buffers
		if (leftCoordinates == null) {
			leftCoordinates = new FloatBuffer((int)(linePoints.size() * 1.5));
		} else {
			leftCoordinates.clear();
		}

		if (rightCoordinates == null) {
			rightCoordinates = new FloatBuffer((int)(linePoints.size() * 1.5));
		} else {
			rightCoordinates.clear();
		}



		// and we're done - add to Path
		if (path != null) {
			addToPath(path);
		}
	}



	private void addToPath(Path path) {
		float x = leftCoordinates.get(0);
		float y = leftCoordinates.get(1);
		path.moveTo(x, y);

		for (int i = 2, N = leftCoordinates.size(); i < N; i += 2) {
			x = leftCoordinates.get(i);
			y = leftCoordinates.get(i + 1);
			path.lineTo(x, y);
		}

		for (int i = rightCoordinates.size() - 2; i >= 0; i -= 2) {
			x = rightCoordinates.get(i);
			y = rightCoordinates.get(i+1);
			path.lineTo(x,y);
		}

		path.close();
	}

}
