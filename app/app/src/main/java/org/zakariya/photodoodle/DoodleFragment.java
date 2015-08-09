package org.zakariya.photodoodle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleFragment extends Fragment {

	@Bind(R.id.doodleView) DoodleView doodleView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_doodle,container,false);
		ButterKnife.bind(this,v);
		return v;
	}
}
