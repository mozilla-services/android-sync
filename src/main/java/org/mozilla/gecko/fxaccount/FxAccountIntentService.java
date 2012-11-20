package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class FxAccountIntentService extends IntentService {
  public static final String LOG_TAG = FxAccountIntentService.class.getSimpleName();

  private static final String WORKER_THREAD_NAME = LOG_TAG + "Worker";

  public FxAccountIntentService() {
    super(WORKER_THREAD_NAME);
    Logger.debug(LOG_TAG, "Starting " + WORKER_THREAD_NAME + ".");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String action = intent.getAction();
    Logger.debug(LOG_TAG, "onHandleIntent: " + action + ".");

    boolean newAccount;

    if (FxAccountConstants.FXACCOUNT_CREATE_ANDROID_ACCOUNT_FOR_NEW_FX_ACCOUNT_ACTION.equals(action)) {
      newAccount = true;
    } else if (FxAccountConstants.FXACCOUNT_CREATE_ANDROID_ACCOUNT_FOR_EXISTING_FX_ACCOUNT_ACTION.equals(action)) {
      newAccount = false;
    } else {
      Logger.error(LOG_TAG, "Unrecognized action; aborting.");
      return;
    }

    Bundle extras = intent.getExtras();
    if (extras == null) {
      Logger.warn(LOG_TAG, "No extras; aborting.");
      return;
    }

    ResultReceiver receiver = extras.getParcelable(FxAccountConstants.PARAM_RECEIVER);
    if (receiver == null) {
      Logger.warn(LOG_TAG, "No receiver; aborting.");
      return;
    }

    String email = extras.getString(FxAccountConstants.PARAM_EMAIL);
    String password = extras.getString(FxAccountConstants.PARAM_PASSWORD);

    if (email == null || password == null) {
      Logger.warn(LOG_TAG, "No email or no password; aborting.");
      return;
    }

    Bundle data = new Bundle();

    try {
      Account account;
      if (newAccount) {
        account = FxAccountAuthenticator.createAndroidAccountForNewFxAccount(this, email, password);
      } else {
        account = FxAccountAuthenticator.createAndroidAccountForExistingFxAccount(this, email, password);
      }

      data.putParcelable(FxAccountConstants.PARAM_ACCOUNT, account);

      receiver.send(Activity.RESULT_OK, data);
    } catch (FxAccountCreationException e) {
      data.putString(FxAccountConstants.PARAM_ERROR, e.getMessage());

      receiver.send(Activity.RESULT_CANCELED, data);
    }
  }
}
