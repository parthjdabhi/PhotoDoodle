package org.zakariya.photodoodle.util;

import java.util.Arrays;

/**
 * Created by shamyl on 9/29/15.
 */
public class FloatBuffer {
	private float buffer[] = null;
	private int size = 0;

	public FloatBuffer() {
		this(256);
	}

	public FloatBuffer(int initialBufferSize) {
		buffer = new float[initialBufferSize];
		this.size = 0;
	}

	public void add(float v) {
		if (size == buffer.length) {
			buffer = Arrays.copyOf(buffer, buffer.length * 2);
		}

		buffer[size++] = v;
	}

	public float get(int i) {
		return buffer[i];
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	/**
	 * Get a compacted copy of the buffer
	 *
	 * @return buffer's contents, compacted to size, such that buffer.length == size
	 */
	public float[] getBuffer() {
		return Arrays.copyOf(buffer, size);
	}
}
