/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class FetchMetaGlobalStage implements GlobalSyncStage {

  public class StageMetaGlobalDelegate implements MetaGlobalDelegate {

    private GlobalSession session;
    public StageMetaGlobalDelegate(GlobalSession session) {
      this.session = session;
    }

    @Override
    public void handleSuccess(MetaGlobal global, SyncStorageResponse response) {
      session.processMetaGlobal(global);
    }

    @Override
    public void handleFailure(SyncStorageResponse response) {
      session.handleHTTPError(response, "Failure fetching meta/global.");
    }

    @Override
    public void handleError(Exception e) {
      session.abort(e, "Failure fetching meta/global.");
    }

    @Override
    public void handleMissing(MetaGlobal global, SyncStorageResponse response) {
      session.processMissingMetaGlobal(global);
    }

    @Override
    public MetaGlobalDelegate deferred() {
      // TODO: defer!
      return this;
    }
  }

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    try {
      session.fetchMetaGlobal(new StageMetaGlobalDelegate(session));
    } catch (URISyntaxException e) {
      session.abort(e, "Invalid URI.");
    }
  }

}
