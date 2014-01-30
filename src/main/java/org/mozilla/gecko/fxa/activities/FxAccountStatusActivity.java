/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.config.activities.SelectEnginesActivity;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Activity which displays account status.
 */
public class FxAccountStatusActivity extends FxAccountAbstractActivity implements OnClickListener {
  protected static final String LOG_TAG = FxAccountStatusActivity.class.getSimpleName();

  // Set in onCreate.
  protected TextView syncStatusTextView;
  protected ViewFlipper connectionStatusViewFlipper;
  protected View connectionStatusUnverifiedView;
  protected View connectionStatusSignInView;
  protected TextView emailTextView;

  protected CheckBox bookmarksCheckBox;
  protected CheckBox historyCheckBox;
  protected CheckBox passwordsCheckBox;
  protected CheckBox tabsCheckBox;

  // Set in onResume.
  protected AndroidFxAccount fxAccount;

  public FxAccountStatusActivity() {
    super(CANNOT_RESUME_WHEN_NO_ACCOUNTS_EXIST);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.setThreadLogTag(FxAccountConstants.GLOBAL_LOG_TAG);
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_status);

    syncStatusTextView = (TextView) ensureFindViewById(null, R.id.sync_status_text, "sync status text");
    connectionStatusViewFlipper = (ViewFlipper) ensureFindViewById(null, R.id.connection_status_view, "connection status frame layout");
    connectionStatusUnverifiedView = ensureFindViewById(null, R.id.unverified_view, "unverified view");
    connectionStatusSignInView = ensureFindViewById(null, R.id.sign_in_view, "sign in view");

    launchActivityOnClick(connectionStatusSignInView, FxAccountUpdateCredentialsActivity.class);

    emailTextView = (TextView) findViewById(R.id.email);

    bookmarksCheckBox = (CheckBox) findViewById(R.id.bookmarks_checkbox);
    historyCheckBox = (CheckBox) findViewById(R.id.history_checkbox);
    passwordsCheckBox = (CheckBox) findViewById(R.id.passwords_checkbox);
    tabsCheckBox = (CheckBox) findViewById(R.id.tabs_checkbox);
    bookmarksCheckBox.setOnClickListener(this);
    historyCheckBox.setOnClickListener(this);
    passwordsCheckBox.setOnClickListener(this);
    tabsCheckBox.setOnClickListener(this);

    if (FxAccountConstants.LOG_PERSONAL_INFORMATION) {
      createDebugButtons();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Account accounts[] = FxAccountAuthenticator.getFirefoxAccounts(this);
    if (accounts.length < 1 || accounts[0] == null) {
      Logger.warn(LOG_TAG, "No Android accounts.");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    this.fxAccount = new AndroidFxAccount(this, accounts[0]);
    if (fxAccount == null) {
      Logger.warn(LOG_TAG, "Could not get Firefox Account from Android account.");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    refresh();
  }

  protected void showNeedsUpgrade() {
    syncStatusTextView.setText(R.string.fxaccount_status_sync);
    connectionStatusViewFlipper.setVisibility(View.VISIBLE);
    connectionStatusViewFlipper.setDisplayedChild(0);
  }

  protected void showNeedsPassword() {
    syncStatusTextView.setText(R.string.fxaccount_status_sync);
    connectionStatusViewFlipper.setVisibility(View.VISIBLE);
    connectionStatusViewFlipper.setDisplayedChild(1);
  }

  protected void showNeedsVerification() {
    syncStatusTextView.setText(R.string.fxaccount_status_sync);
    connectionStatusViewFlipper.setVisibility(View.VISIBLE);
    connectionStatusViewFlipper.setDisplayedChild(2);
  }

  protected void showConnected() {
    syncStatusTextView.setText(R.string.fxaccount_status_sync_enabled);
    connectionStatusViewFlipper.setVisibility(View.GONE);
  }

  protected void refresh() {
    emailTextView.setText(fxAccount.getEmail());

    // Interrogate the Firefox Account's state.
    State state = fxAccount.getState();
    switch (state.getNeededAction()) {
    case NeedsUpgrade:
      showNeedsUpgrade();
      break;
    case NeedsPassword:
      showNeedsPassword();
      break;
    case NeedsVerification:
      showNeedsVerification();
      break;
    default:
      showConnected();
    }

    try {
      Set<String> engines = SelectEnginesActivity.getEnginesToSelect(fxAccount.getSyncPrefs());
      bookmarksCheckBox.setChecked(engines.contains("bookmarks"));
      historyCheckBox.setChecked(engines.contains("history"));
      passwordsCheckBox.setChecked(engines.contains("passwords"));
      tabsCheckBox.setChecked(engines.contains("tabs"));
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception getting engines to select; ignoring.", e);
      return;
    }
  }

  @Override
  public void onClick(View view) {
    if (view == bookmarksCheckBox ||
        view == historyCheckBox ||
        view == passwordsCheckBox ||
        view == tabsCheckBox) {
      saveEngineSelections();
    }
  }

  protected void saveEngineSelections() {
    Map<String, Boolean> engineSelections = new HashMap<String, Boolean>();
    engineSelections.put("bookmarks", bookmarksCheckBox.isChecked());
    engineSelections.put("history", historyCheckBox.isChecked());
    engineSelections.put("passwords", passwordsCheckBox.isChecked());
    engineSelections.put("tabs", tabsCheckBox.isChecked());
    Logger.info(LOG_TAG, "Persisting engine selections: " + engineSelections.toString());

    try {
      // No GlobalSession.config, so store directly to prefs.
      SyncConfiguration.storeSelectedEnginesToPrefs(fxAccount.getSyncPrefs(), engineSelections);
      // Request a sync sometime soon.
      ContentResolver.requestSync(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY, Bundle.EMPTY);
      // Request immediate sync.
      // SyncAdapter.requestImmediateSync(fxAccount.getAndroidAccount(), null);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception persisting selected engines; ignoring.", e);
      return;
    }
  }

  protected void createDebugButtons() {
    if (!FxAccountConstants.LOG_PERSONAL_INFORMATION) {
      return;
    }

    final LinearLayout existingUserView = (LinearLayout) findViewById(R.id.existing_user);
    if (existingUserView == null) {
      return;
    }

    final LinearLayout debugButtonsView = new LinearLayout(this);
    debugButtonsView.setOrientation(LinearLayout.VERTICAL);
    debugButtonsView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    existingUserView.addView(debugButtonsView, existingUserView.getChildCount());

    Button button;

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Refresh status view");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Logger.info(LOG_TAG, "Refreshing.");
        refresh();
      }
    });

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Dump account details");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        fxAccount.dump();
      }
    });

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Force sync");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Logger.info(LOG_TAG, "Syncing.");
        final Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(fxAccount.getAndroidAccount(), BrowserContract.AUTHORITY, extras);
        // No sense refreshing, since the sync will complete in the future.
      }
    });

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Forget certificate (if applicable)");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        State state = fxAccount.getState();
        try {
          Married married = (Married) state;
          Logger.info(LOG_TAG, "Moving to Cohabiting state: Forgetting certificate.");
          fxAccount.setState(married.makeCohabitingState());
          refresh();
        } catch (ClassCastException e) {
          Logger.info(LOG_TAG, "Not in Married state; can't forget certificate.");
          // Ignore.
        }
      }
    });

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Require password");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Logger.info(LOG_TAG, "Moving to Separated state: Forgetting password.");
        State state = fxAccount.getState();
        fxAccount.setState(state.makeSeparatedState());
        refresh();
      }
    });

    button = new Button(this);
    debugButtonsView.addView(button, debugButtonsView.getChildCount());
    button.setText("Require upgrade");
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Logger.info(LOG_TAG, "Moving to Doghouse state: Requiring upgrade.");
        State state = fxAccount.getState();
        fxAccount.setState(state.makeDoghouseState());
        refresh();
      }
    });
  }
}
