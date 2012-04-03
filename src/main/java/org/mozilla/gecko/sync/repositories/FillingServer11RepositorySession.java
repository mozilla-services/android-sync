/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;

/**
 * A <code>Server11RepositorySession</code> that persists a list of guids to
 * fill in over time.
 * <p>
 * We modify </code>fetchSince<code> as follows:
 * <ul>
 * <li>No guids persisted? (I.e., first sync?) Do a guids-only fetch. This will
 * return tons and tons of guids to fill in. Persist these.</li>
 * <li>On every sync, do a limited sortindex-ordered fetch of full
 * records (to catch recent changes) and maybe fetch another batch of records by
 * guid, until the collection of guids to fill in is empty.</li>
 * <ul>
 */
public class FillingServer11RepositorySession extends Server11RepositorySession {
  public static final String LOG_TAG = "FillingServer11RepoSess";

  protected final FillingServer11Repository fillingServerRepository;
  protected final Context context; // For persisting guids.

  public FillingServer11RepositorySession(FillingServer11Repository repository, Context context) {
    super(repository);
    fillingServerRepository = repository;
    this.context = context;
  }

  protected void safelyPersistGuidsToFill(String[] guids) {
    try {
      fillingServerRepository.persistGuidsRemaining(guids, context);
      Logger.debug(LOG_TAG, "Persisted " + guids.length + " old guids.");
    } catch (Exception e) {
      Logger.debug(LOG_TAG, "Got exception persisting " + guids.length + " old guids.", e);
    }
  }

  public class FillGuidsRequestFetchDelegateAdapter extends RequestFetchDelegateAdapter {
    protected long newer;
    public final ArrayList<String> remaining;

    public FillGuidsRequestFetchDelegateAdapter(RepositorySessionFetchRecordsDelegate delegate, final String[] guids, final long newer) {
      super(delegate);
      this.remaining = new ArrayList<String>(Arrays.asList(guids));
      this.newer = newer;
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      // Remove records we have received so that we don't fill again (if the
      // store fails, we don't want to repeat it).
      remaining.remove(record.guid); // Could be improved with a sorted list implementation.
      super.handleWBO(record);
    }

    public void finish(int expected, int gotten, long end) {
      if (expected >= 0 && expected != gotten) {
        Logger.debug(LOG_TAG, "Expected "
            + expected + " records but got "
            + gotten + " records, failing.");
        delegate.onFetchFailed(null, null);
        return;
      }
      Logger.debug(LOG_TAG, "Expected and got " + expected + " records from initial fetchSince(" + newer + ").");

      // Successful fetch, with actual records? Persist what's left, since we
      // may have cleared the queue by downloading recently modified.
      String[] guidsRemaining = remaining.toArray(new String[0]);
      if (gotten > 0) {
        safelyPersistGuidsToFill(guidsRemaining);
      }

      final String[] guidsToFillList = fillingServerRepository.guidsToFillThisSession(guidsRemaining, gotten);
      if (guidsToFillList == null || guidsToFillList.length == 0) {
        Logger.debug(LOG_TAG, "Not given any old guids to fill in; running delayed onFetchCompleted.");
        delegate.onFetchCompleted(end);
        return;
      }
      Logger.debug(LOG_TAG, "Attempting to fill in " + guidsToFillList.length + " old guids, running fetch.");

      RepositorySessionFetchRecordsDelegate persistingDelegate = new RepositorySessionFetchRecordsDelegate() {
        @Override
        public void onFetchedRecord(Record record) {
          delegate.onFetchedRecord(record);
        }

        @Override
        public void onFetchFailed(Exception ex, Record record) {
          delegate.onFetchFailed(ex, record);
        }

        @Override
        public void onFetchCompleted(long fetchEnd) {
          // Remove records we have asked for, even if not received, so that bad
          // guids (that are ignored by the server) don't pile up.
          for (String guid : guidsToFillList) {
            remaining.remove(guid); // Could be improved with a sorted list implementation.
          }

          String[] rs = remaining.toArray(new String[0]);
          safelyPersistGuidsToFill(rs);
          delegate.onFetchCompleted(fetchEnd);
        }

        @Override
        public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
          return null;
        }
      };

      fetch(guidsToFillList, persistingDelegate);
    }
  }

  /**
   * A fetchSince that might first kick off a guidsSince to generate a persisted
   * list of guids to fill.
   */
  @Override
  public void fetchSince(final long timestamp, final RepositorySessionFetchRecordsDelegate delegate) {
    RepositorySessionGuidsSinceDelegate guidsSinceDelegate = new RepositorySessionGuidsSinceDelegate() {
      @Override
      public void onGuidsSinceFailed(Exception ex) {
        Logger.warn(LOG_TAG, "Got exception in guidsSince fetching old guids to fill in; continuing to fetchSinceHelper.", ex);
        fetchSinceHelper(timestamp, delegate, new String[] { }); // Empty list => don't try to fill after fetchSince.
      }

      @Override
      public void onGuidsSinceSucceeded(String[] guids) {
        safelyPersistGuidsToFill(guids);
        fetchSinceHelper(timestamp, delegate, guids);
      }
    };

    String[] guids;
    try {
      guids = fillingServerRepository.guidsRemaining(context);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception trying to get persisted guids; continuing.", e);
      guids = null;
    }

    if (guids == null) {
      // No guids persisted.  Either first sync or error; in any case, try to get all old guids.
      Logger.debug(LOG_TAG, "No old guids persisted; running guidsSince(0).");
      guidsSince(0, guidsSinceDelegate);
      return;
    }
    Logger.debug(LOG_TAG, "Got " + guids.length + " persisted guids to fill in.");

    // We found persisted guids.  Do a regular fetchSince, possibly followed by a fill.
    fetchSinceHelper(timestamp, delegate, guids);
  }

  /**
   * Kick off a regular fetchSince that requests a fill-in afterwards if there
   * are remaining guids to fill.
   */
  protected void fetchSinceHelper(long timestamp, RepositorySessionFetchRecordsDelegate delegate, String[] remainingGuids) {
    RequestFetchDelegateAdapter adapter;

    if (remainingGuids == null) {
      throw new IllegalArgumentException("null guids passed in to fetchSinceHelper!");
    }
    if (remainingGuids.length > 0) {
      Logger.debug(LOG_TAG, "fetchSinceHelper given " + remainingGuids.length + " old guids still needing fill in;" +
          " using FillGuidsRequestFetchDelegateAdapter in fetchSince(" + timestamp + ").");
      adapter = new FillGuidsRequestFetchDelegateAdapter(delegate, remainingGuids, timestamp);
    } else {
      Logger.debug(LOG_TAG, "fetchSinceHelper given no old guids needing fill in;" +
          " using RequestFetchDelegateAdapter in fetchSince(" + timestamp + ").");
      adapter = new RequestFetchDelegateAdapter(delegate);
    }

    try {
      long limit = serverRepository.getDefaultFetchLimit();
      String sort = serverRepository.getDefaultSort();
      this.fetchWithParameters(timestamp, limit, true, sort, null, adapter);
    } catch (URISyntaxException e) {
      delegate.onFetchFailed(e, null);
    }
  }
}
