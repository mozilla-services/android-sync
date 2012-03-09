/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;

import android.content.Context;
import android.util.Log;

/**
 * I perform a sync.
 *
 * Initialize me by calling `load` with a SynchronizerConfiguration.
 *
 * Start synchronizing by calling `synchronize` with a SynchronizerDelegate. I
 * provide coarse-grained feedback by calling my delegate's callback methods.
 *
 * I always call exactly one of my delegate's `onSynchronized` or
 * `onSynchronizeFailed` callback methods. In addition, I call
 * `onSynchronizeAborted` before `onSynchronizeFailed` when I encounter a fetch,
 * store, or session error while synchronizing.
 *
 * After synchronizing, call `save` to get back a SynchronizerConfiguration with
 * updated bundle information.
 */
public class Synchronizer {

  /**
   * I translate the fine-grained feedback of a SynchronizerSessionDelegate into
   * the coarse-grained feedback of a SynchronizerDelegate.
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

  /**
   * Start synchronizing, calling delegate's callback methods.
   */
  public void synchronize(Context context, SynchronizerDelegate delegate) {
    SynchronizerDelegateSessionDelegate sessionDelegate = new SynchronizerDelegateSessionDelegate(delegate);
    SynchronizerSession session = new SynchronizerSession(this, sessionDelegate);
    session.init(context, bundleA, bundleB);
  }

  public SynchronizerConfiguration save() {
    String syncID = null;      // TODO: syncID.
    return new SynchronizerConfiguration(syncID, bundleA, bundleB);
  }

  /**
   * Set my repository session bundles from a SynchronizerConfiguration.
   *
   * This method is not thread-safe.
   *
   * @param config
   */
  public void load(SynchronizerConfiguration config) {
    bundleA = config.remoteBundle;
    bundleB = config.localBundle;
    // TODO: syncID.
  }
}
