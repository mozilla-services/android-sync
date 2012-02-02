package org.mozilla.gecko.sync.setup.auth;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;

import android.util.Log;

public class AccountAuthenticator {
  private final String LOG_TAG = "AccountSetupAuthenticator";

  private AccountActivity activityCallback;
  private List<AuthenticatorStage> stages;

  private int stageIndex = -1; // Stages not started.

  // Values for authentication.
  public String password;
  public String usernameHash;

  public String authServer;
  public String nodeServer;

  public boolean isSuccess = false;
  public boolean isCanceled = false;

  public AccountAuthenticator(AccountActivity activity) {
    activityCallback = activity;
    prepareStages();
  }

  private void prepareStages() {
    stages = new ArrayList<AuthenticatorStage>();
    stages.add(new FetchUserNodeStage());
    stages.add(new AuthenticateAccountStage());
  }

  public void authenticate(String server, String username, String password) {
    // Set authentication values.
    if (!server.endsWith("/")) {
      server += "/";
    }
    nodeServer = server;
    this.password = password;

    // Calculate and save username hash.
    try {
      usernameHash = Utils.sha1Base32(username.toLowerCase()).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      abort("Username hash error.", e);
    } catch (UnsupportedEncodingException e) {
      abort("Username hash error.", e);
    }
    Log.d(LOG_TAG, "usernameHash:" + usernameHash);

    // Start first stage of authentication.
    runNextStage();
  }

  /**
   * Run next stage of authentication.
   */
  public void runNextStage() {
    if (isCanceled) {
      return;
    }
    if (++stageIndex == stages.size()) {
      activityCallback.authCallback(isSuccess);
      return;
    }
    try {
      stages.get(stageIndex).execute(this);
    } catch (Exception e) {
      Log.w(LOG_TAG, "Exception in stage " + stages.get(stageIndex));
      abort("Stage exception.", e);
    }
  }

  /**
   * Abort authentication.
   *
   * @param e
   *    Exception causing abort.
   * @param reason
   *    Reason for abort.
   */
  public void abort(String reason, Exception e) {
    if (isCanceled) {
      return;
    }
    Log.w(LOG_TAG, reason, e);
    activityCallback.authCallback(false);
  }

}
