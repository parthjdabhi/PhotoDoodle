package org.zakariya.photodoodle.activities;

import android.support.v4.app.Fragment;

import org.zakariya.photodoodle.fragments.AboutFragment;

/**
 * Created by shamyl on 12/28/15.
 */
public class AboutActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new AboutFragment();
	}

}
