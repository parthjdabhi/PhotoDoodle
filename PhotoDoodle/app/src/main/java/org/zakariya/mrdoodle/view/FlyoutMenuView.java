package org.zakariya.mrdoodle.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
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

	/**
	 * Base interface for items to be added to the FlyoutMenuView
	 */
	public interface MenuItem {

		/**
		 * Draw the contents of the MenuItem
		 *
		 * @param canvas the canvas to draw with
		 * @param alpha  the opacity to draw the contents at - ranging from [0,1].
		 */
		void onDraw(Canvas canvas, Rect bounds, float alpha);
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
		Rect frame;
		Rect bounds;
		float normalizedDistanceFromOrigin;
	}


	private static final String TAG = FlyoutMenuView.class.getSimpleName();

	private static final int ANIMATION_DURATION_MILLIS = 225; // 225 normal

	private static final int MENU_CORNER_RADIUS_DP = 4;
	private static final int MENU_SHADOW_RADIUS_DP = 24;
	private static final int MENU_SHADOW_INSET_DP = 16;
	private static final int MENU_SHADOW_OFFSET_DP = 8;

	private static final int BUTTON_SHADOW_RADIUS_DP = 12;
	private static final int BUTTON_SHADOW_OFFSET_DP = 5;

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
	float menuBackgroundCornerRadius;
	int menuBackgroundColor = 0xFFFFFFFF;
	float menuOpenTransition; // 0 is closed, 1 is menuOpen
	RectF menuOpenRect;
	RectF menuFillOval = new RectF();
	float menuOpenRadius;
	PointF buttonCenter;
	RectF buttonFillOval = new RectF();
	float buttonRadius;

	int menuShadowRadius;
	int menuShadowInset;
	int menuShadowOffset;
	int buttonShadowRadius;
	Bitmap menuShadowBitmap;
	Rect menuShadowSrcRect = new Rect();
	Rect menuShadowDstRect = new Rect();

	Bitmap buttonShadowBitmap;
	int buttonShadowOffset;

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

		setMenuBackgroundColor(a.getColor(R.styleable.FlyoutMenuView_menuColor, menuBackgroundColor));
		setItemWidth(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemWidth, (int) dp2px(DEFAULT_ITEM_SIZE_DP)));
		setItemHeight(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemHeight, (int) dp2px(DEFAULT_ITEM_SIZE_DP)));
		setItemMargin(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_itemMargin, (int) dp2px(DEFAULT_ITEM_MARGIN_DP)));

		setHorizontalMenuAnchor(a.getFloat(R.styleable.FlyoutMenuView_horizontalMenuAnchor, DEFAULT_HORIZONTAL_MENU_ANCHOR));
		setHorizontalMenuAnchorOutside(a.getBoolean(R.styleable.FlyoutMenuView_horizontalMenuAnchorOutside, DEFAULT_HORIZONTAL_MENU_ANCHOR_OUTSIDE));
		setVerticalMenuAnchor(a.getFloat(R.styleable.FlyoutMenuView_verticalMenuAnchor, DEFAULT_VERTICAL_MENU_ANCHOR));
		setVerticalMenuAnchorOutside(a.getBoolean(R.styleable.FlyoutMenuView_verticalMenuAnchorOutside, DEFAULT_VERTICAL_MENU_ANCHOR_OUTSIDE));
		setMenuAnchorOutsideMargin(a.getDimensionPixelSize(R.styleable.FlyoutMenuView_menuAnchorOutsideMargin, (int) dp2px(DEFAULT_MENU_ANCHOR_OUTSIDE_MARGIN)));

		a.recycle();

		menuBackgroundCornerRadius = dp2px(MENU_CORNER_RADIUS_DP);
		menuShadowRadius = (int) dp2px(MENU_SHADOW_RADIUS_DP);
		menuShadowInset = (int) dp2px(MENU_SHADOW_INSET_DP);
		menuShadowOffset = (int) dp2px(MENU_SHADOW_OFFSET_DP);
		menuShadowBitmap = createMenuShadowBitmap(SHADOW_COLOR, menuShadowRadius);

		buttonShadowRadius = (int) dp2px(BUTTON_SHADOW_RADIUS_DP);
		buttonShadowOffset = (int) dp2px(BUTTON_SHADOW_OFFSET_DP);
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
		buttonShadowBitmap = createButtonShadowBitmap(SHADOW_COLOR, (int) buttonRadius, buttonShadowRadius);
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// this is cheap to call every frame - only does real work if layout or other matters have been invalidated
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
		paint.setAlpha((int) ((alpha * alpha) * 255));
		canvas.drawBitmap(buttonShadowBitmap, buttonCenter.x - buttonShadowBitmap.getWidth() / 2, buttonCenter.y - buttonShadowBitmap.getHeight() / 2 + buttonShadowOffset, paint);

		paint.setAlpha((int) (alpha * 255));
		canvas.drawOval(buttonFillOval, paint);
	}

	void drawMenu(Canvas canvas, float alpha) {
		drawMenuShadow(canvas, paint, menuOpenRect, menuShadowBitmap, menuShadowRadius, menuShadowInset, 0, menuShadowOffset, alpha * alpha);

		// set clip to the menu shape
		canvas.save();
		canvas.clipPath(menuOpenShapePath);

		// fill an oval centered at origin
		float radius = buttonRadius + (alpha * (menuOpenRadius - buttonRadius));
		menuFillOval.left = buttonCenter.x - radius;
		menuFillOval.top = buttonCenter.y - radius;
		menuFillOval.right = buttonCenter.x + radius;
		menuFillOval.bottom = buttonCenter.y + radius;

		paint.setAlpha(255);
		canvas.drawOval(menuFillOval, paint);
		canvas.restore();

		float itemAlphaRange = 0.1f;
		for (MenuItemLayout menuItemLayout : itemLayouts) {
			if (alpha > menuItemLayout.normalizedDistanceFromOrigin) {
				float itemAlpha = Math.min((alpha - menuItemLayout.normalizedDistanceFromOrigin) / itemAlphaRange, 1f);

				canvas.save();
				canvas.translate(menuOpenRect.left + menuItemLayout.frame.left, menuOpenRect.top + menuItemLayout.frame.top);
				menuItemLayout.item.onDraw(canvas, menuItemLayout.bounds, itemAlpha);
				canvas.restore();
			}
		}
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
				animateMenuOpenChange(false);
				return true;
		}

		return super.onTouchEvent(event);
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

	public int getItemWidth() {
		return itemWidth;
	}

	public void setItemWidth(int itemWidth) {
		this.itemWidth = itemWidth;
		invalidateMenuItemLayout();
	}

	public int getItemHeight() {
		return itemHeight;
	}

	public void setItemHeight(int itemHeight) {
		this.itemHeight = itemHeight;
		invalidateMenuItemLayout();
	}

	public int getItemMargin() {
		return itemMargin;
	}

	public void setItemMargin(int itemMargin) {
		this.itemMargin = itemMargin;
		invalidateMenuItemLayout();
	}

	public int getMenuBackgroundColor() {
		return menuBackgroundColor;
	}

	public void setMenuBackgroundColor(int menuBackgroundColor) {
		this.menuBackgroundColor = menuBackgroundColor;
		paint.setColor(this.menuBackgroundColor);
		invalidate();
	}

	public float getHorizontalMenuAnchor() {
		return horizontalMenuAnchor;
	}

	public void setHorizontalMenuAnchor(float horizontalMenuAnchor) {
		this.horizontalMenuAnchor = Math.min(Math.max(horizontalMenuAnchor, 0), 1);
		invalidateMenuFill();
	}

	public float getVerticalMenuAnchor() {
		return verticalMenuAnchor;
	}

	public void setVerticalMenuAnchor(float verticalMenuAnchor) {
		this.verticalMenuAnchor = Math.min(Math.max(verticalMenuAnchor, 0), 1);
		invalidateMenuFill();
	}

	public boolean isHorizontalMenuAnchorOutside() {
		return horizontalMenuAnchorOutside;
	}

	public void setHorizontalMenuAnchorOutside(boolean horizontalMenuAnchorOutside) {
		this.horizontalMenuAnchorOutside = horizontalMenuAnchorOutside;
		invalidateMenuFill();
	}

	public boolean isVerticalMenuAnchorOutside() {
		return verticalMenuAnchorOutside;
	}

	public void setVerticalMenuAnchorOutside(boolean verticalMenuAnchorOutside) {
		this.verticalMenuAnchorOutside = verticalMenuAnchorOutside;
		invalidateMenuFill();
	}

	public int getMenuAnchorOutsideMargin() {
		return menuAnchorOutsideMargin;
	}

	public void setMenuAnchorOutsideMargin(int menuAnchorOutsideMargin) {
		this.menuAnchorOutsideMargin = menuAnchorOutsideMargin;
		invalidateMenuFill();
	}

	void updateLayoutInfo() {
		float innerWidth = getWidth() - getPaddingLeft() + getPaddingRight();
		float innerHeight = getHeight() - getPaddingTop() + getPaddingBottom();
		buttonCenter = new PointF(getPaddingLeft() + innerWidth / 2, getPaddingTop() + innerHeight / 2);
		buttonRadius = Math.min(innerWidth / 2, innerHeight / 2);
		buttonFillOval = new RectF(
				buttonCenter.x - buttonRadius,
				buttonCenter.y - buttonRadius,
				buttonCenter.x + buttonRadius,
				buttonCenter.y + buttonRadius);


		invalidateMenuFill();
	}

	float distToOrigin(float x, float y) {
		return (float) Math.sqrt((x - buttonCenter.x) * (x - buttonCenter.x) + (y - buttonCenter.y) * (y - buttonCenter.y));
	}

	Bitmap createMenuShadowBitmap(@ColorInt int color, int shadowRadiusPx) {
		int size = 2 * shadowRadiusPx + 1;
		Bitmap shadowBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		shadowBitmap.eraseColor(0x0); // clear

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(new RadialGradient(shadowRadiusPx + 1, shadowRadiusPx + 1, shadowRadiusPx, color, ColorUtils.setAlphaComponent(color, 0), Shader.TileMode.CLAMP));

		Canvas canvas = new Canvas(shadowBitmap);
		canvas.drawRect(0, 0, size, size, paint);

		return shadowBitmap;
	}

	Bitmap createButtonShadowBitmap(@ColorInt int color, int buttonRadius, int shadowRadius) {
		int radius = buttonRadius + shadowRadius / 2;
		int size = radius * 2;
		Bitmap shadowBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		shadowBitmap.eraseColor(0x0);

		int colors[] = {color, ColorUtils.setAlphaComponent(color, 0)};
		float stops[] = {(float) (buttonRadius - (shadowRadius / 2)) / (float) radius, 1f};
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(new RadialGradient(radius, radius, radius, colors, stops, Shader.TileMode.CLAMP));

		Canvas canvas = new Canvas(shadowBitmap);
		canvas.drawRect(0, 0, size, size, paint);

		return shadowBitmap;
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
				itemLayout.frame = layout.getLayoutRectForItem(i, itemWidth, itemHeight, itemMargin);
				itemLayout.bounds = new Rect(0, 0, itemWidth, itemHeight);

				float cx = menuOpenRect.left + itemLayout.frame.centerX();
				float cy = menuOpenRect.top + itemLayout.frame.centerY();
				float distance = distToOrigin(cx, cy);
				itemLayout.normalizedDistanceFromOrigin = distance / menuOpenRadius;

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

			float buttonSize = 2 * buttonRadius;
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
			float a = distToOrigin(menuOpenRect.left, menuOpenRect.top);
			float b = distToOrigin(menuOpenRect.right, menuOpenRect.top);
			float c = distToOrigin(menuOpenRect.right, menuOpenRect.bottom);
			float d = distToOrigin(menuOpenRect.left, menuOpenRect.bottom);
			menuOpenRadius = Math.max(a, Math.max(b, Math.max(c, d)));
		}
	}
}
