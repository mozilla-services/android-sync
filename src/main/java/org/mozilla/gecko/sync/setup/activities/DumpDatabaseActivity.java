/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils.QueryHelper;

import android.app.ListActivity;
import android.content.ContentProviderClient;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.ArrayAdapter;

public class DumpDatabaseActivity extends ListActivity {
  public static final String LOG_TAG = DumpDatabaseActivity.class.getSimpleName();

  // protected Account localAccount;
  protected ArrayAdapter<String> adapter;

  protected final List<String> options = new ArrayList<String>();

  @Override
  public void onResume() {
    ActivityUtils.prepareLogging();
    super.onResume();
    /*
    AccountManager accountManager = AccountManager.get(getApplicationContext());
    Account[] accts = accountManager.getAccountsByType(SyncConstants.ACCOUNTTYPE_SYNC);

    // A Sync account exists.
    if (accts.length > 0) {
      localAccount = accts[0];
      clearAndFillList();
      return;
    }

    Intent intent = new Intent(this, RedirectToSetupActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
    */

    clearAndFillList();
  }

  protected void clearAndFillList() {
    options.clear();

    ContentProviderClient tabsProvider = getContentResolver().acquireContentProviderClient(BrowserContract.Tabs.CONTENT_URI);
    QueryHelper tabsHelper = new RepoUtils.QueryHelper(this, BrowserContract.Tabs.CONTENT_URI, LOG_TAG);
    try {
      Cursor cur = tabsHelper.safeQuery(tabsProvider, ".fetchSince()", null, null, null, null);
      cur.moveToFirst();

      while (!cur.isAfterLast()) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < cur.getColumnCount(); ++i) {
          sb.append(cur.getString(i));
          sb.append("|");
        }

        Logger.info(LOG_TAG, sb.toString());
        options.add(sb.toString());

        cur.moveToNext();
      }

    } catch (NullCursorException e) {
      Logger.warn(LOG_TAG, "Got exception.", e);
    } catch (RemoteException e) {
      Logger.warn(LOG_TAG, "Got exception.", e);
    }

    adapter.notifyDataSetChanged();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options);
    setListAdapter(adapter);
  }
}
