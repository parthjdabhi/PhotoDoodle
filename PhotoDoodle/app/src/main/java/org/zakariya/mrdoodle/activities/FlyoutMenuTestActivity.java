package org.zakariya.mrdoodle.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import org.zakariya.mrdoodle.R;
import org.zakariya.mrdoodle.util.BusProvider;
import org.zakariya.mrdoodle.view.FlyoutMenuView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by shamyl on 5/20/16.
 */
public class FlyoutMenuTestActivity extends BaseActivity {

	@Bind(R.id.toolbar)
	Toolbar toolbar;

	@Bind(R.id.flyoutMenu)
	FlyoutMenuView flyoutMenuView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_flyout_menu_test);
		ButterKnife.bind(this);
		BusProvider.getBus().register(this);

		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onDestroy() {
		BusProvider.getBus().unregister(this);
		super.onDestroy();
	}

}
