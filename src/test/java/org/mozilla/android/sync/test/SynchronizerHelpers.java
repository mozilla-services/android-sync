package org.mozilla.android.sync.test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.FetchFailedException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.StoreFailedException;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;

public class SynchronizerHelpers {
  public static final String FAIL_SENTINEL = "Fail";

  /**
   * Store one at a time, failing if the guid contains FAIL_SENTINEL.
   */
  public static class FailFetchWBORepository extends WBORepository {
    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this) {
        @Override
        public void fetchSince(long timestamp,
                               final RepositorySessionFetchRecordsDelegate delegate) {
          super.fetchSince(timestamp, new RepositorySessionFetchRecordsDelegate() {
            @Override
            public void onFetchedRecord(Record record) {
              if (record.guid.contains(FAIL_SENTINEL)) {
                delegate.onFetchFailed(new FetchFailedException(), record);
              } else {
                delegate.onFetchedRecord(record);
              }
            }

            @Override
            public void onFetchSucceeded(Record[] records, long fetchEnd) {
              delegate.onFetchSucceeded(records, fetchEnd);
            }

            @Override
            public void onFetchFailed(Exception ex, Record record) {
              delegate.onFetchFailed(ex, record);
            }

            @Override
            public void onFetchCompleted(long fetchEnd) {
              delegate.onFetchCompleted(fetchEnd);
            }

            @Override
            public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
              return this;
            }
          });
        }
      });
    }
  }

  /**
   * Store one at a time, failing if the guid contains FAIL_SENTINEL.
   */
  public static class SerialFailStoreWBORepository extends WBORepository {
    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this) {
        @Override
        public void store(final Record record) throws NoStoreDelegateException {
          if (delegate == null) {
            throw new NoStoreDelegateException();
          }
          if (record.guid.contains(FAIL_SENTINEL)) {
            delegate.onRecordStoreFailed(new StoreFailedException(), record.guid);
          } else {
            super.store(record);
          }
        }
      });
    }
  }

  /**
   * Store in batches, failing if any of the batch guids contains "Fail".
   * <p>
   * This will drop the final batch.
   */
  public static class BatchFailStoreWBORepository extends WBORepository {
    public final int batchSize;
    public ArrayList<Record> batch = new ArrayList<Record>();
    public boolean batchShouldFail = false;

    public class BatchFailStoreWBORepositorySession extends WBORepositorySession {
      public BatchFailStoreWBORepositorySession(WBORepository repository) {
        super(repository);
      }

      public void superStore(final Record record) throws NoStoreDelegateException {
        super.store(record);
      }

      @Override
      public void store(final Record record) throws NoStoreDelegateException {
        if (delegate == null) {
          throw new NoStoreDelegateException();
        }
        synchronized (batch) {
          batch.add(record);
          if (record.guid.contains("Fail")) {
            batchShouldFail = true;
          }

          if (batch.size() >= batchSize) {
            flush();
          }
        }
      }

      public void flush() {
        final ArrayList<Record> thisBatch = new ArrayList<Record>(batch);
        final boolean thisBatchShouldFail = batchShouldFail;
        batchShouldFail = false;
        batch.clear();
        storeWorkQueue.execute(new Runnable() {
          @Override
          public void run() {
            Logger.trace("XXX", "Notifying about batch.  Failure? " + thisBatchShouldFail);
            for (Record batchRecord : thisBatch) {
              if (thisBatchShouldFail) {
                delegate.onRecordStoreFailed(new StoreFailedException(), batchRecord.guid);
              } else {
                try {
                  superStore(batchRecord);
                } catch (NoStoreDelegateException e) {
                  delegate.onRecordStoreFailed(e, batchRecord.guid);
                }
              }
            }
          }
        });
      }

      @Override
      public void storeDone() {
        synchronized (batch) {
          flush();
          // Do this in a Runnable so that the timestamp is grabbed after any upload.
          final Runnable r = new Runnable() {
            @Override
            public void run() {
              synchronized (batch) {
                Logger.trace("XXX", "Calling storeDone.");
                storeDone(now());
              }
            }
          };
          storeWorkQueue.execute(r);
        }
      }
    }
    public BatchFailStoreWBORepository(int batchSize) {
      super();
      this.batchSize = batchSize;
    }

    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.deferredCreationDelegate().onSessionCreated(new BatchFailStoreWBORepositorySession(this));
    }
  }

  public static class TrackingWBORepository extends WBORepository {
    @Override
    public synchronized boolean shouldTrack() {
      return true;
    }

    public void store(final Record record) throws NoStoreDelegateException {
    }
  }
}
