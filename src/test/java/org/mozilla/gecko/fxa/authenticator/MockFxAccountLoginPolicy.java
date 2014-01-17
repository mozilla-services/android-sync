/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import java.util.concurrent.Executor;

import org.mozilla.gecko.background.fxa.FxAccountClient;

public class MockFxAccountLoginPolicy extends FxAccountLoginPolicy {
  public final FxAccountClient client;

  public MockFxAccountLoginPolicy(AbstractFxAccount fxAccount, FxAccountClient client) {
    super(null, fxAccount, new Executor() {
      @Override
      public void execute(Runnable command) {
        command.run();
      }
    });
    this.client = client;
  }

  protected FxAccountClient makeFxAccountClient() {
    return client;
  }
}
