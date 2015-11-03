package org.zakariya.photodoodle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.zakariya.photodoodle.model.Brush;
import org.zakariya.photodoodle.model.Doodle;
import org.zakariya.photodoodle.model.IncrementalInputStrokeDoodle;

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
	static final String[] DRAWING_TOOLS = {"Pen", "Brush", "Eraser"};

	@Bind(R.id.doodleView)
	DoodleView doodleView;

	@Bind(R.id.toolSelector)
	Spinner toolSelector;

	Doodle doodle;

	@State
	int selectedTool = 0;

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

		ArrayAdapter<String> toolOptionsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DRAWING_TOOLS);
		toolOptionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		toolSelector.setAdapter(toolOptionsAdapter);
		toolSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				switch (position) {
					case 0:
						doodle.setBrush(new Brush(0xFF222222, 1, 4, 800, false));
						break;

					case 1:
						doodle.setBrush(new Brush(0xFF222222, 12, 12, 800, false));
						break;

					case 2:
						doodle.setBrush(new Brush(0x0, 12, 12, 800, true));
						break;

					default:
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		toolSelector.setSelection(selectedTool);

		return v;
	}

	@OnClick(R.id.clearButton)
	public void onClearButtonTap() {
		doodle.clear();
	}

	@OnClick(R.id.undoButton)
	public void onUndoTap(){
		if (doodle instanceof IncrementalInputStrokeDoodle) {
			IncrementalInputStrokeDoodle incrementalInputStrokeDoodle = (IncrementalInputStrokeDoodle) doodle;
			incrementalInputStrokeDoodle.undo();
		}
	}

	@OnClick(R.id.saveAndReloadButton)
	public void onSaveAndReloadTap() {
		if (doodle instanceof IncrementalInputStrokeDoodle) {
			IncrementalInputStrokeDoodle incrementalInputStrokeDoodle = (IncrementalInputStrokeDoodle) doodle;
			incrementalInputStrokeDoodle.TEST_saveAndReload();
		}
	}

}
