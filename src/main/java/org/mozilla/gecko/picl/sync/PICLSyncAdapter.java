/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class PICLSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String LOG_TAG = PICLSyncAdapter.class.getSimpleName();

  // private static final String TABS_CLIENT_GUID_IS = BrowserContract.Tabs.CLIENT_GUID + " = ?";

  public PICLSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Logger.info(LOG_TAG, "Syncing PICL account named " + account.name + " for authority " + authority);
    //syncTabs();
  }

  /*private void syncTabs() {
    ContentProviderClient tabsClient = getTabsClient();

    try {
      Cursor cursor = tabsClient.query(BrowserContract.Tabs.CONTENT_URI, null, TABS_CLIENT_GUID_IS, new String[0], null);
      if (cursor != null) {
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
          TabsRecord tabsRecord = FennecTabsRepository.tabsRecordFromCursor(cursor, clientGuid, clientName);


          cursor.moveToNext();
        }
      }
    } catch (RemoteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }*/

  @SuppressWarnings("unused")
  private ContentProviderClient getTabsClient() {
    ContentResolver cr = getContext().getApplicationContext().getContentResolver();
    return cr.acquireContentProviderClient(BrowserContract.Tabs.CONTENT_URI);
  }
}
