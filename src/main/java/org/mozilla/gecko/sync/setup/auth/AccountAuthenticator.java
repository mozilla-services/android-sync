/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.auth;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;

public class AccountAuthenticator {
  private final String LOG_TAG = "AccountAuthenticator";

  private AccountActivity activityCallback;
  private Queue<AuthenticatorStage> stages;

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
    stages = new LinkedList<AuthenticatorStage>();
    stages.add(new EnsureUserExistenceStage());
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
    Logger.debug(LOG_TAG, "usernameHash:" + usernameHash);

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
    if (stages.size() == 0) {
      Logger.debug(LOG_TAG, "Authentication completed.");
      activityCallback.authCallback(isSuccess);
      return;
    }
    AuthenticatorStage nextStage = stages.remove();
    try {
      nextStage.execute(this);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Exception in stage " + nextStage);
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
    Logger.warn(LOG_TAG, reason, e);
    activityCallback.authCallback(false);
  }

  /* Helper functions */
  public static void runOnThread(Runnable run) {
    ThreadPool.run(run);
  }
}
