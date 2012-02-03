/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.test.AndroidTestCase;

public class Crypto5MiddlewareRepositorySessionTest extends AndroidTestCase {

  public class MockRecord extends Record {
    private int value;
    public MockRecord(String guid, String collection, long lastModified,
                      boolean deleted, int value) {
      super(guid, collection, lastModified, deleted);
      this.value = value;
    }

    @Override
    public void initFromPayload(CryptoRecord payload) {
      this.value = ((Long)payload.payload.get("myValue")).intValue();
    }

    @Override
    public CryptoRecord getPayload() {
      ExtendedJSONObject payload = new ExtendedJSONObject();
      payload.put("myValue", this.value);
      payload.put("id",      this.guid);
      return new CryptoRecord(payload);
    }

    public boolean eq(Object in) {
      if (in == null ||
          !(in instanceof MockRecord)) {
        return false;
      }
      MockRecord rec = (MockRecord) in;
      return rec.guid.equals(this.guid) &&
             rec.collection.equals(this.collection) &&
             rec.lastModified == this.lastModified &&
             rec.deleted == this.deleted &&
             rec.value == this.value;
    }

    @Override
    public String toJSONString() {
      throw new RuntimeException("Can't JSONify MockRecord.");
    }

    @Override
    public Record copyWithIDs(String guid, long androidID) {
      MockRecord out = new MockRecord(guid, this.collection, this.lastModified, this.deleted, this.value);
      out.androidID = androidID;
      out.sortIndex = this.sortIndex;
      return out;
    }
  }

  public class CryptoTestRepository extends Repository {
    HashMap<String, CryptoRecord> wbos = new HashMap<String, CryptoRecord>();

    public void put(String key, CryptoRecord record) {
      wbos.put(key, record);
    }

    @Override
    public void createSession(RepositorySessionCreationDelegate delegate,
                              Context context) {
      delegate.onSessionCreated(new CryptoTestRepositorySession(this, this.wbos));
    }
  }

  public class CryptoTestRepositorySession extends RepositorySession {
    HashMap<String, CryptoRecord> wbos = new HashMap<String, CryptoRecord>();

    public CryptoTestRepositorySession(Repository repository, HashMap<String, CryptoRecord> wbos) {
      super(repository);
      this.wbos = wbos;
    }

    @SuppressWarnings("unused")
    private HashMap<String, CryptoRecord> recordsSince(long timestamp) {
      HashMap<String, CryptoRecord> result = new HashMap<String, CryptoRecord>();
      for (Entry<String, CryptoRecord> entry : this.wbos.entrySet()) {
        if (entry.getValue().lastModified >= timestamp) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
      return result;
    }
    
    @Override
    public void guidsSince(long timestamp,
                           RepositorySessionGuidsSinceDelegate delegate) {
      
      ArrayList<String> result = new ArrayList<String>();
      for (Entry<String, CryptoRecord> entry : this.wbos.entrySet()) {
        if (entry.getValue().lastModified >= timestamp) {
          result.add(entry.getKey());
        }
      }
      String[] guids = new String[result.size()];
      result.toArray(guids);
      delegate.onGuidsSinceSucceeded(guids);
    }

    @Override
    public void fetchSince(long timestamp,
                           RepositorySessionFetchRecordsDelegate delegate) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void fetch(String[] guids,
                      RepositorySessionFetchRecordsDelegate delegate) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void store(Record record) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void wipe(RepositorySessionWipeDelegate delegate) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void storeDone() {
      // TODO Auto-generated method stub

    }
    
  }

  public void testGuidsSince() {
    CryptoTestRepository repo = new CryptoTestRepository();
    repo.createSession(new RepositorySessionCreationDelegate() {
      @Override
      public void onSessionCreated(RepositorySession session) {
        ((CryptoTestRepositorySession) session).guidsSince(0, new ExpectGuidsSinceDelegate(new String[0]));
      }
      
      @Override
      public void onSessionCreateFailed(Exception ex) {
        fail("Session creation should not fail.");
      }

      @Override
      public RepositorySessionCreationDelegate deferredCreationDelegate() {
        // TODO: do we need to defer here?
        return this;
      }
    }, null);
  }

  public void testFetchSince() {
  }

  public void testFetch() {
    
  }

  public void testFetchAll() {
  }

  public void testStore() {
  }

  public void testWipe() {
  }

  public void testCrypto5MiddlewareRepositorySession() {
  }

}
