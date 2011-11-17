package org.mozilla.android.sync.net;

import org.mozilla.android.sync.net.SyncStorageResponse;

public interface MetaGlobalDelegate {
  public void handleSuccess(MetaGlobal global);
  public void handleMissing(MetaGlobal global);
  public void handleFailure(SyncStorageResponse response);
  public void handleError(Exception e);
}
