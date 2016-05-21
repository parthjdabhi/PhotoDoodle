package org.zakariya.mrdoodle.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.zakariya.mrdoodle.R;

public class FlyoutMenuView extends View implements ValueAnimator.AnimatorUpdateListener {

	private static final String TAG = FlyoutMenuView.class.getSimpleName();
	private static final int MENU_CORNER_RADIUS_DP = 4;

	Paint menuBackgroundFillPaint;
	Path menuBackgroundClipPath;
	float menuBackgroundCornerRadius;
	int menuBackgroundColor = 0xFFFFFFFF;
	float menuExtentOpen; // 0 is closed, 1 is menuOpen
	PointF menuBackgroundFillOrigin;
	RectF menuOpenRect;
	RectF menuBackgroundFillOval = new RectF();
	float menuClosedRadius;
	float menuOpenRadius;

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
		menuBackgroundFillPaint = new Paint();
		menuBackgroundFillPaint.setAntiAlias(true);

		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.FlyoutMenuView, defStyle, 0);

		setMenuBackgroundColor(a.getColor(R.styleable.FlyoutMenuView_menuColor, menuBackgroundColor));
		a.recycle();

		menuBackgroundCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MENU_CORNER_RADIUS_DP, getResources().getDisplayMetrics());
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

		// set clip to the menu shape
		canvas.clipPath(menuBackgroundClipPath);

		// fill an oval centered at origin
		float radius = menuClosedRadius + (menuExtentOpen * (menuOpenRadius - menuClosedRadius));
		menuBackgroundFillOval.left = menuBackgroundFillOrigin.x - radius;
		menuBackgroundFillOval.top = menuBackgroundFillOrigin.y - radius;
		menuBackgroundFillOval.right = menuBackgroundFillOrigin.x + radius;
		menuBackgroundFillOval.bottom = menuBackgroundFillOrigin.y + radius;
		canvas.drawOval(menuBackgroundFillOval, menuBackgroundFillPaint);
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
		ValueAnimator a = ValueAnimator.ofFloat(menuExtentOpen, open ? 1 : 0);
		a.setDuration(225);
		a.setInterpolator(new AccelerateDecelerateInterpolator());
		a.addUpdateListener(this);
		a.start();
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		menuExtentOpen = (float) animation.getAnimatedValue();
		updateLayoutInfo();
		invalidate();
	}

	public int getMenuBackgroundColor() {
		return menuBackgroundColor;
	}

	public void setMenuBackgroundColor(int menuBackgroundColor) {
		this.menuBackgroundColor = menuBackgroundColor;
		menuBackgroundFillPaint.setColor(this.menuBackgroundColor);
		invalidate();
	}

	void updateLayoutInfo() {
		float innerWidth = getWidth() - getPaddingLeft() + getPaddingRight();
		float innerHeight = getHeight() - getPaddingTop() + getPaddingBottom();
		menuBackgroundFillOrigin = new PointF(getPaddingLeft() + innerWidth / 2, getPaddingTop() + innerHeight / 2);
		menuClosedRadius = Math.min(innerWidth / 2, innerHeight / 2);

		float openRectWidth = innerWidth * 3;
		float openRectHeight = innerHeight * 3;
		menuOpenRect = new RectF(menuBackgroundFillOrigin.x - openRectWidth / 2, menuBackgroundFillOrigin.y - openRectHeight / 2, menuBackgroundFillOrigin.x + openRectWidth/2, menuBackgroundFillOrigin.y + openRectHeight/2);
		menuBackgroundClipPath = new Path();
		menuBackgroundClipPath.addRoundRect(menuOpenRect, menuBackgroundCornerRadius, menuBackgroundCornerRadius, Path.Direction.CW);

		// compute the circular radius to fill the menuOpenRect
		float a = distToOrigin(menuOpenRect.left, menuOpenRect.top);
		float b = distToOrigin(menuOpenRect.right, menuOpenRect.top);
		float c = distToOrigin(menuOpenRect.right, menuOpenRect.bottom);
		float d = distToOrigin(menuOpenRect.left, menuOpenRect.bottom);
		menuOpenRadius = Math.max(a, Math.max(b, Math.max(c, d)));
	}

	float distToOrigin(float x, float y) {
		return (float) Math.sqrt((x - menuBackgroundFillOrigin.x) * (x - menuBackgroundFillOrigin.x) + (y - menuBackgroundFillOrigin.y) * (y - menuBackgroundFillOrigin.y));
	}
}
