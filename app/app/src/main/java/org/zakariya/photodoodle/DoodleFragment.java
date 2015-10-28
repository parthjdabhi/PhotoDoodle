package org.zakariya.photodoodle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
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
import icepick.Icepick;
import icepick.State;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleFragment extends Fragment {

	private static final String TAG = "DoodleFragment";
	static final String[] DRAWING_TOOLS = {"Pen", "Brush", "Eraser"};

	@Bind(R.id.doodleView)
	DoodleView doodleView;

	@Bind(R.id.toolSelector)
	Spinner toolSelector;

	IncrementalInputStrokeDoodle doodle;

	@State
	int selectedTool = 0;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		doodle = new IncrementalInputStrokeDoodle(getActivity());

		if (savedInstanceState != null) {
			doodle.onCreate(savedInstanceState);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_doodle, container, false);
		ButterKnife.bind(this, v);

		// this makes doodle consume touch input from doodleView
		doodleView.setInputDelegate(doodle.inputDelegate());

		// this forwards doodleView's draw() calls to doodle
		doodleView.setDrawDelegate(new DoodleView.DrawDelegate() {
			@Override
			public void draw(Canvas canvas) {
				doodle.draw(canvas);
			}
		});

		// this allows doodle to invalidate doodleView, queueing a draw
		doodle.setInvalidationDelegate(new Doodle.InvalidationDelegate() {

			@Override
			public void invalidate() {
				doodleView.invalidate();
			}

			@Override
			public void invalidate(RectF rect) {
				doodleView.invalidate((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
			}

			@Override
			public RectF getBounds() {
				return new RectF(0, 0, doodleView.getWidth(), doodleView.getHeight());
			}
		});

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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		doodle.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

}
