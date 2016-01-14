package org.zakariya.photodoodle.fragments;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.activities.AboutActivity;
import org.zakariya.photodoodle.activities.DoodleActivity;
import org.zakariya.photodoodle.activities.SyncSettingsActivity;
import org.zakariya.photodoodle.adapters.DoodleDocumentAdapter;
import org.zakariya.photodoodle.model.PhotoDoodleDocument;

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

	public DoodleDocumentGridFragment() {
		setHasOptionsMenu(true);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		realm = Realm.getInstance(getContext());
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
			case R.id.menuItemSync:
				showSync();
				return true;

			case R.id.menuItemAbout:
				showAbout();
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
		int columns = (int) Math.ceil((float) displayWidth / maxItemSize);

		RealmResults<PhotoDoodleDocument> docs = PhotoDoodleDocument.all(realm);
		adapter = new DoodleDocumentAdapter(getContext(), recyclerView, columns, docs, emptyView);
		adapter.setOnClickListener(this);
		adapter.setOnLongClickListener(this);


		layoutManager = new GridLayoutManager(getContext(), columns);
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.addItemDecoration(new DividerItemDecoration(
				getResources().getDimension(R.dimen.doodle_grid_item_border_width),
				ContextCompat.getColor(getContext(), R.color.doodleGridThumbnailBorderColor)
		));

		recyclerView.setAdapter(adapter);
		return v;
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
		PhotoDoodleDocument newDoc = PhotoDoodleDocument.create(realm, getString(R.string.untitled_document));
		adapter.addItem(newDoc);
		recyclerView.smoothScrollToPosition(0);

		editPhotoDoodle(newDoc);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

		View rootView = getView();
		if (rootView == null) {
			throw new IllegalStateException("Called on unattached fragment");
		}

		// hide document from adapter
		Log.i(TAG, "deletePhotoDoodle: deleting document");
		adapter.removeItem(doc);

		Snackbar snackbar = Snackbar.make(rootView, R.string.snackbar_document_deleted, Snackbar.LENGTH_LONG);

		// make text white
		View view = snackbar.getView();
		TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
		tv.setTextColor(Color.WHITE);

		final String docUuid = doc.getUuid();

		snackbar.setCallback(new Snackbar.Callback() {
			@Override
			public void onDismissed(Snackbar snackbar, int event) {
				super.onDismissed(snackbar, event);
				PhotoDoodleDocument doc = PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm, docUuid);
				if (doc != null && !adapter.contains(doc)) {
					PhotoDoodleDocument.delete(getContext(), realm, doc);
				}

			}
		});

		snackbar.setAction(R.string.snackbar_document_deleted_undo, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PhotoDoodleDocument doc = PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm, docUuid);
				if (doc != null && !adapter.contains(doc)) {
					adapter.addItem(doc);
				}
			}
		});

		//noinspection deprecation
		snackbar.setActionTextColor(getResources().getColor(R.color.accent));

		snackbar.show();
	}

	void editPhotoDoodle(PhotoDoodleDocument doc) {
		editPhotoDoodle(doc, null);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	void editPhotoDoodle(PhotoDoodleDocument doc, @Nullable View tappedItem) {

		Intent intent = new Intent(getContext(), DoodleActivity.class);
		intent.putExtra(DoodleActivity.EXTRA_DOODLE_DOCUMENT_UUID, doc.getUuid());
		startActivityForResult(intent, REQUEST_EDIT_DOODLE);
	}

	void showAbout() {
		startActivity(new Intent(getContext(), AboutActivity.class));
	}

	void showSync() {
		startActivity(new Intent(getContext(), SyncSettingsActivity.class));
	}

	private class DividerItemDecoration extends RecyclerView.ItemDecoration {

		float thickness;
		private Paint paint;

		public DividerItemDecoration(float thickness, int color) {
			this.thickness = thickness;

			paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(color);
			paint.setStyle(Paint.Style.FILL);
		}

		@Override
		public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
			super.onDrawOver(c, parent, state);

			GridLayoutManager glm = (GridLayoutManager) parent.getLayoutManager();
			DoodleDocumentAdapter adapter = (DoodleDocumentAdapter) parent.getAdapter();

			int columns = glm.getSpanCount();
			int rows = (int) Math.ceil((double) adapter.getItemCount() / (double) columns);

			int childCount = parent.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = parent.getChildAt(i);
				int adapterPos = parent.getChildAdapterPosition(child);
				int row = (int) Math.floor((double) adapterPos / (double) columns);
				int col = adapterPos % columns;

				float leftBorderWidth = thickness;
				float topBorderWidth = thickness;
				float rightBorderWidth = thickness;
				float bottomBorderWidth = thickness;

				if (row > 0) {
					topBorderWidth *= 0.5;
				}

				if (row < rows - 1) {
					bottomBorderWidth *= 0.5;
				}

				if (col > 0) {
					leftBorderWidth *= 0.5;
				}

				if (col < columns - 1) {
					rightBorderWidth *= 0.5;
				}

				float left = child.getLeft();
				float top = child.getTop();
				float right = child.getRight();
				float bottom = child.getBottom();

				c.drawRect(left, top, right, top + topBorderWidth, paint);
				c.drawRect(left, bottom - bottomBorderWidth, right, bottom, paint);
				c.drawRect(left, top + topBorderWidth, left + leftBorderWidth, bottom - bottomBorderWidth, paint);
				c.drawRect(right - rightBorderWidth, top + topBorderWidth, right, bottom - bottomBorderWidth, paint);
			}
		}
	}

}
