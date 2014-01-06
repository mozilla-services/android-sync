/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient.RequestDelegate;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Comment about callbacks and UX discussion needed here.
 */
class FxAccountLoginTask extends AsyncTask<Void, Void, Void> {
  protected static final String LOG_TAG = FxAccountLoginTask.class.getSimpleName();

  protected final Context context;
  protected final boolean createAccount;
  protected final String email;
  protected final byte[] emailUTF8;
  protected final String password;
  protected final byte[] passwordUTF8;
  protected final FxAccountClient20 client;

  protected ProgressDialog progressDialog = null;

  public FxAccountLoginTask(Context context, boolean createAccount, String email, String password, FxAccountClient20 client) throws UnsupportedEncodingException {
    this.context = context;
    this.createAccount = createAccount;
    this.email = email;
    this.emailUTF8 = email.getBytes("UTF-8");
    this.password = password;
    this.passwordUTF8 = password.getBytes("UTF-8");
    this.client = client;
  }

  @Override
  protected void onPreExecute() {
    progressDialog = new ProgressDialog(context);
    progressDialog.setTitle("Logging in to Firefox Account...");
    progressDialog.setMessage("Please wait.");
    progressDialog.setCancelable(false);
    progressDialog.setIndeterminate(true);
    progressDialog.show();
  }

  protected class CreateAccountRequestDelegate implements RequestDelegate<String> {
    protected final CountDownLatch latch;
    protected final LoginReqestDelegate loginRequestDelegate;

    protected CreateAccountRequestDelegate(CountDownLatch latch, LoginReqestDelegate loginReqestDelegate) {
      this.latch = latch;
      this.loginRequestDelegate = loginReqestDelegate;
    }

    @Override
    public void handleError(Exception e) {
      Logger.error(LOG_TAG, "Got exception creating account.", e);
    }

    @Override
    public void handleFailure(int status, HttpResponse response) {
      Logger.warn(LOG_TAG, "Got failure creating account.", new HTTPFailureException(new SyncStorageResponse(response)));
    }

    @Override
    public void handleSuccess(String result) {
      Logger.info(LOG_TAG, "Got success creating account.");
      client.loginAndGetKeys(emailUTF8, passwordUTF8, loginRequestDelegate);
    }
  }

  protected class LoginReqestDelegate implements RequestDelegate<LoginResponse> {
    private final CountDownLatch latch;

    protected LoginReqestDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void handleError(Exception e) {
      Logger.error(LOG_TAG, "Got exception logging in.", e);
      latch.countDown();
    }

    @Override
    public void handleFailure(int status, HttpResponse response) {
      Logger.warn(LOG_TAG, "Got failure logging in.", new HTTPFailureException(new SyncStorageResponse(response)));
      latch.countDown();
    }

    @Override
    public void handleSuccess(LoginResponse result) {
      Logger.info(LOG_TAG, "Got success logging in: " + result.uid);

      // We're on a background thread.  Let's create the account here.
      try {
        Account account = AndroidFxAccount.addAndroidAccount(context, email, password,
            result.serverURI, result.sessionToken, result.keyFetchToken, result.verified);
        if (account == null) {
          // XXX throw?
          Logger.warn(LOG_TAG, "Failed to add account.");
          return;
        }
        ExtendedJSONObject o = new AndroidFxAccount(context, account).toJSONObject();
        for (String key : o.keySet()) {
          System.out.println(key + ": " + o.getString(key));
        }
      } catch (Exception e) {
        Logger.warn(LOG_TAG, "Failed to add account.", e);
      } finally {
        latch.countDown();
      }
    }
  }

  @Override
  protected Void doInBackground(Void... arg0) {
    final CountDownLatch latch = new CountDownLatch(1);
    LoginReqestDelegate loginRequestDelegate = new LoginReqestDelegate(latch);
    CreateAccountRequestDelegate createAccountRequestDelegate = new CreateAccountRequestDelegate(latch, loginRequestDelegate);

    if (createAccount) {
      client.createAccount(emailUTF8, passwordUTF8, false, createAccountRequestDelegate);
    } else {
      client.loginAndGetKeys(emailUTF8, passwordUTF8, loginRequestDelegate);
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.error(LOG_TAG, "Got exception logging in.", e);
    }

    return null;
  }

  @Override
  protected void onPostExecute(Void result) {
    if (progressDialog != null) {
      progressDialog.dismiss();
    }
  }
}
