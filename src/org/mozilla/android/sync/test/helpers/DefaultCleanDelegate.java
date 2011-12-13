package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCleanDelegate;

public class DefaultCleanDelegate extends DefaultDelegate implements RepositorySessionCleanDelegate {
  
  @Override
  public void onCleaned(Repository repo) {
    sharedFail("Default begin delegate hit.");
  }

  @Override
  public void onCleanFailed(Repository repo, Exception ex) {
    sharedFail("Shouldn't fail.");
  }

}
