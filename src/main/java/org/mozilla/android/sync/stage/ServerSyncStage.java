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

package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.NoCollectionKeysSetException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.Server11Repository;
import org.mozilla.android.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.android.sync.synchronizer.Synchronizer;
import org.mozilla.android.sync.synchronizer.SynchronizerDelegate;

import android.util.Log;

/**
 * Fetch from a server collection into a local repository, encrypting
 * and decrypting along the way.
 *
 * @author rnewman
 *
 */
public abstract class ServerSyncStage implements
    GlobalSyncStage,
    SynchronizerDelegate {

  protected GlobalSession session;
  protected String LOG_TAG = "ServerSyncStage";

  /**
   * Override these in your subclasses.
   *
   * @return
   */
  protected boolean isEnabled() {
    return true;
  }
  protected abstract String getCollection();
  protected abstract Repository getLocalRepository();

  /**
   * Return a Crypto5Middleware-wrapped Server11Repository.
   *
   * @param clusterURI
   * @param data.username
   * @param collection
   * @return
   * @throws NoCollectionKeysSetException
   */
  protected Repository wrappedServerRepo() throws NoCollectionKeysSetException {
    String collection = this.getCollection();
    KeyBundle collectionKey = session.keyForCollection(collection);
    Server11Repository serverRepo = new Server11Repository(session.config.clusterURL.toASCIIString(),
                                                           session.config.username,
                                                           collection,
                                                           session);
    Crypto5MiddlewareRepository cryptoRepo = new Crypto5MiddlewareRepository(serverRepo, collectionKey);
    cryptoRepo.recordFactory = new BookmarkRecordFactory();
    return cryptoRepo;
  }

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    Log.d(LOG_TAG, "Starting execute.");

    this.session = session;
    if (!this.isEnabled()) {
      Log.i(LOG_TAG, "Stage disabled; skipping.");
      session.advance();
      return;
    }

    Repository remote;
    try {
      remote = wrappedServerRepo();
    } catch (NoCollectionKeysSetException e) {
      session.abort(e, "No CollectionKeys.");
      return;
    }
    Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = this.getLocalRepository();
    synchronizer.bundleA = null; // Fresh sync, effectively. TODO
    synchronizer.bundleA = null;
    Log.d(LOG_TAG, "Invoking synchronizer.");
    synchronizer.synchronize(session.getContext(), this);
    Log.d(LOG_TAG, "Reached end of execute.");
  }

  @Override
  public void onSynchronized(Synchronizer synchronizer) {
    Log.d(LOG_TAG, "onSynchronized.");
    try {
      session.advance();
    } catch (NoSuchStageException e) {
      // TODO
    }
  }

  @Override
  public void onSynchronizeFailed(Synchronizer synchronizer,
                                  Exception lastException, String reason) {
    Log.i(LOG_TAG, "onSynchronizeFailed: " + reason);
    session.abort(lastException, reason);
  }

  @Override
  public void onSynchronizeAborted(Synchronizer synchronize) {
    Log.i(LOG_TAG, "onSynchronizeAborted.");
    session.abort(null, "Synchronization was aborted.");
  }
}