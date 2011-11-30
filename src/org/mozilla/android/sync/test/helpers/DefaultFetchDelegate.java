/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultFetchDelegate extends DefaultBaseFetchDelegate implements RepositorySessionFetchRecordsDelegate {

  public void onFetchFailed(Exception ex) {
    sharedFail("Shouldn't fail");
  }

  public void onFetchSucceeded(Record[] records) {
    sharedFail("Hit default delegate");
  }

}
