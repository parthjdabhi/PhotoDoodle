package org.zakariya.mrdoodle.util;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.Pair;

import org.zakariya.doodle.model.PhotoDoodle;
import org.zakariya.mrdoodle.model.DoodleDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;

/**
 * Singleton for rendering thumbnails of DoodleDocument instances
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

	/**
	 * Get an id usable to identify a thumbnail for a given document
	 *
	 * @param document the document
	 * @param width    the width of the thumbnail
	 * @param height   the height of the thumbnail
	 * @return a string passable to getThumbnailById
	 */
	public static String getThumbnailId(DoodleDocument document, int width, int height) {
		final String documentUuid = document.getUuid();
		final long modificationTimestampSeconds = document.getModificationDate().getTime() / 1000;
		return generateRenderTaskKey(documentUuid, modificationTimestampSeconds, width, height);
	}

	/**
	 * Get a rendered thumbnail - if it exists - by its ID
	 *
	 * @param id the id of a thumbnail. You can get the ID from getThumbnailId
	 * @return a bitmap if it exists in the cache
	 */
	@Nullable
	public Bitmap getThumbnailById(String id) {
		return TextUtils.isEmpty(id) ? null : cache.get(id);
	}

	/**
	 * Renders a thumbnail for a given DoodleDocument synchronously, or returns existing cached one if available.
	 *
	 * @param context  the context
	 * @param document the document
	 * @param width    the width to render the thumbnail
	 * @param height   the height to render the thumbnail
	 * @return a bitmap containing the document's rendering, at the provided width/height
	 */
	public Pair<Bitmap, String> renderThumbnail(Context context, DoodleDocument document, int width, int height) {

		String thumbnailId = getThumbnailId(document, width, height);
		Bitmap thumbnail = cache.get(thumbnailId);

		if (thumbnail != null) {
			return new Pair<>(thumbnail, thumbnailId);
		} else {
			thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			thumbnail.eraseColor(0xFFFFFFFF);
			Canvas bitmapCanvas = new Canvas(thumbnail);

			Realm realm = Realm.getInstance(context);
			PhotoDoodle doodle = DoodleDocument.load(context, document);
			doodle.draw(bitmapCanvas, width, height);
			realm.close();

			cache.put(thumbnailId, thumbnail);

			return new Pair<>(thumbnail, thumbnailId);
		}
	}

	/**
	 * Asynchronously render a thumbnail at a provided width/height for a document
	 *
	 * @param document  the document whos thumbnail you wish to render
	 * @param width     the target width
	 * @param height    the target height
	 * @param callbacks invoked when the thumbnail is ready
	 * @return a RenderTask which is cancelable
	 * <p/>
	 * If a cached version of the thumbnail exists, it will be passed immediately to the callback.
	 */
	@Nullable
	public RenderTask renderThumbnail(final DoodleDocument document, final int width, final int height, final Callbacks callbacks) {

		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
		}

		final String documentUuid = document.getUuid();
		final String taskId = getThumbnailId(document, width, height);

		Bitmap thumbnail = cache.get(taskId);
		if (thumbnail != null) {
			callbacks.onThumbnailReady(thumbnail);
			return null;
		} else {
			RenderTask task = new RenderTask();
			addRenderTask(taskId, task);

			task.setFuture(executor.submit(new Runnable() {
				@Override
				public void run() {
					performRenderThumbnail(documentUuid, taskId, width, height, handler, callbacks);
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

	private void performRenderThumbnail(String documentUuid, final String taskId, int width, int height, Handler handler, final Callbacks callbacks) {

		try {
			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.eraseColor(0xFFFFFFFF);
			Canvas bitmapCanvas = new Canvas(bitmap);

			Realm realm = Realm.getInstance(context);
			DoodleDocument document = DoodleDocument.byUUID(realm, documentUuid);
			PhotoDoodle doodle = DoodleDocument.load(context, document);
			doodle.draw(bitmapCanvas, width, height);
			realm.close();

			// cache it
			cache.put(taskId, bitmap);

			// notify on main thread
			handler.post(new Runnable() {
				@Override
				public void run() {
					RenderTask task = getRenderTask(taskId);
					if (task != null) {
						if (!task.isCanceled()) {
							//Log.i(TAG, "performRenderThumbnail: run: sending bitmap to callback");
							callbacks.onThumbnailReady(bitmap);
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
