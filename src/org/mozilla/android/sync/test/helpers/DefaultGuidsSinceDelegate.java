/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;

public class DefaultGuidsSinceDelegate extends DefaultDelegate implements RepositorySessionGuidsSinceDelegate {

  @Override
  public void onGuidsSinceFailed(Exception ex) {
    sharedFail("shouldn't fail");
  }

  @Override
  public void onGuidsSinceSucceeded(String[] guids) {
    sharedFail("default guids since delegate called");
  }
}
