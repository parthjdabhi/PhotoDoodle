package org.zakariya.mrdoodle.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.zakariya.mrdoodle.R;
import org.zakariya.mrdoodle.util.BusProvider;
import org.zakariya.mrdoodle.view.FlyoutMenuView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by shamyl on 5/20/16.
 */
public class FlyoutMenuTestActivity extends BaseActivity implements FlyoutMenuView.SelectionListener {

	private static final String TAG = FlyoutMenuTestActivity.class.getSimpleName();

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.flyoutMenu)
	FlyoutMenuView flyoutMenu;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_flyout_menu_test);
		ButterKnife.bind(this);
		BusProvider.getBus().register(this);

		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Configure the test flyout menu
		flyoutMenu.setLayout(new FlyoutMenuView.GridLayout(4, FlyoutMenuView.GridLayout.UNSPECIFIED));

		Random rng = new Random();
		List<TestMenuItem> items = new ArrayList<>();
		for (int i = 0; i < 19; i++) {
			items.add(new TestMenuItem(i, rng));
		}

		flyoutMenu.setAdapter(new FlyoutMenuView.ArrayAdapter<TestMenuItem>(items));
		flyoutMenu.setSelectionListener(this);
	}

	@Override
	protected void onDestroy() {
		BusProvider.getBus().unregister(this);
		super.onDestroy();
	}

	@Override
	public void onItemSelected(FlyoutMenuView flyoutMenuView, FlyoutMenuView.MenuItem item) {
		Log.i(TAG, "onItemSelected: selected: " + item.getId());

		int color = ((TestMenuItem) item).color;
		Drawable d = flyoutMenuView.getButtonDrawable();
		Drawable wd = DrawableCompat.wrap(d);
		DrawableCompat.setTint(wd, color);
		flyoutMenuView.setButtonDrawable(wd);
	}

	@Override
	public void onDismissWithoutSelection(FlyoutMenuView flyoutMenuView) {
		Log.i(TAG, "onDismissWithoutSelection: nothing was selected");
	}

	private static final class TestMenuItem extends FlyoutMenuView.MenuItem {

		@ColorInt
		int color;

		Paint paint;

		public TestMenuItem(int id, Random r) {
			super(id);

			color = Color.rgb(128 + r.nextInt(128), 128 + r.nextInt(128), 128 + r.nextInt(128));
			paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(color);
		}

		public void onDraw(Canvas canvas, Rect bounds) {
			canvas.drawRect(bounds, paint);
		}
	}

}
