package org.zakariya.photodoodle.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.zakariya.photodoodle.fragments.ColorPickerTestFragment;

public class ColorPickerTestActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new ColorPickerTestFragment();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
