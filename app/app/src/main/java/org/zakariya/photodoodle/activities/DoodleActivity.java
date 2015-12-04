package org.zakariya.photodoodle.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.TintManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.model.Brush;
import org.zakariya.photodoodle.model.PhotoDoodle;
import org.zakariya.photodoodle.view.ColorPickerView;
import org.zakariya.photodoodle.view.ColorSwatchView;
import org.zakariya.photodoodle.view.DoodleView;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

public class DoodleActivity extends AppCompatActivity
		implements TabLayout.OnTabSelectedListener, CameraPopupController.Callbacks, DrawPopupController.Callbacks {

	private static final String TAG = "DoodleActivity";

	private static final String DOODLE_STATE = "DOODLE_STATE";

	private static final int REQUEST_TAKE_PHOTO = 1;

	enum BrushType {PENCIL, BRUSH, LARGE_ERASER, SMALL_ERASER}


	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.modeTabs)
	TabLayout modeTabs;

	@Bind(R.id.doodleView)
	DoodleView doodleView;

	@State
	int color = 0xFF000000;

	@State
	int selectedBrush = BrushType.PENCIL.ordinal();

	@State
	File photoFile;

	PhotoDoodle doodle;

	TabLayout.Tab cameraTab;
	TabLayout.Tab drawingTab;
	Drawable cameraTabIcon;
	Drawable drawingTabIcon;

	boolean suppressTabPopup;
	PopupWindow tabPopup;

	DrawPopupController drawPopupController;
	CameraPopupController cameraPopupController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_doodle);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
		buildModeTabs();

		doodle = new PhotoDoodle(this);
		doodle.setDrawDebugPositioningOverlay(false);

		if (savedInstanceState != null) {
			Bundle doodleState = savedInstanceState.getBundle(DOODLE_STATE);
			if (doodleState != null) {
				doodle.onCreate(doodleState);
			}
		}

		doodleView.setDoodle(doodle);

		setColor(color);
		setSelectedBrush(BrushType.values()[selectedBrush]);
		setInteractionMode(doodle.getInteractionMode());

		// be tidy - the photo file may have been left over after a crash
		deletePhotoTempFile();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);

		Bundle doodleState = new Bundle();
		doodle.onSaveInstanceState(doodleState);
		outState.putBundle(DOODLE_STATE, doodleState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_doodle, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		suppressTabPopup = true;
		switch (getInteractionMode()) {
			case PHOTO:
				cameraTab.select();
				break;
			case DRAW:
				drawingTab.select();
				break;
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemUndo:
				doodle.undo();
				return true;

			case R.id.menuItemClear:
				doodle.clear();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_TAKE_PHOTO:
				if (resultCode == Activity.RESULT_OK) {
					Log.i(TAG, "onActivityResult(REQUEST_TAKE_PHOTO) - RESULT_OK - will load bitmap from file: " + photoFile);
					Bitmap photo = loadPhotoFromFile(photoFile);
					if (photo != null) {
						doodle.setPhoto(photo);
					}
				} else {
					Log.w(TAG, "onActivityResult(REQUEST_TAKE_PHOTO) - photo wasn't taken.");
				}

				// clean up
				deletePhotoTempFile();
				break;

			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}


	@Override
	public void onTabSelected(TabLayout.Tab tab) {
		if (tab == cameraTab) {
			setInteractionMode(PhotoDoodle.InteractionMode.PHOTO);
		} else if (tab == drawingTab) {
			setInteractionMode(PhotoDoodle.InteractionMode.DRAW);
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

	@Override
	public void onTakePhoto() {
		Log.i(TAG, "onTakePhoto: ");

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

			photoFile = getPhotoTempFile();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
			startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
		} else {
			new AlertDialog.Builder(this).setTitle(R.string.no_camera_dialog_title)
					.setMessage(R.string.no_camera_dialog_title)
					.setPositiveButton(R.string.no_camera_dialog_positive_button_title, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// nothing
						}
					})
					.show();
		}
	}

	@Override
	public void onClearPhoto() {
		doodle.clearPhoto();
	}

	@Override
	public void onSelectPencil() {
		setSelectedBrush(BrushType.PENCIL);
	}

	@Override
	public void onSelectBrush() {
		setSelectedBrush(BrushType.BRUSH);
	}

	@Override
	public void onSelectSmallEraser() {
		setSelectedBrush(BrushType.SMALL_ERASER);
	}

	@Override
	public void onSelectBigEraser() {
		setSelectedBrush(BrushType.LARGE_ERASER);
	}

	@Override
	public void onSelectColor(ColorSwatchView colorSwatchView) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(builder.getContext());
		View view = inflater.inflate(R.layout.dialog_color_picker, null);
		final ColorPickerView colorPickerView = (ColorPickerView) view.findViewById(R.id.colorPicker);
		colorPickerView.setInitialColor(colorSwatchView.getColor());

		builder.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int color = colorPickerView.getCurrentColor();
						setColor(color);
						drawPopupController.setColorSwatchColor(color);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();

	}

	@Override
	public void onClearDrawing() {
		doodle.clearDrawing();
	}

	public void setSelectedBrush(BrushType selectedBrush) {
		this.selectedBrush = selectedBrush.ordinal();
		switch (selectedBrush) {
			case PENCIL:
				doodle.setBrush(new Brush(color, 1, 4, 600, false));
				break;

			case BRUSH:
				doodle.setBrush(new Brush(color, 8, 32, 600, false));
				break;

			case LARGE_ERASER:
				doodle.setBrush(new Brush(0x0, 24, 38, 600, true));
				break;

			case SMALL_ERASER:
				doodle.setBrush(new Brush(0x0, 12, 16, 600, true));
				break;
		}
	}

	@SuppressWarnings("unused")
	public BrushType getSelectedBrush() {
		return BrushType.values()[selectedBrush];
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;

		// update the current brush to use selected color
		setSelectedBrush(BrushType.values()[selectedBrush]);
	}

	public PhotoDoodle.InteractionMode getInteractionMode() {
		return doodle.getInteractionMode();
	}

	public void setInteractionMode(PhotoDoodle.InteractionMode interactionMode) {
		doodle.setInteractionMode(interactionMode);
	}

	private void buildModeTabs() {
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

		View popupView = null;
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		switch (getInteractionMode()) {
			case PHOTO:
				if (cameraPopupController == null) {
					popupView = inflater.inflate(R.layout.popup_camera, null);
					cameraPopupController = new CameraPopupController(popupView, this);
				} else {
					popupView = cameraPopupController.getPopupView();
				}
				break;
			case DRAW:
				if (drawPopupController == null) {
					popupView = inflater.inflate(R.layout.drawing_popup, null);
					drawPopupController = new DrawPopupController(popupView, this);
				} else {
					popupView = drawPopupController.getPopupView();
				}
				break;
		}

		popupView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		tabPopup = new PopupWindow(this);
		tabPopup.setContentView(popupView);
		tabPopup.setWidth(popupView.getMeasuredWidth());
		tabPopup.setHeight(popupView.getMeasuredHeight());

		//noinspection deprecation
		tabPopup.setBackgroundDrawable(new BitmapDrawable());
		tabPopup.setOutsideTouchable(true);

		tabPopup.showAsDropDown(getSelectedTabItemView());
	}

	private void updateModeTabDrawables() {
		switch (getInteractionMode()) {
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

	@Nullable
	private Bitmap loadPhotoFromFile(File file) {

		if (!file.exists()) {
			return null;
		}

		try {
			Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

			// determine a minimum size of the bitmap which would fill the device screen
			Point displaySize = new Point();
			getWindowManager().getDefaultDisplay().getSize(displaySize);

			float maxDisplayDim = Math.max(displaySize.x, displaySize.y);
			float minImageDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
			float scale = maxDisplayDim / minImageDim;

			if (scale < 1f) {
				int targetWidth = Math.round((float) bitmap.getWidth() * scale);
				int targetHeight = Math.round((float) bitmap.getHeight() * scale);
				return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
			} else {
				return bitmap;
			}
		} catch (Exception e) {
			Log.e(TAG, "loadPhotoFromFile - TROUBLE");
			e.printStackTrace();
			return null;
		}
	}

	private File getPhotoTempFile() {
		File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		return new File(path, "snap.jpg");
	}

	private void deletePhotoTempFile() {
		File photoTempFile = getPhotoTempFile();
		if (photoTempFile.exists()) {
			if (!photoTempFile.delete()) {
				Log.e(TAG, "Unable to delete the photo temp save file at " + photoTempFile);
			}
		}
	}
}
