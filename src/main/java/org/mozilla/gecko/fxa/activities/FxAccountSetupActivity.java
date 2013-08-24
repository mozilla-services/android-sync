/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.accounts.AccountAuthenticatorActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * Activity which displays login screen to the user.
 */
public class FxAccountSetupActivity extends AccountAuthenticatorActivity {
  public static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_setup);

    TabHost tabs = (TabHost) findViewById(R.id.tabhost);
    tabs.setup();

    TabHost.TabSpec createAccountTab = tabs.newTabSpec("create_account");
    createAccountTab.setContent(R.id.create_account_tab);
    createAccountTab.setIndicator(getResources().getString(R.string.fxaccount_create_account_label));
    tabs.addTab(createAccountTab);

    TabHost.TabSpec signInTab = tabs.newTabSpec("sign_in");
    signInTab.setContent(R.id.sign_in_tab);
    signInTab.setIndicator(getResources().getString(R.string.fxaccount_sign_in_label));
    tabs.addTab(signInTab);

    for (int id : new int[] { R.id.description, R.id.policy, R.id.forgot_password }) {
      TextView textView = (TextView) findViewById(id);
      if (textView == null) {
        Logger.warn(LOG_TAG, "Could not process links for view with id " + id + ".");
        continue;
      }
      textView.setMovementMethod(LinkMovementMethod.getInstance());
      textView.setText(Html.fromHtml(textView.getText().toString()));
    }
  }

  public void onCreateAccount(View view) {
    view = findViewById(R.id.create_account_tab);
    Logger.debug(LOG_TAG, "onCreateAccount: Asking for username/password for new account.");
    String email = ((EditText) view.findViewById(R.id.email)).getText().toString();
    String password = ((EditText) view.findViewById(R.id.password)).getText().toString();
    String password2 = ((EditText) view.findViewById(R.id.password2)).getText().toString();
    Logger.debug(LOG_TAG, "onCreateAccount: email: " + email);
    Logger.debug(LOG_TAG, "onCreateAccount: password: " + password);
    Logger.debug(LOG_TAG, "onCreateAccount: password2: " + password2);
  }

  public void onSignIn(View view) {
    view = findViewById(R.id.sign_in_tab);
    Logger.debug(LOG_TAG, "onSignIn: Asking for username/password for existing account.");
    String email = ((EditText) view.findViewById(R.id.email)).getText().toString();
    String password = ((EditText) view.findViewById(R.id.password)).getText().toString();
    Logger.debug(LOG_TAG, "onCreateAccount: email: " + email);
    Logger.debug(LOG_TAG, "onCreateAccount: password: " + password);
  }
}
