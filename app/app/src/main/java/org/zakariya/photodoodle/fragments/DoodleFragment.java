package org.zakariya.photodoodle.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
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
import org.zakariya.photodoodle.model.Doodle;
import org.zakariya.photodoodle.model.IncrementalInputStrokeDoodle;
import org.zakariya.photodoodle.view.ColorPickerView;
import org.zakariya.photodoodle.view.ColorSwatchView;
import org.zakariya.photodoodle.view.DoodleView;

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

	enum BrushType {PENCIL, BRUSH, LARGE_ERASER, SMALL_ERASER}


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

	@State
	int color = 0xFF000000;

	@State
	int selectedBrush = BrushType.PENCIL.ordinal();

	Doodle doodle;

	public DoodleFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_doodle, menu);
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

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		doodle = new IncrementalInputStrokeDoodle(getActivity());

		if (savedInstanceState != null) {
			Bundle doodleState = savedInstanceState.getBundle(DOODLE_STATE);
			if (doodleState != null) {
				doodle.onCreate(doodleState);
			}
		}
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

		return v;
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
		Log.i(TAG, "onColorSwatchTap");

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

		LayoutInflater inflater = LayoutInflater.from(builder.getContext());
		View view = inflater.inflate(R.layout.dialog_color_picker, null);
		final ColorPickerView colorPickerView = (ColorPickerView) view.findViewById(R.id.colorPicker);
		colorPickerView.setInitialColor(colorSwatch.getSwatchColor());

		builder.setTitle(R.string.color_dialog_title);
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int color = colorPickerView.getCurrentColor();
				setColor(color);
			}
		});

		builder.setNegativeButton(android.R.string.cancel, null);

		builder.show();
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		colorSwatch.setSwatchColor(color);

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

	public void onSaveAndReloadTap() {
		if (doodle instanceof IncrementalInputStrokeDoodle) {
			IncrementalInputStrokeDoodle incrementalInputStrokeDoodle = (IncrementalInputStrokeDoodle) doodle;
			incrementalInputStrokeDoodle.TEST_saveAndReload();
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
		if (doodle instanceof IncrementalInputStrokeDoodle) {
			IncrementalInputStrokeDoodle incrementalInputStrokeDoodle = (IncrementalInputStrokeDoodle) doodle;
			incrementalInputStrokeDoodle.undo();
		}
	}

	private void performClear() {
		doodle.clear();
	}

}
