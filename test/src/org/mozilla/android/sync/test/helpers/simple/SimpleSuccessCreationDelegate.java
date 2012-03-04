/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.android.sync.test.helpers.simple;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.util.Log;

public abstract class SimpleSuccessCreationDelegate implements RepositorySessionCreationDelegate {
  @Override
  public void onSessionCreateFailed(Exception ex) {
    Log.w("SimpleSuccessCreationDelegate", "Session creation failed.", ex);
    final AssertionFailedError e = new AssertionFailedError("Session creation failed: " + ex.getMessage());
    e.initCause(ex);
    WaitHelper.getTestWaiter().performNotify(e);
  }

  @Override
  public RepositorySessionCreationDelegate deferredCreationDelegate() {
    return this;
  }
}
