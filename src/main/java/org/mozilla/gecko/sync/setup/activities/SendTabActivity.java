/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.CommandProcessor;
import org.mozilla.gecko.sync.CommandRunner;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SendTabActivity extends Activity {
  public static final String LOG_TAG = "SendTabActivity";
  private ClientRecordArrayAdapter arrayAdapter;
  private AccountManager accountManager;
  private Account localAccount;
  private SendTabData sendTabData;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    if (intent == null) {
      Logger.warn(LOG_TAG, "intent was null; aborting without sending tab.");
      notifyAndFinish(false);
      return;
    }

    Bundle extras = intent.getExtras();
    if (extras == null) {
      Logger.warn(LOG_TAG, "extras was null; aborting without sending tab.");
      notifyAndFinish(false);
      return;
    }

    sendTabData = SendTabData.fromBundle(extras);
    if (sendTabData == null) {
      Logger.warn(LOG_TAG, "send tab data was null; aborting without sending tab.");
      notifyAndFinish(false);
      return;
    }

    if (sendTabData.uri == null) {
      Logger.warn(LOG_TAG, "uri was null; aborting without sending tab.");
      notifyAndFinish(false);
      return;
    }

    if (sendTabData.title == null) {
      Logger.warn(LOG_TAG, "title was null; ignoring and sending tab anyway.");
    }
  }

  /**
   * Ensure that the view's list of clients is backed by a recently populated
   * array adapter. But only once, so we don't end up blowing away your selections
   * just because you got a text message.
   */
  protected synchronized void ensureClientList(final Context context,
                                               final ListView listview) {
    if (arrayAdapter != null) {
      Logger.debug(LOG_TAG, "Already have an array adapter for client lists.");
      listview.setAdapter(arrayAdapter);
      return;
    }

    arrayAdapter = new ClientRecordArrayAdapter(context, R.layout.sync_list_item);
    listview.setAdapter(arrayAdapter);

    // Fetching the client list hits the clients database, so we spin this onto
    // a background task.
    new AsyncTask<Void, Void, Collection<ClientRecord>>() {

      @Override
      protected Collection<ClientRecord> doInBackground(Void... params) {
        return getOtherClients();
      }

      @Override
      protected void onPostExecute(final Collection<ClientRecord> clientArray) {
        // We're allowed to update the UI from here.

        Logger.debug(LOG_TAG, "Got " + clientArray.size() + " clients.");
        arrayAdapter.setClientRecordList(clientArray);
        if (clientArray.size() == 1) {
          arrayAdapter.checkItem(0, true);
        }
      }
    }.execute();
  }

  @Override
  public void onResume() {
    ActivityUtils.prepareLogging();
    Logger.info(LOG_TAG, "Called SendTabActivity.onResume.");
    super.onResume();

    redirectIfNoSyncAccount();
    registerDisplayURICommand();

    setContentView(R.layout.sync_send_tab);
    final ListView listview = (ListView) findViewById(R.id.device_list);
    listview.setItemsCanFocus(true);
    listview.setTextFilterEnabled(true);
    listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    enableSend(false);

    ensureClientList(this, listview);

    TextView textView = (TextView) findViewById(R.id.title);
    textView.setText(sendTabData.title);

    textView = (TextView) findViewById(R.id.uri);
    textView.setText(sendTabData.uri);
  }

  private static void registerDisplayURICommand() {
    final CommandProcessor processor = CommandProcessor.getProcessor();
    processor.registerCommand("displayURI", new CommandRunner(3) {
      @Override
      public void executeCommand(final GlobalSession session, List<String> args) {
        CommandProcessor.displayURI(args, session.getContext());
      }
    });
  }

  private void redirectIfNoSyncAccount() {
    accountManager = AccountManager.get(getApplicationContext());
    Account[] accts = accountManager.getAccountsByType(SyncConstants.ACCOUNTTYPE_SYNC);

    // A Sync account exists.
    if (accts.length > 0) {
      localAccount = accts[0];
      return;
    }

    Intent intent = new Intent(this, RedirectToSetupActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
  }

  /**
   * @return Return null if there is no account set up. Return the account GUID otherwise.
   */
  private String getAccountGUID() {
    if (localAccount == null) {
      Logger.warn(LOG_TAG, "Null local account; aborting.");
      return null;
    }

    SharedPreferences prefs;
    try {
      prefs = SyncAccounts.blockingPrefsFromDefaultProfileV0(this, accountManager, localAccount);
      return prefs.getString(SyncConfiguration.PREF_ACCOUNT_GUID, null);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Could not get Sync account parameters or preferences; aborting.");
      return null;
    }
  }

  public void sendClickHandler(View view) {
    Logger.info(LOG_TAG, "Send was clicked.");
    final List<String> remoteClientGuids = arrayAdapter.getCheckedGUIDs();

    if (remoteClientGuids == null) {
      // Should never happen.
      Logger.warn(LOG_TAG, "guids was null; aborting without sending tab.");
      notifyAndFinish(false);
      return;
    }

    // Fetching local client GUID hits the DB, and we want to update the UI
    // afterward, so we perform the tab sending on another thread.
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        final CommandProcessor processor = CommandProcessor.getProcessor();

        final String accountGUID = getAccountGUID();
        Logger.debug(LOG_TAG, "Retrieved local account GUID '" + accountGUID + "'.");
        if (accountGUID == null) {
          return false;
        }

        for (String remoteClientGuid : remoteClientGuids) {
          processor.sendURIToClientForDisplay(sendTabData.uri, remoteClientGuid, sendTabData.title, accountGUID, getApplicationContext());
        }

        Logger.info(LOG_TAG, "Requesting immediate clients stage sync.");
        SyncAdapter.requestImmediateSync(localAccount, new String[] { SyncClientsEngineStage.COLLECTION_NAME });

        return true;
      }

      @Override
      protected void onPostExecute(final Boolean success) {
        // We're allowed to update the UI from here.
        notifyAndFinish(success.booleanValue());
      }
    }.execute();
  }

  /**
   * Notify the user about sent tabs status and then finish the activity.
   * <p>
   * "Success" is a bit of a misnomer: we wrote "displayURI" commands to the local
   * command database, and they will be sent on next sync. There is no way to
   * verify that the commands were successfully received by the intended remote
   * client, so we lie and say they were sent.
   *
   * @param success true if tab was sent successfully; false otherwise.
   */
  protected void notifyAndFinish(final boolean success) {
    int textId;
    if (success) {
      textId = R.string.sync_text_tab_sent;
    } else {
      textId = R.string.sync_text_tab_not_sent;
    }

    Toast.makeText(this, textId, Toast.LENGTH_LONG).show();
    finish();
  }

  public void enableSend(boolean shouldEnable) {
    View sendButton = findViewById(R.id.send_button);
    sendButton.setEnabled(shouldEnable);
    sendButton.setClickable(shouldEnable);
  }

  protected Map<String, ClientRecord> getClients() {
    ClientsDatabaseAccessor db = new ClientsDatabaseAccessor(this.getApplicationContext());
    try {
      return db.fetchAllClients();
    } catch (NullCursorException e) {
      Logger.warn(LOG_TAG, "NullCursorException while populating device list.", e);
      return null;
    } finally {
      db.close();
    }
  }

  /**
   * @return a collection of client records, excluding our own.
   */
  protected Collection<ClientRecord> getOtherClients() {
    final Map<String, ClientRecord> all = getClients();
    final ArrayList<ClientRecord> out = new ArrayList<ClientRecord>(all.size());
    final String ourGUID = getAccountGUID();
    for (Entry<String, ClientRecord> entry : all.entrySet()) {
      if (ourGUID.equals(entry.getKey())) {
        continue;
      }
      out.add(entry.getValue());
    }
    return out;
  }
}
