package org.zakariya.photodoodle.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import org.zakariya.photodoodle.model.DoodleDocument;

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

	@Bind(R.id.recyclerView)
	RecyclerView recyclerView;

	@Bind(R.id.fab)
	FloatingActionButton newDoodleFab;

	@Bind(R.id.emptyView)
	View emptyView;

	RecyclerView.LayoutManager layoutManager;
	DoodleDocumentAdapter adapter;

	public DoodleDocumentGridFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		adapter.onDestroy();
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

		layoutManager = new GridLayoutManager(getContext(), 2);
		recyclerView.setLayoutManager(layoutManager);

		RealmResults<DoodleDocument> docs = Realm.getInstance(getContext()).allObjectsSorted(DoodleDocument.class, "modificationDate", Sort.DESCENDING);
		adapter = new DoodleDocumentAdapter(getContext(), docs, emptyView);
		adapter.setOnClickListener(this);
		adapter.setOnLongClickListener(this);
		recyclerView.setAdapter(adapter);


		return v;
	}

	@OnClick(R.id.fab)
	public void createNewPhotoDoodle() {
		DoodleDocument doc = DoodleDocument.create(Realm.getInstance(getContext()),getString(R.string.untitled_document));
		//editPhotoDoodle(doc);
	}

	@Override
	public void onDoodleDocumentClick(DoodleDocument document) {
		Log.i(TAG, "onDoodleDocumentClick: " + document.getUuid());
	}

	@Override
	public boolean onDoodleDocumentLongClick(DoodleDocument document) {
		Log.i(TAG, "onDoodleDocumentLongClick: " + document.getUuid());
		return true;
	}

	public void editPhotoDoodle(DoodleDocument doc) {
		Intent intent = new Intent(getContext(), DoodleActivity.class);

		startActivity(intent);
	}

}
