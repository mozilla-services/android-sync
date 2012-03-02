/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.AndroidBrowserRepositoryTest;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;

public abstract class SimpleSuccessStoreDelegate implements RepositorySessionStoreDelegate {
  @Override
  public void onRecordStoreFailed(Exception ex) {
    AndroidBrowserRepositoryTest.performNotify("Store failed", ex);
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService executor) {
    return this;
  }
}
