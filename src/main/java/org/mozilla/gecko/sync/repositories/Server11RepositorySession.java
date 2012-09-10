/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.DelayedWorkTracker;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Server11PreviousPostFailedException;
import org.mozilla.gecko.sync.Server11RecordPostFailedException;
import org.mozilla.gecko.sync.UnexpectedJSONException;
import org.mozilla.gecko.sync.net.ByteArraysEntity;
import org.mozilla.gecko.sync.net.SyncServer11CollectionRequest;
import org.mozilla.gecko.sync.net.SyncServer11CollectionRequestDelegate;
import org.mozilla.gecko.sync.net.SyncServer11RecordRequest;
import org.mozilla.gecko.sync.net.SyncServer11RequestDelegate;
import org.mozilla.gecko.sync.net.SyncServer11Response;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class Server11RepositorySession extends RepositorySession {
  public static final String LOG_TAG = "Server11Session";

  protected static final int UPLOAD_BYTE_THRESHOLD = 1024 * 1024 - 512;    // 1MB minus some characters for wrapping in a JSON array.
  protected static final int UPLOAD_ITEM_THRESHOLD = 50;

  /**
   * Used to track outstanding requests, so that we can abort them as needed.
   */
  private Set<SyncServer11CollectionRequest> pending = Collections.synchronizedSet(new HashSet<SyncServer11CollectionRequest>());

  @Override
  public void abort() {
    super.abort();
    for (SyncServer11CollectionRequest request : pending) {
      request.abort();
    }
    pending.clear();
  }

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
    private SyncServer11CollectionRequest request;

    public AtomicInteger numRecordsProcessed = new AtomicInteger(0);

    public void setRequest(SyncServer11CollectionRequest request) {
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
    public String credentials() {
      return serverRepository.credentialsSource.credentials();
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncServer11Response response) {
      Logger.debug(LOG_TAG, "Fetch done.");
      removeRequestFromPending();

      final long normalizedTimestamp = response.getNormalizedTimestamp();
      Logger.debug(LOG_TAG, "Fetch completed. Timestamp is " + normalizedTimestamp);

      final AtomicInteger numRecordsExpected = new AtomicInteger(-1);
      try {
        numRecordsExpected.set(response.weaveRecords());
      } catch (NumberFormatException e) {
        // We just won't verify.
      }

      // When we're done processing other events, finish.
      workTracker.delayWorkItem(new Runnable() {
        @Override
        public void run() {
          finish(numRecordsExpected.get(), numRecordsProcessed.get(), normalizedTimestamp);
        }
      });
    }

    /**
     * Called after a successful request.
     *
     * @param expected The number of records expected, based on the server's response.
     * @param gotten The number of records received and processed.
     * @param end The timestamp of the server's response.
     */
    public void finish(int expected, int gotten, long end) {
      if (expected >= 0 && expected != gotten) {
        Logger.debug(LOG_TAG, "Expected "
            + expected + " records but got "
            + gotten + " records, failing.");
        delegate.onFetchFailed(null, null);
        return;
      }

      Logger.debug(LOG_TAG, "Expected and got " + expected + " records; running delayed onFetchCompleted.");
      delegate.onFetchCompleted(end);
    }

    @Override
    public void handleRequestFailure(SyncServer11Response response) {
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
        numRecordsProcessed.incrementAndGet();
      } catch (Exception ex) {
        Logger.warn(LOG_TAG, "Got exception calling onFetchedRecord with WBO.", ex);
        // TODO: handle this better.
        throw new RuntimeException(ex);
      } finally {
        workTracker.decrementOutstanding();
      }
    }
  }

  Server11Repository serverRepository;
  AtomicLong uploadTimestamp = new AtomicLong(0);

  private void bumpUploadTimestamp(long ts) {
    while (true) {
      long existing = uploadTimestamp.get();
      if (existing > ts) {
        return;
      }
      if (uploadTimestamp.compareAndSet(existing, ts)) {
        return;
      }
    }
  }

  public Server11RepositorySession(Repository repository) {
    super(repository);
    serverRepository = (Server11Repository) repository;
  }

  /**
   * URL-encode the provided string. If the input is null,
   * the empty string is returned.
   *
   * @param in the string to encode.
   * @return a URL-encoded version of the input.
   */
  protected static String encode(String in) {
    if (in == null) {
      return "";
    }
    try {
      return URLEncoder.encode(in, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Will never occur.
      return null;
    }
  }

  private String flattenIDs(String[] guids) {
    // Consider using Utils.toDelimitedString if and when the signature changes
    // to Collection<String> guids.
    if (guids.length == 0) {
      return "";
    }
    if (guids.length == 1) {
      return guids[0];
    }
    StringBuilder b = new StringBuilder();
    for (String guid : guids) {
      b.append(encode(guid));
      b.append(",");
    }
    return b.substring(0, b.length() - 1);
  }

  public class RequestGuidsDelegateAdapter extends SyncServer11CollectionRequestDelegate {
    public ArrayList<String> guids = new ArrayList<String>();

    public RepositorySessionGuidsSinceDelegate delegate = null;

    public RequestGuidsDelegateAdapter(RepositorySessionGuidsSinceDelegate delegate) {
      this.delegate = delegate;
    }

    // So that we can clean up.
    private SyncServer11CollectionRequest request;

    public void setRequest(SyncServer11CollectionRequest request) {
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
    public void handleRequestProgress(String progress) {
      try {
        guids.add((String) ExtendedJSONObject.parse(progress));
      } catch (IOException e) {
      } catch (ParseException e) {
      }
    }

    @Override
    public String credentials() {
      return serverRepository.credentialsSource.credentials();
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncServer11Response response) {
      Logger.debug(LOG_TAG, "guidsSince done.");
      String[] guidsArray = new String[guids.size()];
      guids.toArray(guidsArray);
      delegate.onGuidsSinceSucceeded(guidsArray);
    }

    @Override
    public void handleRequestFailure(SyncServer11Response response) {
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
    URI collectionURI;
    try {
      String sort = serverRepository.getDefaultSort();
      collectionURI = serverRepository.collectionURI(false, timestamp, -1, sort, null);
    } catch (URISyntaxException e) {
      delegate.onGuidsSinceFailed(e);
      return;
    }

    SyncServer11CollectionRequest request = new SyncServer11CollectionRequest(collectionURI);
    RequestGuidsDelegateAdapter adapter = new RequestGuidsDelegateAdapter(delegate);
    // So it can clean up.
    adapter.setRequest(request);
    request.delegate = adapter;
    pending.add(request);
    request.get();
  }

  protected void fetchWithParameters(long newer,
                                     long limit,
                                     boolean full,
                                     String sort,
                                     String ids,
                                     RequestFetchDelegateAdapter delegate)
                                         throws URISyntaxException {

    URI collectionURI = serverRepository.collectionURI(full, newer, limit, sort, ids);
    SyncServer11CollectionRequest request = new SyncServer11CollectionRequest(collectionURI);
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
      long limit = -1;
      String sort = serverRepository.getDefaultSort();
      this.fetchWithParameters(-1, limit, true, sort, ids, new RequestFetchDelegateAdapter(delegate));
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

  protected Object recordsBufferMonitor = new Object();

  /**
   * Data of outbound records.
   * <p>
   * We buffer the data (rather than the <code>Record</code>) so that we can
   * flush the buffer based on outgoing transmission size.
   * <p>
   * Access should be synchronized on <code>recordsBufferMonitor</code>.
   */
  protected ArrayList<byte[]> recordsBuffer = new ArrayList<byte[]>();

  /**
   * GUIDs of outbound records.
   * <p>
   * Used to fail entire outgoing uploads.
   * <p>
   * Access should be synchronized on <code>recordsBufferMonitor</code>.
   */
  protected ArrayList<String> recordGuidsBuffer = new ArrayList<String>();
  protected int byteCount = 0;

  @Override
  public void store(Record record) throws NoStoreDelegateException {
    if (delegate == null) {
      throw new NoStoreDelegateException();
    }
    this.enqueue(record);
  }

  /**
   * Batch incoming records until some reasonable threshold (e.g., 50),
   * some size limit is hit (probably way less than 3MB!), or storeDone
   * is received.
   * @param record
   */
  protected void enqueue(Record record) {
    // JSONify and store the bytes, rather than the record.
    byte[] json = record.toJSONBytes();
    int delta   = json.length;
    synchronized (recordsBufferMonitor) {
      if ((delta + byteCount     > UPLOAD_BYTE_THRESHOLD) ||
          (recordsBuffer.size() >= UPLOAD_ITEM_THRESHOLD)) {

        // POST the existing contents, then enqueue.
        flush();
      }
      recordsBuffer.add(json);
      recordGuidsBuffer.add(record.guid);
      byteCount += delta;
    }
  }

  // Asynchronously upload records.
  // Must be locked!
  protected void flush() {
    if (recordsBuffer.size() > 0) {
      final ArrayList<byte[]> outgoing = recordsBuffer;
      final ArrayList<String> outgoingGuids = recordGuidsBuffer;
      RepositorySessionStoreDelegate uploadDelegate = this.delegate;
      storeWorkQueue.execute(new RecordUploadRunnable(uploadDelegate, outgoing, outgoingGuids, byteCount));

      recordsBuffer = new ArrayList<byte[]>();
      recordGuidsBuffer = new ArrayList<String>();
      byteCount = 0;
    }
  }

  @Override
  public void storeDone() {
    Logger.debug(LOG_TAG, "storeDone().");
    synchronized (recordsBufferMonitor) {
      flush();
      // Do this in a Runnable so that the timestamp is grabbed after any upload.
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          synchronized (recordsBufferMonitor) {
            final long end = uploadTimestamp.get();
            Logger.debug(LOG_TAG, "Calling storeDone with " + end);
            storeDone(end);
          }
        }
      };
      storeWorkQueue.execute(r);
    }
  }

  /**
   * <code>true</code> if a record upload has failed this session.
   * <p>
   * This is only set in begin and possibly by <code>RecordUploadRunnable</code>.
   * Since those are executed serially, we can use an unsynchronized
   * volatile boolean here.
   */
  protected volatile boolean recordUploadFailed;

  public void begin(RepositorySessionBeginDelegate delegate) throws InvalidSessionTransitionException {
    recordUploadFailed = false;
    super.begin(delegate);
  }

  /**
   * Make an HTTP request, and convert HTTP request delegate callbacks into
   * store callbacks within the context of this RepositorySession.
   *
   * @author rnewman
   *
   */
  protected class RecordUploadRunnable implements Runnable, SyncServer11RequestDelegate {

    public final String LOG_TAG = "RecordUploadRunnable";
    private ArrayList<byte[]> outgoing;
    private ArrayList<String> outgoingGuids;
    private long byteCount;

    public RecordUploadRunnable(RepositorySessionStoreDelegate storeDelegate,
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
    public String credentials() {
      return serverRepository.credentialsSource.credentials();
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncServer11Response response) {
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
    public void handleRequestFailure(SyncServer11Response response) {
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
      SyncServer11RecordRequest request = new SyncServer11RecordRequest(u);

      request.delegate = this;

      // We don't want the task queue to proceed until this request completes.
      // Fortunately, BaseResource is currently synchronous.
      // If that ever changes, you'll need to block here.
      ByteArraysEntity body = getBodyEntity();
      request.post(body);
    }
  }
}
