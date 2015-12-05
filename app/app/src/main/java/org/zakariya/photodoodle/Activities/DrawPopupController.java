package org.zakariya.photodoodle.activities;

import android.graphics.drawable.Drawable;
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

	public enum ActiveTool {
		PENCIL,
		BRUSH,
		SMALL_ERASER,
		BIG_ERASER
	}

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
	ActiveTool activeTool;

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
		setActiveTool(ActiveTool.PENCIL);
	}

	public View getPopupView() {
		return popupView;
	}

	public ActiveTool getActiveTool() {
		return activeTool;
	}

	public void setActiveTool(ActiveTool activeTool) {
		this.activeTool = activeTool;

		ImageButton btn = null;
		switch (activeTool){
			case PENCIL:
				btn = pencilToolButton;
				break;
			case BRUSH:
				btn = brushToolButton;
				break;
			case SMALL_ERASER:
				btn = smallEraserToolButton;
				break;
			case BIG_ERASER:
				btn = bigEraserToolButton;
				break;
		}

		@SuppressWarnings("deprecation")
		Drawable highlight = popupView.getResources().getDrawable(R.drawable.popup_button_selected_background);

		pencilToolButton.setBackground(btn == pencilToolButton ? highlight : null);
		brushToolButton.setBackground(btn == brushToolButton ? highlight : null);
		smallEraserToolButton.setBackground(btn == smallEraserToolButton ? highlight : null);
		bigEraserToolButton.setBackground(btn == bigEraserToolButton ? highlight : null);
	}

	@OnClick(R.id.pencilToolButton)
	void onPencilToolButtonClick(){
		setActiveTool(ActiveTool.PENCIL);
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectPencil();
		}
	}

	@OnClick(R.id.brushToolButton)
	void onBrushToolButtonClick(){
		setActiveTool(ActiveTool.BRUSH);
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectBrush();
		}
	}

	@OnClick(R.id.smallEraserToolButton)
	void onSmallEraserToolButtonClick(){
		setActiveTool(ActiveTool.SMALL_ERASER);
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onSelectSmallEraser();
		}
	}

	@OnClick(R.id.bigEraserToolButton)
	void onBigEraserToolButtonClick(){
		setActiveTool(ActiveTool.BIG_ERASER);
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


	@SuppressWarnings("unused")
	public int getColorSwatchColor() {
		return colorSwatchView.getColor();
	}
}
