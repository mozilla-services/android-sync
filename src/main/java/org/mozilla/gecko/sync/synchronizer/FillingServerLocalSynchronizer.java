package org.mozilla.gecko.sync.synchronizer;

/**
 * A <code>Synchronizer</code> that downloads fresh GUIDs and then downloads
 * records in batches.
 */
public class FillingServerLocalSynchronizer extends ServerLocalSynchronizer {
  protected final FillingGuidsManager manager;

  public FillingServerLocalSynchronizer(final FillingGuidsManager manager) {
    super();
    this.manager = manager;
  }

  public SynchronizerSession getSynchronizerSession() {
    return new FillingServerLocalSynchronizerSession(this, this, manager);
  }
}
