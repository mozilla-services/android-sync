package org.mozilla.gecko.sync.repositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.DelayedWorkTracker;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;

import ch.boye.httpclientandroidlib.HttpStatus;

public class Server20RepositorySession extends ServerRepositorySession {
  public static final String LOG_TAG = "Server20RepoSess";

  public Server20RepositorySession(final Repository repository) {
    super(repository);
  }

  @Override
  protected Runnable makeUploadRunnable(
      RepositorySessionStoreDelegate storeDelegate, ArrayList<byte[]> outgoing,
      ArrayList<String> outgoingGuids, long byteCount) {

    return new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "Uploading not yet implemented.");
      }
    };
  }

  /**
   * Convert Sync protocol 2.0 HTTP request delegate callbacks into fetch
   * callbacks.
   */
  protected static class Server20RequestFetchDelegateAdapter extends WBOCollectionRequestDelegate {
    protected final RepositorySessionFetchRecordsDelegate delegate;

    protected DelayedWorkTracker workTracker = new DelayedWorkTracker();

    // So that we can clean up.
    protected SyncStorageRequest request;

    public Server20RequestFetchDelegateAdapter(RepositorySessionFetchRecordsDelegate delegate) {
      this.delegate = delegate;
    }

    public void setRequest(SyncStorageRequest request) {
      this.request = request;
    }

    private void removeRequestFromPending() {
      if (this.request == null) {
        return;
      }
      // XXX pending.remove(this.request);
      this.request = null;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      Logger.debug(LOG_TAG, "Fetch done.");
      removeRequestFromPending();

      final long normalizedTimestamp = response.getNormalizedTimestamp();
      Logger.debug(LOG_TAG, "Fetch completed. Timestamp is " + normalizedTimestamp);

      // When we're done processing other events, finish.
      workTracker.delayWorkItem(new Runnable() {
        @Override
        public void run() {
          Logger.debug(LOG_TAG, "Delayed onFetchCompleted running.");
          // TODO: verify number of returned records.
          delegate.onFetchCompleted(normalizedTimestamp);
        }
      });
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      removeRequestFromPending();

      if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
        // This isn't failure at all!  Just no data to fetch.
        this.handleRequestSuccess(response);
        return;
      }

      // TODO: ensure that delegate methods don't get called more than once.
      this.handleRequestError(new HTTPFailureException(response));
    }

    @Override
    public void handleRequestError(final Exception ex) {
      removeRequestFromPending();
      Logger.warn(LOG_TAG, "Got request error.", ex);
      // When we're done processing other events, finish.
      workTracker.delayWorkItem(new Runnable() {
        @Override
        public void run() {
          Logger.debug(LOG_TAG, "Running onFetchFailed.");
          delegate.onFetchFailed(ex, null);
        }
      });
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      workTracker.incrementOutstanding();
      try {
        delegate.onFetchedRecord(record);
      } catch (Exception ex) {
        Logger.warn(LOG_TAG, "Got exception calling onFetchedRecord with WBO.", ex);
        // TODO: handle this better.
        throw new RuntimeException(ex);
      } finally {
        workTracker.decrementOutstanding();
      }
    }
  }

  protected void fetchWithParameters(long newer,
      long limit,
      boolean full,
      String sort,
      String ids,
      RepositorySessionFetchRecordsDelegate delegate)
          throws URISyntaxException {

    URI collectionURI = serverRepository.collectionURI(full, newer, limit, sort, ids);
    SyncStorageRequest request = new SyncStorageCollectionRequest(collectionURI, serverRepository.credentialsSource);
    request.locallyModifiedVersion = Long.valueOf(newer);

    Server20RequestFetchDelegateAdapter delegateAdapter = new Server20RequestFetchDelegateAdapter(delegate);
    request.delegate = delegateAdapter;

    // So it can clean up.
    delegateAdapter.setRequest(request);
    pending.add(request);
    request.get();
  }

  protected void innerFetchSince(final long timestamp,
      final RepositorySessionFetchRecordsDelegate delegate) {
    try {
      long limit = serverRepository.getDefaultFetchLimit();
      String sort = serverRepository.getDefaultSort();
      this.fetchWithParameters(timestamp, limit, true, sort, null, delegate);
    } catch (URISyntaxException e) {
      delegate.onFetchFailed(e, null);
    }
  }

  @Override
  public void fetchSince(final long timestamp,
      final RepositorySessionFetchRecordsDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        innerFetchSince(timestamp, delegate);
      }
    });
  }

  @Override
  public void fetchAll(final RepositorySessionFetchRecordsDelegate delegate) {
    fetchSince(0, delegate);
  }

  @Override
  public void fetch(final String[] guids,
      final RepositorySessionFetchRecordsDelegate delegate)
      throws InactiveSessionException {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "fetch not yet implemented.");
        delegate.onFetchCompleted(System.currentTimeMillis());
      }
    });
  }


  @Override
  public void guidsSince(final long timestamp,
      final RepositorySessionGuidsSinceDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "guidsSince not yet implemented.");
        delegate.onGuidsSinceSucceeded(new String[] {});
      }
    });
  }

  @Override
  public void wipe(final RepositorySessionWipeDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "wipe not yet implemented.");
        delegate.onWipeSucceeded();
      }
    });
  }
}
