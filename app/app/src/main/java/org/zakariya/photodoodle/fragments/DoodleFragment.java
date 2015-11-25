package org.zakariya.photodoodle.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.model.Brush;
import org.zakariya.photodoodle.model.PhotoDoodle;
import org.zakariya.photodoodle.view.ColorPickerView;
import org.zakariya.photodoodle.view.ColorSwatchView;
import org.zakariya.photodoodle.view.DoodleView;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleFragment extends Fragment {

	private static final String TAG = "DoodleFragment";
	private static final String DOODLE_STATE = "DOODLE_STATE";

	private static final int REQUEST_TAKE_PHOTO = 1;

	enum BrushType {PENCIL, BRUSH, LARGE_ERASER, SMALL_ERASER}

	enum InteractionMode {PHOTO, DRAW}


	@Bind(R.id.doodleView)
	DoodleView doodleView;

	@Bind(R.id.pencilToolButton)
	ImageButton pencilToolButton;

	@Bind(R.id.brushToolButton)
	ImageButton brushToolButton;

	@Bind(R.id.largeEraserToolButton)
	ImageButton largeEraserToolButton;

	@Bind(R.id.smallEraserToolButton)
	ImageButton smallEraserToolButton;

	@Bind(R.id.colorSwatch)
	ColorSwatchView colorSwatch;

	@Bind(R.id.photoToolbar)
	ViewGroup photoToolbar;

	@Bind(R.id.drawToolbar)
	ViewGroup drawToolbar;

	MenuItem cameraModeMenuItem;
	MenuItem drawModeMenuItem;

	@State
	int color = 0xFF000000;

	@State
	int selectedBrush = BrushType.PENCIL.ordinal();

	@State
	int interactionMode = InteractionMode.DRAW.ordinal();

	@State
	File photoFile;

	PhotoDoodle doodle;

	public DoodleFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_doodle, menu);
		drawModeMenuItem = menu.findItem(R.id.menuItemDrawMode);
		cameraModeMenuItem = menu.findItem(R.id.menuItemCameraMode);
		updateUIToShowInteractionMode();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemUndo:
				performUndo();
				return true;

			case R.id.menuItemClear:
				performClear();
				return true;

			case R.id.menuItemCameraMode:
				setInteractionMode(InteractionMode.PHOTO);
				return true;

			case R.id.menuItemDrawMode:
				setInteractionMode(InteractionMode.DRAW);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		doodle = new PhotoDoodle(getActivity());

		if (savedInstanceState != null) {
			Bundle doodleState = savedInstanceState.getBundle(DOODLE_STATE);
			if (doodleState != null) {
				doodle.onCreate(doodleState);
			}
		}

		// be tidy - the photo file may have been left over after a crash
		deletePhotoTempFile();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);

		Bundle doodleState = new Bundle();
		doodle.onSaveInstanceState(doodleState);
		outState.putBundle(DOODLE_STATE, doodleState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_doodle, container, false);
		ButterKnife.bind(this, v);

		doodleView.setDoodle(doodle);

		setColor(color);
		setSelectedBrush(BrushType.values()[selectedBrush]);
		setInteractionMode(InteractionMode.values()[interactionMode]);

		return v;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

	@OnClick(R.id.pencilToolButton)
	public void onPencilToolSelected() {
		doodle.setBrush(new Brush(color, 1, 4, 600, false));
		selectedBrush = BrushType.PENCIL.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.brushToolButton)
	public void onBrushToolSelected() {
		doodle.setBrush(new Brush(color, 8, 32, 600, false));
		selectedBrush = BrushType.BRUSH.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.largeEraserToolButton)
	public void onLargeEraserToolSelected() {
		doodle.setBrush(new Brush(0x0, 24, 38, 600, true));
		selectedBrush = BrushType.LARGE_ERASER.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.smallEraserToolButton)
	public void onSmallEraserToolSelected() {
		doodle.setBrush(new Brush(0x0, 12, 16, 600, true));
		selectedBrush = BrushType.SMALL_ERASER.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.colorSwatch)
	public void onColorSwatchTap() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

		LayoutInflater inflater = LayoutInflater.from(builder.getContext());
		View view = inflater.inflate(R.layout.dialog_color_picker, null);
		final ColorPickerView colorPickerView = (ColorPickerView) view.findViewById(R.id.colorPicker);
		colorPickerView.setInitialColor(colorSwatch.getColor());

		builder.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int color = colorPickerView.getCurrentColor();
						setColor(color);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@OnClick(R.id.takePhotoButton)
	void takePhoto() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {

			photoFile = getPhotoTempFile();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
			startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
		} else {
			new AlertDialog.Builder(getActivity()).setTitle(R.string.no_camera_dialog_title)
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

	@OnClick(R.id.cropPhotoButton)
	void cropPhoto() {
		Log.w(TAG, "cropPhoto - UNIMPLEMENTED");
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		colorSwatch.setColor(color);

		// update the current brush to use selected color
		setSelectedBrush(BrushType.values()[selectedBrush]);
	}

	public BrushType getSelectedBrush() {
		return BrushType.values()[selectedBrush];
	}

	public void setSelectedBrush(BrushType brushType) {
		this.selectedBrush = brushType.ordinal();
		switch (brushType) {
			case PENCIL:
				onPencilToolSelected();
				break;
			case BRUSH:
				onBrushToolSelected();
				break;
			case LARGE_ERASER:
				onLargeEraserToolSelected();
				break;
			case SMALL_ERASER:
				onSmallEraserToolSelected();
				break;
		}
	}

	public InteractionMode getInteractionMode() {
		return InteractionMode.values()[interactionMode];
	}

	public void setInteractionMode(InteractionMode interactionMode) {
		this.interactionMode = interactionMode.ordinal();
		updateUIToShowInteractionMode();
	}

	private void updateUIToShowInteractionMode() {
		switch (getInteractionMode()) {
			case PHOTO:
				drawToolbar.setVisibility(View.GONE);
				photoToolbar.setVisibility(View.VISIBLE);
				if (drawModeMenuItem != null && drawModeMenuItem.getIcon() != null) {
					drawModeMenuItem.getIcon().setAlpha(64);
					cameraModeMenuItem.getIcon().setAlpha(255);
				}
				break;
			case DRAW:
				drawToolbar.setVisibility(View.VISIBLE);
				photoToolbar.setVisibility(View.GONE);
				if (drawModeMenuItem != null && drawModeMenuItem.getIcon() != null) {
					drawModeMenuItem.getIcon().setAlpha(255);
					cameraModeMenuItem.getIcon().setAlpha(64);
				}
				break;
		}
	}

	private void updateToolIcons() {
		pencilToolButton.setImageResource(selectedBrush == BrushType.PENCIL.ordinal()
				? R.drawable.doodle_tool_pencil_active
				: R.drawable.doodle_tool_pencil_inactive);

		brushToolButton.setImageResource(selectedBrush == BrushType.BRUSH.ordinal()
				? R.drawable.doodle_tool_brush_active
				: R.drawable.doodle_tool_brush_inactive);

		largeEraserToolButton.setImageResource(selectedBrush == BrushType.LARGE_ERASER.ordinal()
				? R.drawable.doodle_tool_large_eraser_active
				: R.drawable.doodle_tool_large_eraser_inactive);

		smallEraserToolButton.setImageResource(selectedBrush == BrushType.SMALL_ERASER.ordinal()
				? R.drawable.doodle_tool_small_eraser_active
				: R.drawable.doodle_tool_small_eraser_inactive);
	}

	private void performUndo() {
		doodle.undo();
	}

	private void performClear() {
		doodle.clear();
	}

	private Bitmap loadPhotoFromFile(File file) {
		try {
			String filePath = file.getAbsolutePath();
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			Bitmap bitmap = BitmapFactory.decodeFile(filePath, bmOptions);

			// determine a minimum size of the bitmap which would fill the device screen
			Point displaySize = new Point();
			getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);

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
		File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		return new File(path, "snap.jpg");
	}

	private void deletePhotoTempFile() {
		File photoTempFile = getPhotoTempFile();
		if (photoTempFile.exists()) {
			if (!photoTempFile.delete()){
				Log.e(TAG, "Unable to delete the photo temp save file at " + photoTempFile);
			}
		}
	}

}
