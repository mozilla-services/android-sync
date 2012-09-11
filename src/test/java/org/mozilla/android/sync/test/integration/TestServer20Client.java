/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockRecord;
import org.mozilla.android.sync.test.integration.Server20Client.MockServer20ResourceDelegate;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.repositories.Server20Repository;
import org.mozilla.gecko.sync.repositories.domain.Record;

import ch.boye.httpclientandroidlib.HttpStatus;

public class TestServer20Client implements CredentialsSource {
  public static final String LOG_TAG = "TestServer20";

  public static final long   TEST_PORT         = 8080;
  public static final String TEST_SERVER_URL   = "http://localhost" + ":" + TEST_PORT + "/";
  public static final String TEST_USERNAME     = "123456";
  public static final String TEST_PASSWORD     = "test0425";
  public static final String TEST_SYNC_KEY     = "fuyx96ea8rkfazvjdfuqumupye";

  public static final String TEST_COLLECTION   = "test";

  protected KeyBundle syncKeyBundle;

  protected Server20Repository repository = null;
  protected Server20Client client;
  protected String guid1;
  protected String guid2;
  protected MockRecord record1;
  protected MockRecord record2;

  @Override
  public String credentials() {
    return TEST_USERNAME + ":" + TEST_PASSWORD;
  }

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false; // No sense rewriting when we're running the unit tests.
    BaseResourceDelegate.connectionTimeoutInMillis = 1000; // No sense waiting a long time for a local connection.

    Logger.resetLogging();
    // Logger.startLoggingTo(new StdoutLogWriter());

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);

    repository = new Server20Repository(TEST_SERVER_URL, TEST_USERNAME, TEST_COLLECTION, this);
    client = new Server20Client(repository, syncKeyBundle);

    guid1 = "testGuid1";
    guid2 = "testGuid2";
    record1 = new MockRecord(guid1, TEST_COLLECTION, 0, false);
    record2 = new MockRecord(guid2, TEST_COLLECTION, 0, false);
  }

  protected static void assertStatusCodeEqualsAndNullBody(final int statusCode, final MockServer20ResourceDelegate delegate) {
    assertEquals(statusCode, delegate.statusCode);
    assertNull(delegate.responseBody);
    assertNull(delegate.responseObject);
  }

  protected static void assertStatusCodeEqualsAndNotNullBody(final int statusCode, final MockServer20ResourceDelegate delegate) {
    assertEquals(statusCode, delegate.statusCode);
    assertNotNull(delegate.responseBody);
    assertNotNull(delegate.responseObject);
  }

  @Test
  public void testDelete() throws Exception {
    // PUT two records.
    client.put(record1);
    client.put(record2);

    // GET them both.
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.get(guid1));
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.get(guid2));

    // DELETE them both.
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete());
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NOT_FOUND, client.get(guid1));
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NOT_FOUND, client.get(guid1));
  }

  @Test
  public void testDeleteGuid() throws Exception {
    // PUT two records.
    client.put(record1);
    client.put(record2);

    // GET them both.
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.get(guid1));
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.get(guid2));

    // DELETE one of them.
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete(guid1));
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NOT_FOUND, client.get(guid1));
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete(guid1));

    // GET the other one.
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.get(guid2));
  }

  @Test
  public void testPutGet() throws Exception {
    // PUT record.
    record1.deleted = true;
    client.put(record1);

    // GET it back.
    final MockServer20ResourceDelegate d1 = client.get(guid1);
    assertEquals(HttpStatus.SC_OK, d1.statusCode);
    assertNotNull(d1.responseBody);
    assertNotNull(d1.responseObject);
    assertEquals(guid1, d1.responseObject.getString("id"));

    final CryptoRecord c = CryptoRecord.fromJSONRecord(d1.responseObject);
    assertEquals(guid1, c.guid);

    c.keyBundle = syncKeyBundle;
    c.decrypt();

    record2.initFromEnvelope((CryptoRecord) c);

    assertEquals(guid1, record2.guid);
    assertTrue(record2.deleted);
  }

  /**
   * Bug 790397: deleted GUIDs can never be re-used.
   *
   * @throws Exception
   */
  @Test
  public void testPutDeletePut() throws Exception {
    // PUT record.
    client.put(record1);

    // DELETE it.
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete(guid1));

    // PUT it back.
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_CREATED, client.put(record1));

    // GET it.
    final MockServer20ResourceDelegate d1 = client.get(guid1);
    assertEquals(HttpStatus.SC_OK, d1.statusCode);
    assertNotNull(d1.responseBody);
    assertNotNull(d1.responseObject);

    assertEquals(guid1, d1.responseObject.getString("id"));
  }

  /**
   * Bug 790397: deleted GUIDs can never be re-used.
   *
   * @throws Exception
   */
  @Test
  public void testPostDeletePost() throws Exception {
    // POST records.
    client.post(new Record[] { record1, record2 });

    // DELETE it.
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete(guid1));
    assertStatusCodeEqualsAndNullBody(HttpStatus.SC_NO_CONTENT, client.delete(guid2));

    // POST them back.
    assertStatusCodeEqualsAndNotNullBody(HttpStatus.SC_OK, client.post(new Record[] { record1, record2 }));

    // GET them.
    final MockServer20ResourceDelegate d1 = client.get(guid1);
    assertEquals(HttpStatus.SC_OK, d1.statusCode);
    assertNotNull(d1.responseBody);
    assertNotNull(d1.responseObject);
    assertEquals(guid1, d1.responseObject.getString("id"));

    final MockServer20ResourceDelegate d2 = client.get(guid2);
    assertEquals(HttpStatus.SC_OK, d2.statusCode);
    assertNotNull(d2.responseBody);
    assertNotNull(d2.responseObject);
    assertEquals(guid2, d2.responseObject.getString("id"));
  }
}
