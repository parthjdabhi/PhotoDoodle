package org.zakariya.mrdoodle.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

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
public class FlyoutMenuTestActivity extends BaseActivity {

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
			items.add(new TestMenuItem(rng));
		}

		flyoutMenu.setAdapter(new FlyoutMenuView.ArrayAdapter<TestMenuItem>(items));
	}

	@Override
	protected void onDestroy() {
		BusProvider.getBus().unregister(this);
		super.onDestroy();
	}

	private static final class TestMenuItem implements FlyoutMenuView.MenuItem {

		@ColorInt
		int color;

		Paint paint;

		public TestMenuItem(Random r) {
			color = Color.rgb(128 + r.nextInt(128), 128 + r.nextInt(128), 128 + r.nextInt(128));
			paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(color);
		}

		@Override
		public void onDraw(Canvas canvas, Rect bounds, float alpha) {
			paint.setAlpha((int)(alpha * 255));
			canvas.drawRect(bounds, paint);
		}
	}

}
