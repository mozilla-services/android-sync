/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.DelayedWorkTracker;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Server11PreviousPostFailedException;
import org.mozilla.gecko.sync.Server11RecordPostFailedException;
import org.mozilla.gecko.sync.UnexpectedJSONException;
import org.mozilla.gecko.sync.net.ByteArraysEntity;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRequestIncrementalDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;

public class Server11RepositorySession extends ServerRepositorySession {
  public static final String LOG_TAG = "Server11RepoSession";

  /**
   * Convert HTTP request delegate callbacks into fetch callbacks within the
   * context of this RepositorySession.
   *
   * @author rnewman
   *
   */
  public class RequestFetchDelegateAdapter extends WBOCollectionRequestDelegate {
    RepositorySessionFetchRecordsDelegate delegate;
    private DelayedWorkTracker workTracker = new DelayedWorkTracker();

    // So that we can clean up.
    private SyncStorageCollectionRequest request;

    public void setRequest(SyncStorageCollectionRequest request) {
      this.request = request;
    }

    private void removeRequestFromPending() {
      if (this.request == null) {
        return;
      }
      pending.remove(this.request);
      this.request = null;
    }

    public RequestFetchDelegateAdapter(RepositorySessionFetchRecordsDelegate delegate) {
      this.delegate = delegate;
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

  public Server11RepositorySession(Repository repository) {
    super(repository);
  }

  public class RequestGuidsDelegateAdapter implements SyncStorageRequestIncrementalDelegate {
    public ArrayList<String> guids = new ArrayList<String>();

    public RepositorySessionGuidsSinceDelegate delegate = null;

    public RequestGuidsDelegateAdapter(RepositorySessionGuidsSinceDelegate delegate) {
      this.delegate = delegate;
    }

    // So that we can clean up.
    private SyncStorageCollectionRequest request;

    public void setRequest(SyncStorageCollectionRequest request) {
      this.request = request;
    }

    private void removeRequestFromPending() {
      if (this.request == null) {
        return;
      }
      pending.remove(this.request);
      this.request = null;
    }

    @Override
    public void handleRequestProgress(String progress) throws Exception {
      guids.add((String) ExtendedJSONObject.parse(progress));
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      Logger.debug(LOG_TAG, "guidsSince done.");
      String[] guidsArray = new String[guids.size()];
      guids.toArray(guidsArray);
      delegate.onGuidsSinceSucceeded(guidsArray);
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      this.handleRequestError(new HTTPFailureException(response));
    }

    @Override
    public void handleRequestError(Exception ex) {
      removeRequestFromPending();
      Logger.warn(LOG_TAG, "guidsSince got error.", ex);
      delegate.onGuidsSinceFailed(ex);
    }
  }

  @Override
  public void guidsSince(long timestamp,
                         RepositorySessionGuidsSinceDelegate delegate) {
    // TODO Auto-generated method stub

  }

  protected void fetchWithParameters(long newer,
                                     long limit,
                                     boolean full,
                                     String sort,
                                     String ids,
                                     RequestFetchDelegateAdapter delegate)
                                         throws URISyntaxException {

    URI collectionURI = serverRepository.collectionURI(full, newer, limit, sort, ids);
    SyncStorageCollectionRequest request = new SyncStorageCollectionRequest(collectionURI, serverRepository.credentialsSource);
    request.delegate = delegate;

    // So it can clean up.
    delegate.setRequest(request);
    pending.add(request);
    request.get();
  }

  @Override
  public void fetchSince(long timestamp,
                         RepositorySessionFetchRecordsDelegate delegate) {
    try {
      long limit = serverRepository.getDefaultFetchLimit();
      String sort = serverRepository.getDefaultSort();
      this.fetchWithParameters(timestamp, limit, true, sort, null, new RequestFetchDelegateAdapter(delegate));
    } catch (URISyntaxException e) {
      delegate.onFetchFailed(e, null);
    }
  }

  @Override
  public void fetchAll(RepositorySessionFetchRecordsDelegate delegate) {
    this.fetchSince(-1, delegate);
  }

  @Override
  public void fetch(String[] guids,
                    RepositorySessionFetchRecordsDelegate delegate) {
    // TODO: watch out for URL length limits!
    try {
      String ids = flattenIDs(guids);
      this.fetchWithParameters(-1, -1, true, "index", ids, new RequestFetchDelegateAdapter(delegate));
    } catch (URISyntaxException e) {
      delegate.onFetchFailed(e, null);
    }
  }

  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    if (!isActive()) {
      delegate.onWipeFailed(new InactiveSessionException(null));
      return;
    }
    // TODO: implement wipe.
  }

  protected Runnable makeUploadRunnable(RepositorySessionStoreDelegate storeDelegate,
      ArrayList<byte[]> outgoing,
      ArrayList<String> outgoingGuids,
      long byteCount) {
    return new Server11RecordUploadRunnable(storeDelegate, outgoing, outgoingGuids, byteCount);
  }

  /**
   * Make an HTTP request, and convert HTTP request delegate callbacks into
   * store callbacks within the context of this RepositorySession.
   *
   * @author rnewman
   *
   */
  protected class Server11RecordUploadRunnable implements Runnable, SyncStorageRequestDelegate {
    public static final String LOG_TAG = "Server11RecordUploadRunnable";

    private ArrayList<byte[]> outgoing;
    private ArrayList<String> outgoingGuids;
    private long byteCount;

    public Server11RecordUploadRunnable(RepositorySessionStoreDelegate storeDelegate,
                                ArrayList<byte[]> outgoing,
                                ArrayList<String> outgoingGuids,
                                long byteCount) {
      Logger.debug(LOG_TAG, "Preparing record upload for " +
                  outgoing.size() + " records (" +
                  byteCount + " bytes).");
      this.outgoing = outgoing;
      this.outgoingGuids = outgoingGuids;
      this.byteCount = byteCount;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      Logger.trace(LOG_TAG, "POST of " + outgoing.size() + " records done.");

      ExtendedJSONObject body;
      try {
        body = response.jsonObjectBody(); // jsonObjectBody() throws or returns non-null.
      } catch (Exception e) {
        Logger.error(LOG_TAG, "Got exception parsing POST success body.", e);
        this.handleRequestError(e);
        return;
      }

      // Be defensive when logging timestamp.
      if (body.containsKey("modified")) {
        Long modified = body.getTimestamp("modified");
        if (modified != null) {
          Logger.trace(LOG_TAG, "POST request success. Modified timestamp: " + modified.longValue());
        } else {
          Logger.warn(LOG_TAG, "POST success body contains malformed 'modified': " + body.toJSONString());
        }
      } else {
        Logger.warn(LOG_TAG, "POST success body does not contain key 'modified': " + body.toJSONString());
      }

      try {
        JSONArray          success = body.getArray("success");
        if ((success != null) &&
            (success.size() > 0)) {
          Logger.trace(LOG_TAG, "Successful records: " + success.toString());
          for (Object o : success) {
            try {
              delegate.onRecordStoreSucceeded((String) o);
            } catch (ClassCastException e) {
              Logger.error(LOG_TAG, "Got exception parsing POST success guid.", e);
              // Not much to be done.
            }
          }

          long normalizedTimestamp = response.getNormalizedTimestamp();
          Logger.trace(LOG_TAG, "Passing back upload X-Weave-Timestamp: " + normalizedTimestamp);
          bumpUploadTimestamp(normalizedTimestamp);
        }
        success = null; // Want to GC this ASAP.

        ExtendedJSONObject failed  = body.getObject("failed");
        if ((failed != null) &&
            (failed.object.size() > 0)) {
          Logger.debug(LOG_TAG, "Failed records: " + failed.object.toString());
          Exception ex = new Server11RecordPostFailedException();
          for (String guid : failed.keySet()) {
            delegate.onRecordStoreFailed(ex, guid);
          }
        }
        failed = null; // Want to GC this ASAP.
      } catch (UnexpectedJSONException e) {
        Logger.error(LOG_TAG, "Got exception processing success/failed in POST success body.", e);
        // TODO
        return;
      }
      Logger.debug(LOG_TAG, "POST of " + outgoing.size() + " records handled.");
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      // TODO: call session.interpretHTTPFailure.
      this.handleRequestError(new HTTPFailureException(response));
    }

    @Override
    public void handleRequestError(final Exception ex) {
      Logger.warn(LOG_TAG, "Got request error.", ex);

      recordUploadFailed = true;
      ArrayList<String> failedOutgoingGuids = outgoingGuids;
      outgoingGuids = null; // Want to GC this ASAP.
      for (String guid : failedOutgoingGuids) {
        delegate.onRecordStoreFailed(ex, guid);
      }
      return;
    }

    public ByteArraysEntity getBodyEntity() {
      ByteArraysEntity body = new ByteArraysEntity(outgoing, byteCount);
      return body;
    }

    @Override
    public void run() {
      if (recordUploadFailed) {
        Logger.info(LOG_TAG, "Previous record upload failed.  Failing all records and not retrying.");
        Exception ex = new Server11PreviousPostFailedException();
        for (String guid : outgoingGuids) {
          delegate.onRecordStoreFailed(ex, guid);
        }
        return;
      }

      if (outgoing == null ||
          outgoing.size() == 0) {
        Logger.debug(LOG_TAG, "No items: RecordUploadRunnable returning immediately.");
        return;
      }

      URI u = serverRepository.collectionURI();
      SyncStorageRecordRequest request = new SyncStorageRecordRequest(u, serverRepository.credentialsSource);
      request.delegate = this;

      // We don't want the task queue to proceed until this request completes.
      // Fortunately, BaseResource is currently synchronous.
      // If that ever changes, you'll need to block here.
      ByteArraysEntity body = getBodyEntity();
      request.post(body);
    }
  }
}
