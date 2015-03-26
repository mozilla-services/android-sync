/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.sync;

import java.util.concurrent.Semaphore;

import org.mozilla.gecko.background.common.log.Logger;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * We have multiple SyncAdapter implementations, all of which drive the Firefox
 * Account state.  This is inherently racy.
 *
 * The Android way to handle the races is to have the FxAccountAuthenticator own
 * the Firefox Account and manage updating its state.  We'll work towards that;
 * for now, this avoids racing the brute force way.
 */
public abstract class AbstractSequentialSyncAdapter extends AbstractThreadedSyncAdapter {
  protected static final Semaphore sLock = new Semaphore(1, true /* fair */);

  protected AbstractSequentialSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(final Account account, final Bundle extras, final String authority, ContentProviderClient provider, final SyncResult syncResult) {
    // Serialize syncs from different subclasses.

    final String classTag = this.getClass().getSimpleName();
    // No Logger since we don't want the thread log tag to change.
    Log.i(Logger.DEFAULT_LOG_TAG, "Locking for " + classTag + "...");
    try {
      sLock.acquire();
    } catch (InterruptedException e) {
      // Skip this sync!  We might get this while waiting for a lock; roll with it.
      Log.i(Logger.DEFAULT_LOG_TAG, "Locking for " + classTag + "... INTERRUPTED!  Skipping sync request.");
      syncResult.moreRecordsToGet = true;
      return;
    }
    try {
      Log.i(Logger.DEFAULT_LOG_TAG, "Locking for " + classTag + "... LOCKED");
      performSync(account, extras, authority, provider, syncResult);
    } finally {
      Log.i(Logger.DEFAULT_LOG_TAG, "Locking for " + classTag + "... UNLOCKED");
      sLock.release();
    }
  }

  protected abstract void performSync(final Account account, final Bundle extras, final String authority, ContentProviderClient provider, final SyncResult syncResult);
}
