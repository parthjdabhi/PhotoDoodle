package org.zakariya.photodoodle.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.zakariya.photodoodle.R;

import butterknife.ButterKnife;
import icepick.Icepick;

/**
 * Created by shamyl on 12/16/15.
 */
public class DoodleDocumentGridFragment extends Fragment {

	public DoodleDocumentGridFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_doodle_document_grid, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menuItemNewDoodle:
				return true;
			case R.id.menuItemSettings:
				return true;
		}

		return false;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_doodle_document_grid, container, false);
		ButterKnife.bind(this, v);


		return v;
	}
}
