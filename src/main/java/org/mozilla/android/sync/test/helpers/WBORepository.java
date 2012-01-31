package org.mozilla.android.sync.test.helpers;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.RecordFilter;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.StoreTrackingRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.util.Log;

public class WBORepository extends Repository {

  public static final String LOG_TAG = "WBORepository";

  public class WBORepositorySession extends StoreTrackingRepositorySession {

    private WBORepository wboRepository;
    private ExecutorService delegateExecutor = Executors.newSingleThreadExecutor();
    public ConcurrentHashMap<String, Record> wbos;

    public WBORepositorySession(WBORepository repository) {
      super(repository);
      wboRepository = repository;
      wbos = new ConcurrentHashMap<String, Record>();
    }

    @Override
    protected synchronized void trackRecord(Record record) {
      if (wboRepository.shouldTrack()) {
        super.trackRecord(record);
      }
    }

    @Override
    public void guidsSince(long timestamp,
                           RepositorySessionGuidsSinceDelegate delegate) {
      throw new RuntimeException("guidsSince not implemented.");
    }

    @Override
    public void fetchSince(long timestamp,
                           RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegin = System.currentTimeMillis();
      RecordFilter filter = storeTracker.getFilter();

      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        if (record.lastModified >= timestamp) {
          if (filter != null &&
              filter.excludeRecord(record)) {
            Log.d(LOG_TAG, "Excluding record " + record.guid);
            continue;
          }
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
      Record existing = wbos.get(record.guid);
      Log.d(LOG_TAG, "Existing record is " + (existing == null ? "<null>" : (existing.guid + ", " + existing)));
      if (existing != null &&
          existing.lastModified > record.lastModified) {
        Log.d(LOG_TAG, "Local record is newer. Not storing.");
        delegate.deferredStoreDelegate(delegateExecutor).onRecordStoreSucceeded(record);
        return;
      }
      if (existing != null) {
        Log.d(LOG_TAG, "Replacing local record.");
      }
      wbos.put(record.guid, record);
      trackRecord(record);
      delegate.deferredStoreDelegate(delegateExecutor).onRecordStoreSucceeded(record);
      return;
    }

    @Override
    public void wipe(final RepositorySessionWipeDelegate delegate) {
      this.wbos = new ConcurrentHashMap<String, Record>();
      ((WBORepository) this.repository).wbos = new ConcurrentHashMap<String, Record>();
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

  public ConcurrentHashMap<String, Record> wbos;

  public WBORepository() {
    super();
    wbos = new ConcurrentHashMap<String, Record>();
  }

  public synchronized boolean shouldTrack() {
    return false;
  }

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate,
                            Context context) {
    delegate.deferredCreationDelegate().onSessionCreated(new WBORepositorySession(this));
  }

  public ConcurrentHashMap<String, Record> cloneWBOs() {
    ConcurrentHashMap<String, Record> out = new ConcurrentHashMap<String, Record>();
    for (Entry<String, Record> entry : wbos.entrySet()) {
      out.put(entry.getKey(), entry.getValue()); // Assume that records are
                                                 // immutable.
    }
    return out;
  }
}
