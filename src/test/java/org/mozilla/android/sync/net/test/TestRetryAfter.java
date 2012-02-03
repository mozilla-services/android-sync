package org.mozilla.android.sync.net.test;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mozilla.gecko.sync.net.SyncResponse;

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

    final SyncResponse syncResponse = new SyncResponse(response);
    assertEquals(TEST_SECONDS, syncResponse.retryAfter());
  }

  @Test
  public void testRetryAfterParsesHTTPDate() {
    Date future = new Date(System.currentTimeMillis() + TEST_SECONDS * 1000);

    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", DateUtils.formatDate(future));

    final SyncResponse syncResponse = new SyncResponse(response);
    assertTrue(syncResponse.retryAfter() > TEST_SECONDS - 15);
    assertTrue(syncResponse.retryAfter() < TEST_SECONDS + 15);
  }

  @Test
  public void testRetryAfterParsesMalformed() {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
    response.addHeader("Retry-After", "10X");

    final SyncResponse syncResponse = new SyncResponse(response);
    assertEquals(-1, syncResponse.retryAfter());
  }
}
