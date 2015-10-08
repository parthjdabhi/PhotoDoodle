package org.zakariya.photodoodle.util;

/**
 * Created by shamyl on 9/22/15.
 */
public class Accumulator {
	float[] values;
	float current;
	float accumulator;
	int size = 0;
	int index = 0;

	public Accumulator(int size, float current) {
		if (size > 0) {
			values = new float[size];
		}
		this.size = size;
		this.index = 0;
		this.accumulator = 0;
		this.current = current;
	}

	public float add(float v) {

		if (size == 0) {

			//
			// for zero-size, just pass through
			//

			current = v;
			return v;
		} else if (index < size) {

			//
			//  We're still filling the buffer
			//

			values[index++] = v;
			accumulator += v;
		} else {

			//
			//  Buffer is full - round-robin and average
			//  Subtract oldest value from accumulator
			//  Replace oldest with newest
			//  add newest to accumulator
			//

			accumulator -= values[index % size];
			values[index++ % size] = v;
			accumulator += v;
		}
		current = accumulator / (float) index;
		return current;
	}

	public float getCurrent() {
		return current;
	}

	public boolean isPrimed() {
		return size <= 0 || index >= size;
	}
}
