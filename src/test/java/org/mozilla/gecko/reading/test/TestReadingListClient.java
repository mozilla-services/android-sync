/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10.AuthorizationResponse;
import org.mozilla.gecko.background.fxa.test.FxAccountTestHelper;
import org.mozilla.gecko.background.fxa.test.FxAccountTestHelper.StableDevTestHelper;
import org.mozilla.gecko.reading.FetchSpec;
import org.mozilla.gecko.reading.ReadingListClient;
import org.mozilla.gecko.reading.ReadingListClient.ReadingListRecordResponse;
import org.mozilla.gecko.reading.ReadingListClient.ReadingListResponse;
import org.mozilla.gecko.reading.ReadingListDeleteDelegate;
import org.mozilla.gecko.reading.ReadingListRecord;
import org.mozilla.gecko.reading.ReadingListRecordDelegate;
import org.mozilla.gecko.reading.ReadingListRecordUploadDelegate;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.net.BearerAuthHeaderProvider;
import org.mozilla.gecko.sync.net.MozResponse;

public class TestReadingListClient {
  final FxAccountTestHelper helper = new StableDevTestHelper();

  public class TestRecordDeleteDelegate implements ReadingListDeleteDelegate {
    public volatile ReadingListRecordResponse response;
    public volatile ReadingListRecord record;
    public volatile String guid;
    public volatile Exception error;
    public volatile MozResponse mozResponse;

    private final CountDownLatch latch;

    public TestRecordDeleteDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onSuccess(ReadingListRecordResponse response,
                          ReadingListRecord record) {
      this.response = response;
      this.record = record;
      latch.countDown();
    }

    @Override
    public void onPreconditionFailed(String guid, MozResponse response) {
      this.mozResponse = response;
      this.guid = guid;
      latch.countDown();
    }

    @Override
    public void onRecordMissingOrDeleted(String guid, MozResponse response) {
      this.mozResponse = response;
      this.guid = guid;
      latch.countDown();
    }

    @Override
    public void onFailure(Exception e) {
      this.error = e;
      latch.countDown();
    }

    @Override
    public void onFailure(MozResponse response) {
      this.mozResponse = response;
      latch.countDown();
    }
  }

  public static final class TestRecordDelegate implements ReadingListRecordDelegate {
    private final CountDownLatch latch;
    public volatile Exception error;
    public volatile MozResponse mozResponse;
    public volatile ReadingListResponse response;
    public final List<ReadingListRecord> records = new ArrayList<ReadingListRecord>();
    public volatile boolean called;

    public TestRecordDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onRecordReceived(ReadingListRecord record) {
      this.called = true;
      this.records.add(record);
    }

    @Override
    public void onFailure(Exception error) {
      this.called = true;
      this.error = error;
      latch.countDown();
    }

    @Override
    public void onFailure(MozResponse response) {
      this.called = true;
      this.mozResponse = response;
      latch.countDown();
    }

    @Override
    public void onComplete(ReadingListResponse response) {
      this.called = true;
      this.response = response;
      latch.countDown();
    }
  }

  public static class TestRecordUploadDelegate implements ReadingListRecordUploadDelegate {
    private final CountDownLatch latch;
    public volatile Exception error;
    public volatile MozResponse mozResponse;
    public volatile ReadingListResponse response;
    public volatile ReadingListRecord record;

    public TestRecordUploadDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onInvalidUpload(ReadingListResponse response) {
      this.response = response;
      latch.countDown();
    }

    @Override
    public void onConflict(ReadingListResponse response) {
      this.response = response;
      latch.countDown();
    }

    @Override
    public void onSuccess(ReadingListRecordResponse response,
                          ReadingListRecord record) {
      this.response = response;
      this.record = record;
      latch.countDown();
    }

    @Override
    public void onBadRequest(MozResponse response) {
      this.mozResponse = response;
      latch.countDown();
    }

    @Override
    public void onFailure(Exception ex) {
      this.error = ex;
      latch.countDown();
    }

    @Override
    public void onFailure(MozResponse response) {
      this.mozResponse = response;
      latch.countDown();
    }
  }

  private static final String DEFAULT_SERVICE_URI = "https://readinglist.dev.mozaws.net/v0/";

  private TestRecordDelegate fetchAll(final ReadingListClient client) throws Exception {
    final FetchSpec spec = new FetchSpec.Builder()
                                        .setStatus("0", false)
                                        .build();
    final long ifModifiedSince = -1L;
    return fetch(client, spec, ifModifiedSince);
  }

  private TestRecordDelegate fetchSince(final ReadingListClient client, final long since) throws Exception {
    final FetchSpec spec = new FetchSpec.Builder()
                                        .setSince(since)
                                        .build();
    final long ifModifiedSince = -1L;
    return fetch(client, spec, ifModifiedSince);
  }

  private TestRecordDelegate fetchIMS(final ReadingListClient client, final long ifModifiedSince) throws Exception {
    final FetchSpec spec = new FetchSpec.Builder()
                                        .build();
    return fetch(client, spec, ifModifiedSince);
  }

  private TestRecordDelegate fetch(final ReadingListClient client,
                                   final FetchSpec spec,
                                   final long ifModifiedSince) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestRecordDelegate delegate = new TestRecordDelegate(latch);

    client.getAll(spec, delegate, ifModifiedSince);
    latch.await(10000, TimeUnit.MILLISECONDS);
    return delegate;
  }

  final TestRecordUploadDelegate uploadRecord(final ReadingListClient client, final ReadingListRecord record) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestRecordUploadDelegate uploadDelegate = new TestRecordUploadDelegate(latch);

    client.add(record, uploadDelegate);
    latch.await(5000, TimeUnit.MILLISECONDS);
    return uploadDelegate;
  }

  final TestRecordDeleteDelegate deleteRecord(final ReadingListClient client, final ReadingListRecord record) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestRecordDeleteDelegate deleteDelegate = new TestRecordDeleteDelegate(latch);

    client.delete(record.id, deleteDelegate, record.lastModified);
    latch.await(5000, TimeUnit.MILLISECONDS);
    return deleteDelegate;
  }

  @Test
  public final void test() throws Exception {
    final AuthHeaderProvider auth = new BasicAuthHeaderProvider("rnewmantest" + System.currentTimeMillis(), "nopassword");
    final ReadingListClient client = new ReadingListClient(new URI(DEFAULT_SERVICE_URI), auth);

    final TestRecordDelegate delegate = fetchAll(client);

    final List<ReadingListRecord> existingRecords = delegate.records;

    Assert.assertTrue(delegate.called);
    Assert.assertNull(delegate.error);
    Assert.assertNull(delegate.mozResponse);
    Assert.assertNotNull(delegate.response);

    final long lastServerTimestamp = delegate.response.getLastModified();
    Assert.assertTrue(lastServerTimestamp > -1L);

    // Upload a record.
    final ReadingListRecord record = new ReadingListRecord("http://reddit.com", "Reddit", "Test Device");
    Assert.assertEquals(record.url, "http://reddit.com");
    Assert.assertEquals(record.title, "Reddit");
    Assert.assertEquals(record.addedBy, "Test Device");

    final TestRecordUploadDelegate uploadDelegate = uploadRecord(client, record);

    Assert.assertNull(uploadDelegate.error);
    Assert.assertNull(uploadDelegate.mozResponse);
    Assert.assertNotNull(uploadDelegate.response);
    Assert.assertNotNull(uploadDelegate.record);
    Assert.assertEquals(record.url, uploadDelegate.record.url);
    Assert.assertEquals(record.title, uploadDelegate.record.title);
    Assert.assertEquals(record.addedBy, uploadDelegate.record.addedBy);
    Assert.assertEquals(201, uploadDelegate.response.getStatusCode());
    Assert.assertNotNull(uploadDelegate.record.id);
    Assert.assertTrue(lastServerTimestamp < uploadDelegate.record.lastModified);

    // Implementation detail.
    final long uploadLastModified = uploadDelegate.response.getLastModified();
    Assert.assertEquals(-1L, uploadLastModified);

    // Now fetch from our last timestamp. The record we uploaded should always be included,
    // but nothing earlier.
    final TestRecordDelegate sinceDelegate = fetchSince(client, lastServerTimestamp);
    Assert.assertEquals(1, sinceDelegate.records.size());

    // Another fetch with an If-Modified-Since should return a 304.
    final TestRecordDelegate nothingDelegate = fetchIMS(client, sinceDelegate.response.getLastModified());
    Assert.assertEquals(304, nothingDelegate.response.getStatusCode());

    // Delete all the old records.
    for (ReadingListRecord rec : existingRecords) {
      final TestRecordDeleteDelegate deleteDelegate = deleteRecord(client, rec);
      Assert.assertTrue(deleteDelegate.response.wasSuccessful());
      Assert.assertEquals(200, deleteDelegate.response.getStatusCode());
      Assert.assertNull(deleteDelegate.guid);       // Only set on failure.
      Assert.assertEquals(rec.id, deleteDelegate.record.id);
    }

    // The new records should still be around.
    final TestRecordDelegate afterDelegate = fetchSince(client, lastServerTimestamp);
    Assert.assertEquals(1, afterDelegate.records.size());
  }

  @Test
  public final void testWithAuthorization() throws Throwable {
    // For now, the scope is "profile". It will be "readinglist" eventually
    // (tracked by https://github.com/mozilla-services/readinglist/issues/16).
    final String scope = "profile";

    final AuthorizationResponse authorization = helper.doTestAuthorization("testtesto@mockmyid.com", "testtesto@mockmyid.com", scope);
    final AuthHeaderProvider auth = new BearerAuthHeaderProvider(authorization.access_token);
    final ReadingListClient client = new ReadingListClient(new URI(DEFAULT_SERVICE_URI), auth);

    final ReadingListRecord record = new ReadingListRecord("http://reddit.com", "Reddit", "Test Device");

    // Verify that we can upload a record.
    final TestRecordUploadDelegate uploadDelegate = uploadRecord(client, record);
    Assert.assertNull(uploadDelegate.error);
    Assert.assertNull(uploadDelegate.mozResponse);
    Assert.assertNotNull(uploadDelegate.response);
    Assert.assertNotNull(uploadDelegate.record);
    Assert.assertEquals(record.url, uploadDelegate.record.url);
    Assert.assertEquals(record.title, uploadDelegate.record.title);
    Assert.assertEquals(record.addedBy, uploadDelegate.record.addedBy);
    // Accept a 200 (record did not exist on the server) or a 201 (record already existed).
    Assert.assertEquals(2, uploadDelegate.response.getStatusCode() / 100);
    Assert.assertNotNull(uploadDelegate.record.id);

    // Verify that we can delete a record.
    final TestRecordDeleteDelegate deleteDelegate = deleteRecord(client, uploadDelegate.record);
    Assert.assertTrue(deleteDelegate.response.wasSuccessful());
    Assert.assertEquals(200, deleteDelegate.response.getStatusCode());
  }
}
