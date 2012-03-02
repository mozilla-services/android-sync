/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.android.sync.test.helpers.simple;

import org.mozilla.android.sync.test.helpers.DefaultDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

public abstract class SimpleSuccessCreationDelegate extends DefaultDelegate implements RepositorySessionCreationDelegate {
  @Override
  public void onSessionCreateFailed(Exception ex) {
    performNotify("Session creation failed", ex);
  }

  @Override
  public RepositorySessionCreationDelegate deferredCreationDelegate() {
    return this;
  }
}
