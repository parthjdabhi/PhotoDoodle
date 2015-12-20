package org.zakariya.photodoodle.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
public class DoodleThumbnailRenderer {

	private static final String TAG = DoodleThumbnailRenderer.class.getSimpleName();

	public interface Callbacks {
		void onThumbnailReady(Bitmap thumbnail);
	}

	public class RenderTask {
		Future future;
		boolean canceled;

		public RenderTask() {
			future = null;
			canceled = false;
		}

		public void cancel() {
			if (!canceled) {
				future.cancel(false);
				canceled = true;
			}
		}

		public boolean isCanceled() {
			return canceled;
		}
	}

	private static DoodleThumbnailRenderer instance;

	private Handler handler;
	private ExecutorService executor;
	private Map<String, RenderTask> tasks;

	static {
		instance = new DoodleThumbnailRenderer();
	}

	public static DoodleThumbnailRenderer getInstance() {
		return instance;
	}

	private DoodleThumbnailRenderer() {
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		tasks = new HashMap<>();
	}

	public RenderTask renderThumbnail(final Context context, final PhotoDoodleDocument document, final int width, final int height, final Callbacks callbacks) {

		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
		}

		final String documentUuid = document.getUuid();
		String taskId = generateRenderTaskKey(documentUuid, width, height);
		Log.i(TAG, "renderThumbnail: submitting task id: " + taskId);

		RenderTask task = new RenderTask();
		tasks.put(taskId, task);

		task.future = executor.submit(new Runnable() {
			@Override
			public void run() {
				performRenderThumbnail(context, documentUuid, width, height, handler, callbacks);
			}
		});

		return task;
	}

	private static String generateRenderTaskKey(String documentUuid, int width, int height) {
		return documentUuid + "@(w:" + width + "-h:" + height + ")";
	}

	private void performRenderThumbnail(Context context, String documentUuid, int width, int height, Handler handler, final Callbacks callbacks) {

		try {
			final String taskId = generateRenderTaskKey(documentUuid, width, height);
			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.eraseColor(0xFFFFFFFF);
			Canvas bitmapCanvas = new Canvas(bitmap);

			Realm realm = Realm.getInstance(context);
			PhotoDoodleDocument document = PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm,documentUuid);
			PhotoDoodle doodle = PhotoDoodleDocument.loadPhotoDoodle(context,document);
			doodle.draw(bitmapCanvas, width, height);
			realm.close();


			// notify on main thread
			handler.post(new Runnable() {
				@Override
				public void run() {
					final RenderTask task = tasks.get(taskId);
					if (task != null){
						if (!task.isCanceled()) {
							Log.i(TAG, "performRenderThumbnail: run: sending bitmap to callback");
							callbacks.onThumbnailReady(bitmap);
						} else {
							Log.i(TAG, "performRenderThumbnail: run: task was canceled");
						}
						tasks.remove(taskId);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
