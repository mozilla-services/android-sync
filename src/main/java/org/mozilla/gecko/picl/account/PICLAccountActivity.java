/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.account;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.kevinsawicki.http.HttpRequest;

public class PICLAccountActivity extends AccountAuthenticatorActivity {

  private static final String TAG = "PICLAccountAuthenticatorActivity";

  static final String KEY_SERVER = "http://192.168.1.108:8090";
  static final String KEY_SERVER_USER = "user";

  static final Uri KEY_SERVER_USER_URI = Uri.parse(KEY_SERVER).buildUpon()
      .appendPath(KEY_SERVER_USER).build();

  static final String KEY_SERVER_POST = KEY_SERVER + "/" + KEY_SERVER_USER;
  static final String KEY_SERVER_GET = KEY_SERVER_POST + "/";

  private EditText emailText;
  private Button submitButton;

  private boolean isGetting = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.warn(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.picl_login);

    emailText = (EditText) findViewById(R.id.email);
    submitButton = (Button) findViewById(R.id.submit);

    submitButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Logger.warn(TAG, "submitButton.onClick()");

        String email = emailText.getText().toString();
        if (TextUtils.isEmpty(email)) return;

        if (isGetting) return;
        isGetting = true;

        new GetUserKeyTask().execute(email);
      }

    });
  }

  protected void onKey(KeyResponse res) {
    Logger.debug(TAG, "onKey(res)");

    Account account = PICLAccountAuthenticator.createAccount(this, res.email, res.kA, res.deviceId, res.version);

    if (account != null) {
      Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      setAccountAuthenticatorResult(result);
    }

    finish();
  }

  private class GetUserKeyTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      String email = params[0];
      String uri = KEY_SERVER_USER_URI.buildUpon()
          .appendQueryParameter("email", email)
          .build().toString();

      // if email has been used before, we can get GET the key
      HttpRequest request = null;
      String response = null;

      Logger.debug(TAG, "get(" + uri + ")");

      request = HttpRequest.get(uri);
      if (request.ok()) {
        response = request.body();
      } else {
        Logger.info(TAG, request.code() + " " + request.message() + "\n" + request.body());
      }

      // if that GET is 4xx, then we should POST to create a key
      if (response == null) {
        String send = "email=" + email;
        request = HttpRequest.post(KEY_SERVER_USER_URI.toString()).send(send);
        if (request.created()) {
          response = request.body();
        } else {
          Logger.info(TAG, request.code() + " " + request.message() + "\n" + request.body());
        }

      }



      return response;
    }

    @Override
    protected void onPostExecute(String json) {
      isGetting = false;


      if (json != null) {
        //lets find the key in the JSON
        try {
          JSONObject obj = (JSONObject) new JSONTokener(json).nextValue();

          KeyResponse res = new KeyResponse();

          res.email = emailText.getText().toString();
          res.kA = obj.optString("kA");
          res.deviceId = obj.optString("deviceId");
          res.version = obj.optString("version");

          onKey(res);
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  private static class KeyResponse {
    public String email;
    public String kA;
    public String version;
    public String deviceId;
  }
}
