package org.zakariya.photodoodle.activities;

import android.support.v4.app.Fragment;

import org.zakariya.photodoodle.fragments.DoodleDocumentGridFragment;

/**
 * Created by shamyl on 12/16/15.
 */
public class MainActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new DoodleDocumentGridFragment();
	}
}
