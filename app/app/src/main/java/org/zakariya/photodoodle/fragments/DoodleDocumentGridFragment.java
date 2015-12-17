package org.zakariya.photodoodle.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.activities.DoodleActivity;
import org.zakariya.photodoodle.model.DoodleDocument;

import java.text.DateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by shamyl on 12/16/15.
 */
public class DoodleDocumentGridFragment extends Fragment {

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

		layoutManager = new GridLayoutManager(getContext(), 3);
		recyclerView.setLayoutManager(layoutManager);

		RealmResults<DoodleDocument> docs = Realm.getInstance(getContext()).allObjectsSorted(DoodleDocument.class, "modificationDate", Sort.DESCENDING);
		adapter = new DoodleDocumentAdapter(getContext(), docs, emptyView);
		recyclerView.setAdapter(adapter);


		return v;
	}

	@OnClick(R.id.fab)
	public void createNewPhotoDoodle() {
		DoodleDocument doc = DoodleDocument.create(Realm.getInstance(getContext()),getString(R.string.untitled_document));
		//editPhotoDoodle(doc);
	}

	public void editPhotoDoodle(DoodleDocument doc) {
		Intent intent = new Intent(getContext(), DoodleActivity.class);

		startActivity(intent);
	}

	static class DoodleDocumentAdapter extends RecyclerView.Adapter<DoodleDocumentAdapter.ViewHolder> {


		public class ViewHolder extends RecyclerView.ViewHolder {
			public ImageView imageView;
			public TextView nameTextView;
			public TextView dateTextView;
			public TextView uuidTextView;

			public ViewHolder(View v) {
				super(v);
				imageView = (ImageView) v.findViewById(R.id.imageView);
				nameTextView = (TextView) v.findViewById(R.id.nameTextView);
				dateTextView = (TextView) v.findViewById(R.id.dateTextView);
				uuidTextView = (TextView) v.findViewById(R.id.uuidTextView);
			}
		}

		Context context;
		View emptyView;
		RealmResults<DoodleDocument> realmResults;
		private final RealmChangeListener realmChangeListener;
		DateFormat dateFormatter;

		DoodleDocumentAdapter(Context context, RealmResults<DoodleDocument> realmResults, View emptyView) {
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

			Realm.getInstance(context).addChangeListener(realmChangeListener);

			dateFormatter = DateFormat.getDateTimeInstance();
		}

		void onDestroy() {
			Realm.getInstance(context).removeChangeListener(realmChangeListener);
		}

		void updateEmptyView(){
			emptyView.setVisibility(realmResults.isEmpty() ? View.VISIBLE : View.GONE);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			View v = inflater.inflate(R.layout.doodle_document_grid_item, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {

			DoodleDocument doc = realmResults.get(position);
			holder.nameTextView.setText(doc.getName());

			Date date = doc.getModificationDate() != null ? doc.getModificationDate() : doc.getCreationDate();
			holder.dateTextView.setText(dateFormatter.format(date));

			holder.uuidTextView.setText(doc.getUuid());
		}

		@Override
		public int getItemCount() {
			return realmResults.size();
		}
	}
}
