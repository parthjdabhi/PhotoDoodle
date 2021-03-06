package org.zakariya.mrdoodle.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.zakariya.doodle.model.Brush;
import org.zakariya.doodle.model.PhotoDoodle;
import org.zakariya.doodle.view.DoodleView;
import org.zakariya.mrdoodle.R;
import org.zakariya.mrdoodle.model.DoodleDocument;
import org.zakariya.mrdoodle.view.ColorPickerView;
import org.zakariya.mrdoodle.view.ColorSwatchView;

import java.io.File;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import io.realm.Realm;

public class DoodleActivity extends BaseActivity
		implements TabLayout.OnTabSelectedListener, CameraPopupController.Callbacks, DrawPopupController.Callbacks {

	public static final String EXTRA_DOODLE_DOCUMENT_UUID = "DoodleActivity.EXTRA_DOODLE_DOCUMENT_UUID";

	public static final String RESULT_DID_EDIT_DOODLE = "DoodleActivity.RESULT_DID_EDIT_DOODLE";
	public static final String RESULT_DOODLE_DOCUMENT_UUID = "DoodleActivity.RESULT_DOODLE_DOCUMENT_UUID";

	private static final String STATE_DOODLE = "DoodleActivity.STATE_DOODLE";

	private static final String TAG = "DoodleActivity";
	private static final int REQUEST_TAKE_PHOTO = 1;

	enum BrushType {PENCIL, BRUSH, LARGE_ERASER, SMALL_ERASER}

	@Bind(R.id.titleEditText)
	EditText titleEditText;

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
	String documentUuid;

	@State
	long documentModificationTime = 0;

	@State
	File photoFile;

	Realm realm;
	DoodleDocument document;
	PhotoDoodle photoDoodle;

	TabLayout.Tab cameraTab;
	TabLayout.Tab drawingTab;
	Drawable cameraTabIcon;
	Drawable drawingTabIcon;

	boolean suppressTabPopup;
	PopupWindow tabPopup;
	Handler handler = new Handler(Looper.getMainLooper());

	DrawPopupController drawPopupController;
	CameraPopupController cameraPopupController;

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_doodle);
		ButterKnife.bind(this);

		//
		// setup the toolbar - note: we are providing our own titleTextView so we'll hide the built-in title later
		//

		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();

		buildModeTabs();

		//
		//  Load the DoodleDocument
		//

		realm = Realm.getInstance(this);
		if (savedInstanceState == null) {
			documentUuid = getIntent().getStringExtra(EXTRA_DOODLE_DOCUMENT_UUID);

			if (!TextUtils.isEmpty(documentUuid)) {

				document = DoodleDocument.byUUID(realm, documentUuid);
				if (document == null) {
					throw new IllegalArgumentException("Document UUID didn't refer to an existing DoodleDocument");
				}

				documentModificationTime = document.getModificationDate().getTime();
			}
		} else {
			Icepick.restoreInstanceState(this, savedInstanceState);
			if (!TextUtils.isEmpty(documentUuid)) {
				document = DoodleDocument.byUUID(realm, documentUuid);
				if (document == null) {
					throw new IllegalArgumentException("Document UUID didn't refer to an existing DoodleDocument");
				}
			}
		}

		if (document != null) {

			// show the up button
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}

			// hide default title
			if (actionBar != null) {
				actionBar.setDisplayShowTitleEnabled(false);
			}

			// update title
			titleEditText.setText(document.getName());
			titleEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					setDocumentName(s.toString());
				}
			});

			titleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						v.clearFocus();
						goFullscreen();
						return true;
					} else {
						return false;
					}
				}
			});

			titleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					Log.i(TAG, "onFocusChange: hasFocus:" + hasFocus);
					if (!hasFocus) {
						hideKeyboard(v);
						goFullscreen();
					}
				}
			});
		} else {

			// we don't have a document, this means we're running in a test harness where
			// DoodleActivity was launched directly.
			titleEditText.setVisibility(View.GONE);
		}

		//
		//  Create the PhotoDoodle. If this is result of a state restoration
		//  load from the saved instance state, otherwise, load from the saved document.
		//

		if (savedInstanceState != null) {
			photoDoodle = new PhotoDoodle(this);

			Bundle doodleState = savedInstanceState.getBundle(STATE_DOODLE);
			if (doodleState != null) {
				photoDoodle.onCreate(doodleState);
			}
		} else {
			if (document != null) {
				photoDoodle = DoodleDocument.load(this, document);
			} else {
				// DoodleActivity was launched directly for testing, create a blank document
				photoDoodle = new PhotoDoodle(this);
			}
		}

		photoDoodle.setDrawDebugPositioningOverlay(false);
		photoDoodle.setScaleMode(PhotoDoodle.ScaleMode.FIT);
		photoDoodle.setPadding(getResources().getDimension(R.dimen.doodle_canvas_padding));
		photoDoodle.setCanvasShadowOffset(getResources().getDimension(R.dimen.doodle_canvas_shadow_offset));
		photoDoodle.setCanvasBorderColor(ContextCompat.getColor(this,R.color.canvasBorder));
		photoDoodle.setCanvasShadowColor(ContextCompat.getColor(this,R.color.canvasShadow));
		photoDoodle.setBackgroundColor(ContextCompat.getColor(this,R.color.windowBackground));

		doodleView.setDoodle(photoDoodle);

		setColor(color);
		setSelectedBrush(BrushType.values()[selectedBrush]);
		setInteractionMode(photoDoodle.getInteractionMode());

		//
		// be tidy - in case a photo temp file survived a crash or whatever
		//

		deletePhotoTempFile();
	}

	@Override
	protected void onDestroy() {
		realm.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);

		Bundle doodleState = new Bundle();
		photoDoodle.onSaveInstanceState(doodleState);
		outState.putBundle(STATE_DOODLE, doodleState);

		super.onSaveInstanceState(outState);
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
	protected void onPause() {
		saveDoodleIfEdited();
		super.onPause();
	}

	@Override
	public void onBackPressed() {

		if (titleEditText.hasFocus()){
			titleEditText.clearFocus();
			return;
		}

		if (dismissTabItemPopup(false)){
			return;
		}

		saveAndSetActivityResult();
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemUndo:
				photoDoodle.undo();
				return true;

			case R.id.menuItemClear:
				photoDoodle.clear();
				return true;

			case android.R.id.home:
				saveAndSetActivityResult();
				NavUtils.navigateUpFromSameTask(this);
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
						photoDoodle.setPhoto(photo);
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
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			goFullscreen();
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

		if (tabPopup == null) {
			showTabItemPopup();
		} else {
			dismissTabItemPopup(false);
		}
	}

	@Override
	public void onTakePhoto() {
		Log.i(TAG, "onTakePhoto: ");

		dismissTabItemPopup(true);
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
		photoDoodle.clearPhoto();
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
		photoDoodle.clearDrawing();
	}

	/**
	 * If the doodle is dirty (edits were made) saves it to its file, and returns true.
	 *
	 * @return true if the doodle had edits and needed to be saved
	 */
	public boolean saveDoodleIfEdited() {
		if (document == null) {
			return false;
		}

		if (photoDoodle.isDirty()) {
			DoodleDocument.save(this, document, photoDoodle);
			photoDoodle.setDirty(false);

			// mark that the document was modified
			DoodleDocument.markModified(realm, document);
			return true;
		} else {
			return false;
		}
	}

	private void saveAndSetActivityResult() {
		if (document == null) {
			return;
		}

		saveDoodleIfEdited();

		boolean edited = document.getModificationDate().getTime() > documentModificationTime;

		Intent resultData = new Intent();
		resultData.putExtra(RESULT_DID_EDIT_DOODLE, edited);
		resultData.putExtra(RESULT_DOODLE_DOCUMENT_UUID, document.getUuid());
		setResult(RESULT_OK, resultData);
	}

	public void setDocumentName(String documentName) {
		if (!documentName.equals(document.getName())) {
			realm.beginTransaction();
			document.setName(documentName);
			document.setModificationDate(new Date());
			realm.commitTransaction();
		}
	}

	public String getDocumentName() {
		return document.getName();
	}

	public void setSelectedBrush(BrushType selectedBrush) {
		this.selectedBrush = selectedBrush.ordinal();
		switch (selectedBrush) {
			case PENCIL:
				photoDoodle.setBrush(new Brush(color, 1, 4, 600, false));
				break;

			case BRUSH:
				photoDoodle.setBrush(new Brush(color, 8, 32, 600, false));
				break;

			case LARGE_ERASER:
				photoDoodle.setBrush(new Brush(0x0, 24, 38, 600, true));
				break;

			case SMALL_ERASER:
				photoDoodle.setBrush(new Brush(0x0, 12, 16, 600, true));
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
		return photoDoodle.getInteractionMode();
	}

	public void setInteractionMode(PhotoDoodle.InteractionMode interactionMode) {
		photoDoodle.setInteractionMode(interactionMode);
	}

	@SuppressWarnings("deprecation")
	private void buildModeTabs() {
		cameraTab = modeTabs.newTab();
		drawingTab = modeTabs.newTab();

		cameraTabIcon = getResources().getDrawable(R.drawable.icon_tab_camera);
		cameraTabIcon = DrawableCompat.wrap(cameraTabIcon);
		DrawableCompat.setTint(cameraTabIcon, getResources().getColor(R.color.immersiveActionBarIcon));
		cameraTab.setIcon(cameraTabIcon);

		drawingTabIcon = getResources().getDrawable(R.drawable.icon_tab_draw);
		drawingTabIcon = DrawableCompat.wrap(drawingTabIcon);
		DrawableCompat.setTint(drawingTabIcon, getResources().getColor(R.color.immersiveActionBarIcon));
		drawingTab.setIcon(drawingTabIcon);

		modeTabs.addTab(cameraTab);
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
					popupView = inflater.inflate(R.layout.popup_drawing, null);
					drawPopupController = new DrawPopupController(popupView, this);
					drawPopupController.setColorSwatchColor(getColor());
					switch (getSelectedBrush()) {
						case PENCIL:
							drawPopupController.setActiveTool(DrawPopupController.ActiveTool.PENCIL);
							break;
						case BRUSH:
							drawPopupController.setActiveTool(DrawPopupController.ActiveTool.BRUSH);
							break;
						case SMALL_ERASER:
							drawPopupController.setActiveTool(DrawPopupController.ActiveTool.SMALL_ERASER);
							break;
						case LARGE_ERASER:
							drawPopupController.setActiveTool(DrawPopupController.ActiveTool.BIG_ERASER);
							break;
					}
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			tabPopup.setElevation(16);
			tabPopup.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this,R.color.popupBackground)));
		}

		tabPopup.setOutsideTouchable(true);

		tabPopup.showAsDropDown(getSelectedTabItemView());
	}

	private boolean dismissTabItemPopup(boolean delay) {
		if (tabPopup != null) {
			if (delay) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						tabPopup.dismiss();
						tabPopup = null;
					}
				}, 200);

			} else {
				tabPopup.dismiss();
				tabPopup = null;
			}
			return true;
		} else {
			return false;
		}
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

	private void hideKeyboard(View view) {
		InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(INPUT_METHOD_SERVICE);
		if (manager != null) {
			manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	private void goFullscreen() {

		int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

		getWindow().getDecorView().setSystemUiVisibility(flags);
	}
}
