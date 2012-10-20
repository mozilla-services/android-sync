package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public interface FxAccountClientDelegate {
  public void onSuccess(ExtendedJSONObject result);
  public void onFailure(Exception e);
  public void onError(Exception e);
}
