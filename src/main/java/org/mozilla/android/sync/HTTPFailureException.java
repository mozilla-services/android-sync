package org.mozilla.android.sync;

import org.mozilla.android.sync.net.SyncStorageResponse;

import android.content.SyncResult;

public class HTTPFailureException extends SyncException {
  private static final long serialVersionUID = -5415864029780770619L;
  public SyncStorageResponse response;

  public HTTPFailureException(SyncStorageResponse response) {
    this.response = response;
  }

  @Override
  public void updateStats(GlobalSession globalSession, SyncResult syncResult) {
    switch (response.getStatusCode()) {
    case 401:
      // Node reassignment 401s get handled internally.
      syncResult.stats.numAuthExceptions++;
      return;
    case 500:
    case 501:
    case 503:
      // TODO: backoff.
      syncResult.stats.numIoExceptions++;
      return;
    }
  }
}
