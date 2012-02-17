/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCleanDelegate;

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
