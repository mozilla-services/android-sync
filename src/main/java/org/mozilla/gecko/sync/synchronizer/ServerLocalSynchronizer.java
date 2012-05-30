package org.mozilla.gecko.sync.synchronizer;

/**
 * A <code>SynchronizerSession</code> designed to be used between a remote
 * server and a local repository.
 * <p>
 * See <code>ServerLocalSynchronizerSession</code> for error handling details.
 */
public class ServerLocalSynchronizer extends Synchronizer {
  public SynchronizerSession getSynchronizerSession() {
    return new ServerLocalSynchronizerSession(this, this);
  }
}
