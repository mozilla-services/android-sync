/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public abstract class SimpleSuccessFetchDelegate implements
    RepositorySessionFetchRecordsDelegate {
  @Override
  public void onFetchFailed(Exception ex, Record record) {
    final AssertionFailedError e = new AssertionFailedError("Fetch failed: " + ex.getMessage());
    e.initCause(ex);
    WaitHelper.getTestWaiter().performNotify(e);
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    for (Record record : records) {
      this.onFetchedRecord(record);
    }
    this.onFetchCompleted(end);
  }

  @Override
  public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
    return this;
  }
}
