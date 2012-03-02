/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.android.sync.test.helpers.simple;

import java.util.concurrent.ExecutorService;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;

public abstract class SimpleSuccessBeginDelegate implements RepositorySessionBeginDelegate {
  @Override
  public void onBeginFailed(Exception ex) {
    final AssertionFailedError e = new AssertionFailedError("Begin failed: " + ex.getMessage());
    e.initCause(ex);
    WaitHelper.getTestWaiter().performNotify(e);
  }

  @Override
  public RepositorySessionBeginDelegate deferredBeginDelegate(ExecutorService executor) {
    return this;
  }
}
