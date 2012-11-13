/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient.AvatarDelegate;
import org.mozilla.gecko.fxaccount.FxAccountClient;
import org.mozilla.gecko.fxaccount.MockMyIdFxAccountClient;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public class FxAccountAvatarUpdateActivity extends Activity {
  private static final String LOG_TAG = FxAccountAvatarUpdateActivity.class.getSimpleName();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.info(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.fxa_avatar_update);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onResume() {
    super.onResume();

    Intent intent = this.getIntent();
    Logger.info(LOG_TAG, "onResume(" + intent + ")");

    String resultString = intent.getStringExtra(FxAccountSetupActivity.PARAM_RESULT);
    ExtendedJSONObject result;
    try {
      result = new ExtendedJSONObject(resultString);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception!", e);
      return;
    }

    final Activity activity = this;

    new AsyncTask<ExtendedJSONObject, String, Boolean>() {
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
      protected Boolean doInBackground(ExtendedJSONObject... params) {
        ExtendedJSONObject result = params[0];

        Logger.info(LOG_TAG, "Got result: " + result.toJSONString());

        publishProgress("Getting Browser ID assertion.");

        final FxAccountClient accountClient = new MockMyIdFxAccountClient();

        final String audience = "https://myapps.mozillalabs.com";
        String assertion;
        try {
          assertion = accountClient.getAssertion(result, audience);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Got exception.", e);
          return false;
        }

        publishProgress("Got Browser ID assertion.  Fetching avatar.");

        FxAccountAvatarClient avatarClient = new FxAccountAvatarClient(FxAccountSetupActivity.AVATAR_SERVICE_URI);

        Logger.info(LOG_TAG, "Fetching avatar with assertion '" + assertion + "'.");


        avatarClient.getAvatar(assertion, new AvatarDelegate() {
          @Override
          public void onSuccess(final ExtendedJSONObject avatar) {
            Logger.info(LOG_TAG, "Got avatar: " + avatar.toJSONString());

            publishProgress("Fetched avatar!");

            activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                Intent updateIntent = new Intent(activity, FxAccountAvatarStatusActivity.class);
                updateIntent.putExtra(FxAccountSetupActivity.PARAM_RESULT, avatar.toJSONString());
                startActivity(updateIntent);

                Intent data = new Intent();
                setResult(RESULT_OK, data);
                finish();
              }
            });
          }

          @Override
          public void onFailure(Exception e) {
            publishProgress("Failed to fetch avatar: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }

          @Override
          public void onError(Exception e) {
            publishProgress("Failed to fetch avatar: " + e.toString());

            Logger.warn(LOG_TAG, "Got exception." + e.toString(), new RuntimeException());
          }
        });

        return true;
      }
    }.execute(result);
  }
}
