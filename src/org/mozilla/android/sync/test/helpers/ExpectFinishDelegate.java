package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionBundle;

public class ExpectFinishDelegate extends DefaultFinishDelegate {
  
  @Override
  public void onFinishSucceeded(RepositorySession session, RepositorySessionBundle bundle) {
    // no-op: finished successfully
  }
}
