package org.mozilla.gecko.browserid.verifier;

public interface BrowserIDVerifierClient {
  public abstract void verify(String audience, String assertion, BrowserIDVerifierDelegate delegate);
}
