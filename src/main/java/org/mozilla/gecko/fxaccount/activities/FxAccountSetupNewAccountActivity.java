/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient.AvatarDelegate;
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

public class FxAccountSetupNewAccountActivity extends Activity {
  private static final String LOG_TAG = FxAccountSetupNewAccountActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Log.i(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.fxa_setup_new_account);
  }

  public void onNext(View view) {
    EditText emailEdit = (EditText) findViewById(R.id.fxaccount_email);
    EditText passwordEdit = (EditText) findViewById(R.id.fxaccount_password);
    EditText passwordEdit2 = (EditText) findViewById(R.id.fxaccount_password2);

    final String email = emailEdit.getText().toString();
    final String password = passwordEdit.getText().toString();
    String password2 = passwordEdit2.getText().toString();

    if (password == null || !password2.equals(password2)) {
      return;
    }

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
      protected void onProgressUpdate(String... progress) {
        progressDialog.setMessage(progress[0]);
      }

      @Override
      protected void onPostExecute(Boolean result) {
        progressDialog.dismiss();
      }

      @Override
      protected Boolean doInBackground(Void... params) {
        publishProgress("Creating new account");

        accountClient.createAccount(email, password, new FxAccountClientDelegate() {
          @Override
          public void onSuccess(final ExtendedJSONObject result) {
            Logger.info(LOG_TAG, "Got result: " + result.toJSONString());
            publishProgress("Created account.  Getting Browser ID assertion.");

            final String audience = "https://myapps.mozillalabs.com";

            String assertion;
            try {
              assertion = accountClient.getAssertion(result, audience);
            } catch (Exception e) {
              e.printStackTrace();
              onError(e);
              return;
            }

            publishProgress("Got Browser ID assertion.  Publishing avatar.");

            ExtendedJSONObject avatar = new ExtendedJSONObject();
            avatar.put("name", "name");
            avatar.put("image", "image");

            FxAccountAvatarClient avatarClient = new FxAccountAvatarClient(FxAccountSetupActivity.AVATAR_SERVICE_URI);

            Logger.info(LOG_TAG, "Publishing avatar with assertion '" + assertion + "'.");

            avatarClient.putAvatar(assertion, avatar, new AvatarDelegate() {
              @Override
              public void onSuccess(ExtendedJSONObject o) {
                Logger.info(LOG_TAG, "Put avatar!");

                publishProgress("Published avatar!");

                activity.runOnUiThread(new Runnable() {
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
                publishProgress("Failed to publish avatar: " + e.toString());

                Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
              }

              @Override
              public void onError(Exception e) {
                publishProgress("Failed to publish avatar: " + e.toString());

                Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
              }
            });
          }

          @Override
          public void onFailure(Exception e) {
            publishProgress("Failed to create account: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }

          @Override
          public void onError(Exception e) {
            publishProgress("Failed to create account: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }
        });

        return true;
      }
    }.execute();
  }
}