package org.zakariya.photodoodle.util;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import org.zakariya.doodle.model.PhotoDoodle;
import org.zakariya.photodoodle.model.PhotoDoodleDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;

/**
 * Singleton for rendering thumbnails of PhotoDoodleDocument instances
 */
public class DoodleThumbnailRenderer implements ComponentCallbacks2 {

	private static final String TAG = DoodleThumbnailRenderer.class.getSimpleName();

	public interface Callbacks {
		void onThumbnailReady(Bitmap thumbnail);
	}

	public class RenderTask {
		private Future future;
		private boolean canceled;

		public RenderTask() {
			future = null;
			canceled = false;
		}

		synchronized public void setFuture(Future future) {
			this.future = future;
		}

		synchronized public void cancel() {
			if (!canceled) {
				future.cancel(false);
				canceled = true;
			}
		}

		synchronized public boolean isCanceled() {
			return canceled;
		}
	}

	private static DoodleThumbnailRenderer instance;

	private Context context;
	private Handler handler;
	private ExecutorService executor;
	private Map<String, RenderTask> tasks;
	private Cache cache;

	public static DoodleThumbnailRenderer getInstance() {
		return instance;
	}

	public static void init(Context context) {
		instance = new DoodleThumbnailRenderer(context);
	}

	private DoodleThumbnailRenderer(Context context) {
		this.context = context;
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		tasks = new HashMap<>();

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int maxKb = am.getMemoryClass() * 1024;
		int limitKb = maxKb / 8; // 1/8th of total ram
		cache = new Cache(limitKb);
	}

	@Override
	public void onTrimMemory(int level) {
		if (level >= TRIM_MEMORY_MODERATE) {
			cache.evictAll();
		} else if (level >= TRIM_MEMORY_BACKGROUND) {
			cache.trimToSize(cache.size() / 2);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

	@Override
	public void onLowMemory() {
		cache.evictAll();
	}

	@Nullable
	public Bitmap getThumbnail(PhotoDoodleDocument document, int width, int height) {
		final String documentUuid = document.getUuid();
		final long modificationTimestampSeconds = document.getModificationDate().getTime() / 1000;
		String taskId = generateRenderTaskKey(documentUuid, modificationTimestampSeconds, width, height);
		return cache.get(taskId);
	}

	@Nullable
	public RenderTask renderThumbnail(final PhotoDoodleDocument document, final int width, final int height, final Callbacks callbacks) {

		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
		}

		final String documentUuid = document.getUuid();
		final long modificationTimestampSeconds = document.getModificationDate().getTime() / 1000;
		String taskId = generateRenderTaskKey(documentUuid, modificationTimestampSeconds, width, height);

		Bitmap thumbnail = cache.get(taskId);
		if (thumbnail != null) {

			Log.i(TAG, "renderThumbnail: thumbnail for id: " + taskId + " exists in cache, re-using!");
			callbacks.onThumbnailReady(thumbnail);
			return null;

		} else {
			Log.i(TAG, "renderThumbnail: submitting task id: " + taskId);

			RenderTask task = new RenderTask();
			addRenderTask(taskId, task);

			task.setFuture(executor.submit(new Runnable() {
				@Override
				public void run() {
					performRenderThumbnail(documentUuid, modificationTimestampSeconds, width, height, handler, callbacks);
				}
			}));
			return task;
		}
	}

	private static String generateRenderTaskKey(String documentUuid, long timestampSeconds, int width, int height) {
		return documentUuid + "-mod:" + timestampSeconds + "-(w:" + width + "-h:" + height + ")";
	}

	@Nullable
	synchronized private RenderTask getRenderTask(String taskId) {
		return tasks.get(taskId);
	}

	synchronized private void addRenderTask(String taskId, RenderTask task) {
		tasks.put(taskId, task);
	}

	synchronized private void clearRenderTask(String taskId) {
		tasks.remove(taskId);
	}

	private void performRenderThumbnail(String documentUuid, long timestampSeconds, int width, int height, Handler handler, final Callbacks callbacks) {

		try {
			final String taskId = generateRenderTaskKey(documentUuid, timestampSeconds, width, height);
			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.eraseColor(0xFFFFFFFF);
			Canvas bitmapCanvas = new Canvas(bitmap);

			Realm realm = Realm.getInstance(context);
			PhotoDoodleDocument document = PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm, documentUuid);
			PhotoDoodle doodle = PhotoDoodleDocument.loadPhotoDoodle(context, document);
			doodle.draw(bitmapCanvas, width, height);
			realm.close();

			// cache it
			cache.put(taskId,bitmap);

			// notify on main thread
			handler.post(new Runnable() {
				@Override
				public void run() {
					RenderTask task = getRenderTask(taskId);
					if (task != null) {
						if (!task.isCanceled()) {
							Log.i(TAG, "performRenderThumbnail: run: sending bitmap to callback");
							callbacks.onThumbnailReady(bitmap);
						} else {
							Log.i(TAG, "performRenderThumbnail: run: task was canceled");
						}
						clearRenderTask(taskId);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class Cache extends LruCache<String, Bitmap> {
		public Cache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount() / 1024;
		}
	}
}
