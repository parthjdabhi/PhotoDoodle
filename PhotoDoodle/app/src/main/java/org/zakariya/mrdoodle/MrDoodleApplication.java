package org.zakariya.mrdoodle;

import android.content.res.Configuration;

import org.zakariya.mrdoodle.util.DoodleThumbnailRenderer;
import org.zakariya.mrdoodle.util.GoogleSignInManager;

/**
 * Created by shamyl on 12/20/15.
 */
public class MrDoodleApplication extends android.app.Application {

	private static final String TAG = "MrDoodleApplication";

	private static MrDoodleApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		initSingletons();
	}

	public static MrDoodleApplication getInstance() {
		return instance;
	}

	public void onApplicationBackgrounded() {
		GoogleSignInManager.getInstance().disconnect();
	}

	public void onApplicationResumed() {
		GoogleSignInManager.getInstance().connect();
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
		GoogleSignInManager.init(this);
	}
}
