package org.mozilla.gecko.sync.setup.auth;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

public interface AuthenticatorStage {
  public void execute(AccountAuthenticator aa) throws URISyntaxException, UnsupportedEncodingException;
}
