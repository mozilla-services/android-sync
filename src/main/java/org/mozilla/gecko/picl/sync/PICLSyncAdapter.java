/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozilla.gecko.background.common.log.Logger;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class PICLSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String LOG_TAG = PICLSyncAdapter.class.getSimpleName();

  protected final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PICLSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Logger.setThreadLogTag("PICLLogger");
    Logger.resetLogging();

    Logger.info(LOG_TAG, "Pickling account named " + account.name + " for authority " + authority);

    final PICLConfig config = configFromAccount(account);

    // For now, hard code syncing tabs. Next step will be to split the Fennec
    // content authority and update this code to correctly sync tabs and another
    // data type (I suggest form history -- it's flat and lightweight)
    // independently.
    final PICLTabsGlobalSession tabsGlobalSession = new PICLTabsGlobalSession(config);
    tabsGlobalSession.syncTabs();
  }

  /**
   * Extract a PICL sync configuration from an Android Account object.
   * <p>
   * This should get auth tokens, keys, server URLs, as appropriate. This is the
   * last time a PICL sync should see the Android Account object.
   *
   * @param account
   *          to extract from.
   * @return a <code>PICLConfig</code> instance.
   */
  protected PICLConfig configFromAccount(Account account) {
    return new PICLConfig(getContext(), executor, account.name, null);
  }
}
