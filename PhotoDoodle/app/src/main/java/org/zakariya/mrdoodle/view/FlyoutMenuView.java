package org.zakariya.mrdoodle.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.zakariya.mrdoodle.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//@SuppressWarnings("unused")
public class FlyoutMenuView extends View implements ValueAnimator.AnimatorUpdateListener {

	public interface SelectionListener {
		void onItemSelected(FlyoutMenuView flyoutMenuView, MenuItem item);

		void onDismissWithoutSelection(FlyoutMenuView flyoutMenuView);
	}

	@SuppressWarnings("unused")
	public static class Size {
		int width;
		int height;

		public Size() {
		}

		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if (o instanceof Size) {
				Size other = (Size) o;
				return other.width == width && other.height == height;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + width;
			result = 31 * result + height;
			return result;
		}
	}

	public interface ButtonRenderer {
		void onDraw(Canvas canvas, RectF buttonBounds, @ColorInt int buttonColor, float alpha);
	}

	/**
	 * Base interface for items to be added to the FlyoutMenuView
	 */
	public static class MenuItem {

		int id;

		public MenuItem(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		/**
		 * Draw the contents of the MenuItem
		 *
		 * @param canvas the canvas to draw with
		 * @param bounds the bounds of this item in its coordinate space, where the origin (top,left) is 0,0
		 */
		public void onDraw(Canvas canvas, RectF bounds) {
		}
	}

	/**
	 * Base class for layout managers which position MenuItem instances in the FlyoutMenu.
	 * Layout is responsible for determining the minimum size menu that can show all items, and
	 * is responsible for positioning items individually.
	 */
	public interface Layout {
		/**
		 * @param itemCount the number of items in the menu
		 * @return the minimum size the FlyoutMenu must be to display all items. How they're packed is up to the Layout
		 */
		Size getMinimumSizeForItems(int itemCount, int itemWidthPx, int itemHeightPx, int itemMarginPx);

		/**
		 * @param positionInList the individual MenuItem's index in the adapter
		 * @param itemWidthPx    width of item to layout
		 * @param itemHeightPx   height of the item to layout
		 * @param itemMarginPx   the margin around the item
		 * @return the Rect describing the position of this item, in pixels, where the origin (0,0) is the top left of the flyout menu
		 */
		Rect getLayoutRectForItem(int positionInList, int itemWidthPx, int itemHeightPx, int itemMarginPx);
	}

	@SuppressWarnings("unused")
	public static class GridLayout implements Layout {

		public static final int UNSPECIFIED = 0;

		int cols, rows;

		public GridLayout(int cols, int rows) {
			this.cols = cols;
			this.rows = rows;

			if (this.cols > 0 && this.rows > 0) {
				throw new IllegalArgumentException("one of cols or rows attribute must be 0, both cannot be set");
			}
		}

		@Override
		public Size getMinimumSizeForItems(int itemCount, int itemWidthPx, int itemHeightPx, int itemMarginPx) {
			Size size = new Size();
			if (cols > 0) {
				// fixed number of columns
				int requiredRows = (int) Math.ceil((float) itemCount / (float) cols);
				size.width = cols * itemWidthPx + ((cols + 1) * itemMarginPx);
				size.height = requiredRows * itemHeightPx + ((requiredRows + 1) * itemMarginPx);
			} else if (rows > 0) {
				int requiredCols = (int) Math.ceil((float) itemCount / (float) rows);
				size.width = requiredCols * itemWidthPx + ((requiredCols + 1) * itemMarginPx);
				size.height = rows * itemHeightPx + ((rows + 1) * itemMarginPx);
			} else {
				throw new IllegalArgumentException("one of cols or rows attribute must be 0, both cannot be set");
			}

			return size;
		}

		@Override
		public Rect getLayoutRectForItem(int positionInList, int itemWidthPx, int itemHeightPx, int itemMarginPx) {
			int row;
			int col;

			if (cols > 0) {
				row = (int) Math.floor((float) positionInList / cols);
				col = positionInList - row * cols;
			} else if (rows > 0) {
				col = (int) Math.floor((float) positionInList / rows);
				row = positionInList - col * rows;
			} else {
				throw new IllegalArgumentException("one of cols or rows attribute must be 0, both cannot be set");
			}

			Rect rect = new Rect();
			rect.left = col * itemWidthPx + (col + 1) * itemMarginPx;
			rect.top = row * itemHeightPx + (row + 1) * itemMarginPx;
			rect.right = rect.left + itemWidthPx;
			rect.bottom = rect.top + itemHeightPx;

			return rect;
		}
	}

	/**
	 * Base class for a FlyoutMenu's data source - the thing providing the MenuItems
	 */
	public interface Adapter {
		/**
		 * @return the number of MenuItems in this Adapter
		 */
		int getCount();

		/**
		 * @param position the index of the item to vend
		 * @return the MenuItem at position
		 */
		MenuItem getItem(int position);
	}

	@SuppressWarnings("unused")
	public static class ArrayAdapter<T> implements Adapter {

		private List<T> items;

		public ArrayAdapter(List<T> items) {
			this.items = items;
		}

		public ArrayAdapter(T[] items) {
			this.items = Arrays.asList(items);
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public MenuItem getItem(int position) {
			return (MenuItem) items.get(position);
		}
	}

	private static class MenuItemLayout {
		MenuItem item;
		int itemAdapterPosition;
		RectF frame;
		RectF bounds;
		float normalizedDistanceFromOrigin;
	}


	private static final String TAG = FlyoutMenuView.class.getSimpleName();

	private static final int ANIMATION_DURATION_MILLIS = 225; // 225 normal

	private static final int DEFAULT_BUTTON_ELEVATION_DP = 4;
	private static final int DEFAULT_MENU_ELEVATION_DP = 8;

	private static final int MENU_CORNER_RADIUS_DP = 4;
	private static final int DEFAULT_ITEM_SIZE_DP = 48;
	private static final int DEFAULT_ITEM_MARGIN_DP = 8;

	private static final float DEFAULT_HORIZONTAL_MENU_ANCHOR = 1f;
	private static final boolean DEFAULT_HORIZONTAL_MENU_ANCHOR_OUTSIDE = false;
	private static final float DEFAULT_VERTICAL_MENU_ANCHOR = 0.5f;
	private static final boolean DEFAULT_VERTICAL_MENU_ANCHOR_OUTSIDE = false;
	private static final int DEFAULT_MENU_ANCHOR_OUTSIDE_MARGIN = 8;

	private static final int SHADOW_COLOR = 0x44000000;


	Paint paint;
	Path menuOpenShapePath;
	Path menuFillOvalPath = new Path();
	float menuBackgroundCornerRadius;

	@ColorInt
	int buttonBackgroundColor = 0xFFFFFFFF;

	@ColorInt
	int menuBackgroundColor = 0xFFFFFFFF;

	@ColorInt
	int selectedItemBackgroundColor = 0x33000000;

	float buttonElevation;
	float menuElevation;

	float menuOpenTransition; // 0 is closed, 1 is menuOpen
	RectF menuOpenRect;
	RectF menuFillOval = new RectF();
	int menuOpenRadius;
	PointF buttonCenter;
	RectF buttonFillOval = new RectF();
	int buttonRadius;

	Drawable buttonDrawable;
	Bitmap menuShadowBitmap;
	Bitmap buttonShadowBitmap;
	int menuShadowRadius;
	int menuShadowInset;
	Rect menuShadowSrcRect = new Rect();
	Rect menuShadowDstRect = new Rect();

	float horizontalMenuAnchor = 0.5f;
	float verticalMenuAnchor = 0.5f;
	boolean horizontalMenuAnchorOutside = false;
	boolean verticalMenuAnchorOutside = false;
	int menuAnchorOutsideMargin = 0;

	int itemWidth;
	int itemHeight;
	int itemMargin;
	Adapter adapter;
	Layout layout;
	ArrayList<MenuItemLayout> itemLayouts = new ArrayList<>();
	MenuItem selectedMenuItem;

	SelectionListener selectionListener;
	ButtonRenderer buttonRenderer;

	public FlyoutMenuView(Context context) {
		super(context);
		init(null, 0);
	}

	public FlyoutMenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public FlyoutMenuView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		paint = new Paint();
		paint.setAntiAlias(true);

		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.FlyoutMenuView, defStyle, 0);

		setButtonBackgroundColor(a.getColor(R.styleable.FlyoutMenuView_buttonBackgroundColor, buttonBackgroundColor));
		setMenuBackgroundColor(a.getColor(R.styleable.FlyoutMenuView_menuBackgroundColor, menuBackgroundColor));
		setSelectedItemBackgroundColor(a.getColor(R.styleable.FlyoutMenuView_selectedItemBackgroundColor, selectedItemBackgroundColor));
		setItemWidth(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemWidth, (int) dp2px(DEFAULT_ITEM_SIZE_DP)));
		setItemHeight(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemHeight, (int) dp2px(DEFAULT_ITEM_SIZE_DP)));
		setItemMargin(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemMargin, (int) dp2px(DEFAULT_ITEM_MARGIN_DP)));

		setHorizontalMenuAnchor(a.getFloat(R.styleable.FlyoutMenuView_horizontalMenuAnchor, DEFAULT_HORIZONTAL_MENU_ANCHOR));
		setHorizontalMenuAnchorOutside(a.getBoolean(R.styleable.FlyoutMenuView_horizontalMenuAnchorOutside, DEFAULT_HORIZONTAL_MENU_ANCHOR_OUTSIDE));
		setVerticalMenuAnchor(a.getFloat(R.styleable.FlyoutMenuView_verticalMenuAnchor, DEFAULT_VERTICAL_MENU_ANCHOR));
		setVerticalMenuAnchorOutside(a.getBoolean(R.styleable.FlyoutMenuView_verticalMenuAnchorOutside, DEFAULT_VERTICAL_MENU_ANCHOR_OUTSIDE));
		setMenuAnchorOutsideMargin(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_menuAnchorOutsideMargin, (int) dp2px(DEFAULT_MENU_ANCHOR_OUTSIDE_MARGIN)));

		buttonDrawable = a.getDrawable(R.styleable.FlyoutMenuView_buttonSrc);

		setButtonElevation(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_buttonElevation, (int)dp2px(DEFAULT_BUTTON_ELEVATION_DP)));
		setMenuElevation(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_menuElevation, (int)dp2px(DEFAULT_MENU_ELEVATION_DP)));

		a.recycle();

		menuBackgroundCornerRadius = dp2px(MENU_CORNER_RADIUS_DP);
	}


	public Adapter getAdapter() {
		return adapter;
	}

	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
		this.invalidateMenuItemLayout();
	}

	public Layout getLayout() {
		return layout;
	}

	public void setLayout(Layout layout) {
		this.layout = layout;
		this.invalidateMenuItemLayout();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getParent() != null) {
			ViewGroup v = (ViewGroup) getParent();
			v.setClipChildren(false);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateLayoutInfo();
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// these are cheap to call every frame - only do real work if layout or other matters have been invalidated
		computeMenuFill();
		layoutMenuItems();

		final float pinion = 0.25f;
		if (menuOpenTransition < pinion) {
			drawButton(canvas, 1 - menuOpenTransition / pinion);
		} else {
			drawMenu(canvas, (menuOpenTransition - pinion) / (1 - pinion));
		}
	}

	void drawButton(Canvas canvas, float alpha) {

		// scale button down as it fades out
		if (alpha < 1) {
			Matrix m = new Matrix();
			m.preTranslate(-buttonCenter.x, -buttonCenter.y);
			m.postScale(alpha, alpha);
			m.postTranslate(buttonCenter.x, buttonCenter.y);
			canvas.concat(m);
		}

		if (buttonElevation > 0) {
			if (buttonShadowBitmap == null) {
				buttonShadowBitmap = createButtonShadowBitmap();
			}

			paint.setAlpha((int) ((alpha * alpha * alpha) * 255));
			float buttonShadowOffset = buttonElevation / 2;
			canvas.drawBitmap(buttonShadowBitmap, buttonCenter.x - buttonShadowBitmap.getWidth() / 2, buttonCenter.y - buttonShadowBitmap.getHeight() / 2 + buttonShadowOffset, paint);
		}

		int scaledAlpha = (int) (alpha * 255);
		paint.setAlpha(scaledAlpha);

		if (buttonRenderer == null) {
			paint.setColor(buttonBackgroundColor);
			canvas.drawOval(buttonFillOval, paint);
		}

		if (buttonDrawable != null) {
			buttonDrawable.setAlpha(scaledAlpha);

			// scale the radius to fit drawable inside circle
			float innerRadius = buttonRadius / 1.41421356237f;
			buttonDrawable.setBounds(
					(int) (buttonCenter.x - innerRadius),
					(int) (buttonCenter.y - innerRadius),
					(int) (buttonCenter.x + innerRadius),
					(int) (buttonCenter.y + innerRadius));

			buttonDrawable.draw(canvas);
		} else if (buttonRenderer != null) {
			buttonRenderer.onDraw(canvas, buttonFillOval, buttonBackgroundColor, alpha);
		}
	}

	void drawMenu(Canvas canvas, float alpha) {

		float pinion = 0.5f;

		if (menuElevation > 0 && alpha > pinion) {
			if (menuShadowBitmap == null) {
				menuShadowBitmap = createMenuShadowBitmap();
			}
			float shadowAlpha = (alpha - pinion) / (1f - pinion);
			int menuShadowOffset = (int)(menuElevation / 2);
			drawMenuShadow(canvas, paint, menuOpenRect, menuShadowBitmap, menuShadowRadius, menuShadowInset, 0, menuShadowOffset, shadowAlpha * shadowAlpha);
		}

		// set clip to the menu shape
		canvas.save();
		canvas.clipPath(menuOpenShapePath);

		// add oval clip for reveal animation
		float radius = (float) buttonRadius + (alpha * (float) (menuOpenRadius - buttonRadius));
		menuFillOval.left = buttonCenter.x - radius;
		menuFillOval.top = buttonCenter.y - radius;
		menuFillOval.right = buttonCenter.x + radius;
		menuFillOval.bottom = buttonCenter.y + radius;

		menuFillOvalPath.reset();
		menuFillOvalPath.addOval(menuFillOval, Path.Direction.CW);
		canvas.clipPath(menuFillOvalPath);

		// fill menu background
		paint.setAlpha(255);
		paint.setColor(menuBackgroundColor);
		canvas.drawRect(menuOpenRect, paint);

		// draw menu items - note: clip path is still active
		for (MenuItemLayout menuItemLayout : itemLayouts) {
			canvas.save();
			canvas.translate(menuOpenRect.left + menuItemLayout.frame.left, menuOpenRect.top + menuItemLayout.frame.top);

			if (menuItemLayout.item == selectedMenuItem) {
				paint.setColor(selectedItemBackgroundColor);
				canvas.drawRoundRect(menuItemLayout.bounds, menuBackgroundCornerRadius, menuBackgroundCornerRadius, paint);
			}

			menuItemLayout.item.onDraw(canvas, menuItemLayout.bounds);
			canvas.restore();
		}

		canvas.restore();
	}


	void drawMenuShadow(Canvas canvas, Paint paint, RectF rect, Bitmap shadowBitmap, int shadowRadius, int inset, int xOffset, int yOffset, float alpha) {
		canvas.save();
		paint.setAlpha((int) (alpha * 255));

		int left = (int) rect.left - shadowRadius + inset + xOffset;
		int top = (int) rect.top - shadowRadius + inset + yOffset;
		int right = (int) rect.right + shadowRadius - inset + xOffset;
		int bottom = (int) rect.bottom + shadowRadius - inset + yOffset;

		// top left corner
		menuShadowSrcRect.left = 0;
		menuShadowSrcRect.top = 0;
		menuShadowSrcRect.right = shadowRadius;
		menuShadowSrcRect.bottom = shadowRadius;
		menuShadowDstRect.left = left;
		menuShadowDstRect.top = top;
		menuShadowDstRect.right = left + shadowRadius;
		menuShadowDstRect.bottom = top + shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// top right corner
		menuShadowSrcRect.left = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.top = 0;
		menuShadowSrcRect.right = shadowBitmap.getWidth();
		menuShadowSrcRect.bottom = shadowRadius;
		menuShadowDstRect.left = right - shadowRadius;
		menuShadowDstRect.top = top;
		menuShadowDstRect.right = right;
		menuShadowDstRect.bottom = top + shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// bottom right corner
		menuShadowSrcRect.left = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.top = shadowBitmap.getHeight() - shadowRadius;
		menuShadowSrcRect.right = shadowBitmap.getWidth();
		menuShadowSrcRect.bottom = shadowBitmap.getHeight();
		menuShadowDstRect.left = right - shadowRadius;
		menuShadowDstRect.top = bottom - shadowRadius;
		menuShadowDstRect.right = right;
		menuShadowDstRect.bottom = bottom;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// bottom left corner
		menuShadowSrcRect.left = 0;
		menuShadowSrcRect.top = shadowBitmap.getHeight() - shadowRadius;
		menuShadowSrcRect.right = shadowRadius;
		menuShadowSrcRect.bottom = shadowBitmap.getHeight();
		menuShadowDstRect.left = left;
		menuShadowDstRect.top = bottom - shadowRadius;
		menuShadowDstRect.right = left + shadowRadius;
		menuShadowDstRect.bottom = bottom;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// draw the top edge
		menuShadowSrcRect.left = shadowRadius;
		menuShadowSrcRect.top = 0;
		menuShadowSrcRect.right = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.bottom = shadowRadius;
		menuShadowDstRect.left = left + shadowRadius;
		menuShadowDstRect.top = top;
		menuShadowDstRect.right = right - shadowRadius;
		menuShadowDstRect.bottom = top + shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// draw the right edge
		menuShadowSrcRect.left = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.top = shadowRadius;
		menuShadowSrcRect.right = shadowBitmap.getWidth();
		menuShadowSrcRect.bottom = shadowBitmap.getHeight() - shadowRadius;
		menuShadowDstRect.left = right - shadowRadius;
		menuShadowDstRect.top = top + shadowRadius;
		menuShadowDstRect.right = right;
		menuShadowDstRect.bottom = bottom - shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// draw the bottom edge
		menuShadowSrcRect.left = shadowRadius;
		menuShadowSrcRect.top = shadowBitmap.getHeight() - shadowRadius;
		menuShadowSrcRect.right = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.bottom = shadowBitmap.getHeight();
		menuShadowDstRect.left = left + shadowRadius;
		menuShadowDstRect.top = bottom - shadowRadius;
		menuShadowDstRect.right = right - shadowRadius;
		menuShadowDstRect.bottom = bottom;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// draw center
		menuShadowSrcRect.left = shadowRadius;
		menuShadowSrcRect.top = shadowRadius;
		menuShadowSrcRect.right = shadowBitmap.getWidth() - shadowRadius;
		menuShadowSrcRect.bottom = shadowBitmap.getHeight() - shadowRadius;
		menuShadowDstRect.left = left + shadowRadius;
		menuShadowDstRect.top = top + shadowRadius;
		menuShadowDstRect.right = right - shadowRadius;
		menuShadowDstRect.bottom = bottom - shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);

		// draw the left edge
		menuShadowSrcRect.left = 0;
		menuShadowSrcRect.top = shadowRadius;
		menuShadowSrcRect.right = shadowRadius;
		menuShadowSrcRect.bottom = shadowBitmap.getHeight() - shadowRadius;
		menuShadowDstRect.left = left;
		menuShadowDstRect.top = top + shadowRadius;
		menuShadowDstRect.right = left + shadowRadius;
		menuShadowDstRect.bottom = bottom - shadowRadius;
		canvas.drawBitmap(shadowBitmap, menuShadowSrcRect, menuShadowDstRect, paint);


		// restore drawing state
		canvas.restore();
		paint.setAlpha(255);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				animateMenuOpenChange(true);
				return true;

			case MotionEvent.ACTION_UP:
				MenuItem item = findMenuItemUnderPosition(event.getX(), event.getY());

				if (item != null) {
					setSelectedMenuItem(item);
				} else if (selectionListener != null) {
					selectionListener.onDismissWithoutSelection(this);
				}

				animateMenuOpenChange(false);
				return true;
		}

		return super.onTouchEvent(event);
	}

	@Nullable
	MenuItem findMenuItemUnderPosition(float x, float y) {
		float menuX = x - menuOpenRect.left;
		float menuY = y - menuOpenRect.top;

		for (MenuItemLayout menuItemLayout : itemLayouts) {
			if (menuX >= menuItemLayout.frame.left && menuX <= menuItemLayout.frame.right &&
					menuY >= menuItemLayout.frame.top && menuY <= menuItemLayout.frame.bottom) {
				return menuItemLayout.item;
			}
		}

		return null;
	}

	void animateMenuOpenChange(boolean open) {
		ValueAnimator a = ValueAnimator.ofFloat(menuOpenTransition, open ? 1 : 0);
		a.setDuration(ANIMATION_DURATION_MILLIS);
		a.setInterpolator(new AccelerateDecelerateInterpolator());
		a.addUpdateListener(this);
		a.start();
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		menuOpenTransition = (float) animation.getAnimatedValue();
		invalidate();
	}

	/**
	 * @return the drawable rendered by the button
	 */
	public Drawable getButtonDrawable() {
		return buttonDrawable;
	}

	public ButtonRenderer getButtonRenderer() {
		return buttonRenderer;
	}

	public void setButtonRenderer(ButtonRenderer buttonRenderer) {
		this.buttonRenderer = buttonRenderer;
		this.buttonDrawable = null;
	}

	/**
	 * Set the drawable drawn by the button
	 *
	 * @param buttonDrawable the thing to draw in the button
	 */
	public void setButtonDrawable(Drawable buttonDrawable) {
		this.buttonDrawable = buttonDrawable;
		this.buttonRenderer = null;
		invalidate();
	}

	/**
	 * Set a bitmap to use as image drawn by button
	 *
	 * @param bitmap the bitmap to draw in the button
	 */
	public void setButtonImage(Bitmap bitmap) {
		setButtonDrawable(new BitmapDrawable(getResources(), bitmap));
	}

	/**
	 * @return the width in pixels of items in the menu
	 */
	public int getItemWidth() {
		return itemWidth;
	}

	/**
	 * Set the width in pixels of items in the menu
	 *
	 * @param itemWidth the width in pixels of items in the menu
	 */
	public void setItemWidth(int itemWidth) {
		this.itemWidth = itemWidth;
		invalidateMenuItemLayout();
	}

	/**
	 * @return the height in pixels of items in the menu
	 */
	public int getItemHeight() {
		return itemHeight;
	}

	/**
	 * Set the height in pixels of items in the menu
	 *
	 * @param itemHeight the height in pixels of items in the menu
	 */
	public void setItemHeight(int itemHeight) {
		this.itemHeight = itemHeight;
		invalidateMenuItemLayout();
	}

	/**
	 * @return the margin in pixels around items in the menu
	 */
	public int getItemMargin() {
		return itemMargin;
	}

	/**
	 * Set the margin in pixels around items in the menu
	 *
	 * @param itemMargin the margin in pixels around items in the menu
	 */
	public void setItemMargin(int itemMargin) {
		this.itemMargin = itemMargin;
		invalidateMenuItemLayout();
	}

	/**
	 * @return the argb color of the background fill of the menu
	 */
	@ColorInt
	public int getMenuBackgroundColor() {
		return menuBackgroundColor;
	}

	/**
	 * Set the background argb color of the menu
	 *
	 * @param menuBackgroundColor the argb color of the background of the menu
	 */
	public void setMenuBackgroundColor(@ColorInt int menuBackgroundColor) {
		this.menuBackgroundColor = ColorUtils.setAlphaComponent(menuBackgroundColor, 255);
		invalidate();
	}

	/**
	 * @return Get the background fill color for the button
	 */
	public int getButtonBackgroundColor() {
		return buttonBackgroundColor;
	}

	/**
	 * Set the background fill color for the button
	 * @param buttonBackgroundColor the argb color for the background of the button
	 */
	public void setButtonBackgroundColor(int buttonBackgroundColor) {
		this.buttonBackgroundColor = ColorUtils.setAlphaComponent(buttonBackgroundColor,255);
		invalidate();
	}

	/**
	 * @return the color used to highlight the selected menu item
	 */
	public int getSelectedItemBackgroundColor() {
		return selectedItemBackgroundColor;
	}

	/**
	 * @param selectedItemBackgroundColor the argb color used to highlight selection
	 */
	public void setSelectedItemBackgroundColor(int selectedItemBackgroundColor) {
		this.selectedItemBackgroundColor = selectedItemBackgroundColor;
		invalidate();
	}


	/**
	 * @return the horizontal anchor point of the menu.
	 */
	public float getHorizontalMenuAnchor() {
		return horizontalMenuAnchor;
	}

	/**
	 * Set the horizontal anchorpoint of the menu. A value of 0 anchors the menu
	 * to the left of the button, and a value of 1 anchors to the right. A value of 0.5
	 * centers the menu horizontally.
	 *
	 * @param horizontalMenuAnchor the anchor value, from 0 to 1
	 */
	public void setHorizontalMenuAnchor(float horizontalMenuAnchor) {
		this.horizontalMenuAnchor = Math.min(Math.max(horizontalMenuAnchor, 0), 1);
		invalidateMenuFill();
	}

	/**
	 * @return the vertical anchor point of the menu.
	 */
	public float getVerticalMenuAnchor() {
		return verticalMenuAnchor;
	}

	/**
	 * Set the vertical anchorpoint of the menu. A value of 0 anchors the menu
	 * to the top of the button, and a value of 1 anchors to the bottom. A value of 0.5
	 * centers the menu vertically.
	 *
	 * @param verticalMenuAnchor the anchor value, from 0 to 1
	 */
	public void setVerticalMenuAnchor(float verticalMenuAnchor) {
		this.verticalMenuAnchor = Math.min(Math.max(verticalMenuAnchor, 0), 1);
		invalidateMenuFill();
	}

	/**
	 * @return if true, the horizontal anchor point attaches to the left edge of the button when horizontal anchor is 0, and to the right edge when horizontal anchor is 1
	 */
	public boolean isHorizontalMenuAnchorOutside() {
		return horizontalMenuAnchorOutside;
	}

	/**
	 * @return get the SelectionListener instance
	 */
	public SelectionListener getSelectionListener() {
		return selectionListener;
	}

	/**
	 * Set a listener to be notified when items in the menu are selected
	 *
	 * @param selectionListener listener to be notified on item selection
	 */
	public void setSelectionListener(SelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	/**
	 * @param horizontalMenuAnchorOutside if true, a horizontal anchor of 0 will hang the menu's right edge off the left edge of the button, and a value of 1 will hang the menu's left edge off the right edge of the button. If false, and the horizontal anchor is 0, the menu's left edge will hang off the button's right edge, and if the horizontal anchor is 1, the menu's right edge will anchor to the button's left
	 */
	public void setHorizontalMenuAnchorOutside(boolean horizontalMenuAnchorOutside) {
		this.horizontalMenuAnchorOutside = horizontalMenuAnchorOutside;
		invalidateMenuFill();
	}

	/**
	 * @return if true, the vertical anchor point attaches to the top edge of the button when vertical anchor is 0, and to the bottom edge when vertical anchor is 1
	 */
	public boolean isVerticalMenuAnchorOutside() {
		return verticalMenuAnchorOutside;
	}

	/**
	 * @param verticalMenuAnchorOutside if true, a vertical anchor of 0 will hang the menu's bottom edge off the top edge of the button, and a value of 1 will hang the menu's top edge off the bottom edge of the button. If false, and the vertical anchor is 0, the menu's bottom edge will hang off the button's bottom edge, and if the vertical anchor is 1, the menu's top edge will anchor to the button's top edge
	 */
	public void setVerticalMenuAnchorOutside(boolean verticalMenuAnchorOutside) {
		this.verticalMenuAnchorOutside = verticalMenuAnchorOutside;
		invalidateMenuFill();
	}

	/**
	 * @return the margin in pixels around the button used to anchor menus when the anchor is outside
	 */
	public int getMenuAnchorOutsideMargin() {
		return menuAnchorOutsideMargin;
	}

	/**
	 * Set the margin in pixels around the button used when verticalMenuAnchorOutside or horizontalMenuAnchorOutside are true.
	 *
	 * @param menuAnchorOutsideMargin margin in pixels around the button used to outset position of menu if verticalMenuAnchorOutside or horizontalMenuAnchorOutside are true
	 */
	public void setMenuAnchorOutsideMargin(int menuAnchorOutsideMargin) {
		this.menuAnchorOutsideMargin = menuAnchorOutsideMargin;
		invalidateMenuFill();
	}


	public float getButtonElevation() {
		return buttonElevation;
	}

	/**
	 * Set the button element's elevation in pixels
	 * @param buttonElevation the button element's elevation in pixels. Set to zero to disable elevation shadow.
	 */
	public void setButtonElevation(float buttonElevation) {
		this.buttonElevation = buttonElevation;
		buttonShadowBitmap = null; // invalidate
	}

	public float getMenuElevation() {
		return menuElevation;
	}

	/**
	 * Set the menu element's elevation in pixels
	 * @param menuElevation the menu element's elevation in pixels. Set to zero to disable elevation shadow.
	 */
	public void setMenuElevation(float menuElevation) {
		this.menuElevation = menuElevation;
		menuShadowBitmap = null; // invalidate
	}

	/**
	 * @return get the currently selected menu item, or null if none is selected
	 */
	@Nullable
	public MenuItem getSelectedMenuItem() {
		return selectedMenuItem;
	}

	/**
	 * Set the current menu item selection. Triggers notification of SelectionListener, if one assigned
	 * @param selectedMenuItem MenuItem to make the current selection
	 */
	public void setSelectedMenuItem(@Nullable MenuItem selectedMenuItem) {
		this.selectedMenuItem = selectedMenuItem;
		invalidate();

		if (selectionListener != null && selectedMenuItem != null) {
			selectionListener.onItemSelected(this, selectedMenuItem);
		}
	}

	/**
	 * Set the current selection to the menu item at a given adapter position
	 * @param adapterPosition adapter position of menu item to make the current selection
	 */
	public void setSelectedMenuItemByAdapterPosition(int adapterPosition) {
		setSelectedMenuItem(adapter.getItem(adapterPosition));
	}

	/**
	 * Set the current selection to the menu item with a given ID
	 * @param menuItemId id of the menu item to make the current selection
	 */
	public void setSelectedMenuItemById(int menuItemId) {
		for (MenuItemLayout menuItemLayout : itemLayouts) {
			if (menuItemLayout.item.getId() == menuItemId) {
				setSelectedMenuItem(menuItemLayout.item);
				return;
			}
		}
	}

	void updateLayoutInfo() {
		float innerWidth = getWidth() - getPaddingLeft() + getPaddingRight();
		float innerHeight = getHeight() - getPaddingTop() + getPaddingBottom();
		buttonCenter = new PointF(getPaddingLeft() + innerWidth / 2, getPaddingTop() + innerHeight / 2);
		buttonRadius = (int) Math.min(innerWidth / 2, innerHeight / 2);
		buttonFillOval = new RectF(
				buttonCenter.x - buttonRadius,
				buttonCenter.y - buttonRadius,
				buttonCenter.x + buttonRadius,
				buttonCenter.y + buttonRadius);

		invalidateMenuFill();
	}

	Bitmap createMenuShadowBitmap() {
		menuShadowRadius = (int)menuElevation * 2;
		menuShadowInset = menuShadowRadius/2;
		int size = 2 * menuShadowRadius + 1;
		Bitmap shadowBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		shadowBitmap.eraseColor(0x0); // clear

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(new RadialGradient(menuShadowRadius + 1, menuShadowRadius + 1, menuShadowRadius, SHADOW_COLOR, ColorUtils.setAlphaComponent(SHADOW_COLOR, 0), Shader.TileMode.CLAMP));

		Canvas canvas = new Canvas(shadowBitmap);
		canvas.drawRect(0, 0, size, size, paint);

		return shadowBitmap;
	}

	Bitmap createButtonShadowBitmap() {
		int shadowRadius = (int)buttonElevation;
		int radius = buttonRadius + shadowRadius / 2;
		int size = radius * 2;
		Bitmap shadowBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		shadowBitmap.eraseColor(0x0);

		int colors[] = {SHADOW_COLOR, ColorUtils.setAlphaComponent(SHADOW_COLOR, 0)};
		float stops[] = {(float) (buttonRadius - (shadowRadius / 2)) / (float) radius, 1f};
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(new RadialGradient(radius, radius, radius, colors, stops, Shader.TileMode.CLAMP));

		Canvas canvas = new Canvas(shadowBitmap);
		canvas.drawRect(0, 0, size, size, paint);

		return shadowBitmap;
	}

	float distanceToButtonCenter(float x, float y) {
		return (float) Math.sqrt((x - buttonCenter.x) * (x - buttonCenter.x) + (y - buttonCenter.y) * (y - buttonCenter.y));
	}

	float dp2px(float dp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

	void invalidateMenuItemLayout() {
		itemLayouts.clear();
		invalidateMenuFill(); // a new layout affects positioning of the menu as well
	}

	boolean needsLayoutMenuItems() {
		return itemLayouts.isEmpty();
	}

	void layoutMenuItems() {
		if (adapter != null && layout != null && needsLayoutMenuItems()) {

			for (int i = 0, n = adapter.getCount(); i < n; i++) {
				MenuItemLayout itemLayout = new MenuItemLayout();
				itemLayout.item = adapter.getItem(i);
				itemLayout.itemAdapterPosition = i;
				itemLayout.frame = new RectF(layout.getLayoutRectForItem(i, itemWidth, itemHeight, itemMargin));
				itemLayout.bounds = new RectF(0, 0, itemWidth, itemHeight);

				float cx = menuOpenRect.left + itemLayout.frame.centerX();
				float cy = menuOpenRect.top + itemLayout.frame.centerY();
				float distance = distanceToButtonCenter(cx, cy);
				itemLayout.normalizedDistanceFromOrigin = distance / (float) menuOpenRadius;

				itemLayouts.add(itemLayout);
			}

			invalidateMenuFill();
		}
	}

	void invalidateMenuFill() {
		menuOpenRect = null;
		menuOpenShapePath = null;
		menuOpenRadius = 0;
	}

	boolean needsComputeMenuFill() {
		return menuOpenRadius <= 0;
	}

	void computeMenuFill() {
		if (adapter != null && layout != null && needsComputeMenuFill()) {
			Size menuSize = layout.getMinimumSizeForItems(adapter.getCount(), itemWidth, itemHeight, itemMargin);

			float buttonSize = 2f * buttonRadius;
			float leftMin = buttonCenter.x + buttonRadius - menuSize.width;
			float leftMax = buttonCenter.x - buttonRadius;
			float topMin = buttonCenter.y + buttonRadius - menuSize.height;
			float topMax = buttonCenter.y - buttonRadius;

			if (horizontalMenuAnchorOutside) {
				leftMin -= buttonSize + menuAnchorOutsideMargin;
				leftMax += buttonSize + menuAnchorOutsideMargin;
			}

			if (verticalMenuAnchorOutside) {
				topMin -= buttonSize + menuAnchorOutsideMargin;
				topMax += buttonSize + menuAnchorOutsideMargin;
			}

			int menuLeft = (int) (leftMin + horizontalMenuAnchor * (leftMax - leftMin));
			int menuTop = (int) (topMin + verticalMenuAnchor * (topMax - topMin));

			menuOpenRect = new RectF(menuLeft, menuTop, menuLeft + menuSize.width, menuTop + menuSize.height);

			menuOpenShapePath = new Path();
			menuOpenShapePath.addRoundRect(menuOpenRect, menuBackgroundCornerRadius, menuBackgroundCornerRadius, Path.Direction.CW);

			// compute the circular radius to fill the menuOpenRect
			float a = distanceToButtonCenter(menuOpenRect.left, menuOpenRect.top);
			float b = distanceToButtonCenter(menuOpenRect.right, menuOpenRect.top);
			float c = distanceToButtonCenter(menuOpenRect.right, menuOpenRect.bottom);
			float d = distanceToButtonCenter(menuOpenRect.left, menuOpenRect.bottom);
			menuOpenRadius = (int) Math.max(a, Math.max(b, Math.max(c, d)));
		}
	}
}