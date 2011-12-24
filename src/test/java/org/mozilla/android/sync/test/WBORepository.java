package org.mozilla.android.sync.test;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map.Entry;

import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;

public class WBORepository extends Repository {

  public class WBORepositorySession extends RepositorySession {

    public HashMap<String, Record> wbos;

    public WBORepositorySession(WBORepository repository) {
      super(repository);
      wbos = new HashMap<String, Record>();
    }

    @Override
    public void guidsSince(long timestamp,
                           RepositorySessionGuidsSinceDelegate delegate) {
      fail("TODO");
    }

    @Override
    public void fetchSince(long timestamp,
                           RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegin = System.currentTimeMillis();
      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        if (record.lastModified >= timestamp) {
          delegate.onFetchedRecord(record);
        }
      }
      delegate.onFetchCompleted(fetchBegin);
    }

    // TODO: replace by direct ThreadPool use.
    private abstract class ThreadRunnable implements Runnable {
      public void runOnThread() {
        ThreadPool.run(this);
      }
    }

    @Override
    public void fetch(final String[] guids,
                      final RepositorySessionFetchRecordsDelegate delegate) {
      new ThreadRunnable() {
        @Override
        public void run() {
          long fetchBegin = System.currentTimeMillis();
          for (String guid : guids) {
            if (wbos.containsKey(guid)) {
              delegate.onFetchedRecord(wbos.get(guid));
            }
          }
          delegate.onFetchCompleted(fetchBegin);
        }
      }.runOnThread();
    }

    @Override
    public void fetchAll(final RepositorySessionFetchRecordsDelegate delegate) {
      new ThreadRunnable() {
        @Override
        public void run() {
          long fetchBegin = System.currentTimeMillis();
          for (Entry<String, Record> entry : wbos.entrySet()) {
            Record record = entry.getValue();
            delegate.onFetchedRecord(record);
          }
          delegate.onFetchCompleted(fetchBegin);
        }
      }.runOnThread();
    }

    @Override
    public void store(final Record record) throws NoStoreDelegateException {
      if (delegate == null) {
        throw new NoStoreDelegateException();
      }
      wbos.put(record.guid, record);
      delegate.deferredStoreDelegate().onRecordStoreSucceeded(record);
    }

    @Override
    public void wipe(final RepositorySessionWipeDelegate delegate) {

      this.wbos = new HashMap<String, Record>();
      ((WBORepository) this.repository).wbos = new HashMap<String, Record>();
      delegate.deferredWipeDelegate().onWipeSucceeded();
    }

    @Override
    public void finish(RepositorySessionFinishDelegate delegate) {
      ((WBORepository) repository).wbos = this.wbos;
      delegate.deferredFinishDelegate().onFinishSucceeded(this, this.getBundle());
    }

    @Override
    public void begin(RepositorySessionBeginDelegate delegate) {
      this.wbos = ((WBORepository) repository).cloneWBOs();
      super.begin(delegate);
    }

    @Override
    public void storeDone() {
      // TODO: this is not guaranteed to be called after all of the record store callbacks have completed!
      new ThreadRunnable() {
        @Override
        public void run() {
          delegate.onStoreCompleted();
        }
      }.runOnThread();
    }
  }

  public HashMap<String, Record> wbos;

  public WBORepository() {
    super();
    wbos = new HashMap<String, Record>();
  }

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate,
                            Context context) {
    delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this));
  }

  public HashMap<String, Record> cloneWBOs() {
    HashMap<String, Record> out = new HashMap<String, Record>();
    for (Entry<String, Record> entry : wbos.entrySet()) {
      out.put(entry.getKey(), entry.getValue()); // Assume that records are
                                                 // immutable.
    }
    return out;
  }
}
