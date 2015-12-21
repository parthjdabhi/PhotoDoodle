package org.zakariya.photodoodle.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

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
	String editedDocumentUuid;
	Handler delayedUpdateHandler;

	public DoodleDocumentGridFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		realm = Realm.getInstance(getContext());
		delayedUpdateHandler = new Handler(Looper.getMainLooper());
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

		int columns = 2;
		layoutManager = new GridLayoutManager(getContext(), columns);
		recyclerView.setLayoutManager(layoutManager);

		RealmResults<PhotoDoodleDocument> docs = realm.allObjectsSorted(PhotoDoodleDocument.class, "modificationDate", Sort.DESCENDING);
		adapter = new DoodleDocumentAdapter(getContext(), recyclerView, columns, docs, emptyView);
		adapter.setOnClickListener(this);
		adapter.setOnLongClickListener(this);
		recyclerView.setAdapter(adapter);


		return v;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_EDIT_DOODLE:
				if (resultCode == DoodleActivity.RESULT_OK) {
					boolean didEdit = data.getBooleanExtra(DoodleActivity.RESULT_DID_EDIT_DOODLE, false);
					final String uuid = data.getStringExtra(DoodleActivity.RESULT_DOODLE_DOCUMENT_UUID);
					Log.i(TAG, "onActivityResult: uuid: " + uuid + " didEdit: " + didEdit);

					if (didEdit && !TextUtils.isEmpty(uuid)) {
						editedDocumentUuid = uuid;
					} else {
						editedDocumentUuid = null;
					}
				}
				break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!TextUtils.isEmpty(editedDocumentUuid)) {
			delayedUpdateHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					adapter.itemWasUpdated(PhotoDoodleDocument.getPhotoDoodleDocumentByUuid(realm, editedDocumentUuid));
					recyclerView.smoothScrollToPosition(0);
					editedDocumentUuid = null;
				}
			}, 500);
		}
	}

	@OnClick(R.id.fab)
	public void createNewPhotoDoodle() {
		PhotoDoodleDocument doc = PhotoDoodleDocument.create(realm, getString(R.string.untitled_document));
		adapter.addItem(doc);
		recyclerView.smoothScrollToPosition(0);
		editPhotoDoodle(doc);
	}

	@Override
	public void onDoodleDocumentClick(PhotoDoodleDocument document) {
		editPhotoDoodle(document);
	}

	@Override
	public boolean onDoodleDocumentLongClick(PhotoDoodleDocument document) {
		queryDeletePhotoDoodle(document);
		return true;
	}

	void queryDeletePhotoDoodle(final PhotoDoodleDocument document) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.dialog_delete_document)
				.setMessage(getString(R.string.dialog_delete_document_message, document.getName()))
				.setNeutralButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.dialog_delete_document_destructive_button_title, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deletePhotoDoodle(document);
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
		Intent intent = new Intent(getContext(), DoodleActivity.class);
		intent.putExtra(DoodleActivity.EXTRA_DOODLE_DOCUMENT_UUID, doc.getUuid());
		startActivityForResult(intent, REQUEST_EDIT_DOODLE);
	}

}
