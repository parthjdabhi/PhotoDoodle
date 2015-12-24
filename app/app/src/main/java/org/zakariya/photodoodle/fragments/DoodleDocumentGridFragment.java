package org.zakariya.photodoodle.fragments;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.activities.DoodleActivity;
import org.zakariya.photodoodle.adapters.DoodleDocumentAdapter;
import org.zakariya.photodoodle.model.PhotoDoodleDocument;
import org.zakariya.photodoodle.util.DoodleThumbnailRenderer;
import org.zakariya.photodoodle.util.FragmentUtils;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by shamyl on 12/16/15.
 */
public class DoodleDocumentGridFragment extends Fragment implements DoodleDocumentAdapter.OnClickListener, DoodleDocumentAdapter.OnLongClickListener {

	private static final String TAG = DoodleDocumentGridFragment.class.getSimpleName();
	private static final int REQUEST_EDIT_DOODLE = 1;

	@Bind(R.id.recyclerView)
	RecyclerView recyclerView;

	@Bind(R.id.fab)
	FloatingActionButton newDoodleFab;

	@Bind(R.id.emptyView)
	View emptyView;

	Realm realm;
	RecyclerView.LayoutManager layoutManager;
	DoodleDocumentAdapter adapter;

	PhotoDoodleDocument newlyCreatedDocumentPendingEdit;
	Handler handler;

	public DoodleDocumentGridFragment() {
		setHasOptionsMenu(true);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		realm = Realm.getInstance(getContext());
		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	public void onDestroy() {
		adapter.onDestroy();
		realm.close();
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Icepick.saveInstanceState(this, outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_doodle_document_grid, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemSettings:
				return true;
		}

		return false;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_doodle_document_grid, container, false);
		ButterKnife.bind(this, v);

		// compute a good columns count such that items are no bigger than R.dimen.max_doodle_grid_item_size
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int displayWidth = metrics.widthPixels;
		float maxItemSize = getResources().getDimension(R.dimen.max_doodle_grid_item_size);
		int columns = (int) Math.ceil((float) displayWidth / (float) maxItemSize);

		layoutManager = new GridLayoutManager(getContext(), columns);
		recyclerView.setLayoutManager(layoutManager);

		RealmResults<PhotoDoodleDocument> docs = realm.allObjects(PhotoDoodleDocument.class);
		adapter = new DoodleDocumentAdapter(getContext(), recyclerView, columns, docs, emptyView);
		adapter.setOnClickListener(this);
		adapter.setOnLongClickListener(this);
		recyclerView.setAdapter(adapter);

		recyclerView.setItemAnimator(new DefaultItemAnimator(){
			@Override
			public void onAddFinished(RecyclerView.ViewHolder item) {
				super.onAddFinished(item);
				DoodleDocumentGridFragment.this.onAddItemAnimationFinished(item);
			}
		});

		return v;
	}

	@Override
	public void onDestroyView() {
		// the item animator set in onCreateView holds a strong reference to this
		recyclerView.setItemAnimator(null);
		super.onDestroyView();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_EDIT_DOODLE:
				if (resultCode == DoodleActivity.RESULT_OK) {
					boolean didEdit = data.getBooleanExtra(DoodleActivity.RESULT_DID_EDIT_DOODLE, false);
					String uuid = data.getStringExtra(DoodleActivity.RESULT_DOODLE_DOCUMENT_UUID);

					if (didEdit && !TextUtils.isEmpty(uuid)) {
						adapter.itemWasUpdated(PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm, uuid));
					}
				}
				break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@OnClick(R.id.fab)
	public void createNewPhotoDoodle() {
		// when the add animation completes, this document will be edited
		newlyCreatedDocumentPendingEdit = PhotoDoodleDocument.create(realm, getString(R.string.untitled_document));
		adapter.addItem(newlyCreatedDocumentPendingEdit);
		recyclerView.smoothScrollToPosition(0);
	}

	@Override
	public void onDoodleDocumentClick(PhotoDoodleDocument document, View tappedItem) {
		editPhotoDoodle(document, tappedItem);
	}

	@Override
	public boolean onDoodleDocumentLongClick(PhotoDoodleDocument document, View tappedItem) {
		queryDeletePhotoDoodle(document);
		return true;
	}

	void queryDeletePhotoDoodle(final PhotoDoodleDocument document) {
		final WeakReference<DoodleDocumentGridFragment> weakThis = new WeakReference<>(this);

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMessage(R.string.dialog_delete_document_message)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.dialog_delete_document_destructive_button_title, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						DoodleDocumentGridFragment strongThis = weakThis.get();
						if (strongThis != null) {
							strongThis.deletePhotoDoodle(document);
						}
					}
				})
				.show();
	}

	void deletePhotoDoodle(PhotoDoodleDocument doc) {
		adapter.removeItem(doc);

		realm.beginTransaction();
		doc.removeFromRealm();
		realm.commitTransaction();
	}

	void editPhotoDoodle(PhotoDoodleDocument doc) {
		editPhotoDoodle(doc, null);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	void editPhotoDoodle(PhotoDoodleDocument doc, @Nullable View tappedItem) {

		Intent intent = new Intent(getContext(), DoodleActivity.class);
		intent.putExtra(DoodleActivity.EXTRA_DOODLE_DOCUMENT_UUID, doc.getUuid());

		if (tappedItem != null && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {

			//
			// render a thumbnail for the animation to scale. note, we're relying on the thumbnail
			// being available later in DoodleThumbnailRenderer's cache since we can't pass a thumbnail via intent
			//

			int width = tappedItem.getWidth();
			int height = tappedItem.getHeight();
			Pair<Bitmap,String> result = DoodleThumbnailRenderer.getInstance().renderThumbnail(getActivity(), doc, width, height);

			intent.putExtra(DoodleActivity.EXTRA_DOODLE_THUMBNAIL_ID, result.second);
			intent.putExtra(DoodleActivity.EXTRA_DOODLE_THUMBNAIL_WIDTH, tappedItem.getWidth());
			intent.putExtra(DoodleActivity.EXTRA_DOODLE_THUMBNAIL_HEIGHT, tappedItem.getHeight());

			String transitionName = getString(R.string.transition_name_doodle_view);
			ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(), tappedItem, transitionName);

			// this is a workaround for the issue where Fragment doesn't have the options variant for startActivityForResult
			FragmentUtils.startActivityForResult(this, intent, REQUEST_EDIT_DOODLE, options.toBundle());

		} else {
			startActivityForResult(intent, REQUEST_EDIT_DOODLE);
		}
	}

	private void onAddItemAnimationFinished(RecyclerView.ViewHolder viewHolder) {
		if (newlyCreatedDocumentPendingEdit != null) {

			final DoodleDocumentAdapter.ViewHolder ddaViewHolder = (DoodleDocumentAdapter.ViewHolder) viewHolder;
			final PhotoDoodleDocument document = newlyCreatedDocumentPendingEdit;
			newlyCreatedDocumentPendingEdit = null;

			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					editPhotoDoodle(document,ddaViewHolder.imageView);
				}
			}, 500);
		}
	}

}
