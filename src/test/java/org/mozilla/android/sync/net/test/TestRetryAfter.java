package org.mozilla.android.sync.net.test;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.impl.cookie.DateUtils;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

public class TestRetryAfter {
  private int TEST_SECONDS = 120;

  @Test
  public void testRetryAfterParsesSeconds() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", Long.toString(TEST_SECONDS)); // Retry-After given in seconds.

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertEquals(TEST_SECONDS, SyncStorageResponse.retryAfterInSeconds());
  }

  @Test
  public void testRetryAfterParsesHTTPDate() {
    Date future = new Date(System.currentTimeMillis() + TEST_SECONDS * 1000);

    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", DateUtils.formatDate(future));

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertTrue(SyncStorageResponse.retryAfterInSeconds() > TEST_SECONDS - 15);
    assertTrue(SyncStorageResponse.retryAfterInSeconds() < TEST_SECONDS + 15);
  }

  @Test
  public void testRetryAfterParsesMalformed() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", "10X");

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertEquals(-1, SyncStorageResponse.retryAfterInSeconds());
  }

  @Test
  public void testRetryAfterParsesNeither() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertEquals(-1, SyncStorageResponse.retryAfterInSeconds());
  }

  @Test
  public void testRetryAfterParsesLargerRetryAfter() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", Long.toString(TEST_SECONDS + 1));
    response.addHeader("X-Weave-Backoff", Long.toString(TEST_SECONDS));

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertEquals(1000 * (TEST_SECONDS + 1), SyncStorageResponse.totalBackoffInMilliseconds());
  }

  @Test
  public void testRetryAfterParsesLargerXWeaveBackoff() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", Long.toString(TEST_SECONDS));
    response.addHeader("X-Weave-Backoff", Long.toString(TEST_SECONDS + 1));

    final SyncStorageResponse SyncStorageResponse = new SyncStorageResponse(response);
    assertEquals(1000 * (TEST_SECONDS + 1), SyncStorageResponse.totalBackoffInMilliseconds());
  }
}
