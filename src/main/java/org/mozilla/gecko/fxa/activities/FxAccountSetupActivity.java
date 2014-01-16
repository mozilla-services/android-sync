/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.activities;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.mozilla.gecko.R;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.FxAccountClient10.RequestDelegate;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.activities.FxAccountSetupTask.FxAccountSignInTask;
import org.mozilla.gecko.fxa.activities.FxAccountSetupTask.FxAccountSignUpTask;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.fxa.authenticator.FxAccountAuthenticator;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Activity which displays sign up/sign in screen to the user.
 */
public class FxAccountSetupActivity extends Activity {
  protected static final String LOG_TAG = FxAccountSetupActivity.class.getSimpleName();

  protected static final int INDEX_OF_GET_STARTED = 0;
  protected static final int INDEX_OF_CREATE_ACCOUNT = 1;
  protected static final int INDEX_OF_SIGN_IN = 2;
  protected static final int INDEX_OF_CREATE_ACCOUNT_SUCCESS = 3;
  protected static final int INDEX_OF_SIGN_IN_SUCCESS = 4;

  protected View signUpView;
  protected View signInView;

  protected TextView remoteError;

  protected EditText signUpEmailEdit;
  protected EditText signUpPasswordEdit;
  protected EditText signInEmailEdit;
  protected EditText signInPasswordEdit;

  protected ViewFlipper viewFlipper;

  /**
   * Helper to find view or error if it is missing.
   *
   * @param id of view to find.
   * @param description to print in error.
   * @return non-null <code>View</code> instance.
   */
  public View ensureFindViewById(View v, int id, String description) {
    View view;
    if (v != null) {
      view = v.findViewById(id);
    } else {
      view = findViewById(id);
    }
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

    linkifyTextViews(null, new int[] { R.id.old_firefox, R.id.policy });

    viewFlipper = (ViewFlipper) ensureFindViewById(null, R.id.viewflipper, "view flipper");

    createViewOnClickListeners();
    createYearSpinner();

    if (icicle != null) {
      viewFlipper.setDisplayedChild(icicle.getInt("viewflipper", INDEX_OF_GET_STARTED));
    }

    View createAccountView = findViewById(R.id.create_account_view);
    ensureFindViewById(createAccountView, R.id.create_account_button, "sign up button").setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(FxAccountSetupActivity.this, FxAccountCreateSuccessActivity.class);
        intent.putExtra("email", "test@test.com");
        startActivity(intent);
        finish();
      }
    });

    //
    //    signUpView = findViewById(R.id.sign_up_view);
    //    signUpView.setVisibility(View.VISIBLE);
    //
    //    Spinner yearSpinner = (Spinner) ensureFindViewById(signUpView, R.id.year_spinner, "year spinner");
    //    // Create an ArrayAdapter using the string array and a default spinner layout
    //    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
    //    adapter.add("2014");
    //    adapter.add("2013");
    //    adapter.add("2012");
    //    adapter.add("2011");
    //    // Specify the layout to use when the list of choices appears
    //    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    //    // Apply the adapter to the spinner
    //    yearSpinner.setAdapter(adapter);
    //    // yearSpinner.set
    //
    //    signInView = findViewById(R.id.sign_in_view);
    //    signInView.setVisibility(View.GONE);
    //
    //    signUpEmailEdit = (EditText) ensureFindViewById(signUpView, R.id.email, "email");
    //    signUpPasswordEdit = (EditText) ensureFindViewById(signUpView, R.id.password, "password");
    //
    //    signInEmailEdit = (EditText) ensureFindViewById(signInView, R.id.email, "email");
    //    signInPasswordEdit = (EditText) ensureFindViewById(signInView, R.id.password, "password");
    //
    //    ensureFindViewById(signUpView, R.id.sign_in_instead, "sign in instead button").setOnClickListener(new OnClickListener() {
    //      @Override
    //      public void onClick(View arg0) {
    //        showSignIn();
    //      }
    //    });
    //
    //    ensureFindViewById(signInView, R.id.sign_up_instead, "sign up instead button").setOnClickListener(new OnClickListener() {
    //      @Override
    //      public void onClick(View arg0) {
    //        showSignUp();
    //      }
    //    });
    //
    //    ensureFindViewById(signUpView, R.id.sign_up_button, "sign up button").setOnClickListener(new OnClickListener() {
    //      @Override
    //      public void onClick(View v) {
    //        String email = signUpEmailEdit.getText().toString();
    //        String password = signUpPasswordEdit.getText().toString();
    //        hideRemoteError();
    //        signUp(email, password);
    //      }
    //    });
    //
    //    ensureFindViewById(signInView, R.id.sign_in_button, "sign in button").setOnClickListener(new OnClickListener() {
    //      @Override
    //      public void onClick(View v) {
    //        String email = signInEmailEdit.getText().toString();
    //        String password = signInPasswordEdit.getText().toString();
    //        hideRemoteError();
    //        signIn(email, password);
    //      }
    //    });
    //
    //    remoteError = (TextView) findViewById(R.id.remote_error);
  }

  protected void createViewOnClickListeners() {
    OnClickListener showCreateAccount = new OnClickListener() {
      @Override
      public void onClick(View v) {
        viewFlipper.setDisplayedChild(INDEX_OF_CREATE_ACCOUNT);
      }
    };

    OnClickListener showSignIn = new OnClickListener() {
      @Override
      public void onClick(View v) {
        viewFlipper.setDisplayedChild(INDEX_OF_SIGN_IN);
      }
    };

    final View introView = ensureFindViewById(viewFlipper, R.id.intro_view, "intro view");
    ensureFindViewById(introView, R.id.get_started_button, "get started button").setOnClickListener(showCreateAccount);

    final View createAccountView = ensureFindViewById(viewFlipper, R.id.create_account_view, "create account view");
    ensureFindViewById(createAccountView, R.id.sign_in_instead_link, "sign in instead link").setOnClickListener(showSignIn);

    final View signInView = ensureFindViewById(viewFlipper, R.id.sign_in_view, "sign in view");
    ensureFindViewById(signInView, R.id.create_account, "create account instead link").setOnClickListener(showCreateAccount);
  }

  @Override
  public void onBackPressed() {
    if (viewFlipper.getDisplayedChild() < 1) {
      super.onBackPressed();
      return;
    }
    viewFlipper.showPrevious();
  }

  protected void createYearSpinner() {
    final View createAccountView = ensureFindViewById(viewFlipper, R.id.create_account_view, "create account view");
    final EditText yearSpinner = (EditText) ensureFindViewById(createAccountView, R.id.year_edit, "year of birth button");
    yearSpinner.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        final String[] years = new String[20];
        for (int i = 0; i < years.length; i++) {
          years[i] = Integer.toString(2014 - i);
        }

        android.content.DialogInterface.OnClickListener listener = new Dialog.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            yearSpinner.setText(years[which]);
          }
        };

        AlertDialog dialog = new AlertDialog.Builder(FxAccountSetupActivity.this)
        .setTitle(R.string.fxaccount_when_were_you_born)
        .setItems(years, listener)
        .setIcon(R.drawable.fxaccount_icon)
        .create();

        dialog.show();
      }
    });
  }

  @Override
  public void onSaveInstanceState(Bundle bundle) {
    bundle.putInt("viewflipper", viewFlipper.getDisplayedChild());
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

  public void linkifyTextViews(View view, int[] textViews) {
    for (int id : textViews) {
      TextView textView;
      if (view != null) {
        textView = (TextView) view.findViewById(id);
      } else {
        textView = (TextView) findViewById(id);
      }
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
      if (FxAccountConstants.LOG_PERSONAL_INFORMATION) {
        new AndroidFxAccount(activity, account).dump();
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
      if (FxAccountConstants.LOG_PERSONAL_INFORMATION) {
        new AndroidFxAccount(activity, account).dump();
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
