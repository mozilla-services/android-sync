package org.mozilla.gecko.aitc;

import java.net.URI;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient;
import org.mozilla.gecko.fxaccount.FxAccountAvatarClient.AvatarDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class BlockingFxAccountAvatarClient {
  protected final URI endpoint;
  protected final String assertion;

  protected final FxAccountAvatarClient client;

  public BlockingFxAccountAvatarClient(URI endpoint, String assertion) throws Exception {
    this.endpoint = endpoint;
    this.assertion = assertion;

    this.client = new FxAccountAvatarClient(endpoint);
  }

  protected class BlockingFxAccountAvatarClientDelegate implements AvatarDelegate {
    public ExtendedJSONObject result;

    @Override
    public void onSuccess(ExtendedJSONObject result) {
      this.result = result;

      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void onFailure(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void onError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  public void putAvatar(final ExtendedJSONObject avatar) {
    final BlockingFxAccountAvatarClientDelegate delegate = new BlockingFxAccountAvatarClientDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.putAvatar(assertion, avatar, delegate);
      }
    });
  }

  public ExtendedJSONObject getAvatar() {
    final BlockingFxAccountAvatarClientDelegate delegate = new BlockingFxAccountAvatarClientDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getAvatar(assertion, delegate);
      }
    });

    return delegate.result;
  }
}
