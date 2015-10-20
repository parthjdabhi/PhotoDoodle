package org.zakariya.photodoodle;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.zakariya.photodoodle.model.Doodle;
import org.zakariya.photodoodle.model.IncrementalInputStrokeDoodle;

import butterknife.Bind;
import butterknife.ButterKnife;
import icepick.Icepick;

/**
 * Created by shamyl on 8/9/15.
 */
public class DoodleFragment extends Fragment {

	@Bind(R.id.doodleView)
	DoodleView doodleView;

	Doodle doodle;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		//doodle = new LineTessellationDoodle(getActivity());
		//doodle = new InputStrokeTessellationDoodle(getActivity());
		doodle = new IncrementalInputStrokeDoodle();

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

		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		doodle.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

}
