package org.zakariya.photodoodle.activities;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.view.ColorSwatchView;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by shamyl on 12/4/15.
 */
public class DrawPopupController {

	public interface Callbacks {
		void onSelectPencil();
		void onSelectBrush();
		void onSelectSmallEraser();
		void onSelectBigEraser();
		void onSelectColor(ColorSwatchView colorSwatchView);
		void onClearDrawing();
	}

	WeakReference<Callbacks> callbacksWeakReference;
	View popupView;

	@Bind(R.id.pencilToolButton)
	ImageButton pencilToolButton;

	@Bind(R.id.brushToolButton)
	ImageButton brushToolButton;

	@Bind(R.id.smallEraserToolButton)
	ImageButton smallEraserToolButton;

	@Bind(R.id.bigEraserToolButton)
	ImageButton bigEraserToolButton;

	@Bind(R.id.colorSwatchTool)
	ColorSwatchView colorSwatchView;

	@Bind(R.id.clearDrawingButton)
	Button clearDrawingButton;

	public DrawPopupController(View popupView, Callbacks callbacks) {
		this.popupView = popupView;
		callbacksWeakReference = new WeakReference<>(callbacks);
		ButterKnife.bind(this,popupView);

	}

	public View getPopupView() {
		return popupView;
	}

	@OnClick(R.id.pencilToolButton)
	void onPencilToolButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectPencil();
		}
	}

	@OnClick(R.id.brushToolButton)
	void onBrushToolButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectBrush();
		}
	}

	@OnClick(R.id.smallEraserToolButton)
	void onSmallEraserToolButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectSmallEraser();
		}
	}

	@OnClick(R.id.bigEraserToolButton)
	void onBigEraserToolButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectBigEraser();
		}
	}

	@OnClick(R.id.colorSwatchTool)
	void onColorSwatchViewClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectColor(colorSwatchView);
		}
	}

	@OnClick(R.id.clearDrawingButton)
	void onClearPhotoButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onClearDrawing();
		}
	}

	public void setColorSwatchColor(int color) {
		colorSwatchView.setColor(color);
	}

	public int getColorSwatchColor() {
		return colorSwatchView.getColor();
	}
}
