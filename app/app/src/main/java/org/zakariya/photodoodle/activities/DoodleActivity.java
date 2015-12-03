package org.zakariya.photodoodle.activities;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.TintManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

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
	Drawable cameraTabIcon;
	Drawable drawingTabIcon;

	DoodleFragment doodleFragment;

	boolean suppressTabPopup;
	PopupWindow tabPopup;

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
			suppressTabPopup = true;
			switch (doodleFragment.getInteractionMode()) {
				case PHOTO:
					cameraTab.select();
					break;
				case DRAW:
					drawingTab.select();
					break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (tabPopup != null) {
			tabPopup.dismiss();
			tabPopup = null;
		} else {
			super.onBackPressed();
		}
	}

	private void configureModeTabs() {
		cameraTab = modeTabs.newTab();
		cameraTabIcon = TintManager.get(this).getDrawable(R.drawable.ic_photo_camera);
		cameraTab.setIcon(cameraTabIcon);
		modeTabs.addTab(cameraTab);

		drawingTab = modeTabs.newTab();
		drawingTabIcon = TintManager.get(this).getDrawable(R.drawable.ic_mode_edit);
		drawingTab.setIcon(drawingTabIcon);
		modeTabs.addTab(drawingTab, true); // make this tab selected by default

		modeTabs.setOnTabSelectedListener(this);
	}

	@Override
	public void onTabSelected(TabLayout.Tab tab) {
		if (tab == cameraTab) {
			doodleFragment.setInteractionMode(PhotoDoodle.InteractionMode.PHOTO);
		} else if (tab == drawingTab) {
			doodleFragment.setInteractionMode(PhotoDoodle.InteractionMode.DRAW);
		}

		updateModeTabDrawables();
		showTabItemPopup();
	}

	@Override
	public void onTabUnselected(TabLayout.Tab tab) {
	}

	@Override
	public void onTabReselected(TabLayout.Tab tab) {
		updateModeTabDrawables();
		showTabItemPopup();
	}

	private View getSelectedTabItemView() {
		// this is highly dependant on TabLayout's private implementation. I'm not happy about this.
		ViewGroup tabStrip = (ViewGroup) modeTabs.getChildAt(modeTabs.getChildCount() - 1);
		return tabStrip.getChildAt(modeTabs.getSelectedTabPosition());
	}

	private void showTabItemPopup() {
		if (suppressTabPopup) {
			suppressTabPopup = false;
			return;
		}

		if (tabPopup != null) {
			tabPopup.dismiss();
			tabPopup = null;
		}

		@LayoutRes int layoutId = 0;
		switch (doodleFragment.getInteractionMode()) {
			case PHOTO:
				layoutId = R.layout.popup_camera;
				break;
			case DRAW:
				layoutId = R.layout.drawing_popup;
				break;
		}

		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(layoutId, null);
		layout.measure(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

		tabPopup = new PopupWindow(this);
		tabPopup.setContentView(layout);
		tabPopup.setWidth(layout.getMeasuredWidth());
		tabPopup.setHeight(layout.getMeasuredHeight());

		//noinspection deprecation
		tabPopup.setBackgroundDrawable(new BitmapDrawable());
		tabPopup.setOutsideTouchable(true);

		tabPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				Log.i(TAG, "onDismiss: dismissed popup");
			}
		});

		tabPopup.showAsDropDown(getSelectedTabItemView());
	}

	private void updateModeTabDrawables() {
		if (doodleFragment != null) {
			switch (doodleFragment.getInteractionMode()) {
				case PHOTO:
					cameraTabIcon.setAlpha(255);
					drawingTabIcon.setAlpha(64);
					break;
				case DRAW:
					cameraTabIcon.setAlpha(64);
					drawingTabIcon.setAlpha(255);
					break;
			}
		}
	}
}
