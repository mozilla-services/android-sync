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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fxaccount_create_account_fragment, container, false);

    FxAccountSetupActivity.linkifyTextViews(v, new int[] { R.id.description, R.id.policy });

    Button b = (Button) v.findViewById(R.id.create_account_button);
    b.setOnClickListener(this);
    return v;
  }

  protected void onCreateAccount(View button) {
    View view = getView();
    Logger.debug(LOG_TAG, "onCreateAccount: Asking for username/password for new account.");
    String email = ((EditText) view.findViewById(R.id.email)).getText().toString();
    String password = ((EditText) view.findViewById(R.id.password)).getText().toString();
    String password2 = ((EditText) view.findViewById(R.id.password2)).getText().toString();
    Logger.debug(LOG_TAG, "onCreateAccount: email: " + email);
    Logger.debug(LOG_TAG, "onCreateAccount: password: " + password);
    Logger.debug(LOG_TAG, "onCreateAccount: password2: " + password2);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.create_account_button:
      onCreateAccount(v);
      break;
    }
  }
}
