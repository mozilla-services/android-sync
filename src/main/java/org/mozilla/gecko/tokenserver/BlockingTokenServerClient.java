package org.mozilla.gecko.tokenserver;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class BlockingTokenServerClient {
  protected final TokenServerClient client;

  public BlockingTokenServerClient(URI uri) {
    client = new TokenServerClient(uri);
  }

  public static class BlockingTokenServerException extends Exception {
    private static final long serialVersionUID = -1234932152982932775L;

    public BlockingTokenServerException(Exception e) {
      super(e);
    }
  }

  protected static class BlockingTokenServerClientDelegate implements TokenServerClientDelegate {
    protected final CountDownLatch latch;

    public TokenServerToken token = null;
    public Exception exception = null;

    public BlockingTokenServerClientDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void handleSuccess(TokenServerToken token) {
      this.token = token;

      latch.countDown();
    }

    @Override
    public void handleFailure(TokenServerException e) {
      this.exception = e;

      latch.countDown();
    }

    @Override
    public void handleError(Exception e) {
      this.exception = e;

      latch.countDown();
    }
  }

  public TokenServerToken getTokenFromBrowserIDAssertion(final String assertion, final boolean conditionsAccepted)
      throws BlockingTokenServerException {
    CountDownLatch latch = new CountDownLatch(1);

    final BlockingTokenServerClientDelegate delegate = new BlockingTokenServerClientDelegate(latch);

    client.getTokenFromBrowserIDAssertion(assertion, conditionsAccepted, delegate);

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new BlockingTokenServerException(e);
    }

    if (delegate.exception != null) {
      throw new BlockingTokenServerException(delegate.exception);
    }

    return delegate.token;
  }
}
