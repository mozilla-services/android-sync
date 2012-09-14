package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.log.writers.StdoutLogWriter;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.repositories.Server20Repository;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

import ch.boye.httpclientandroidlib.HttpStatus;

public class TestServer20RepositorySession implements CredentialsSource {
  public static final String LOG_TAG = "TestServer20RepoSess";

  public static final long   TEST_PORT         = 8080;
  public static final String TEST_SERVER_URL   = "http://localhost" + ":" + TEST_PORT + "/";
  public static final String TEST_USERNAME     = "456";
  public static final String TEST_PASSWORD     = "password";
  public static final String TEST_SYNC_KEY     = "fuyx96ea8rkfazvjdfuqumupye";

  public static final String TEST_COLLECTION   = "test";

  protected KeyBundle syncKeyBundle;
  protected Server20Repository repository;
  protected Server20Client client;

  protected String guid1;
  protected String guid2;
  protected FormHistoryRecord record1;
  protected FormHistoryRecord record2;

  @Override
  public String credentials() {
    return TEST_USERNAME + ":" + TEST_PASSWORD;
  }

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false; // No sense rewriting when we're running the unit tests.
    BaseResourceDelegate.connectionTimeoutInMillis = 1000; // No sense waiting a long time for a local connection.

    Logger.resetLogging();
    Logger.startLoggingTo(new StdoutLogWriter());

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);

    repository = new Server20Repository(TEST_SERVER_URL, TEST_USERNAME, TEST_COLLECTION, this);
    client = new Server20Client(repository, syncKeyBundle);

    guid1 = "testGuid1";
    guid2 = "testGuid2";
    record1 = new FormHistoryRecord(guid1, TEST_COLLECTION, 0, false);
    record1.fieldName = "fieldName1";
    record1.fieldValue = "fieldValue1";
    record2 = new FormHistoryRecord(guid2, TEST_COLLECTION, 0, true);
    record2.fieldName = "fieldName2";
    record2.fieldValue = "fieldValue1";
  }

  @Test
  public void testFetchSince() throws Exception {
    client.delete();

    assertEquals(HttpStatus.SC_CREATED, client.put(record1).statusCode);
    assertEquals(HttpStatus.SC_CREATED, client.put(record2).statusCode);

    // repository.createSession(delegate, context);
  }
}
