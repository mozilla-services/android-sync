/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import java.util.Map.Entry;

import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.FennecTabsRepository;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;

/**
 * A <code>PICLServerSyncStage</code> that syncs local tabs to a remote PICL
 * server.
 */
public class PICLTabsServerSyncStage extends PICLServerSyncStage {
  public final static String LOG_TAG = PICLTabsServerSyncStage.class.getSimpleName();

  public PICLTabsServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    super(config, delegate);
  }


  @Override
  protected Repository makeLocalRepository() {
    // For simplicity, hard code name and local client GUID. In future, this
    // should come from the configuration (backed by the Android Account, shared
    // preferences, or possibly Fennec itself).
    return new FennecTabsRepository("LOCAL CLIENT NAME", "LOCALCLIENTGUID");
  }

  @Override
  protected Repository makeRemoteRepository() {
    // This should be a PICL server repository that can store tab records.
    return new WBORepository();
  }

  @Override
  public void onSynchronized(Synchronizer synchronizer) {
    // Just for debugging, print a little information about what records were
    // sent to the remote repository. Remember to remove this when the remote is
    // an actual PICL server repository.
    WBORepository remote = (WBORepository) synchronizer.repositoryA;
    for (Entry<String, Record> entry : remote.wbos.entrySet()) {
      Logger.debug(LOG_TAG, entry.getKey() + " -> " + entry.getValue());
    }
    super.onSynchronized(synchronizer);
  }
}
