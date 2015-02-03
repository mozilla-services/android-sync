/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading.test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.reading.ClientReadingListRecord;
import org.mozilla.gecko.reading.FetchSpec;
import org.mozilla.gecko.reading.ReadingListClient;
import org.mozilla.gecko.reading.ReadingListConstants;
import org.mozilla.gecko.reading.ReadingListDeleteDelegate;
import org.mozilla.gecko.reading.ReadingListRecord;
import org.mozilla.gecko.reading.ReadingListRecordDelegate;
import org.mozilla.gecko.reading.ReadingListRecordResponse;
import org.mozilla.gecko.reading.ReadingListRecordUploadDelegate;
import org.mozilla.gecko.reading.ReadingListResponse;
import org.mozilla.gecko.reading.ServerReadingListRecord;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.net.MozResponse;

public class TestReadingListClient {
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
      if (response.getStatusCode() == 500) {
        Assert.fail("Got 500.");
      }

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
    public void onRecordReceived(ServerReadingListRecord record) {
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
      checkFor500(response);
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

    @Override
    public void onRecordMissingOrDeleted(String guid, ReadingListResponse resp) {
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
    public void onInvalidUpload(ClientReadingListRecord up, ReadingListResponse response) {
      this.response = response;
      latch.countDown();
    }

    @Override
    public void onConflict(ClientReadingListRecord up, ReadingListResponse response) {
      this.response = response;
      latch.countDown();
    }

    @Override
    public void onSuccess(ClientReadingListRecord up, ReadingListRecordResponse response,
                          ServerReadingListRecord down) {
      this.response = response;
      this.record = down;
      latch.countDown();
    }

    @Override
    public void onBadRequest(ClientReadingListRecord up, MozResponse response) {
      this.mozResponse = response;
      latch.countDown();
    }

    @Override
    public void onFailure(ClientReadingListRecord up, Exception ex) {
      this.error = ex;
      latch.countDown();
    }

    @Override
    public void onFailure(ClientReadingListRecord up, MozResponse response) {
      checkFor500(response);
      this.mozResponse = response;
      latch.countDown();
    }

    @Override
    public void onBatchDone() {
    }
  }

  private static final String DEFAULT_SERVICE_URI = ReadingListConstants.DEFAULT_DEV_ENDPOINT;

  static void checkFor500(MozResponse response) {
    if (response.getStatusCode() == 500) {
      try {
        Assert.fail("Got 500 with body " + response.body());
      } catch (IllegalStateException | IOException e) {
        // Failed to fetch the body.
      }
    }
  }

  private TestRecordDelegate fetchAll(final ReadingListClient client) throws Exception {
    final FetchSpec spec = new FetchSpec.Builder()
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

  final TestRecordUploadDelegate uploadRecord(final ReadingListClient client, final ClientReadingListRecord record) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestRecordUploadDelegate uploadDelegate = new TestRecordUploadDelegate(latch);

    client.add(record, uploadDelegate);
    latch.await(5000, TimeUnit.MILLISECONDS);
    return uploadDelegate;
  }

  final TestRecordDeleteDelegate deleteRecord(final ReadingListClient client, final ReadingListRecord record) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestRecordDeleteDelegate deleteDelegate = new TestRecordDeleteDelegate(latch);

    client.delete(record.getGUID(), deleteDelegate, record.getServerLastModified());
    latch.await(5000, TimeUnit.MILLISECONDS);
    return deleteDelegate;
  }

  @Test
  public final void test() throws Exception {
    final AuthHeaderProvider auth = new BasicAuthHeaderProvider("test_android_client", "nopassword");
    final ReadingListClient client = new ReadingListClient(new URI(DEFAULT_SERVICE_URI), auth);

    final TestRecordDelegate delegate = fetchAll(client);

    final List<ReadingListRecord> existingRecords = delegate.records;

    Assert.assertTrue(delegate.called);
    Assert.assertNull(delegate.error);
    Assert.assertNull(delegate.mozResponse);
    Assert.assertNotNull(delegate.response);

    final long lastServerTimestamp = delegate.response.getLastModified();
    Assert.assertTrue(lastServerTimestamp > -1L);

    final String url = "http://reddit.com/" + Utils.generateGuid();

    // Upload a record.
    final ClientReadingListRecord record = new ClientReadingListRecord(url, "Reddit", "Test Device");
    Assert.assertEquals(record.getURL(), url);
    Assert.assertEquals(record.getTitle(), "Reddit");
    Assert.assertEquals(record.getAddedBy(), "Test Device");

    final TestRecordUploadDelegate uploadDelegate = uploadRecord(client, record);

    Assert.assertNull(uploadDelegate.error);
    Assert.assertNull(uploadDelegate.mozResponse);
    Assert.assertNotNull(uploadDelegate.response);
    Assert.assertNotNull(uploadDelegate.record);
    Assert.assertEquals(record.getURL(), uploadDelegate.record.getURL());
    Assert.assertEquals(record.getTitle(), uploadDelegate.record.getTitle());
    Assert.assertEquals(record.getAddedBy(), uploadDelegate.record.getAddedBy());
    Assert.assertNotNull(uploadDelegate.record.getGUID());

    // If the record already exists in an unmodified form, the server will return 200.
    // If it doesn't exist, it'll return 201.
    Assert.assertEquals(201, uploadDelegate.response.getStatusCode());
    Assert.assertTrue(lastServerTimestamp < uploadDelegate.record.getServerLastModified());

    // Implementation detail.
    final long uploadLastModified = uploadDelegate.response.getLastModified();
    Assert.assertEquals(-1L, uploadLastModified);

    // Upload the same record again; we should get a 200.
    // TODO: instead we get a 201, which I think is wrong.
    final TestRecordUploadDelegate reuploadDelegate = uploadRecord(client, record);
    Assert.assertEquals(record.getTitle(), uploadDelegate.record.getTitle());
    Assert.assertEquals(201, uploadDelegate.response.getStatusCode());

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
      Assert.assertEquals(rec.getGUID(), deleteDelegate.record.getGUID());
    }

    // The new records should still be around.
    final TestRecordDelegate afterDelegate = fetchSince(client, lastServerTimestamp);
    Assert.assertEquals(1, afterDelegate.records.size());
  }
}
