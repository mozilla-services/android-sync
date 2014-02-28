/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa.sync;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;

/**
 * Abstract away some details of Android's SyncStatusObserver.
 * <p>
 * Provides a simplified sync started/sync finished delegate.
 * <p>
 * We would prefer to register multiple observers, but it's of limited value
 * right now, so we support only a single observer, and we are as tolerant as
 * possible of non-paired add/remove calls.
 */
public class FxAccountSyncStatusHelper implements SyncStatusObserver {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = FxAccountSyncStatusHelper.class.getSimpleName();

  protected static FxAccountSyncStatusHelper sInstance = null;

  public synchronized static FxAccountSyncStatusHelper getInstance() {
    if (sInstance == null) {
      sInstance = new FxAccountSyncStatusHelper();
    }
    return sInstance;
  }

  public interface Delegate {
    public AndroidFxAccount getAccount();
    public void handleSyncStarted();
    public void handleSyncFinished();
  }

  // Used to unregister this as a listener.
  protected Object handle = null;

  // Maps delegates to whether their underlying Android account was syncing the
  // last time we observed a status change.
  protected Map<Delegate, Boolean> delegates = new IdentityHashMap<Delegate, Boolean>();

  @Override
  public synchronized void onStatusChanged(int which) {
    for (Entry<Delegate, Boolean> entry : delegates.entrySet()) {
      final Delegate delegate = entry.getKey();
      final AndroidFxAccount fxAccount = delegate.getAccount();
      if (fxAccount == null) {
        continue;
      }
      final Account account = fxAccount.getAndroidAccount();
      boolean active = false;
      for (String authority : fxAccount.getAndroidAuthorities()) {
        active |= ContentResolver.isSyncActive(account, authority);
      }
      // Remember for later.
      boolean wasActiveLastTime = entry.getValue();
      // It's okay to update the value of an entry while iterating the entrySet.
      entry.setValue(active);

      if (active && !wasActiveLastTime) {
        // We've started a sync.
        delegate.handleSyncStarted();
      }
      if (!active && wasActiveLastTime) {
        // We've finished a sync.
        delegate.handleSyncFinished();
      }
    }
  }

  protected void addListener() {
    final int mask = ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
    if (this.handle != null) {
      throw new IllegalStateException("Already registered this as an observer?");
    }
    this.handle = ContentResolver.addStatusChangeListener(mask, this);
  }

  protected void removeListener() {
    Object handle = this.handle;
    this.handle = null;
    if (handle != null) {
      ContentResolver.removeStatusChangeListener(handle);
    }
  }

  public synchronized void startObserving(Delegate delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate must not be null");
    }
    if (delegates.containsKey(delegate)) {
      return;
    }
    if (delegates.isEmpty()) {
      addListener();
    }
    delegates.put(delegate, Boolean.FALSE);
  }

  public synchronized void stopObserving(Delegate delegate) {
    delegates.remove(delegate);
    if (delegates.isEmpty()) {
      removeListener();
    }
  }
}
