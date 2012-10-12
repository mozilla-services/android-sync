package org.mozilla.gecko.tokenserver.test;

import java.net.URI;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class BlockingTokenServerClient {
  protected final TokenServerClient client;

  public BlockingTokenServerClient(URI uri) {
    client = new TokenServerClient(uri);
  }

  protected static class BlockingTokenServerClientDelegate implements TokenServerClientDelegate {
    public TokenServerToken token = null;

    @Override
    public void handleSuccess(TokenServerToken token) {
      this.token = token;

      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleFailure(TokenServerException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  public TokenServerToken getTokenFromBrowserIDAssertion(final String assertion, final boolean conditionsAccepted) {
    final BlockingTokenServerClientDelegate delegate = new BlockingTokenServerClientDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(assertion, conditionsAccepted, delegate);
      }
    });

    return delegate.token;
  }
}
