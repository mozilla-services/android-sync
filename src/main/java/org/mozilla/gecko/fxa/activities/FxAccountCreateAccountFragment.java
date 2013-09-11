/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class FxAccountCreateAccountFragment extends Fragment implements OnClickListener {
  protected static final String LOG_TAG = FxAccountCreateAccountFragment.class.getSimpleName();

  protected EditText emailEdit;
  protected EditText passwordEdit;
  protected EditText password2Edit;
  protected Button button;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Retain this fragment across configuration changes. See, for example,
    // http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
    // This fragment will own AsyncTask instances which should not be
    // interrupted by configuration changes (and activity changes).
    setRetainInstance(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fxaccount_create_account_fragment, container, false);

    FxAccountSetupActivity.linkifyTextViews(v, new int[] { R.id.description, R.id.policy });

    emailEdit = (EditText) ensureFindViewById(v, R.id.email, "email");
    passwordEdit = (EditText) ensureFindViewById(v, R.id.password, "password");
    // Second password can be null.
    password2Edit = (EditText) v.findViewById(R.id.password2);

    button = (Button) ensureFindViewById(v, R.id.create_account_button, "button");
    button.setOnClickListener(this);
    return v;
  }

  protected void onCreateAccount(View button) {
    Logger.debug(LOG_TAG, "onCreateAccount: Asking for username/password for new account.");
    String email = emailEdit.getText().toString();
    String password = passwordEdit.getText().toString();
    String password2 = password2Edit.getText().toString();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.create_account_button:
      onCreateAccount(v);
      break;
    }
  }

  /**
   * Helper to find view or error if it is missing.
   *
   * @param id of view to find.
   * @param description to print in error.
   * @return non-null <code>View</code> instance.
   */
  protected View ensureFindViewById(View v, int id, String description) {
    View view = v.findViewById(id);
    if (view == null) {
      String message = "Could not find view " + description + ".";
      Logger.error(LOG_TAG, message);
      throw new RuntimeException(message);
    }
    return view;
  }
}
