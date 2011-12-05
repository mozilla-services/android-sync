/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;

public class DefaultBeginDelegate extends DefaultDelegate implements RepositorySessionBeginDelegate {

  @Override
  public void onBeginFailed(Exception ex) {
    sharedFail("Shouldn't fail.");
  }

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    sharedFail("Default begin delegate hit.");
  }
}
