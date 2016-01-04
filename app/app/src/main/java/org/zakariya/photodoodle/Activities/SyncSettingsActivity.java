package org.zakariya.photodoodle.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import org.zakariya.photodoodle.R;
import org.zakariya.photodoodle.events.GoogleSignInEvent;
import org.zakariya.photodoodle.events.GoogleSignOutEvent;
import org.zakariya.photodoodle.util.BusProvider;
import org.zakariya.photodoodle.util.SignInManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by shamyl on 1/2/16.
 */
public class SyncSettingsActivity extends AppCompatActivity {

	private static final String TAG = "SyncSettingsActivity";
	private static final int RC_GET_SIGN_IN = 1;

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.signedIn)
	ViewGroup signedInView;

	@Bind(R.id.signedOut)
	ViewGroup signedOutView;

	@Bind(R.id.userEmailTextView)
	TextView userEmailTextView;

	@Bind(R.id.userNameTextView)
	TextView userNameTextView;

	@Bind(R.id.avatarImageView)
	ImageView avatarImageView;
	
	@Bind(R.id.syncHistoryRecyclerView)
	RecyclerView syncHistoryRecyclerView;

	MenuItem signOutMenuItem;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sync_settings);
		ButterKnife.bind(this);
		BusProvider.getBus().register(this);

		setSupportActionBar(toolbar);
		syncToCurrentSignedInState();
	}

	@Override
	protected void onDestroy() {
		BusProvider.getBus().unregister(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_sync, menu);
		signOutMenuItem = menu.findItem(R.id.menuItemSignOut);
		syncToCurrentSignedInState();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemSignOut:
				signOut();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RC_GET_SIGN_IN) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			SignInManager.getInstance().setGoogleSignInResult(result);
		}
	}

	@OnClick(R.id.signInButton)
	void signIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(SignInManager.getInstance().getGoogleApiClient());
		startActivityForResult(signInIntent, RC_GET_SIGN_IN);
	}
	
	@OnClick(R.id.syncNowButton)
	void syncNow() {
		Log.i(TAG, "syncNow: ");
	}
	
	@OnClick(R.id.resetAndSyncButton)
	void resetAndSync() {
		Log.i(TAG, "resetAndSync: ");
	}

	void signOut() {
		SignInManager.getInstance().signOut();
	}

	@Subscribe
	public void onSignedIn(GoogleSignInEvent event) {
		showSignedInState(event.getGoogleSignInAccount());
	}

	@Subscribe
	public void onSignedOut(GoogleSignOutEvent event) {
		showSignedOutState();
	}

	private void syncToCurrentSignedInState() {
		GoogleSignInAccount account = SignInManager.getInstance().getGoogleSignInAccount();
		if (account != null) {
			showSignedInState(account);
		} else {
			showSignedOutState();
		}
	}

	private void showSignedOutState() {
		signedInView.setVisibility(View.GONE);
		signedOutView.setVisibility(View.VISIBLE);

		if (signOutMenuItem != null) {
			signOutMenuItem.setVisible(false);
		}
	}

	private void showSignedInState(GoogleSignInAccount account) {
		signedInView.setVisibility(View.VISIBLE);
		signedOutView.setVisibility(View.GONE);

		if (signOutMenuItem != null) {
			signOutMenuItem.setVisible(true);
		}

		Picasso.with(this).load(account.getPhotoUrl()).into(avatarImageView);
		userEmailTextView.setText(account.getEmail());
		userNameTextView.setText(account.getDisplayName());
	}
}
