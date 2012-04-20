/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.GlobalSession;

public class MockServerSyncStage extends BaseMockServerSyncStage {
  @Override
  public void execute(GlobalSession session) {
    session.advance();
  }
}
