/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountClient;
import org.mozilla.gecko.fxaccount.FxAccountClientDelegate;
import org.mozilla.gecko.fxaccount.MockMyIdFxAccountClient;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class FxAccountSetupExistingAccountActivity extends Activity {
  private static final String LOG_TAG = FxAccountSetupExistingAccountActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Log.i(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.fxa_setup_existing_account);
  }

  public void onNext(View view) {
    final Activity self = this;

    EditText emailEdit = (EditText) findViewById(R.id.fxaccount_login_email);
    EditText passwordEdit = (EditText) findViewById(R.id.fxaccount_login_password);

    final String email = emailEdit.getText().toString();
    final String password = passwordEdit.getText().toString();

    final FxAccountClient accountClient = new MockMyIdFxAccountClient();
    final Activity activity = this;

    new AsyncTask<Void, String, Boolean>() {
      protected ProgressDialog progressDialog;

      @Override
      protected void onPreExecute() {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("title");
        progressDialog.setMessage("message");
        progressDialog.show();
      }

      @Override
      protected Boolean doInBackground(Void... params) {
        publishProgress("Logging in to existing account.");

        accountClient.logIn(email, password, new FxAccountClientDelegate() {
          @Override
          public void onSuccess(final ExtendedJSONObject result) {
            Logger.info(LOG_TAG, "Got result: " + result.toJSONString());
            publishProgress("Logged in to existing account!");

            self.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                Intent data = new Intent();
                data.putExtra(FxAccountSetupActivity.PARAM_EMAIL, email);
                data.putExtra(FxAccountSetupActivity.PARAM_PASSWORD, password);
                data.putExtra(FxAccountSetupActivity.PARAM_RESULT, result.toJSONString());

                setResult(RESULT_OK, data);
                finish();
              }
            });
          }

          @Override
          public void onFailure(Exception e) {
            publishProgress("Failed to log in to existing account: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }

          @Override
          public void onError(Exception e) {
            publishProgress("Failed to log in to existing account: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }
        });

        return true;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
        progressDialog.setMessage(progress[0]);
      }

      @Override
      protected void onPostExecute(Boolean result) {
        progressDialog.dismiss();
      }
    }.execute();
  }
}