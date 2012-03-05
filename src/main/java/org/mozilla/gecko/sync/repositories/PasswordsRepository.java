/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.repositories.android.PasswordsRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

/**
 * Shared interface for repositories that consume and produce
 * password records.
 *
 * @author liuche
 *
 */
public class PasswordsRepository extends Repository {

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate,
      Context context) {
    PasswordsRepositorySession session = new PasswordsRepositorySession(PasswordsRepository.this, context);
    final RepositorySessionCreationDelegate deferredCreationDelegate = delegate.deferredCreationDelegate();
    deferredCreationDelegate.onSessionCreated(session);
  }

}
