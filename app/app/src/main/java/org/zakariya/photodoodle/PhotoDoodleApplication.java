package org.zakariya.photodoodle;

import android.content.res.Configuration;

import org.zakariya.photodoodle.util.DoodleThumbnailRenderer;
import org.zakariya.photodoodle.util.SignInManager;

/**
 * Created by shamyl on 12/20/15.
 */
public class PhotoDoodleApplication extends android.app.Application {

	private static final String TAG = "PhotoDoodleApplication";

	private static PhotoDoodleApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		initSingletons();
	}

	public static PhotoDoodleApplication getInstance() {
		return instance;
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		DoodleThumbnailRenderer.getInstance().onTrimMemory(level);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		DoodleThumbnailRenderer.getInstance().onLowMemory();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		DoodleThumbnailRenderer.getInstance().onConfigurationChanged(newConfig);
	}

	private void initSingletons() {
		DoodleThumbnailRenderer.init(this);
		SignInManager.init(this);
	}
}
