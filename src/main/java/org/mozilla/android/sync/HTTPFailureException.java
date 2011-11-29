package org.mozilla.android.sync;

import org.mozilla.android.sync.net.SyncStorageResponse;

public class HTTPFailureException extends SyncException {
  private static final long serialVersionUID = -5415864029780770619L;
  public SyncStorageResponse response;

  public HTTPFailureException(SyncStorageResponse response) {
    this.response = response;
  }
}
