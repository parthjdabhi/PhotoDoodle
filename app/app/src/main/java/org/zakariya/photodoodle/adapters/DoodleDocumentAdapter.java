package org.zakariya.photodoodle.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.model.PhotoDoodleDocument;
import org.zakariya.photodoodle.util.DoodleThumbnailRenderer;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by shamyl on 12/18/15.
 */
public class DoodleDocumentAdapter extends RecyclerView.Adapter<DoodleDocumentAdapter.ViewHolder> {

	private static final String TAG = DoodleDocumentAdapter.class.getSimpleName();

	public interface OnClickListener {
		/**
		 * Called when an item is clicked
		 *
		 * @param document the document represented by this item
		 */
		void onDoodleDocumentClick(PhotoDoodleDocument document);
	}

	public interface OnLongClickListener {
		/**
		 * Called when an item is long clicked
		 *
		 * @param document the document represented by this item
		 * @return true if the long click is handled, false otherwise
		 */
		boolean onDoodleDocumentLongClick(PhotoDoodleDocument document);
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public PhotoDoodleDocument photoDoodleDocument;
		public View rootView;
		public ImageView imageView;
		public ImageView loadingImageView;
		public TextView nameTextView;
		public TextView dateTextView;
		public TextView uuidTextView;

		DoodleThumbnailRenderer.RenderTask thumbnailRenderTask;

		public ViewHolder(View v) {
			super(v);
			rootView = v;
			imageView = (ImageView) v.findViewById(R.id.imageView);
			loadingImageView = (ImageView) v.findViewById(R.id.loadingImageView);
			nameTextView = (TextView) v.findViewById(R.id.nameTextView);
			dateTextView = (TextView) v.findViewById(R.id.dateTextView);
			uuidTextView = (TextView) v.findViewById(R.id.uuidTextView);
		}
	}

	Context context;
	Realm realm;
	View emptyView;
	RealmResults<PhotoDoodleDocument> realmResults;
	private final RealmChangeListener realmChangeListener;
	DateFormat dateFormatter;
	WeakReference<OnClickListener> weakOnClickListener;
	WeakReference<OnLongClickListener> weakOnLongClickListener;
	int crossfadeDuration;

	public DoodleDocumentAdapter(Context context, RealmResults<PhotoDoodleDocument> realmResults, View emptyView) {
		this.context = context;
		this.realmResults = realmResults;
		this.emptyView = emptyView;

		realmChangeListener = new RealmChangeListener() {
			@Override
			public void onChange() {
				updateEmptyView();
				notifyDataSetChanged();
			}
		};

		realm = Realm.getInstance(context);
		realm.addChangeListener(realmChangeListener);
		dateFormatter = DateFormat.getDateTimeInstance();
		crossfadeDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
		updateEmptyView();
	}

	public void setOnClickListener(@Nullable OnClickListener listener) {
		if (listener != null) {
			weakOnClickListener = new WeakReference<>(listener);
		} else {
			weakOnClickListener = null;
		}
	}

	@Nullable
	public OnClickListener getOnClickListener() {
		return weakOnClickListener != null ? weakOnClickListener.get() : null;
	}

	public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
		if (listener != null) {
			weakOnLongClickListener = new WeakReference<>(listener);
		} else {
			weakOnLongClickListener = null;
		}
	}

	@Nullable
	public OnLongClickListener getOnLongClickListener() {
		return weakOnLongClickListener != null ? weakOnLongClickListener.get() : null;
	}

	/**
	 * Your activity/fragment needs to call this to unregister it from listening to realm changes
	 */
	public void onDestroy() {
		realm.removeChangeListener(realmChangeListener);
		realm.close();
	}

	void updateEmptyView() {
		emptyView.setVisibility(realmResults.isEmpty() ? View.VISIBLE : View.GONE);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		View v = inflater.inflate(R.layout.doodle_document_grid_item, parent, false);

		final ViewHolder holder = new ViewHolder(v);
		holder.rootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PhotoDoodleDocument doc = holder.photoDoodleDocument;
				OnClickListener listener = getOnClickListener();
				if (doc != null && listener != null) {
					listener.onDoodleDocumentClick(doc);
				}
			}
		});

		holder.rootView.setLongClickable(true);
		holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PhotoDoodleDocument doc = holder.photoDoodleDocument;
				OnLongClickListener listener = getOnLongClickListener();
				return doc != null && listener != null && listener.onDoodleDocumentLongClick(doc);
			}
		});

		return holder;
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, int position) {

		if (holder.thumbnailRenderTask != null) {
			holder.thumbnailRenderTask.cancel();
			holder.thumbnailRenderTask = null;
		}

		PhotoDoodleDocument doc = realmResults.get(position);
		holder.photoDoodleDocument = doc;

		holder.nameTextView.setText(doc.getName());

		Date date = doc.getModificationDate() != null ? doc.getModificationDate() : doc.getCreationDate();
		holder.dateTextView.setText(dateFormatter.format(date));

		holder.uuidTextView.setText(doc.getUuid());

		holder.loadingImageView.setAlpha(1f);
		holder.loadingImageView.setVisibility(View.VISIBLE);

		// now generate a thumbnail
		// TODO: Compute ideal thumbnail size. MeasuredWidth doesn't work, maybe need treeViewObserver...
		int width = 256;
		int height = 256;

		holder.thumbnailRenderTask = DoodleThumbnailRenderer.getInstance().renderThumbnail(context, doc, width, height, new DoodleThumbnailRenderer.Callbacks() {
			@Override
			public void onThumbnailReady(Bitmap thumbnail) {

				holder.imageView.setImageBitmap(thumbnail);
				holder.loadingImageView.animate()
						.alpha(0)
						.setDuration(crossfadeDuration)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								holder.loadingImageView.setVisibility(View.GONE);
								super.onAnimationEnd(animation);
							}
						});


			}
		});
	}

	@Override
	public int getItemCount() {
		return realmResults.size();
	}

	public void notifyItemChanged(PhotoDoodleDocument document) {

		// can't use realmResults.indexOf - it's not implemented :(
		String uuid = document.getUuid();
		for (int i = 0; i < realmResults.size(); i++) {
			PhotoDoodleDocument doc = realmResults.get(i);
			if (doc.getUuid().equals(uuid)) {
				notifyItemChanged(i);
				break;
			}
		}
	}
}
