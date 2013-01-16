/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.sync.CommandProcessor;
import org.mozilla.gecko.sync.CommandProcessor.Command;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public abstract class SendCommandActivity extends SyncAccountActivity {
  public static final String LOG_TAG = SendCommandActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.debug(LOG_TAG, "onCreate");
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    Logger.debug(LOG_TAG, "onResume");
    super.onResume();

    if (this.localAccount == null) {
      Logger.warn(LOG_TAG, "No local Sync account.");
      return;
    }

    final Command command = getCommand();

    // Fetching local client GUID hits the DB, and we want to update the UI
    // afterward, so we perform the tab sending on another thread.
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        final String accountGUID = getAccountGUID();
        Logger.debug(LOG_TAG, "Retrieved local account GUID '" + accountGUID + "'.");
        if (accountGUID == null) {
          return false;
        }

        final CommandProcessor processor = CommandProcessor.getProcessor();
        processor.sendCommand(accountGUID, command, getApplicationContext());

        String[] stagesToSync = getStagesToSync();

        if (stagesToSync == null) {
          Logger.info(LOG_TAG, "Requesting immediate sync.");
          SyncAdapter.requestImmediateSync(localAccount, null);
        } else if (stagesToSync.length > 0) {
          Logger.info(LOG_TAG, "Requesting immediate sync of stages " + stagesToSync + ".");
          SyncAdapter.requestImmediateSync(localAccount, stagesToSync);
        }

        return true;
      }

      @Override
      protected void onPostExecute(final Boolean success) {
        // We're allowed to update the UI from here.
        notifyStatus(success.booleanValue(), command);

        finish();
      }
    }.execute();
  }

  protected abstract String[] getStagesToSync();

  protected abstract Command getCommand();

  /**
   * Notify the user of command send status.
   * <p>
   * "Success" is a bit of a misnomer: we wrote commands to the local command
   * database, and they will be sent on next sync. There is no way to verify
   * that the commands were successfully received by the intended client, so we
   * lie and say they were sent.
   *
   * @param success
   *          true if command was sent successfully; false otherwise.
   */
  protected void notifyStatus(final boolean success, Command command) {
    Toast toast;
    if (success) {
      toast = Toast.makeText(this, "Sent " + command.commandType + " command.", Toast.LENGTH_LONG);
    } else {
      toast = Toast.makeText(this, "Failed to send " + command.commandType + " command.", Toast.LENGTH_LONG);
    }

    toast.show();
  }
}
