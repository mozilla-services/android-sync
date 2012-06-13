/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.repositories.RepositorySession;

/**
 * A synchronizer session that fetches remote records in batches by GUIDs.
 */
public class FillingServerLocalSynchronizerSession extends ServerLocalSynchronizerSession {
  protected final FillingGuidsManager incomingManager;

  public FillingServerLocalSynchronizerSession(final Synchronizer synchronizer, final SynchronizerSessionDelegate delegate, final FillingGuidsManager incomingManager) {
    super(synchronizer, delegate);
    this.incomingManager = incomingManager;
  }

  @Override
  protected RecordsChannel newFirstRecordsChannel(final RepositorySession source, final RepositorySession sink, final RecordsChannelDelegate delegate) {
    return new FillingRecordsChannel(source, sink, delegate, incomingManager);
  }
}
