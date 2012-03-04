/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;

import android.util.Log;

public abstract class SimpleSuccessStoreDelegate implements RepositorySessionStoreDelegate {
  @Override
  public void onRecordStoreFailed(Exception ex) {
    Log.w("SimpleSuccessStoreDelegate", "Store failed.", ex);
    final AssertionFailedError e = new AssertionFailedError("Store failed: " + ex.getMessage());
    e.initCause(ex);
    WaitHelper.getTestWaiter().performNotify(e);
  }

  @Override
  public RepositorySessionStoreDelegate deferredStoreDelegate(ExecutorService executor) {
    return this;
  }
}
