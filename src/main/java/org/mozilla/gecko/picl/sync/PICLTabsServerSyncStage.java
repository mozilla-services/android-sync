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
    return new PICLServer0Repository(config.serverURL, config.kA, "tabs");
  }

  @Override
  protected Repository makeRemoteRepository() {
    return new FennecTabsRepository(config.getClientName(), config.getClientGUID());
  }
}
