package org.zakariya.photodoodle.activities;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Fade;
import android.transition.Transition;

import org.zakariya.photodoodle.fragments.DoodleDocumentGridFragment;

/**
 * Created by shamyl on 12/16/15.
 */
public class MainActivity extends SingleFragmentActivity {

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Transition fade = new Fade();
			fade.excludeTarget(android.R.id.navigationBarBackground, true);
			getWindow().setExitTransition(fade);
			getWindow().setEnterTransition(fade);
		}
	}

	@Override
	protected Fragment createFragment() {
		return new DoodleDocumentGridFragment();
	}
}
