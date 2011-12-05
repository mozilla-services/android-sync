/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;

import android.content.Context;

public class Crypto5MiddlewareRepositorySessionTest {

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
      delegate.onGuidsSinceSucceeded((String[]) result.toArray());
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
    public void store(Record record, RepositorySessionStoreDelegate delegate) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void wipe(RepositorySessionWipeDelegate delegate) {
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
