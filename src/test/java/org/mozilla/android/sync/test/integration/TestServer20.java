package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.SynchronizerHelpers.TrackingWBORepository;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.test.helpers.SessionTestHelper;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.android.sync.test.integration.Server20Client.MockServer20ResourceDelegate;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.log.writers.StdoutLogWriter;
import org.mozilla.gecko.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.Server20Repository;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.ServerLocalSynchronizer;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;

import ch.boye.httpclientandroidlib.HttpStatus;

public class TestServer20 implements CredentialsSource {
  public static final String LOG_TAG = "TestServer20RepoSess";

  public static final long   TEST_PORT         = 8080;
  public static final String TEST_SERVER_URL   = "http://localhost" + ":" + TEST_PORT + "/";
  public static final String TEST_USERNAME     = "456";
  public static final String TEST_PASSWORD     = "password";
  public static final String TEST_SYNC_KEY     = "fuyx96ea8rkfazvjdfuqumupye";

  public static final String TEST_COLLECTION   = "test";

  protected KeyBundle syncKeyBundle;

  protected Server20Repository serverRepository;
  protected Crypto5MiddlewareRepository cryptoRepository;

  protected Server20Client client;

  @Override
  public String credentials() {
    return TEST_USERNAME + ":" + TEST_PASSWORD;
  }

  public static class MockFHRecord extends FormHistoryRecord {
    public MockFHRecord(String guid) {
      super(guid, TEST_COLLECTION, 0, false);
      fieldName = "fieldName:" + guid;
      fieldValue = "fieldValue:" + guid;
    }

    /**
     * For testing: return true if the input is equal only in terms of payload,
     * ignoring other transient values such as timestamps.
     */
    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof Record)) {
        return false;
      }

      return equalPayloads(o);
    }
  }

  public static class MockFHRecordFactory extends RecordFactory {
    @Override
    public Record createRecord(Record record) {
      MockFHRecord r = new MockFHRecord(record.guid);
      r.initFromEnvelope((CryptoRecord) record);
      r.collection = TEST_COLLECTION; // For testing!
      return r;
    }
  }

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false; // No sense rewriting when we're running the unit tests.
    BaseResourceDelegate.connectionTimeoutInMillis = 1000; // No sense waiting a long time for a local connection.

    Logger.resetLogging();
    Logger.startLoggingTo(new StdoutLogWriter());

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);

    serverRepository = new Server20Repository(TEST_SERVER_URL, TEST_USERNAME, TEST_COLLECTION, this);
    cryptoRepository = new Crypto5MiddlewareRepository(serverRepository, syncKeyBundle, new MockFHRecordFactory());
    client = new Server20Client(serverRepository, syncKeyBundle);

    client.delete();

    assertTrue(WaitHelper.getTestWaiter().isIdle());
    // WaitHelper.resetTestWaiter();
/*
    guid1 = "testGuid1";
    guid2 = "testGuid2";
    record1 = new MockFHRecord(guid1, TEST_COLLECTION, 0, false);
    record2 = new MockFHRecord(guid2, TEST_COLLECTION, 0, false);
*/
  }

  @After
  public void tearDown() {
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  protected Synchronizer getSynchronizer(Repository remote, WBORepository local) {
    MockFHRecord[] outbounds = new MockFHRecord[] {
//        new MockFHRecord("outbound1", "bookmarks", 1, false),
//        new MockFHRecord("outboundFail2", "bookmarks", 1, false),
//        new MockFHRecord("outboundFail3", "bookmarks", 1, false),
//        new MockFHRecord("outboundFail4", "bookmarks", 1, false),
//        new MockFHRecord("outboundFail5", "bookmarks", 1, false),
//        new MockFHRecord("outboundFail6", "bookmarks", 1, false),
    };

    for (MockFHRecord outbound : outbounds) {
      local.wbos.put(outbound.guid, outbound);
    }

//    MockFHRecord[] inbounds = new MockFHRecord[] {
//        new MockFHRecord("inboundSucc1", "bookmarks", 1, false),
//        new MockFHRecord("inboundSucc2", "bookmarks", 1, false),
//        new MockFHRecord("inboundFail1", "bookmarks", 1, false),
//        new MockFHRecord("inboundSucc3", "bookmarks", 1, false),
//        new MockFHRecord("inboundFail2", "bookmarks", 1, false),
//        new MockFHRecord("inboundFail3", "bookmarks", 1, false),
//    };
//    for (MockFHRecord inbound : inbounds) {
//      remote.wbos.put(inbound.guid, inbound);
//    }

    final Synchronizer synchronizer = new ServerLocalSynchronizer();
    synchronizer.repositoryA = remote;
    synchronizer.repositoryB = local;
    return synchronizer;
  }

  protected static Exception doSynchronize(final Synchronizer synchronizer) {
    final ArrayList<Exception> a = new ArrayList<Exception>();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        synchronizer.synchronize(null, new SynchronizerDelegate() {
          @Override
          public void onSynchronized(Synchronizer synchronizer) {
            Logger.trace(LOG_TAG, "Got onSynchronized.");
            a.add(null);
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onSynchronizeFailed(Synchronizer synchronizer, Exception lastException, String reason) {
            Logger.trace(LOG_TAG, "Got onSynchronizedFailed.");
            a.add(lastException);
            WaitHelper.getTestWaiter().performNotify();
          }
        });
      }
    });

    assertEquals(1, a.size()); // Should not be called multiple times!
    return a.get(0);
  }

  protected ArrayList<Record> doFetchSince(final long timestamp) {
    final RepositorySession repoSession = SessionTestHelper.createAndBeginSession(null, cryptoRepository);
    final ExpectSuccessRepositorySessionFetchRecordsDelegate delegate = new ExpectSuccessRepositorySessionFetchRecordsDelegate(WaitHelper.getTestWaiter());

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        repoSession.fetchSince(timestamp, delegate);
      }
    });

    repoSession.abort();

    return delegate.fetchedRecords;
  }

  protected void assertFetchSince(final long timestamp, final Record[] expected) {
    ArrayList<Record> fetched = doFetchSince(timestamp);
    ArrayList<Record> e = new ArrayList<Record>();
    for (Record r : expected) {
      e.add(r);
    }
    assertEquals(e, fetched);
  }

  @Test
  public void testFetchSince() throws Exception {
    final String guid1 = "testGuid1";
    final String guid2 = "testGuid2";
    final String[] guids = new String[] { guid1, guid2 };

    final Map<String, Record> records = new LinkedHashMap<String, Record>();

    for (String guid : guids) {
      final Record record = new MockFHRecord(guid);

      records.put(guid, record);
      MockServer20ResourceDelegate delegate = client.put(record);
      assertEquals(HttpStatus.SC_CREATED, delegate.statusCode);
      record.lastModified = delegate.lastModified;
    }

    assertFetchSince(0, new Record[] { records.get(guid1), records.get(guid2) });
    assertFetchSince(records.get(guid1).lastModified + 1, new Record[] { records.get(guid2) });
    assertFetchSince(records.get(guid2).lastModified + 1, new Record[] { });

    // assertTrue(WaitHelper.getTestWaiter().isIdle());
    // WaitHelper.resetTestWaiter();
  }

  @Test
  public void testNoErrors() throws Exception {
    // assertTrue(WaitHelper.getTestWaiter().isIdle());
    // WaitHelper.resetTestWaiter();

    final String guid = "testGuid1";

    final Map<String, Record> records = new HashMap<String, Record>();
    final MockFHRecord r = new MockFHRecord(guid);
    records.put(guid, r);

    for (Record record : records.values()) {
      assertEquals(HttpStatus.SC_CREATED, client.put(record).statusCode);
    }

    final WBORepository local  = new TrackingWBORepository();
    assertEquals(0, local.wbos.size());

    Synchronizer synchronizer = getSynchronizer(cryptoRepository, local);
    final Exception e = doSynchronize(synchronizer);
    if (e != null) {
      throw e;
    }

    assertEquals(1, local.wbos.size());
    assertTrue(records.equals(local.wbos));
    // assertEquals(12, remote.wbos.size());
  }

  /*
  protected long putRecs(final String collection, final String[] guids) throws Exception {
    long timestamp = -1;

    for (String guid : guids) {
      final MockFHRecord record = new MockFHRecord(guid, collection);
      record.fieldName  = "testFieldName";
      record.fieldValue = "testFieldValue";
      CryptoRecord rec = record.getEnvelope();
      rec.setKeyBundle(syncKeyBundle);
      rec.encrypt();
      final String RECORD_URL = session.config.wboURI(collection, guid).toString();
      LiveDelegate ld = TestBasicFetch.realLivePut(TEST_USERNAME, TEST_PASSWORD, RECORD_URL, rec);
      if (ld.testFailureIgnored) {
        return -1;
      }

      final String timestampString = ld.body();
      assertNotNull(timestampString);
      timestamp = Math.max(timestamp, Utils.decimalSecondsToMilliseconds(timestampString));
    }

    return timestamp;
  }

  protected Server11RepositorySession repoSession;

  protected Server11RepositorySession getSession(final String collection) throws Exception {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        Server11Repository repo;
        try {
          repo = new Server11Repository(TEST_CLUSTER_URL, TEST_USERNAME, collection, TestServer11GuidsSince.this);
        } catch (Exception e) {
          WaitHelper.getTestWaiter().performNotify(e);
          return;
        }
        repo.createSession(new RepositorySessionCreationDelegate() {
          @Override
          public void onSessionCreated(RepositorySession session) {
            TestServer11GuidsSince.this.repoSession = (Server11RepositorySession) session;
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onSessionCreateFailed(Exception ex) {
            WaitHelper.getTestWaiter().performNotify(ex);
          }

          @Override
          public RepositorySessionCreationDelegate deferredCreationDelegate() {
            return this;
          }
        }, null);
      }
    });

    return this.repoSession;
  }

  public Set<String> getGuidsSince(final String collection, final long timestamp) throws Exception {
    final Server11RepositorySession repoSession = getSession(collection);
    assertNotNull(session);
    final ExpectSuccessGuidsSinceDelegate delegate = new ExpectSuccessGuidsSinceDelegate(WaitHelper.getTestWaiter());
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        repoSession.guidsSince(timestamp, delegate);
      }
    });
    repoSession.abort();

    return new HashSet<String>(delegate.guids);
  }

  @Test
  public void testGuidsSince() throws Exception {
    final String COLLECTION = "test";

    final String[] RECS1 = new String[] { "guid1", "guid2" }; // Records to be PUT to server.
    final String[] RECS2 = new String[] { "guid2", "guid3" }; // Records to be PUT to server.

    // Put records -- doesn't matter what type of record.  This overwrites anything already on the server.
    final long timestamp1 = 1 + putRecs(COLLECTION, RECS1);
    final long timestamp2 = 1 + putRecs(COLLECTION, RECS1); // Note: putting this twice for timestamps.
    putRecs(COLLECTION, RECS2);

    // All are modified after initial PUT.
    final Set<String> expected1 = new HashSet<String>();
    for (String guid : RECS1) {
      expected1.add(guid);
    }
    for (String guid : RECS2) {
      expected1.add(guid);
    }
    final Set<String> guidsSince1 = getGuidsSince(COLLECTION, timestamp1);
    assertEquals(expected1, guidsSince1);

    // Only second batch are modified after second PUT.
    final Set<String> expected2 = new HashSet<String>();
    for (String guid : RECS2) {
      expected2.add(guid);
    }
    final Set<String> guidsSince2 = getGuidsSince(COLLECTION, timestamp2);
    assertEquals(expected2, guidsSince2);

    // None modified after future time.
    final long timestamp3 = System.currentTimeMillis() + 12*60*1000;
    final Set<String> expected3 = new HashSet<String>();
    final Set<String> guidsSince3 = getGuidsSince(COLLECTION, timestamp3);
    assertEquals(expected3, guidsSince3);
  }
  */
}
