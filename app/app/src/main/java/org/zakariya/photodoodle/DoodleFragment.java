package org.zakariya.photodoodle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.zakariya.photodoodle.model.Brush;
import org.zakariya.photodoodle.model.Doodle;
import org.zakariya.photodoodle.model.IncrementalInputStrokeDoodle;
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

	enum ToolType {PENCIL, BRUSH, LARGE_ERASER, SMALL_ERASER}

	;

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


	Doodle doodle;

	@State
	int selectedTool = ToolType.PENCIL.ordinal();

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

		switch (ToolType.values()[selectedTool]) {
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

		return v;
	}

	@OnClick(R.id.pencilToolButton)
	public void onPencilToolSelected() {
		doodle.setBrush(new Brush(0xFF222222, 1, 4, 600, false));
		selectedTool = ToolType.PENCIL.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.brushToolButton)
	public void onBrushToolSelected() {
		doodle.setBrush(new Brush(0xFF222222, 8, 32, 600, false));
		selectedTool = ToolType.BRUSH.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.largeEraserToolButton)
	public void onLargeEraserToolSelected() {
		doodle.setBrush(new Brush(0x0, 24, 38, 600, true));
		selectedTool = ToolType.LARGE_ERASER.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.smallEraserToolButton)
	public void onSmallEraserToolSelected() {
		doodle.setBrush(new Brush(0x0, 12, 16, 600, true));
		selectedTool = ToolType.SMALL_ERASER.ordinal();
		updateToolIcons();
	}

	@OnClick(R.id.colorSwatch)
	public void onColorSwatchTap() {
		Log.i(TAG, "onColorSwatchTap");
	}

	public void onSaveAndReloadTap() {
		if (doodle instanceof IncrementalInputStrokeDoodle) {
			IncrementalInputStrokeDoodle incrementalInputStrokeDoodle = (IncrementalInputStrokeDoodle) doodle;
			incrementalInputStrokeDoodle.TEST_saveAndReload();
		}
	}

	private void updateToolIcons() {
		pencilToolButton.setImageResource(selectedTool == ToolType.PENCIL.ordinal()
				? R.drawable.doodle_tool_pencil_active
				: R.drawable.doodle_tool_pencil_inactive);

		brushToolButton.setImageResource(selectedTool == ToolType.BRUSH.ordinal()
				? R.drawable.doodle_tool_brush_active
				: R.drawable.doodle_tool_brush_inactive);

		largeEraserToolButton.setImageResource(selectedTool == ToolType.LARGE_ERASER.ordinal()
				? R.drawable.doodle_tool_large_eraser_active
				: R.drawable.doodle_tool_large_eraser_inactive);

		smallEraserToolButton.setImageResource(selectedTool == ToolType.SMALL_ERASER.ordinal()
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
