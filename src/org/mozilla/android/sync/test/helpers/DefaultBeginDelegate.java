/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;

public class DefaultBeginDelegate extends DefaultDelegate implements RepositorySessionBeginDelegate {

  public void onBeginFailed(Exception ex) {
    sharedFail("shouldn't fail");
  }

  public void onBeginSucceeded() {
    sharedFail("default begin delegate hit");
  }
}
