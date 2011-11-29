/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchAllDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultFetchAllDelegate extends DefaultBaseFetchDelegate implements RepositorySessionFetchAllDelegate {

  public void onFetchAllFailed(Exception ex) {
    sharedFail("shouldn't fail");
  }

  public void onFetchAllSucceeded(Record[] records) {
    sharedFail("hit default fetch all delegate");
  }
}
