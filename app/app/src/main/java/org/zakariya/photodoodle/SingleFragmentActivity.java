package org.zakariya.photodoodle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by shamyl on 8/9/15.
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {

	private static final String TAG = "SingleFragmentActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_single_fragment);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.container);
		if (fragment == null) {
			fragment = createFragment();
			fm.beginTransaction().add(R.id.container,fragment).commit();
		}
	}

	protected abstract Fragment createFragment();
}
