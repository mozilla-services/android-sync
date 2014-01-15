/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

/**
 * Activity which displays sign up/sign in screen to the user.
 */
public class FxAccountCreateAccountActivity extends FxAccountAbstractActivity {
  protected static final String LOG_TAG = FxAccountCreateAccountActivity.class.getSimpleName();

  protected EditText emailEdit;
  protected EditText passwordEdit;
  protected Button showPasswordButton;
  protected EditText yearEdit;
  protected Button createAccountButton;

  protected PopupWindow emailPopupWindow;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_create_account);

    linkifyTextViews(null, new int[] { R.id.policy });

    emailEdit = (EditText) ensureFindViewById(null, R.id.email, "email edit");
    passwordEdit = (EditText) ensureFindViewById(null, R.id.password, "password edit");
    showPasswordButton = (Button) ensureFindViewById(null, R.id.show_password, "show password button");
    yearEdit = (EditText) ensureFindViewById(null, R.id.year_edit, "year edit");
    createAccountButton = (Button) ensureFindViewById(null, R.id.create_account_button, "sign up button");

    createAccountButton();
    createYearEdit();

    // emailEdit.setError("TEST ERROR");
  }

  protected void createAccountButton() {
    createAccountButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(FxAccountCreateAccountActivity.this, FxAccountCreateSuccessActivity.class);
        intent.putExtra("email", emailEdit.getText().toString());
        startActivity(intent);
      }
    });
  }

  protected void createYearEdit() {
    yearEdit.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        final String[] years = new String[20];
        for (int i = 0; i < years.length; i++) {
          years[i] = Integer.toString(2014 - i);
        }

        android.content.DialogInterface.OnClickListener listener = new Dialog.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            yearEdit.setText(years[which]);
          }
        };

        AlertDialog dialog = new AlertDialog.Builder(FxAccountCreateAccountActivity.this)
        .setTitle(R.string.fxaccount_when_were_you_born)
        .setItems(years, listener)
        .setIcon(R.drawable.fxaccount_icon)
        .create();

        dialog.show();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onResume() {
    super.onResume();
    if (FxAccountAuthenticator.getFirefoxAccounts(this).length > 0) {
      redirectToActivity(FxAccountStatusActivity.class);
      return;
    }
  }
}
