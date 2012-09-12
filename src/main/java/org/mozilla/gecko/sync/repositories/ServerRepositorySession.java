package org.mozilla.gecko.sync.repositories;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public abstract class ServerRepositorySession extends RepositorySession {
  public static final String LOG_TAG = "ServerRepoSession";

  protected static final int UPLOAD_BYTE_THRESHOLD = 1024 * 1024 - 512;
  protected static final int UPLOAD_ITEM_THRESHOLD = 50;

  protected final ServerRepository serverRepository;

  protected final AtomicLong uploadTimestamp = new AtomicLong(0);

  /**
   * Used to track outstanding requests, so that we can abort them as needed.
   */
  protected Set<SyncStorageRequest> pending = Collections.synchronizedSet(new HashSet<SyncStorageRequest>());

  public ServerRepositorySession(final Repository repository) {
    super(repository);
    serverRepository = (ServerRepository) repository;
  }

  protected abstract Runnable makeUploadRunnable(RepositorySessionStoreDelegate storeDelegate,
      ArrayList<byte[]> outgoing,
      ArrayList<String> outgoingGuids,
      long byteCount);

  @Override
  public void abort() {
    super.abort();
    for (SyncStorageRequest request : pending) {
      request.abort();
    }
    pending.clear();
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

  protected void bumpUploadTimestamp(long ts) {
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

  protected String flattenIDs(String[] guids) {
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
  /**
   * <code>true</code> if a record upload has failed this session.
   * <p>
   * This is only set in begin and possibly by <code>RecordUploadRunnable</code>.
   * Since those are executed serially, we can use an unsynchronized
   * volatile boolean here.
   */
  protected volatile boolean recordUploadFailed;

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

  protected void flush() {
    if (recordsBuffer.size() > 0) {
      final ArrayList<byte[]> outgoing = recordsBuffer;
      final ArrayList<String> outgoingGuids = recordGuidsBuffer;
      RepositorySessionStoreDelegate uploadDelegate = this.delegate;
      storeWorkQueue.execute(makeUploadRunnable(uploadDelegate, outgoing, outgoingGuids, byteCount));

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

  public void begin(RepositorySessionBeginDelegate delegate) throws InvalidSessionTransitionException {
    recordUploadFailed = false;
    super.begin(delegate);
  }
}
