package org.mozilla.gecko.fxaccount;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public interface FxAccountClient {
  public void logIn(String email, String password, FxAccountClientDelegate delegate);
  public void createAccount(String email, String password, FxAccountClientDelegate delegate);

  public String getAssertion(ExtendedJSONObject result, String audience) throws Exception;
}
