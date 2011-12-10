package org.mozilla.android.sync.test;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map.Entry;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

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
      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        if (record.lastModified >= timestamp) {
          delegate.onFetchedRecord(record);
        }
      }
      delegate.onFetchCompleted(syncBeginTimestamp);
    }

    @Override
    public void fetch(String[] guids,
                      RepositorySessionFetchRecordsDelegate delegate) {
      for (String guid : guids) {
        if (wbos.containsKey(guid)) {
          delegate.onFetchedRecord(wbos.get(guid));
        }
      }
      delegate.onFetchCompleted(syncBeginTimestamp);
    }

    @Override
    public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
      for (Entry<String, Record> entry : wbos.entrySet()) {
        Record record = entry.getValue();
        delegate.onFetchedRecord(record);
      }
      delegate.onFetchCompleted(syncBeginTimestamp);
    }

    @Override
    public void store(Record record, RepositorySessionStoreDelegate delegate) {
      wbos.put(record.guid, record);
      delegate.onStoreSucceeded(record);
    }

    @Override
    public void wipe(RepositorySessionWipeDelegate delegate) {
      this.wbos = new HashMap<String, Record>();
    }

    @Override
    public void finish(RepositorySessionFinishDelegate delegate) {
      ((WBORepository) repository).wbos = this.wbos;
      delegate.onFinishSucceeded(this, this.getBundle());
    }

    @Override
    public void begin(RepositorySessionBeginDelegate delegate) {
      this.wbos = ((WBORepository) repository).cloneWBOs();
      super.begin(delegate);
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
    delegate.onSessionCreated(new WBORepositorySession(this));
  }

  public HashMap<String, Record> cloneWBOs() {
    HashMap<String, Record> out = new HashMap<String, Record>();
    for (Entry<String, Record> entry : wbos.entrySet()) {
      out.put(entry.getKey(), entry.getValue());   // Assume that records are immutable.
    }
    return out;
  }
}
