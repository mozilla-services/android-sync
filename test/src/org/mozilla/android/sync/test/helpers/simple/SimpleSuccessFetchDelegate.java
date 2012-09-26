/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.helpers.DefaultDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

public abstract class SimpleSuccessFetchDelegate extends DefaultDelegate implements
    RepositorySessionFetchRecordsDelegate {
  @Override
  public void onFetchFailed(Exception ex, Record record) {
    performNotify("Fetch failed", ex);
  }

  @Override
  public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(ExecutorService executor) {
    return this;
  }
}
