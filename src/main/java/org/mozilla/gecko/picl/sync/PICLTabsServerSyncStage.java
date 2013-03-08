/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync;

import org.mozilla.gecko.picl.sync.repositories.PICLServer0Repository;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.FennecTabsRepository;

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
    return new PICLServer0Repository(null, config.kA, null);
  }
}
