/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchSinceDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultFetchSinceDelegate extends DefaultDelegate implements RepositorySessionFetchSinceDelegate {

  public void onFetchSinceFailed(Exception ex) {
    sharedFail("Shouldn't fail");
  }

  public void onFetchSinceSucceeded(Record[] records) {
    sharedFail("Default fetch since delegate hit");
  }

}
