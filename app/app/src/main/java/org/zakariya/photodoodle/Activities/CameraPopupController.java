package org.zakariya.photodoodle.activities;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import org.zakariya.photodoodle.R;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by shamyl on 12/4/15.
 */
public class CameraPopupController {

	public interface Callbacks {
		void onTakePhoto();
		void onClearPhoto();
	}

	WeakReference<Callbacks> callbacksWeakReference;
	View popupView;

	@Bind(R.id.takePhotoButton)
	ImageButton takePhotoButton;

	@Bind(R.id.clearPhotoButton)
	Button clearPhotoButton;

	public CameraPopupController(View popupView, Callbacks callbacks) {
		this.popupView = popupView;
		callbacksWeakReference = new WeakReference<>(callbacks);
		ButterKnife.bind(this,popupView);
	}

	public View getPopupView() {
		return popupView;
	}

	@OnClick(R.id.takePhotoButton)
	void onTakePhotoButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onTakePhoto();
		}
	}

	@OnClick(R.id.clearPhotoButton)
	void onClearPhotoButtonClick(){
		Callbacks cb = callbacksWeakReference.get();
		if (cb != null) {
			cb.onClearPhoto();
		}
	}
}
