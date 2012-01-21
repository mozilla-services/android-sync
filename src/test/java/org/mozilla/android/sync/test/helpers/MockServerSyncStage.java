/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

public class MockServerSyncStage extends ServerSyncStage {
  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  protected String getCollection() {
    return null;
  }

  @Override
  protected Repository getLocalRepository() {
    return null;
  }

  @Override
  protected String getEngineName() {
    return null;
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return null;
  }

  @Override
  public void execute(GlobalSession session) {
    session.advance();
  }
}
