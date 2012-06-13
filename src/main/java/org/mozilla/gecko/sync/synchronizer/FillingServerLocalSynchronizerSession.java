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
