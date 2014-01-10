/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

/**
 * Activity which displays account status.
 */
public class FxAccountStatusActivity extends FragmentActivity {
  protected static final String LOG_TAG = FxAccountStatusActivity.class.getSimpleName();

  protected View newUserView;
  protected View existingUserView;

  protected View connectionStatusUnverifiedView;
  protected View connectionStatusSignInView;
  protected View connectionStatusSyncingView;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_status);

    newUserView = findViewById(R.id.new_user);
    existingUserView = findViewById(R.id.existing_user);

    connectionStatusUnverifiedView = findViewById(R.id.unverified_view);
    connectionStatusSignInView = findViewById(R.id.sign_in_view);
    connectionStatusSyncingView = findViewById(R.id.syncing_view);
  }

  @Override
  public void onResume() {
    super.onResume();
    refresh();
  }

  protected void refresh(Account account) {
    TextView email = (TextView) findViewById(R.id.email);

    if (account == null) {
      newUserView.setVisibility(View.VISIBLE);
      existingUserView.setVisibility(View.GONE);
      return;
    }

    newUserView.setVisibility(View.GONE);
    existingUserView.setVisibility(View.VISIBLE);

    AndroidFxAccount fxAccount = new AndroidFxAccount(this, account);

    email.setText(account.name);

    if (!fxAccount.isVerified()) {
      connectionStatusUnverifiedView.setVisibility(View.VISIBLE);
      connectionStatusSignInView.setVisibility(View.GONE);
      connectionStatusSyncingView.setVisibility(View.GONE);
      return;
    }

    connectionStatusUnverifiedView.setVisibility(View.GONE);
    connectionStatusSignInView.setVisibility(View.GONE);
    connectionStatusSyncingView.setVisibility(View.VISIBLE);
  }

  protected void refresh() {
    Account accounts[] = FxAccountAuthenticator.getFirefoxAccounts(this);
    if (accounts.length < 1) {
      refresh(null);
      return;
    }
    refresh(accounts[0]);
  }

  protected void dumpAccountDetails() {
    Account accounts[] = FxAccountAuthenticator.getFirefoxAccounts(this);
    if (accounts.length < 1) {
      return;
    }
    AndroidFxAccount fxAccount = new AndroidFxAccount(this, accounts[0]);
    fxAccount.dump();
  }

  protected void resetAccountTokens() {
    Account accounts[] = FxAccountAuthenticator.getFirefoxAccounts(this);
    if (accounts.length < 1) {
      return;
    }
    AndroidFxAccount fxAccount = new AndroidFxAccount(this, accounts[0]);
    fxAccount.resetAccountTokens();
    fxAccount.dump();
  }

  public void onClickRefresh(View view) {
    Logger.debug(LOG_TAG, "Refreshing.");
    refresh();
  }

  public void onClickResetAccountTokens(View view) {
    Logger.debug(LOG_TAG, "Resetting account tokens.");
    resetAccountTokens();
  }

  public void onClickDumpAccountDetails(View view) {
    Logger.debug(LOG_TAG, "Dumping account details.");
    dumpAccountDetails();
  }

  public void onClickGetStarted(View view) {
    Logger.debug(LOG_TAG, "Launching setup activity.");
    Intent intent = new Intent(this, FxAccountSetupActivity.class);
    startActivity(intent);
    finish();
  }

  public void onClickVerify(View view) {
    Logger.debug(LOG_TAG, "Launching verification activity.");
  }

  public void onClickSignIn(View view) {
    Logger.debug(LOG_TAG, "Launching sign in again activity.");
  }
}
