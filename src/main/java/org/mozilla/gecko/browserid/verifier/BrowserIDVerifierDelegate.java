package org.mozilla.gecko.browserid.verifier;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public interface BrowserIDVerifierDelegate {
  void handleSuccess(ExtendedJSONObject response);
  void handleFailure(String reason);
  void handleError(Exception e);
}