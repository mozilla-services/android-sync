/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;

import android.content.Context;
import android.util.Log;

/**
 * This, sunshine, is where the magic happens.
 *
 * I hope for all our sakes that it's bug-free.
 *
 * @author rnewman
 *
 */
public class Synchronizer {

  /**
   * Wrap a SynchronizerDelegate in a SynchronizerSessionDelegate.
   * Also handle communication of bundled data.
   *
   * @author rnewman
   */
  public class SynchronizerDelegateSessionDelegate implements
      SynchronizerSessionDelegate {

    private static final String LOG_TAG = "SyncDelSDelegate";
    private SynchronizerDelegate synchronizerDelegate;
    private SynchronizerSession  session;

    public SynchronizerDelegateSessionDelegate(SynchronizerDelegate delegate) {
      this.synchronizerDelegate = delegate;
    }

    @Override
    public void onInitialized(SynchronizerSession session) {
      this.session = session;
      session.synchronize();
    }

    @Override
    public void onSynchronized(SynchronizerSession synchronizerSession) {
      Log.d(LOG_TAG, "Got onSynchronized.");
      Log.d(LOG_TAG, "Notifying SynchronizerDelegate.");
      this.synchronizerDelegate.onSynchronized(synchronizerSession.getSynchronizer());
    }

    @Override
    public void onSynchronizeSkipped(SynchronizerSession synchronizerSession) {
      Log.d(LOG_TAG, "Got onSynchronizeSkipped.");
      Log.d(LOG_TAG, "Notifying SynchronizerDelegate as if on success.");
      this.synchronizerDelegate.onSynchronized(synchronizerSession.getSynchronizer());
    }

    @Override
    public void onSynchronizeFailed(SynchronizerSession session,
                                    Exception lastException, String reason) {
      this.synchronizerDelegate.onSynchronizeFailed(session.getSynchronizer(), lastException, reason);
    }

    @Override
    public void onSynchronizeAborted(SynchronizerSession synchronizerSession) {
      this.synchronizerDelegate.onSynchronizeAborted(session.getSynchronizer());
    }

    @Override
    public void onFetchError(Exception e) {
      session.abort();
      synchronizerDelegate.onSynchronizeFailed(session.getSynchronizer(), e, "Got fetch error.");
    }

    @Override
    public void onStoreError(Exception e) {
      session.abort();
      synchronizerDelegate.onSynchronizeFailed(session.getSynchronizer(), e, "Got store error.");
    }

    @Override
    public void onSessionError(Exception e) {
      session.abort();
      synchronizerDelegate.onSynchronizeFailed(session.getSynchronizer(), e, "Got session error.");
    }
  }

  public Repository repositoryA;
  public Repository repositoryB;
  public RepositorySessionBundle bundleA;
  public RepositorySessionBundle bundleB;

  public void synchronize(Context context, SynchronizerDelegate delegate) {
    SynchronizerDelegateSessionDelegate sessionDelegate = new SynchronizerDelegateSessionDelegate(delegate);
    SynchronizerSession session = new SynchronizerSession(this, sessionDelegate);
    session.init(context, bundleA, bundleB);
  }

  public SynchronizerConfiguration save() {
    String syncID = null;      // TODO: syncID.
    return new SynchronizerConfiguration(syncID, bundleA, bundleB);
  }

  // Not thread-safe.
  public void load(SynchronizerConfiguration config) {
    bundleA = config.remoteBundle;
    bundleB = config.localBundle;
    // TODO: syncID.
  }
}
