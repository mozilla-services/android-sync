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

public class FxAccountSignInFragment extends Fragment implements OnClickListener {
  protected static final String LOG_TAG = FxAccountSignInFragment.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Retain this fragment across configuration changes. See, for example,
    // http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
    setRetainInstance(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fxaccount_sign_in_fragment, container, false);

    Button b = (Button) v.findViewById(R.id.sign_in_button);
    b.setOnClickListener(this);

    return v;
  }

  public void onSignIn(View button) {
    View view = getView();
    Logger.debug(LOG_TAG, "onSignIn: Asking for username/password for existing account.");
    String email = ((EditText) view.findViewById(R.id.email)).getText().toString();
    String password = ((EditText) view.findViewById(R.id.password)).getText().toString();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.sign_in_button:
      onSignIn(v);
      break;
    }
  }
}
