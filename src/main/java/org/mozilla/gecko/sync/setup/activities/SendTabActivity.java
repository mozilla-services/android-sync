/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.CommandProcessor;
import org.mozilla.gecko.sync.CommandRunner;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class SendTabActivity extends Activity {
  public static final String LOG_TAG = "SendTabActivity";

  protected ClientRecordArrayAdapter deviceAdapter;
  protected Map<String, Account> accountMap;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.SyncTheme);
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    Logger.info(LOG_TAG, "Called SendTabActivity.onResume.");
    super.onResume();

    // We can't send tabs if there are no Sync accounts. Instead, we'll prompt
    // the user to set up Sync.
    redirectIfNoSyncAccount();
    // We never send commands that we don't know about, so we need to register the command here.
    registerDisplayURICommand();

    setContentView(R.layout.sync_send_tab);

    final ListView deviceListview = (ListView) findViewById(R.id.device_list);
    deviceListview.setItemsCanFocus(true);
    deviceListview.setTextFilterEnabled(true);
    deviceListview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    enableSend(false);

    final Spinner accountSpinner = (Spinner) findViewById(R.id.sendtab_account_spinner);

    // Fetching the client list hits the clients database, so we spin this onto
    // a background task.
    final Context context = this;
    new AsyncTask<Void, Void, ClientRecord[]>() {

      @Override
      protected ClientRecord[] doInBackground(Void... params) {
        return getClientArray();
      }

      @Override
      protected void onPostExecute(final ClientRecord[] clientArray) {
        // We're allowed to update the UI from here.
        deviceAdapter = new ClientRecordArrayAdapter(context, R.layout.sync_list_item, clientArray);
        deviceListview.setAdapter(deviceAdapter);
      }
    }.execute();

    // Fetching the list of Sync accounts hits a system database, so we
    // background this too.
    new AsyncTask<Void, Void, Account[]>() {

      @Override
      protected Account[] doInBackground(Void... params) {
        return SyncAccounts.syncAccounts(context);
      }

      @Override
      protected void onPostExecute(final Account[] syncAccounts) {
        // We're allowed to update the UI from here.
        accountMap = new HashMap<String, Account>();
        for (Account syncAccount : syncAccounts) {
          accountMap.put(syncAccount.name, syncAccount);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
            android.R.layout.simple_spinner_dropdown_item,
            new ArrayList<String>(accountMap.keySet()));
        accountSpinner.setAdapter(adapter);
      }
    }.execute();
  }

  /**
   * If no Sync accounts exist, suggest that the user set up Sync and then
   * finish the Send Tab activity.
   */
  protected void redirectIfNoSyncAccount() {
    if (SyncAccounts.syncAccountsExist(getApplicationContext())) {
      return;
    }

    Intent intent = new Intent(this, RedirectToSetupActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
  }

  /**
   * We never send commands we don't know about, so we need to make sure the
   * command processor knows the "displayURI" command at send time.
   */
  protected static void registerDisplayURICommand() {
    final CommandProcessor processor = CommandProcessor.getProcessor();
    processor.registerCommand("displayURI", new CommandRunner(3) {
      @Override
      public void executeCommand(final GlobalSession session, List<String> args) {
        CommandProcessor.displayURI(args, session.getContext());
      }
    });
  }

  /**
   * Get selected Account.
   *
   * @return selected Account.
   */
  protected Account getSelectedAccount() {
    final Spinner accountSpinner = (Spinner) findViewById(R.id.sendtab_account_spinner);
    final String selectedAccountName = (String) accountSpinner.getSelectedItem();
    return accountMap.get(selectedAccountName);
  }

  /**
   * Get account's client GUID.
   * <p>
   * This hits the shared prefs, so don't call from the main thread!
   *
   * @return client GUID, or null on error.
   */
  protected String getAccountGUID(final Account account) {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    final SyncAccountParameters params = SyncAccounts.blockingFromAndroidAccountV0(context, accountManager, account);
    if (params == null) {
      Logger.warn(LOG_TAG, "Could not get sync account parameters; aborting.");
      return null;
    }

    Logger.debug(LOG_TAG, "Fetching client GUID for account named " + params.username +
        " with server URL " + params.serverURL + ".");
    try {
      final String product = GlobalConstants.BROWSER_INTENT_PACKAGE;
      final String profile = Constants.DEFAULT_PROFILE;
      final long version = SyncConfiguration.CURRENT_PREFS_VERSION;
      final SharedPreferences prefs = Utils.getSharedPreferences(context, product, params.username, params.serverURL, profile, version);
      return prefs.getString(SyncConfiguration.PREF_ACCOUNT_GUID, null);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception fetching sync account parameters.", e);
      return null;
    }
  }

  /**
   * Get selected device client GUIDs.
   *
   * @return non-null list of GUIDs.
   */
  protected List<String> getSelectedDeviceGUIDs() {
    return deviceAdapter.getCheckedGUIDs();
  }

  public void sendClickHandler(View view) {
    Logger.info(LOG_TAG, "Send was clicked.");
    Bundle extras = this.getIntent().getExtras();
    final String uri = extras.getString(Intent.EXTRA_TEXT);
    final String title = extras.getString(Intent.EXTRA_SUBJECT);
    final CommandProcessor processor = CommandProcessor.getProcessor();

    // Fetching the account interacts with the UI only.
    final Account account = getSelectedAccount();
    if (account == null) {
      Logger.warn(LOG_TAG, "account is null; aborting without sending tab.");
      finish();
      return;
    }

    // Fetching the device GUIDs also interacts with the UI only.
    final List<String> deviceGuids = getSelectedDeviceGUIDs();
    if (deviceGuids == null) {
      Logger.warn(LOG_TAG, "device guids was null; aborting without sending tab.");
      finish();
      return;
    }

    // Fetching the sending client's GUID hits prefs, so we background this.
    new AsyncTask<Void, Void, String>() {

      @Override
      protected String doInBackground(Void... params) {
        return getAccountGUID(account);
      }

      @Override
      protected void onPostExecute(final String clientGuid) {
        // We're allowed to update the UI from here.
        if (clientGuid == null) {
          Logger.warn(LOG_TAG, "client guid is null; aborting without sending tab.");
          finish();
          return;
        }

        Logger.info(LOG_TAG, "Sending tab for Sync account named " + account.name +
            " with client GUID " + clientGuid + (deviceGuids.size() > 1 ?
                " to " + deviceGuids.size() + " clients." :
                " to 1 client."));

        for (String deviceGuid : deviceGuids) {
          processor.sendURIToClientForDisplay(uri, deviceGuid, title, clientGuid, getApplicationContext());
        }

        Logger.info(LOG_TAG, "Requesting immediate clients stage sync.");
        SyncAdapter.requestImmediateSync(account, new String[] { SyncClientsEngineStage.COLLECTION_NAME });

        notifyAndFinish();
      }
    }.execute();
  }

  /**
   * Notify the user that tabs were sent and then finish the activity.
   * <p>
   * This is a bit of a misnomer: we wrote "displayURI" commands to the local
   * command database, and they will be sent on next sync. There is no way to
   * verify that the commands were successfully received by the intended remote
   * client, so we lie and say they were sent.
   */
  protected void notifyAndFinish() {
    Toast.makeText(this, R.string.sync_text_tab_sent, Toast.LENGTH_LONG).show();
    finish();
  }

  protected void enableSend(boolean shouldEnable) {
    View sendButton = findViewById(R.id.send_button);
    sendButton.setEnabled(shouldEnable);
    sendButton.setClickable(shouldEnable);
  }

  /**
   * Fetch all known clients from the database.
   * <p>
   * This hits the database, so do not call from main thread!
   *
   * @return client records.
   */
  protected ClientRecord[] getClientArray() {
    final ClientsDatabaseAccessor db = new ClientsDatabaseAccessor(this.getApplicationContext());

    try {
      return db.fetchAllClients().values().toArray(new ClientRecord[0]);
    } catch (NullCursorException e) {
      Logger.warn(LOG_TAG, "NullCursorException while populating device list.", e);
      return null;
    } finally {
      db.close();
    }
  }
}
