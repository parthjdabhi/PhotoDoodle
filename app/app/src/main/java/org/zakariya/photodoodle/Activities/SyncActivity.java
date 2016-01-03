package org.zakariya.photodoodle.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.squareup.otto.Subscribe;

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
public class SyncActivity extends AppCompatActivity {

	private static final String TAG = "SyncActivity";
	private static final int RC_GET_SIGN_IN = 1;

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.signedIn)
	ViewGroup signedIn;

	@Bind(R.id.signedOut)
	ViewGroup signedOut;

	MenuItem signOutMenuItem;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sync);
		ButterKnife.bind(this);
		BusProvider.getBus().register(this);

		setSupportActionBar(toolbar);
		syncToSignedInState();
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
		syncToSignedInState();
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

	private void syncToSignedInState() {
		GoogleSignInAccount account = SignInManager.getInstance().getGoogleSignInAccount();
		if (account != null) {
			showSignedInState(account);
		} else {
			showSignedOutState();
		}
	}

	private void showSignedOutState() {
		signedIn.setVisibility(View.GONE);
		signedOut.setVisibility(View.VISIBLE);

		if (signOutMenuItem != null) {
			signOutMenuItem.setVisible(false);
		}
	}

	private void showSignedInState(GoogleSignInAccount account) {
		signedIn.setVisibility(View.VISIBLE);
		signedOut.setVisibility(View.GONE);

		if (signOutMenuItem != null) {
			signOutMenuItem.setVisible(true);
		}
	}
}
