/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.account;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.picl.PICLAccountConstants;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client.KeyResponse;
import org.mozilla.gecko.picl.account.net.PICLKeyServer0Client.PICLKeyServer0ClientDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class PICLAccountActivity extends AccountAuthenticatorActivity implements PICLKeyServer0ClientDelegate {

  private static final String TAG = "PICLAccountAuthenticatorActivity";

  private static final String PERSONA_URL = "https://picl.personatest.org/communication_iframe";
  private static final String PERSONA_ORIGIN = "https://firefox.com";

  private static final String PICL_JS_OBJECT = "__picl__";

  private EditText emailText;
  private Button submitButton;
  private EditText passwordText;
  private TextView hintView;
  private WebView webView;

  private boolean isPersonaLoaded = false;
  private boolean hasCheckedPersona = false;
  private boolean hasPersonaAccount = false;
  private boolean isLoggedIn = false;

  private String email;
  private String password;

  private PICLKeyServer0Client keyClient = new PICLKeyServer0Client(PICLAccountConstants.KEY_SERVER);
  private boolean isGetting = false;
  private Executor executor = Executors.newSingleThreadExecutor();


  private static final int STATE_EMAIL = 0;
  private static final int STATE_PASSWORD = 1;

  private int state;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.warn(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.picl_login);

    emailText = (EditText) findViewById(R.id.email);
    passwordText = (EditText) findViewById(R.id.password);
    hintView = (TextView) findViewById(R.id.hint);
    submitButton = (Button) findViewById(R.id.submit);
    webView = (WebView) findViewById(R.id.webview);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    webView.addJavascriptInterface(new PICLJavaScript(), PICL_JS_OBJECT);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        if (url.equals(PERSONA_URL)) {
          isPersonaLoaded = true;
          hintView.setText("Enter an e-mail address.");
          emailText.setVisibility(View.VISIBLE);
        }
      }
    });
    webView.loadUrl(PERSONA_URL);
  }

  public void onSubmit(View view) {
    Logger.warn(TAG, "submitButton.onClick()");

    if (!isPersonaLoaded) return;

    email = emailText.getText().toString();
    if (TextUtils.isEmpty(email)) return;

    password = passwordText.getText().toString();
    if (hasCheckedPersona && TextUtils.isEmpty(password)) return;

    if (isGetting) return;
    isGetting = true;


    if (!hasCheckedPersona) {
      hintView.setText("Checking Persona for account...");
      webView.loadUrl("javascript:" + jsAccountExists(email));
    } else if (!isLoggedIn) {
      hintView.setText(hasPersonaAccount ? "Logging in..." : "Creating account...");
      String js = (hasPersonaAccount ? jsSignIn(email, password) : jsCreateAccount(email, password));
      Logger.info(TAG, "JS: " + js);
      webView.loadUrl("javascript:" + js);
    }
  }

  private void getKey(final String assertion) {
    executor.execute(new Runnable() {

      @Override
      public void run() {
        keyClient.get(assertion, PICLAccountActivity.this);
      }

    });
  }

  @Override
  public void handleKey(final KeyResponse res) {
    runOnUiThread(new Runnable() {

      @Override
      public void run() {
        Logger.debug(TAG, "onKey(res)");
        isGetting = false;

        Account account = PICLAccountAuthenticator.createAccount(PICLAccountActivity.this, email, password, res.kA, res.deviceId, res.version);

        if (account != null) {
          ContentResolver.setSyncAutomatically(account, BrowserContract.TABS_AUTHORITY, true);

          Bundle result = new Bundle();
          result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
          result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
          setAccountAuthenticatorResult(result);
        }

        finish();
      }

    });
  }

  @Override
  public void handleError(final Exception e) {
    runOnUiThread(new Runnable() {

      @Override
      public void run() {
        isGetting = false;
        Toast.makeText(PICLAccountActivity.this, e.toString(), Toast.LENGTH_LONG).show();
      }

    });
  }

  private String jsAccountExists(String email) {
    return "BrowserID.internal.accountExists({email: '" + email + "'}, function(err, exists) { " + PICL_JS_OBJECT +".hasAccount(err, exists); });";
  }

  private String jsCreateAccount(String email, String password) {
    return "BrowserID.internal.createAccount({email: '" + email + "', password: '" + password +"', origin: '"+ PERSONA_ORIGIN + "', allowUnverified: true}, function(err, assertion) { "
          + PICL_JS_OBJECT +".onSignIn(err, assertion); });";
  }

  private String jsSignIn(String email, String password) {
    return "BrowserID.internal.signIn({email: '" + email + "', password: '" + password +"', origin: '"+ PERSONA_ORIGIN + "', allowUnverified: true}, function(err, assertion) { "
        + PICL_JS_OBJECT +".onSignIn(err, assertion); });";
  }

  private class PICLJavaScript {
    private boolean jsonState(String exists) {
      try {
        ExtendedJSONObject json = ExtendedJSONObject.parseJSONObject(exists);
        return !"unknown".equals(json.getString("state"));
      } catch (Exception e) {
        Logger.warn(TAG, "JSON Error: " + e);
      }
      return false;
    }

    @SuppressWarnings("unused")
    public void hasAccount(Object err, String exists) {
      Logger.info(TAG, "hasAccount: " + exists);
      final boolean hasAccount = jsonState(exists);
      runOnUiThread(new Runnable() {

        @Override
        public void run() {
          Logger.info(TAG, "Account exists: " + hasAccount);
          isGetting = false;
          hasCheckedPersona = true;

          if (hasAccount) {
            hintView.setText("Enter account password.");
          } else {
            hintView.setText("Create a password for this account.");
          }
          hasPersonaAccount = hasAccount;
          passwordText.setVisibility(View.VISIBLE);
        }

      });
    }

    @SuppressWarnings("unused")
    public void onSignIn(final Object err, final String assertion) {
      runOnUiThread(new Runnable() {

        @Override
        public void run() {
          if (err == null) {
            Logger.info(TAG, "assertion: " + assertion);

            hintView.setText("Logged in! Getting kA...");
            getKey(assertion);
          } else {
            isGetting = false;
            hintView.setText("Failed to login...");
            Logger.warn(TAG, "JS Error: " + err);
          }
        }
      });
    }
  }

}
