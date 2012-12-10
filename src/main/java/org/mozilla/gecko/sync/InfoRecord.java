/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.sync.delegates.InfoCollectionsDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public abstract class InfoRecord {

  private static final long DEFAULT_FETCH_TIMEOUT_MSEC = 2 * 60 * 1000;   // Two minutes.
  private static final String LOG_TAG = "InfoRecord";
  private InfoCollectionsDelegate delegate;
  protected String credentials;
  protected String uri;

  public InfoRecord(String uri, String credentials) {
    super();
    this.uri = uri;
    this.credentials = credentials;
  }

  protected String getURI() {
    return this.uri;
  }

  protected abstract InfoRecord processResponse(ExtendedJSONObject jsonObjectBody) throws Exception;

  private class InfoFetchHandler implements SyncStorageRequestDelegate {

    // SyncStorageRequestDelegate methods for fetching.
    public String credentials() {
      return credentials;
    }

    public String ifUnmodifiedSince() {
      return null;
    }

    public void handleRequestSuccess(SyncStorageResponse response) {
      if (response.wasSuccessful()) {
        try {
          delegate.handleSuccess(processResponse(response.jsonObjectBody()));
        } catch (Exception e) {
          handleRequestError(e);
        }
        return;
      }
      handleRequestFailure(response);
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      delegate.handleFailure(response);
    }

    @Override
    public void handleRequestError(Exception ex) {
      delegate.handleError(ex);
    }
  }

  public void fetch(final InfoCollectionsDelegate delegate) {
    this.delegate = delegate;
    try {
      final SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.getURI());
      r.delegate = new InfoFetchHandler();
      r.get();
    } catch (Exception e) {
      delegate.handleError(e);
    }
  }

  private class LatchedInfoCollectionsDelegate implements InfoCollectionsDelegate {
    public InfoRecord record = null;
    public Exception exception = null;
    private CountDownLatch latch;

    public LatchedInfoCollectionsDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void handleSuccess(InfoRecord record) {
      this.record = record;
      latch.countDown();
    }

    @Override
    public void handleFailure(SyncStorageResponse response) {
      this.exception = new HTTPFailureException(response);
      latch.countDown();
    }

    @Override
    public void handleError(Exception e) {
      this.exception = e;
      latch.countDown();
    }
  }

  /**
   * Fetch the info record, blocking until it returns.
   * @return the info record.
   */
  public InfoRecord fetchBlocking() throws HTTPFailureException, Exception {
    CountDownLatch latch = new CountDownLatch(1);
    LatchedInfoCollectionsDelegate delegate = new LatchedInfoCollectionsDelegate(latch);
    this.delegate = delegate;
    this.fetch(delegate);

    if (!latch.await(DEFAULT_FETCH_TIMEOUT_MSEC, TimeUnit.MILLISECONDS)) {
      Logger.warn(LOG_TAG, "Interrupted fetching info record.");
      throw new InterruptedException("info fetch timed out.");
    }

    if (delegate.record != null) {
      return delegate.record;
    }

    if (delegate.exception != null) {
      throw delegate.exception;
    }

    throw new Exception("Unknown error.");
  }
}