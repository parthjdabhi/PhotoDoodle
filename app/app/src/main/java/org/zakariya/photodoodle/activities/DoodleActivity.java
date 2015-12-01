package org.zakariya.photodoodle.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.fragments.DoodleFragment;
import org.zakariya.photodoodle.model.PhotoDoodle;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DoodleActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

	private static final String TAG = "DoodleActivity";

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.modeTabs)
	TabLayout modeTabs;

	TabLayout.Tab cameraTab;
	TabLayout.Tab drawingTab;

	DoodleFragment doodleFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_doodle);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
		configureModeTabs();

		FragmentManager fm = getSupportFragmentManager();
		doodleFragment = (DoodleFragment) fm.findFragmentById(R.id.container);
		if (doodleFragment == null) {
			doodleFragment = new DoodleFragment();
			fm.beginTransaction().add(R.id.container, doodleFragment).commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (doodleFragment != null) {
			switch(doodleFragment.getInteractionMode()) {
				case PHOTO:
					cameraTab.select();
					break;
				case DRAW:
					drawingTab.select();
					break;
			}
		}
	}

	private void configureModeTabs() {
		cameraTab = modeTabs.newTab();
		cameraTab.setIcon(R.drawable.ic_photo_camera);
		modeTabs.addTab(cameraTab);

		drawingTab = modeTabs.newTab();
		drawingTab.setIcon(R.drawable.ic_mode_edit);
		modeTabs.addTab(drawingTab, true); // make this tab selected by default

		modeTabs.setOnTabSelectedListener(this);
	}

	@Override
	public void onTabSelected(TabLayout.Tab tab) {
		Log.i(TAG, "onTabSelected: tab: " + tab.getPosition());

		if (tab == cameraTab) {
			doodleFragment.setInteractionMode(PhotoDoodle.InteractionMode.PHOTO);
		} else if (tab == drawingTab) {
			doodleFragment.setInteractionMode(PhotoDoodle.InteractionMode.DRAW);
		}
	}

	@Override
	public void onTabUnselected(TabLayout.Tab tab) {
		Log.i(TAG, "onTabUnselected: tab: " + tab.getPosition());

	}

	@Override
	public void onTabReselected(TabLayout.Tab tab) {
		Log.i(TAG, "onTabReselected: tab: " + tab.getPosition());
	}
}
