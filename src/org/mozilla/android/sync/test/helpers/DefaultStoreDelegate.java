/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class DefaultStoreDelegate extends DefaultDelegate implements RepositorySessionStoreDelegate {
  
  public void onStoreFailed(Exception ex) {
    sharedFail("Store failed");
  }

  public void onStoreSucceeded(Record record) {
    sharedFail("DefaultStoreDelegate used");
  }   
}
