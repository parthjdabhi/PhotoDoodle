package org.zakariya.photodoodle.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.Interpolator;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.geom.PointFUtil;

/**
 * TODO: document your custom view class.
 */
public class ColorPickerView extends View {

	public interface OnColorChangeListener {
		void onColorChange(ColorPickerView view, int color);
	}

	private static final String TAG = "ColorPickerView";

	private enum DragState {
		NONE,
		DRAGGING_HUE_HANDLE,
		DRAGGING_TONE_HANDLE
	}

	private static final float SQRT_2 = (float) Math.sqrt(2);
	private static final float HUE_RING_THICKNESS = 8f;
	private static final float SEPARATOR_WIDTH = 4f;
	private static final float KNOB_RADIUS = 30;
	private static final long ANIMATION_DURATION = 200;

	private int color = 0xFF000000;
	private int snappedColor, snappedPureHueColor;
	private float currentDragHue, snappedHue, snappedSaturation, snappedLightness;

	private int precision = 16;
	private Paint paint;
	private Path backgroundFillPath;
	private LayoutInfo layoutInfo = new LayoutInfo();
	private Path hueRingWedgePath;
	private Path hueRingWedgeSeparatorPath;
	private DragState dragState;
	private OnColorChangeListener onColorChangeListener;

	// animation state
	private long dragStartTime, dragEndTime;
	private Interpolator interpolator;
	float hueKnobEngagement;
	float toneKnobEngagement;

	public ColorPickerView(Context context) {
		super(context);
		init(null, 0);
	}

	public ColorPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.ColorPickerView, defStyle, 0);

		color = a.getColor(R.styleable.ColorPickerView_initialColor, color);
		precision = a.getInt(R.styleable.ColorPickerView_precision, precision);
		computeSnappedHSLFromColor(color);

		a.recycle();

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);

		interpolator = new AnticipateOvershootInterpolator();
		setDragState(DragState.NONE);
	}

	public OnColorChangeListener getOnColorChangeListener() {
		return onColorChangeListener;
	}

	public void setOnColorChangeListener(OnColorChangeListener onColorChangeListener) {
		this.onColorChangeListener = onColorChangeListener;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = Math.min(Math.max(precision, 1), 256);
		computeSnappedHSLFromColor(color);
		invalidate();
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		computeSnappedHSLFromColor(color);
		invalidate();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getParent() != null) {
			ViewGroup v = (ViewGroup) getParent();
			v.setClipChildren(false);
			Log.i(TAG, "Disabling child view clipping");
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		computeLayoutInfo();
		generateRenderPaths();
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int backgroundColor = 0xFFFFFFFF;
		Drawable backgroundDrawable = getBackground();
		if (backgroundDrawable instanceof ColorDrawable) {
			backgroundColor = ((ColorDrawable) backgroundDrawable).getColor();
		}

		//
		// first draw the hue ring - a set of wedges radiating from center
		//

		canvas.save();
		canvas.translate(layoutInfo.centerX, layoutInfo.centerY);

		int precision = this.precision;
		float[] hsl = {0, 1, 0.5f};
		final float hueAngleIncrement = layoutInfo.hueAngleIncrement;
		float hueAngle = 0;

		canvas.rotate(-90);
		paint.setStyle(Paint.Style.FILL);
		for (int hueStep = 0; hueStep < precision; hueStep++, hueAngle += hueAngleIncrement) {
			hsl[0] = hueAngle * 180f / (float) Math.PI;
			int color = ColorUtils.HSLToColor(hsl);
			paint.setColor(color);

			float r = (float) (hueStep * hueAngleIncrement * 180 / Math.PI);
			canvas.save();
			canvas.rotate(r);
			canvas.drawPath(hueRingWedgePath, paint);
			canvas.restore();
		}

		canvas.restore();

		//
		// draw the hue angle wedge separators and the background fill on top to leave only the hue ring visible
		//

		canvas.save();
		canvas.translate(layoutInfo.centerX, layoutInfo.centerY);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SEPARATOR_WIDTH);
		paint.setColor(backgroundColor);
		canvas.drawPath(hueRingWedgeSeparatorPath, paint);
		canvas.restore();

		paint.setStyle(Paint.Style.FILL);
		canvas.drawPath(backgroundFillPath, paint);

		//
		// now render the tone square
		//

		float swatchSize = layoutInfo.toneSquareSwatchSize;
		float swatchRadius = swatchSize / 2 - SEPARATOR_WIDTH;
		float swatchLeft = 0, swatchTop = 0;
		float swatchY = 0f;
		float swatchX;
		float swatchIncrement = 1f / (float) (precision - 1);
		int toneSquareBaseColor = snappedPureHueColor;

		for (int row = 0; row < precision; row++, swatchY += swatchIncrement) {
			swatchLeft = layoutInfo.toneSquareLeft;
			swatchTop = layoutInfo.toneSquareTop + row * swatchSize;
			swatchX = 0f;

			// first row is solid white - so draw with a hairline grey circle to clarify
			if (row == 0) {
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(0xFFFFFFFF);
				for (int col = 0; col < precision; col++, swatchLeft += swatchSize) {
					canvas.drawCircle(swatchLeft, swatchTop, swatchRadius, paint);
				}
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(0xFFdddddd);
				paint.setStrokeWidth(1);
				swatchLeft = layoutInfo.toneSquareLeft;
				for (int col = 0; col < precision; col++, swatchLeft += swatchSize) {
					canvas.drawCircle(swatchLeft, swatchTop, swatchRadius, paint);
				}
			} else {
				for (int col = 0; col < precision; col++, swatchLeft += swatchSize, swatchX += swatchIncrement) {
					paint.setColor(plotToneSquareSwatchColor(toneSquareBaseColor, swatchX, swatchY));
					canvas.drawCircle(swatchLeft, swatchTop, swatchRadius, paint);
				}
			}

			paint.setStyle(Paint.Style.FILL);
		}

		final long now = System.currentTimeMillis();

		//
		// Draw the knob handles.
		// First, get the "engagement" factor. The knob grows when it's grabbed, and
		// shrinks when let go. The engagement grows from 0->1, then 1->0 to show this.
		//

		switch (dragState) {
			case DRAGGING_HUE_HANDLE:
				hueKnobEngagement = Math.min((float) (now - dragStartTime) / (float) ANIMATION_DURATION, 1f);
				toneKnobEngagement = 0;
				break;
			case DRAGGING_TONE_HANDLE:
				toneKnobEngagement = Math.min((float) (now - dragStartTime) / (float) ANIMATION_DURATION, 1f);
				hueKnobEngagement = 0;
				break;
			case NONE:
				if (toneKnobEngagement > 0) {
					toneKnobEngagement = 1f - Math.min(((float) (now - dragEndTime) / (float) ANIMATION_DURATION), 1f);
				}
				if (hueKnobEngagement > 0) {
					hueKnobEngagement = 1f - Math.min(((float) (now - dragEndTime) / (float) ANIMATION_DURATION), 1f);
				}
				break;
		}

		float hueKnobAngle = lrpDegrees(snappedHue, currentDragHue, interpolator.getInterpolation(hueKnobEngagement));
		PointF hueKnobPosition = getHueKnobPosition(hueKnobAngle);
		PointF toneKnobPosition = getToneKnobPosition(snappedHue, snappedSaturation, snappedLightness);

		// draw whichever knob is currently engaged on top
		if (hueKnobEngagement > toneKnobEngagement) {
			drawKnob(canvas, snappedColor, toneKnobPosition.x, toneKnobPosition.y, toneKnobEngagement);
			drawKnob(canvas, snappedPureHueColor, hueKnobPosition.x, hueKnobPosition.y, hueKnobEngagement);
		} else {
			drawKnob(canvas, snappedPureHueColor, hueKnobPosition.x, hueKnobPosition.y, hueKnobEngagement);
			drawKnob(canvas, snappedColor, toneKnobPosition.x, toneKnobPosition.y, toneKnobEngagement);
		}

		//
		// now determine if we must continue animation
		//

		boolean animating = false;
		if (dragState == DragState.NONE) {
			if (now - dragEndTime < ANIMATION_DURATION) {
				animating = true;
			}
		} else {
			animating = true;
		}

		if (animating) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	private void drawKnob(Canvas canvas, int color, float x, float y, float engagement) {
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);

		if (engagement > 0) {
			final float rInactive = KNOB_RADIUS;
			final float rActive = KNOB_RADIUS * 1.5f;
			final float extensionInactive = 1f;
			final float extensionActive = 3f;
			final long now = System.currentTimeMillis();
			final float t = interpolator.getInterpolation(engagement);
			float r = lrp(rInactive, rActive, t);
			float extension = lrp(extensionInactive, extensionActive, t);

			Path popupKnobPath = new Path();
			popupKnobPath.addRoundRect(new RectF(x - r, y - extension * r, x + r, y + r), r, r, Path.Direction.CW);
			canvas.drawPath(popupKnobPath, paint);
		} else {
			canvas.drawCircle(x, y, KNOB_RADIUS, paint);
		}
	}

	private PointF getHueKnobPosition(float hue) {
		float selectedHueAngle = (float) ((hue * Math.PI / 180) - Math.PI / 2);
		float knobPositionRadius = (layoutInfo.hueRingOuterRadius + layoutInfo.hueRingInnerRadius) / 2;
		float knobX = layoutInfo.centerX + (float) (Math.cos(selectedHueAngle) * knobPositionRadius);
		float knobY = layoutInfo.centerY + (float) (Math.sin(selectedHueAngle) * knobPositionRadius);
		return new PointF(knobX, knobY);
	}

	private PointF getToneKnobPosition(float snappedHue, float snappedSaturation, float snappedLightness) {
		float x = layoutInfo.toneSquareLeft + (1 - snappedSaturation) * layoutInfo.toneSquareSize;
		float y = layoutInfo.toneSquareTop + (1 - snappedLightness) * layoutInfo.toneSquareSize;
		return new PointF(x, y);
	}

	private int plotToneSquareSwatchColor(int color, float x, float y) {
		float red = Color.red(color) / 255f;
		float green = Color.green(color) / 255f;
		float blue = Color.blue(color) / 255f;

		red = lrp(red, 0.5f, x);
		green = lrp(green, 0.5f, x);
		blue = lrp(blue, 0.5f, x);

		if (y < 0.5f) {
			float white = (1f - (y / 0.5f));
			red = lrp(red, 1f, white);
			green = lrp(green, 1f, white);
			blue = lrp(blue, 1f, white);
		} else {
			float black = (y - 0.5f) * 2f;
			black *= black;
			red = lrp(red, 0f, black);
			green = lrp(green, 0f, black);
			blue = lrp(blue, 0f, black);
		}

		return Color.rgb((int) (red * 255f), (int) (green * 255f), (int) (blue * 255f));
	}

	private static float lrp(float a, float b, float v) {
		return a + v * (b - a);
	}

	private static float lrpDegrees(float a, float b, float v) {
		// normalize to [0,360]
		while (a < 360f) {
			a += 360f;
		}

		while (a > 360f) {
			a -= 360f;
		}

		while (b < 360f) {
			b += 360f;
		}

		while (b > 360f) {
			b -= 360f;
		}

		float ap = a - 360f;
		float bp = b - 360f;

		// find shortest distance to interpolate
		float abd = Math.abs(a-b);
		float apbd = Math.abs(ap-b);
		float abpd = Math.abs(a-bp);

		if (abd < apbd && abd < abpd) {
			return a + v * (b-a);
		} else if ( apbd < abd && apbd < abpd) {
			return ap + v * (b-ap);
		} else {
			return a + v * (bp - a);
		}
	}

	private void computeLayoutInfo() {
		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();
		int paddingBottom = getPaddingBottom();

		int contentWidth = getWidth() - paddingLeft - paddingRight;
		int contentHeight = getHeight() - paddingTop - paddingBottom;
		int contentSize = Math.min(contentWidth, contentHeight);

		float centerX = (float) Math.floor(paddingLeft + contentWidth / 2f);
		float centerY = (float) Math.floor(paddingTop + contentHeight / 2f);

		layoutInfo.contentSize = contentSize;
		layoutInfo.centerX = centerX;
		layoutInfo.centerY = centerY;

		layoutInfo.hueRingOuterRadius = contentSize / 2f;
		layoutInfo.hueRingInnerRadius = layoutInfo.hueRingOuterRadius - HUE_RING_THICKNESS;

		layoutInfo.toneSquareSize = (layoutInfo.hueRingInnerRadius - (KNOB_RADIUS * 2)) / SQRT_2 * 2f;
		layoutInfo.toneSquareSwatchSize = layoutInfo.toneSquareSize / (float) (precision - 1);
		layoutInfo.toneSquareLeft = centerX - layoutInfo.toneSquareSize / 2;
		layoutInfo.toneSquareTop = centerY - layoutInfo.toneSquareSize / 2;

		layoutInfo.hueAngleIncrement = (float) (2 * Math.PI) / (float) precision;
		layoutInfo.hueAngleIncrementDegrees = 360f / (float) precision;
	}

	private void generateRenderPaths() {
		// backgroundPath is rect matching view, with donut cut out for hue ring
		backgroundFillPath = new Path();
		backgroundFillPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
		backgroundFillPath.addCircle(layoutInfo.centerX, layoutInfo.centerY, layoutInfo.hueRingOuterRadius, Path.Direction.CW);
		backgroundFillPath.addCircle(layoutInfo.centerX, layoutInfo.centerY, layoutInfo.hueRingInnerRadius, Path.Direction.CW);
		backgroundFillPath.setFillType(Path.FillType.EVEN_ODD);

		// right-facing wedge centered at origin
		float halfHueIncrement = getHueAngleIncrement() / 2;
		float x = (float) Math.cos(halfHueIncrement);
		float y = (float) Math.sin(halfHueIncrement);
		float outerRadius = layoutInfo.hueRingOuterRadius * 1.1f;
		float innerRadius = layoutInfo.hueRingInnerRadius * 0.9f;

		hueRingWedgePath = new Path();
		hueRingWedgePath.moveTo(innerRadius * x, innerRadius * -y);
		hueRingWedgePath.lineTo(outerRadius * x, outerRadius * -y);
		hueRingWedgePath.lineTo(outerRadius * x, outerRadius * y);
		hueRingWedgePath.lineTo(innerRadius * x, innerRadius * y);
		hueRingWedgePath.close();

		// thin separator lines to delineate the hue wedges
		hueRingWedgeSeparatorPath = new Path();
		float hueAngle = -layoutInfo.hueAngleIncrement / 2;
		for (int hueStep = 0; hueStep < precision; hueStep++, hueAngle += layoutInfo.hueAngleIncrement) {
			hueRingWedgeSeparatorPath.moveTo(0, 0);

			x = (float) Math.cos(hueAngle);
			y = (float) Math.sin(hueAngle);
			hueRingWedgeSeparatorPath.lineTo(outerRadius * x, outerRadius * y);
		}
	}


	private float getHueAngleIncrement() {
		return (float) (2 * Math.PI) / (float) precision;
	}

	private void computeSnappedHSLFromColor(int color) {
		float[] hsl = {0, 0, 0};
		ColorUtils.RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);

		currentDragHue = snappedHue = (float) snapHueValue(hsl[0]);
		snappedSaturation = (float) snapSaturationOrLightnessValue(hsl[1]);
		snappedLightness = (float) snapSaturationOrLightnessValue(hsl[2]);

		updateSnappedColor();
	}

	private void updateSnappedColor() {
		float[] hsl = {snappedHue, snappedSaturation, snappedLightness};
		snappedColor = ColorUtils.HSLToColor(hsl);

		hsl[1] = 1;
		hsl[2] = 0.5f;
		snappedPureHueColor = ColorUtils.HSLToColor(hsl);

		if (onColorChangeListener != null) {
			onColorChangeListener.onColorChange(this, snappedColor);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				return onTouchStart(event);
			case MotionEvent.ACTION_MOVE:
				return onTouchMove(event);
			case MotionEvent.ACTION_UP:
				return onTouchEnd(event);
		}

		return super.onTouchEvent(event);
	}

	private boolean onTouchStart(MotionEvent event) {
		PointF pos = new PointF(event.getX(), event.getY());
		PointF hueKnobPosition = getHueKnobPosition(snappedHue);
		PointF toneKnobPosition = getToneKnobPosition(snappedHue, snappedSaturation, snappedLightness);
		float dist2 = KNOB_RADIUS * KNOB_RADIUS;
		float hueKnobDist2 = PointFUtil.distance2(hueKnobPosition, pos);
		float toneKnobDist2 = PointFUtil.distance2(toneKnobPosition, pos);

		// check if user tapped hue or tone knobs
		if (hueKnobDist2 < dist2 && hueKnobDist2 < toneKnobDist2) {
			updateSnappedHueForTouchPosition(event.getX(), event.getY());
			setDragState(DragState.DRAGGING_HUE_HANDLE);
			return true;
		} else if (toneKnobDist2 < dist2 && toneKnobDist2 < hueKnobDist2) {
			setDragState(DragState.DRAGGING_TONE_HANDLE);
			return true;
		} else {

			// user did not tap a knob. so check if user tapped on the ring, in which case
			// set the hue directly and switch drag state to DRAGGING_HUE_HANDLE, otherwise,
			// see if user tapped on the tone swatch. if so, set sat/light directly and switch
			// drag state to DRAGGING_TONE_HANDLE. Otherwise, do nothing.

			// check if user tapped on hue ring
			float touchRadiusFromCenter = PointFUtil.distance(
					new PointF(layoutInfo.centerX, layoutInfo.centerY),
					new PointF(event.getX(), event.getY()));

			if (touchRadiusFromCenter > layoutInfo.hueRingInnerRadius - KNOB_RADIUS &&
					touchRadiusFromCenter < layoutInfo.hueRingOuterRadius + KNOB_RADIUS) {
				updateSnappedHueForTouchPosition(event.getX(), event.getY());
				setDragState(DragState.DRAGGING_HUE_HANDLE);
				return true;
			}

			// check if user tapped on tone square
			float toneX = (event.getX() - layoutInfo.toneSquareLeft) / layoutInfo.toneSquareSize;
			float toneY = (event.getY() - layoutInfo.toneSquareTop) / layoutInfo.toneSquareSize;
			float fudge = KNOB_RADIUS / layoutInfo.toneSquareSize;

			if (toneX >= -fudge && toneX <= 1 + fudge && toneY >= -fudge && toneY <= 1 + fudge) {
				updateSnappedSaturationAndLightnessForTouchPosition(event.getX(), event.getY());
				setDragState(DragState.DRAGGING_TONE_HANDLE);
				return true;
			}
		}

		setDragState(DragState.NONE);
		return false;
	}

	private boolean onTouchMove(MotionEvent event) {
		switch (dragState) {
			case DRAGGING_HUE_HANDLE:
				updateSnappedHueForTouchPosition(event.getX(), event.getY());
				return true;

			case DRAGGING_TONE_HANDLE:
				updateSnappedSaturationAndLightnessForTouchPosition(event.getX(), event.getY());
				return true;

			case NONE:
				break;
		}
		return false;
	}

	private boolean onTouchEnd(MotionEvent event) {
		setDragState(DragState.NONE);
		return false;
	}

	private void setDragState(DragState dragState) {
		this.dragState = dragState;
		if (dragState == DragState.NONE) {
			dragStartTime = 0;
			dragEndTime = System.currentTimeMillis();
		} else {
			dragStartTime = System.currentTimeMillis();
			dragEndTime = 0;

			// start animation
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	private void updateSnappedHueForTouchPosition(float x, float y) {
		PointF dir = PointFUtil.dir(new PointF(0, 0), new PointF(layoutInfo.centerX - x, layoutInfo.centerY - y)).first;
		currentDragHue = (float) (Math.atan2(dir.y, dir.x) * 180 / Math.PI) - 90f; // hue zero is pointing up, so rotate CCW 90deg
		while (currentDragHue < 0) {
			currentDragHue += 360.0f;
		}

		float snapped = (float) snapHueValue(currentDragHue);
		if (Math.abs(snapped - snappedHue) > 1e-3) {
			snappedHue = snapped;
			updateSnappedColor();
		}
	}

	private void updateSnappedSaturationAndLightnessForTouchPosition(float x, float y) {
		float toneX = (x - layoutInfo.toneSquareLeft) / layoutInfo.toneSquareSize;
		float toneY = (y - layoutInfo.toneSquareTop) / layoutInfo.toneSquareSize;

		toneX = Math.min(Math.max(toneX, 0), 1);
		toneY = Math.min(Math.max(toneY, 0), 1);

		float sat = (float) snapSaturationOrLightnessValue(1 - toneX);
		float light = (float) snapSaturationOrLightnessValue(1 - toneY);

		if (Math.abs(sat - snappedSaturation) > 1e-3 || Math.abs(light - snappedLightness) > 1e-3) {
			snappedSaturation = sat;
			snappedLightness = light;
			updateSnappedColor();
		}
	}

	private double snapHueValue(double hue) {
		// notice, hue uses precision, whereas sat/light uses (precision-1)
		// this is because the hue slider ring has precision+1 entries, because value 360 == value 0,
		// it's like a spiral where the last entry overlaps the first.

		hue /= 360;
		hue = Math.round(hue * precision);
		hue /= precision;
		hue *= 360;
		return hue;
	}

	private double snapSaturationOrLightnessValue(double v) {
		v = Math.round(v * (precision - 1));
		v = v / (double) (precision - 1);
		return v;
	}

	private static final class LayoutInfo {
		float contentSize;
		float centerX, centerY;
		float hueRingOuterRadius, hueRingInnerRadius;
		float toneSquareSize, toneSquareLeft, toneSquareTop, toneSquareSwatchSize;
		float hueAngleIncrement;
		float hueAngleIncrementDegrees;

		LayoutInfo() {
		}
	}
}