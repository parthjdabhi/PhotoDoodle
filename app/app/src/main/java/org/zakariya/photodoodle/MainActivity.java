package org.zakariya.photodoodle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class MainActivity extends SingleFragmentActivity {

	static final String TAG = "MainActivity";

	@Override
	protected Fragment createFragment() {
		return new DoodleFragment();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
