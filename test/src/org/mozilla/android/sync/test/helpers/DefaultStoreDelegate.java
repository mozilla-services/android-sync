/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ExecutorService;

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
  public void onStoreCompleted(long storeEnd) {
    sharedFail("DefaultStoreDelegate used");
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate(final ExecutorService executor) {
    final RepositorySessionStoreDelegate self = this;
    return new RepositorySessionStoreDelegate() {

      @Override
      public void onRecordStoreSucceeded(final Record record) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            self.onRecordStoreSucceeded(record);
          }
        });
      }

      @Override
      public void onRecordStoreFailed(final Exception ex) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            self.onRecordStoreFailed(ex);
          }
        });
      }

      @Override
      public void onStoreCompleted(final long storeEnd) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            self.onStoreCompleted(storeEnd);
          }
        });
      }

      @Override
      public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService newExecutor) {
        if (newExecutor == executor) {
          return this;
        }
        throw new IllegalArgumentException("Can't re-defer this delegate.");
      }
    };
  }
}
