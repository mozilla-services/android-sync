/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.apps;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter implementation for syncing Apps for a Mozilla Persona account.
 */
public class AppsSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String LOG_TAG = "AppsSyncAdapter";

  public AppsSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    Log.i(LOG_TAG, "Asked to sync " + account.name + " against authority " + authority +
        ". Extras bundle is " + extras + ".");
  }
}
