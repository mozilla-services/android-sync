/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class DefaultStoreDelegate extends DefaultDelegate implements RepositorySessionStoreDelegate {
  
  @Override
  public void onRecordStoreFailed(Exception ex) {
    sharedFail("Store failed");
  }

  @Override
  public void onRecordStoreSucceeded(Record record) {
    sharedFail("DefaultStoreDelegate used");
  }

  @Override
  public void onStoreCompleted() {
    sharedFail("DefaultStoreDelegate used");
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate() {
    final RepositorySessionStoreDelegate self = this;
    return new RepositorySessionStoreDelegate() {

      @Override
      public void onRecordStoreSucceeded(final Record record) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onRecordStoreSucceeded(record);
          }
        }).start();
      }

      @Override
      public void onRecordStoreFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onRecordStoreFailed(ex);
          }
        }).start();
      }

      @Override
      public RepositorySessionStoreDelegate deferredStoreDelegate() {
        return this;
      }

      @Override
      public void onStoreCompleted() {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onStoreCompleted();
          }
        }).start();
      }
    };
  }
}
