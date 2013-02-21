/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl;

import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class PICLSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String LOG_TAG = PICLSyncAdapter.class.getSimpleName();

  public PICLSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Logger.info(LOG_TAG, "Syncing PICL account named " + account.name + " for authority " + authority);
  }
}
