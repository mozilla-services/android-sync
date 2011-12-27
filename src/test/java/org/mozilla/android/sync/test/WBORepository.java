package org.mozilla.android.sync.test;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ExecutorService delegateExecutor = Executors.newSingleThreadExecutor();
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
          delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(record);
        }
      }
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchBegin);
    }

    @Override
    public void fetch(final String[] guids,
                      final RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegin = System.currentTimeMillis();
      for (String guid : guids) {
        if (wbos.containsKey(guid)) {
          delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(wbos.get(guid));
        }
      }
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchBegin);
    }

    @Override
    public void fetchAll(final RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegin = System.currentTimeMillis();
      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(record);
      }
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchBegin);
    }

    @Override
    public void store(final Record record) throws NoStoreDelegateException {
      if (delegate == null) {
        throw new NoStoreDelegateException();
      }
      wbos.put(record.guid, record);
      delegate.deferredStoreDelegate(delegateExecutor).onRecordStoreSucceeded(record);
    }

    @Override
    public void wipe(final RepositorySessionWipeDelegate delegate) {

      this.wbos = new HashMap<String, Record>();
      ((WBORepository) this.repository).wbos = new HashMap<String, Record>();
      delegate.deferredWipeDelegate(delegateExecutor).onWipeSucceeded();
    }

    @Override
    public void finish(RepositorySessionFinishDelegate delegate) {
      ((WBORepository) repository).wbos = this.wbos;
      delegate.deferredFinishDelegate(delegateExecutor).onFinishSucceeded(this, this.getBundle());
    }

    @Override
    public void begin(RepositorySessionBeginDelegate delegate) {
      this.wbos = ((WBORepository) repository).cloneWBOs();
      super.begin(delegate);
    }

    @Override
    public void storeDone() {
      // TODO: this is not guaranteed to be called after all of the record
      // store callbacks have completed!
      delegate.deferredStoreDelegate(delegateExecutor).onStoreCompleted();
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
