/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient.RequestDelegate;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.activities.FxAccountSetupTask.FxAccountSignInTask;
import org.mozilla.gecko.fxa.activities.FxAccountSetupTask.FxAccountSignUpTask;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Activity which displays sign up/sign in screen to the user.
 */
public class FxAccountSetupActivity extends FragmentActivity {
  protected static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  protected View signUpView;
  protected View signInView;

  protected TextView remoteError;

  protected EditText signUpEmailEdit;
  protected EditText signUpPasswordEdit;
  protected EditText signInEmailEdit;
  protected EditText signInPasswordEdit;


  /**
   * Helper to find view or error if it is missing.
   *
   * @param id of view to find.
   * @param description to print in error.
   * @return non-null <code>View</code> instance.
   */
  public static View ensureFindViewById(View v, int id, String description) {
    View view = v.findViewById(id);
    if (view == null) {
      String message = "Could not find view " + description + ".";
      Logger.error(LOG_TAG, message);
      throw new RuntimeException(message);
    }
    return view;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(R.layout.fxaccount_setup);

    signUpView = findViewById(R.id.sign_up_view);
    signUpView.setVisibility(View.VISIBLE);

    signInView = findViewById(R.id.sign_in_view);
    signInView.setVisibility(View.GONE);

    signUpEmailEdit = (EditText) ensureFindViewById(signUpView, R.id.email, "email");
    signUpPasswordEdit = (EditText) ensureFindViewById(signUpView, R.id.password, "password");

    signInEmailEdit = (EditText) ensureFindViewById(signInView, R.id.email, "email");
    signInPasswordEdit = (EditText) ensureFindViewById(signInView, R.id.password, "password");

    ensureFindViewById(signUpView, R.id.sign_in_instead, "sign in instead button").setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        showSignIn();
      }
    });

    ensureFindViewById(signInView, R.id.sign_up_instead, "sign up instead button").setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        showSignUp();
      }
    });

    ensureFindViewById(signUpView, R.id.sign_up_button, "sign up button").setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String email = signUpEmailEdit.getText().toString();
        String password = signUpPasswordEdit.getText().toString();
        hideRemoteError();
        signUp(email, password);
      }
    });

    ensureFindViewById(signInView, R.id.sign_in_button, "sign in button").setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String email = signInEmailEdit.getText().toString();
        String password = signInPasswordEdit.getText().toString();
        hideRemoteError();
        signIn(email, password);
      }
    });

    remoteError = (TextView) findViewById(R.id.remote_error);
  }

  protected void redirectToStatusActivity() {
    Intent intent = new Intent(this, FxAccountStatusActivity.class);
    startActivity(intent);
    finish();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onResume() {
    super.onResume();
    if (FxAccountAuthenticator.getFirefoxAccounts(this).length > 0) {
      redirectToStatusActivity();
    }
  }

  public static void linkifyTextViews(View view, int[] textViews) {
    for (int id : textViews) {
      TextView textView = (TextView) view.findViewById(id);
      if (textView == null) {
        Logger.warn(LOG_TAG, "Could not process links for view with id " + id + ".");
        continue;
      }
      textView.setMovementMethod(LinkMovementMethod.getInstance());
      textView.setText(Html.fromHtml(textView.getText().toString()));
    }
  }

  // TODO: Need to persist currently shown view across resume.

  protected void showSignUp() {
    signUpView.setVisibility(View.VISIBLE);
    signInView.setVisibility(View.GONE);
  }

  protected void showSignIn() {
    signUpView.setVisibility(View.GONE);
    signInView.setVisibility(View.VISIBLE);
  }

  protected void showRemoteError(Exception e) {
    Logger.error(LOG_TAG, "Got exception.", e);
    remoteError.setText(e.toString());
    remoteError.setVisibility(View.VISIBLE);
  }

  protected void hideRemoteError() {
    remoteError.setVisibility(View.GONE);
  }

  class SignUpDelegate implements RequestDelegate<String> {
    public final String email;
    public final String password;
    public final String serverURI;

    public SignUpDelegate(String email, String password, String serverURI) {
      this.email = email;
      this.password = password;
      this.serverURI = serverURI;
    }

    @Override
    public void handleError(Exception e) {
      showRemoteError(e);
    }

    @Override
    public void handleFailure(int status, HttpResponse response) {
      handleError(new HTTPFailureException(new SyncStorageResponse(response)));
    }

    @Override
    public void handleSuccess(String result) {
      Activity activity = FxAccountSetupActivity.this;
      Logger.info(LOG_TAG, "Got success creating account.");

      // We're on the UI thread, but it's okay to create the account here.
      Account account;
      try {
        account = AndroidFxAccount.addAndroidAccount(activity, email, password,
            serverURI, null, null, false);
        if (account == null) {
          throw new RuntimeException("XXX what?");
        }
      } catch (Exception e) {
        handleError(e);
        return;
      }

      // For great debugging.
      ExtendedJSONObject o = new AndroidFxAccount(activity, account).toJSONObject();
      for (String key : o.keySet()) {
        System.out.println(key + ": " + o.getString(key));
      }

      Toast.makeText(getApplicationContext(), "Got success creating account.", Toast.LENGTH_LONG).show();
      redirectToStatusActivity();
    }
  }

  public void signUp(String email, String password) {
    String serverURI = FxAccountConstants.DEFAULT_IDP_ENDPOINT;
    RequestDelegate<String> delegate = new SignUpDelegate(email, password, serverURI);
    Executor executor = Executors.newSingleThreadExecutor();
    FxAccountClient20 client = new FxAccountClient20(serverURI, executor);
    try {
      new FxAccountSignUpTask(this, email, password, client, delegate).execute();
    } catch (UnsupportedEncodingException e) {
      showRemoteError(e);
    }
  }

  class SignInDelegate implements RequestDelegate<LoginResponse> {
    public final String email;
    public final String password;
    public final String serverURI;

    public SignInDelegate(String email, String password, String serverURI) {
      this.email = email;
      this.password = password;
      this.serverURI = serverURI;
    }

    @Override
    public void handleError(Exception e) {
      showRemoteError(e);
    }

    @Override
    public void handleFailure(int status, HttpResponse response) {
      handleError(new HTTPFailureException(new SyncStorageResponse(response)));
    }

    @Override
    public void handleSuccess(LoginResponse result) {
      Activity activity = FxAccountSetupActivity.this;
      Logger.info(LOG_TAG, "Got success signing in.");

      // We're on the UI thread, but it's okay to create the account here.
      Account account;
      try {
        account = AndroidFxAccount.addAndroidAccount(activity, email, password,
            serverURI, result.sessionToken, result.keyFetchToken, result.verified);
        if (account == null) {
          throw new RuntimeException("XXX what?");
        }
      } catch (Exception e) {
        handleError(e);
        return;
      }

      // For great debugging.
      ExtendedJSONObject o = new AndroidFxAccount(activity, account).toJSONObject();
      for (String key : o.keySet()) {
        System.out.println(key + ": " + o.getString(key));
      }

      Toast.makeText(getApplicationContext(), "Got success creating account.", Toast.LENGTH_LONG).show();
      redirectToStatusActivity();
    }
  }

  public void signIn(String email, String password) {
    String serverURI = FxAccountConstants.DEFAULT_IDP_ENDPOINT;
    RequestDelegate<LoginResponse> delegate = new SignInDelegate(email, password, serverURI);
    Executor executor = Executors.newSingleThreadExecutor();
    FxAccountClient20 client = new FxAccountClient20(serverURI, executor);
    try {
      new FxAccountSignInTask(this, email, password, client, delegate).execute();
    } catch (UnsupportedEncodingException e) {
      showRemoteError(e);
    }
  }
}
