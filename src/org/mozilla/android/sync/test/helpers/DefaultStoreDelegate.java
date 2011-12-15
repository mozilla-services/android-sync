/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class DefaultStoreDelegate extends DefaultDelegate implements RepositorySessionStoreDelegate {
  
  @Override
  public void onStoreFailed(Exception ex) {
    sharedFail("Store failed");
  }

  @Override
  public void onStoreSucceeded(Record record) {
    sharedFail("DefaultStoreDelegate used");
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate() {
    final RepositorySessionStoreDelegate self = this;
    return new RepositorySessionStoreDelegate() {

      @Override
      public void onStoreSucceeded(final Record record) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onStoreSucceeded(record);
          }
        }).start();
      }

      @Override
      public void onStoreFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onStoreFailed(ex);
          }
        }).start();
      }

      @Override
      public RepositorySessionStoreDelegate deferredStoreDelegate() {
        return this;
      }
    };
  }   
}
