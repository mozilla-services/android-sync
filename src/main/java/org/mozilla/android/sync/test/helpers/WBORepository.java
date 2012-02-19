package org.mozilla.android.sync.test.helpers;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozilla.gecko.sync.Logger;
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

public class WBORepository extends Repository {

  public class WBORepositoryStats {
    public long created         = -1;
    public long begun           = -1;
    public long fetchBegan      = -1;
    public long fetchCompleted  = -1;
    public long storeBegan      = -1;
    public long storeCompleted  = -1;
    public long finished        = -1;
  }

  public static final String LOG_TAG = "WBORepository";

  // Access to stats is not guarded.
  public WBORepositoryStats stats;

  public class WBORepositorySession extends StoreTrackingRepositorySession {

    private WBORepository wboRepository;
    private ExecutorService delegateExecutor = Executors.newSingleThreadExecutor();
    public ConcurrentHashMap<String, Record> wbos;

    public WBORepositorySession(WBORepository repository) {
      super(repository);
      wboRepository = repository;
      wbos          = new ConcurrentHashMap<String, Record>();
      stats         = new WBORepositoryStats();
      stats.created = now();
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
      long fetchBegan  = now();
      stats.fetchBegan = fetchBegan;
      RecordFilter filter = storeTracker.getFilter();

      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        if (record.lastModified >= timestamp) {
          if (filter != null &&
              filter.excludeRecord(record)) {
            Logger.debug(LOG_TAG, "Excluding record " + record.guid);
            continue;
          }
          delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(record);
        }
      }
      long fetchCompleted  = now();
      stats.fetchCompleted = fetchCompleted;
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchCompleted);
    }

    @Override
    public void fetch(final String[] guids,
                      final RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegan  = now();
      stats.fetchBegan = fetchBegan;
      for (String guid : guids) {
        if (wbos.containsKey(guid)) {
          delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(wbos.get(guid));
        }
      }
      long fetchCompleted  = now();
      stats.fetchCompleted = fetchCompleted;
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchCompleted);
    }

    @Override
    public void fetchAll(final RepositorySessionFetchRecordsDelegate delegate) {
      long fetchBegan  = now();
      stats.fetchBegan = fetchBegan;
      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        delegate.deferredFetchDelegate(delegateExecutor).onFetchedRecord(record);
      }
      long fetchCompleted  = now();
      stats.fetchCompleted = fetchCompleted;
      delegate.deferredFetchDelegate(delegateExecutor).onFetchCompleted(fetchCompleted);
    }

    @Override
    public void store(final Record record) throws NoStoreDelegateException {
      if (delegate == null) {
        throw new NoStoreDelegateException();
      }
      if (stats.storeBegan < 0) {
        stats.storeBegan = now();
      }
      Record existing = wbos.get(record.guid);
      Logger.debug(LOG_TAG, "Existing record is " + (existing == null ? "<null>" : (existing.guid + ", " + existing)));
      if (existing != null &&
          existing.lastModified > record.lastModified) {
        Logger.debug(LOG_TAG, "Local record is newer. Not storing.");
        delegate.deferredStoreDelegate(delegateExecutor).onRecordStoreSucceeded(record);
        return;
      }
      if (existing != null) {
        Logger.debug(LOG_TAG, "Replacing local record.");
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
      stats.finished = now();
      delegate.deferredFinishDelegate(delegateExecutor).onFinishSucceeded(this, this.getBundle());
    }

    @Override
    public void begin(RepositorySessionBeginDelegate delegate) {
      this.wbos = ((WBORepository) repository).cloneWBOs();
      stats.begun = now();
      super.begin(delegate);
    }

    @Override
    public void storeDone(long end) {
      // TODO: this is not guaranteed to be called after all of the record
      // store callbacks have completed!
      if (stats.storeBegan < 0) {
        stats.storeBegan = end;
      }
      stats.storeCompleted = end;
      delegate.deferredStoreDelegate(delegateExecutor).onStoreCompleted(end);
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
