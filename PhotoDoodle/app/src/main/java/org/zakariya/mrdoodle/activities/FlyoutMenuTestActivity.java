package org.zakariya.mrdoodle.activities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;

import org.zakariya.mrdoodle.R;
import org.zakariya.mrdoodle.util.BusProvider;
import org.zakariya.mrdoodle.view.FlyoutMenuView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by shamyl on 5/20/16.
 */
public class FlyoutMenuTestActivity extends BaseActivity implements FlyoutMenuView.SelectionListener {

	private static final String TAG = FlyoutMenuTestActivity.class.getSimpleName();

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.paletteFlyoutMenu)
	FlyoutMenuView paletteFlyoutMenu;
	PaletteButtonRenderer paletteButtonRenderer;
	@ColorInt int palette[] = {
			0xFF000000, 0xFF666666, 0xFF999999, 0xFFAAAAAA, 0xFFFFFFFF
	};

	@Bind(R.id.toolSelectorFlyoutMenu)
	FlyoutMenuView toolSelectorFlyoutMenu;

	@DrawableRes int toolIcons[] = {
			R.drawable.icon_popup_pencil, R.drawable.icon_popup_brush,
			R.drawable.icon_popup_small_eraser, R.drawable.icon_popup_big_eraser
	};

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

		//
		// Configure the test palette menu
		//

		paletteFlyoutMenu.setLayout(new FlyoutMenuView.GridLayout(FlyoutMenuView.GridLayout.UNSPECIFIED, 5));

		List<PaletteMenuItem> paletteMenuItems = new ArrayList<>();
		for (int i = 0; i < palette.length; i++) {
			paletteMenuItems.add(new PaletteMenuItem(i, palette[i]));
		}

		paletteFlyoutMenu.setAdapter(new FlyoutMenuView.ArrayAdapter<PaletteMenuItem>(paletteMenuItems));
		paletteFlyoutMenu.setSelectionListener(this);

		float insetDp = 8;
		float insetPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, insetDp, getResources().getDisplayMetrics());
		paletteButtonRenderer = new PaletteButtonRenderer(insetPx);
		paletteFlyoutMenu.setButtonRenderer(paletteButtonRenderer);

		//
		// configure the tools popup menu
		//

		toolSelectorFlyoutMenu.setLayout(new FlyoutMenuView.GridLayout(2, FlyoutMenuView.GridLayout.UNSPECIFIED));

		List<ToolMenuItem> toolMenuItems = new ArrayList<>();
		for (int i = 0; i < toolIcons.length; i++ ) {

			Drawable drawable;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				drawable = getResources().getDrawable(toolIcons[i], getTheme());
			} else {
				drawable = getResources().getDrawable(toolIcons[i]);
			}

			toolMenuItems.add(new ToolMenuItem(i, drawable));
		}

		toolSelectorFlyoutMenu.setAdapter(new FlyoutMenuView.ArrayAdapter<ToolMenuItem>(toolMenuItems));
		toolSelectorFlyoutMenu.setSelectionListener(this);
	}

	@Override
	protected void onDestroy() {
		BusProvider.getBus().unregister(this);
		super.onDestroy();
	}

	@Override
	public void onItemSelected(FlyoutMenuView flyoutMenuView, FlyoutMenuView.MenuItem item) {
		//Log.i(TAG, "onItemSelected: selected: " + item.getId());

		if (flyoutMenuView == paletteFlyoutMenu) {
			int color = ((PaletteMenuItem) item).color;
			paletteButtonRenderer.setCurrentColor(color);
		} else {
			ToolMenuItem toolMenuItem = (ToolMenuItem) item;
			Drawable icon = toolMenuItem.iconDrawable;
			Drawable wd = DrawableCompat.wrap(icon).mutate();
			DrawableCompat.setTint(wd, 0xFF000000);
			toolSelectorFlyoutMenu.setButtonDrawable(wd);
		}
	}

	@Override
	public void onDismissWithoutSelection(FlyoutMenuView flyoutMenuView) {
		//Log.i(TAG, "onDismissWithoutSelection: nothing was selected");
	}

	private static final class PaletteButtonRenderer implements FlyoutMenuView.ButtonRenderer {

		Paint paint;
		RectF insetButtonBounds = new RectF();
		float inset;
		@ColorInt
		int currentColor;

		public PaletteButtonRenderer(float inset) {
			paint = new Paint();
			paint.setAntiAlias(true);
			this.inset = inset;
		}

		public int getCurrentColor() {
			return currentColor;
		}

		public void setCurrentColor(int currentColor) {
			this.currentColor = currentColor;
		}

		@Override
		public void onDraw(Canvas canvas, RectF buttonBounds, @ColorInt int buttonColor, float alpha) {
			paint.setAlpha((int) (alpha * 255f));
			paint.setColor(buttonColor);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawOval(buttonBounds, paint);

			insetButtonBounds.left = buttonBounds.left + inset;
			insetButtonBounds.top = buttonBounds.top + inset;
			insetButtonBounds.right = buttonBounds.right - inset;
			insetButtonBounds.bottom = buttonBounds.bottom - inset;
			paint.setColor(currentColor);
			canvas.drawOval(insetButtonBounds, paint);

			if (buttonColor == 0xFFFFFFFF) {
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(0x33000000);
				canvas.drawOval(insetButtonBounds, paint);
			}
		}
	}

	private static final class ToolMenuItem extends FlyoutMenuView.MenuItem {
		Drawable iconDrawable;

		public ToolMenuItem(int id, Drawable iconDrawable) {
			super(id);
			this.iconDrawable = iconDrawable;
		}

		@Override
		public void onDraw(Canvas canvas, Rect bounds) {
			iconDrawable.setAlpha(255);
			iconDrawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
			iconDrawable.draw(canvas);
		}
	}

	private static final class PaletteMenuItem extends FlyoutMenuView.MenuItem {

		@ColorInt
		int color;

		Paint paint;
		RectF ovalBounds = new RectF();

		public PaletteMenuItem(int id, @ColorInt int color) {
			super(id);
			this.color = ColorUtils.setAlphaComponent(color, 255);

			paint = new Paint();
			paint.setAntiAlias(true);
		}

		@Override
		public void onDraw(Canvas canvas, Rect bounds) {
			ovalBounds.set(bounds);

			paint.setStyle(Paint.Style.FILL);
			paint.setColor(color);
			canvas.drawOval(ovalBounds, paint);

			if (color == 0xFFFFFFFF) {
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(0x33000000);
				canvas.drawOval(ovalBounds, paint);
			}
		}
	}

}
