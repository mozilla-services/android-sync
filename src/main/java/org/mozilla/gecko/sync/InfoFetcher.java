package org.mozilla.gecko.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.sync.delegates.InfoFetchDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

/**
 * An object which fetches a chunk of JSON from a URI, using certain credentials,
 * and informs its delegate of the result.
 */
public class InfoFetcher {
  private static final long DEFAULT_FETCH_TIMEOUT_MSEC = 2 * 60 * 1000;   // Two minutes.
  private static final String LOG_TAG = "InfoFetcher";

  protected final String credentials;
  protected final String uri;
  protected InfoFetchDelegate delegate;

  public InfoFetcher(final String uri, final String credentials) {
    this.uri = uri;
    this.credentials = credentials;
  }

  protected String getURI() {
    return this.uri;
  }

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
          delegate.handleSuccess(response.jsonObjectBody());
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

  public void fetch(final InfoFetchDelegate delegate) {
    this.delegate = delegate;
    try {
      final SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.getURI());
      r.delegate = new InfoFetchHandler();
      r.get();
    } catch (Exception e) {
      delegate.handleError(e);
    }
  }

  private class LatchedInfoCollectionsDelegate implements InfoFetchDelegate {
    public ExtendedJSONObject body = null;
    public Exception exception = null;
    private CountDownLatch latch;

    public LatchedInfoCollectionsDelegate(CountDownLatch latch) {
      this.latch = latch;
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

    @Override
    public void handleSuccess(ExtendedJSONObject body) {
      this.body = body;
      latch.countDown();
    }
  }

  /**
   * Fetch the info record, blocking until it returns.
   * @return the info record.
   */
  public ExtendedJSONObject fetchBlocking() throws HTTPFailureException, Exception {
    CountDownLatch latch = new CountDownLatch(1);
    LatchedInfoCollectionsDelegate delegate = new LatchedInfoCollectionsDelegate(latch);
    this.delegate = delegate;
    this.fetch(delegate);

    if (!latch.await(DEFAULT_FETCH_TIMEOUT_MSEC, TimeUnit.MILLISECONDS)) {
      Logger.warn(LOG_TAG, "Interrupted fetching info record.");
      throw new InterruptedException("info fetch timed out.");
    }

    if (delegate.body != null) {
      return delegate.body;
    }

    if (delegate.exception != null) {
      throw delegate.exception;
    }

    throw new Exception("Unknown error.");
  }
}