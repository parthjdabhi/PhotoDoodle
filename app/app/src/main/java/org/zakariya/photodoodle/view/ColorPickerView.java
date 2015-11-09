package org.zakariya.photodoodle.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.zakariya.photodoodle.R;

/**
 * TODO: document your custom view class.
 */
public class ColorPickerView extends View {

	private static final String TAG = "ColorPickerView";

	private static final float SQRT_2 = (float) Math.sqrt(2);
	private static final float HUE_RING_THICKNESS = 8f;
	private static final float SEPARATOR_WIDTH = 4f;
	private static final float HANDLE_BORDER_WIDTH = 8f;
	private static final float KNOB_RADIUS = 44;

	private int color = 0xFF000000;
	private int snappedColor;
	private int toneSquareBaseColor;
	private float snappedHue, snappedSaturation, snappedLightness;

	private int precision = 16;
	private Paint paint;
	private Path backgroundFillPath;
	private LayoutInfo layoutInfo = new LayoutInfo();
	private Path hueRingWedgePath;
	private Path hueRingWedgeSeparatorPath;

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
		computeSnappedColor();

		a.recycle();

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int backgroundColor = 0xFFFFFFFF;
		Drawable backgroundDrawable = getBackground();
		if (backgroundDrawable instanceof ColorDrawable) {
			backgroundColor = ((ColorDrawable) backgroundDrawable).getColor();
		}

		// first draw the hue ring
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

		// draw the hue angle wedge separators

		canvas.save();
		canvas.translate(layoutInfo.centerX, layoutInfo.centerY);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SEPARATOR_WIDTH);
		paint.setColor(backgroundColor);
		canvas.drawPath(hueRingWedgeSeparatorPath, paint);
		canvas.restore();

		// now draw the background fill on top (this covers the outside and inside of ring to leave rainbow donut)
		paint.setStyle(Paint.Style.FILL);
		canvas.drawPath(backgroundFillPath, paint);

		// now render the tone square
		float swatchSize = layoutInfo.toneSquareSize / (float) precision;
		float swatchLeft = 0, swatchTop = 0;
		float halfPad = SEPARATOR_WIDTH;
		float swatchY = 0f;
		float swatchX;
		float swatchIncrement = 1f / (float) (precision - 1);
		int toneSquareBaseColor = this.toneSquareBaseColor;

		// first rendered row is white, which is drawn by stroking an empty white circle
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(0xFFdddddd);
		paint.setStrokeWidth(1);

		for (int row = 0; row < precision; row++, swatchY += swatchIncrement) {
			swatchLeft = layoutInfo.toneSquareLeft;
			swatchTop = layoutInfo.toneSquareTop + row * swatchSize;
			swatchX = 0f;

			for (int col = 0; col < precision; col++, swatchLeft += swatchSize, swatchX += swatchIncrement) {
				if (row > 0) {
					paint.setColor(plotToneSquareSwatchColor(toneSquareBaseColor, swatchX, swatchY));
				}
				canvas.drawCircle(swatchLeft + swatchSize / 2, swatchTop + swatchSize / 2, swatchSize / 2 - halfPad, paint);
			}

			paint.setStyle(Paint.Style.FILL);
		}

		// now draw selection handles
		PointF hueKnobPosition = getHueKnobPosition(snappedHue);
		paintSelectionHandle(canvas, toneSquareBaseColor, hueKnobPosition.x, hueKnobPosition.y, KNOB_RADIUS);

		PointF toneKnobPosition = getToneKnobPosition(snappedHue, snappedSaturation, snappedLightness);
		paintSelectionHandle(canvas, snappedColor, toneKnobPosition.x, toneKnobPosition.y, KNOB_RADIUS);
	}

	private PointF getHueKnobPosition(float hue) {
		float selectedHueAngle = (float) ((hue * Math.PI / 180) - Math.PI / 2);
		float knobPositionRadius = (layoutInfo.hueRingOuterRadius + layoutInfo.hueRingInnerRadius) / 2;
		float knobX = layoutInfo.centerX + (float) (Math.cos(selectedHueAngle) * knobPositionRadius);
		float knobY = layoutInfo.centerY + (float) (Math.sin(selectedHueAngle) * knobPositionRadius);
		return new PointF(knobX, knobY);
	}

	private PointF getToneKnobPosition(float snappedHue, float snappedSaturation, float snappedLightness) {
		float swatchSize = layoutInfo.toneSquareSize / (float) precision;
		float knobX = layoutInfo.toneSquareLeft + layoutInfo.toneSquareSize * (1 - snappedSaturation) + swatchSize / 2;
		float knobY = layoutInfo.toneSquareTop + layoutInfo.toneSquareSize * (1 - snappedLightness) + swatchSize / 2;
		return new PointF(knobX, knobY);
	}

	private void paintSelectionHandle(Canvas canvas, int color, float x, float y, float radius) {
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(0xFFFFFFFF);
		canvas.drawCircle(x, y, radius + HANDLE_BORDER_WIDTH, paint);

		paint.setColor(color);
		canvas.drawCircle(x, y, radius, paint);
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

	private void computeLayoutInfo() {
		Log.d(TAG, "computeLayoutInfo");

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
		float ringThickness = layoutInfo.hueRingOuterRadius - layoutInfo.hueRingInnerRadius;

		layoutInfo.toneSquareSize = (layoutInfo.hueRingInnerRadius - (ringThickness / 2) - (KNOB_RADIUS * 2)) / SQRT_2 * 2f;
		layoutInfo.toneSquareLeft = centerX - layoutInfo.toneSquareSize / 2;
		layoutInfo.toneSquareTop = centerY - layoutInfo.toneSquareSize / 2;

		layoutInfo.hueAngleIncrement = (float) (2 * Math.PI) / (float) precision;
		layoutInfo.hueAngleIncrementDegrees = 360f / (float) precision;
	}

	private void generateRenderPaths() {
		Log.d(TAG, "generateRenderPaths");

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

	@Override
	public void layout(int l, int t, int r, int b) {
		super.layout(l, t, r, b);

		Log.d(TAG, "layout");
		computeLayoutInfo();
		generateRenderPaths();
		invalidate();
	}

	private float getHueAngleIncrement() {
		return (float) (2 * Math.PI) / (float) precision;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = Math.min(Math.max(precision, 1), 256);
		computeSnappedColor();
		invalidate();
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		computeSnappedColor();
		invalidate();
	}

	private void computeSnappedColor() {
		float[] hsl = {0, 0, 0};
		ColorUtils.RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);

		int hue = ((int) hsl[0] / precision) * precision;
		int sat = (((int) (hsl[1] * 256)) / precision) * precision;
		int light = (((int) (hsl[2] * 256)) / precision) * precision;

		snappedHue = hue;
		snappedSaturation = sat / 256f;
		snappedLightness = light / 256f;

		// convert back to int color
		hsl[0] = snappedHue;
		hsl[1] = snappedSaturation;
		hsl[2] = snappedLightness;
		snappedColor = ColorUtils.HSLToColor(hsl);

		Log.i(TAG, "computeSnappedColor h: " + snappedHue + " s: " + snappedSaturation + " l: " + snappedLightness + " -> #" + Integer.toHexString(snappedColor));

		// make fully bright, and generate base color for toneSquare
		hsl[1] = 1;
		hsl[2] = 0.5f;
		toneSquareBaseColor = ColorUtils.HSLToColor(hsl);
	}

	private static final class LayoutInfo {
		float contentSize;
		float centerX, centerY;
		float hueRingOuterRadius, hueRingInnerRadius;
		float toneSquareSize, toneSquareLeft, toneSquareTop;
		float hueAngleIncrement;
		float hueAngleIncrementDegrees;

		LayoutInfo() {
		}
	}
}
