package org.zakariya.photodoodle.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.view.ColorPickerView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by shamyl on 11/7/15.
 */
public class ColorPickerTestFragment extends Fragment {

	@Bind(R.id.colorPicker)
	ColorPickerView colorPickerView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_color_picker_test, container, false);
		ButterKnife.bind(this, v);
		return v;
	}
}
