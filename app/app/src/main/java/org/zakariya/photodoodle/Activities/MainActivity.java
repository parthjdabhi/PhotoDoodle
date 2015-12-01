package org.zakariya.photodoodle.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.zakariya.photodoodle.fragments.DoodleFragment;

public class MainActivity extends SingleFragmentActivity {

	static final String TAG = "MainActivity";

	@Override
	protected Fragment createFragment() {
		return new DoodleFragment();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		Log.d(TAG, "Starting ColorPickerTestActivity");
//		Intent intent = new Intent(this, ColorPickerTestActivity.class);
//		startActivity(intent);
	}
}
