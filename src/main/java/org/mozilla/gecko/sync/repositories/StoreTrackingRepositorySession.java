/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public abstract class StoreTrackingRepositorySession extends RepositorySession {
  private static final String LOG_TAG = "StoreTrackingRepositorySession";
  protected StoreTracker storeTracker;

  protected static StoreTracker createStoreTracker() {
    return new HashSetStoreTracker();
  }

  public StoreTrackingRepositorySession(Repository repository) {
    super(repository);
  }

  @Override
  public void begin(RepositorySessionBeginDelegate delegate) {
    RepositorySessionBeginDelegate deferredDelegate = delegate.deferredBeginDelegate(delegateQueue);
    try {
      super.sharedBegin();
    } catch (InvalidSessionTransitionException e) {
      deferredDelegate.onBeginFailed(e);
      return;
    }
    // Or do this in your own subclass.
    storeTracker = createStoreTracker();
    deferredDelegate.onBeginSucceeded(this);
  }

  @Override
  protected synchronized void trackRecord(Record record) {
    if (this.storeTracker == null) {
      throw new IllegalStateException("Store tracker not yet initialized!");
    }

    Log.d(LOG_TAG, "Tracking record " + record.guid +
                   " (" + record.lastModified + ") to avoid re-upload.");
    // Future: we care about the timestamp…
    this.storeTracker.trackRecordForExclusion(record.guid);
  }

  @Override
  public void abort(RepositorySessionFinishDelegate delegate) {
    this.storeTracker = null;
    super.abort(delegate);
  }

  @Override
  public void finish(RepositorySessionFinishDelegate delegate) {
    this.storeTracker = null;
    super.finish(delegate);
  }
}