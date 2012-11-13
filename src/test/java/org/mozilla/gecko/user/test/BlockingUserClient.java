package org.mozilla.gecko.user.test;

import java.net.URI;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.user.UserClient;
import org.mozilla.gecko.user.UserClientDelegate;
import org.mozilla.gecko.user.UserClientException;
import org.mozilla.gecko.user.UserClientException.UserClientUserAlreadyExistsException;

public class BlockingUserClient {
  protected final UserClient userClient;

  public BlockingUserClient(URI endpoint) {
    userClient = new UserClient(endpoint);
  }

  public boolean isAvailable(final String email) {
    final BlockingUserClientDelegate delegate = new BlockingUserClientDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        userClient.isAvailable(email, delegate);
      }
    });

    return "0".equals(delegate.body);
  }

  public boolean createAccount(final String email, final String password) {
    final BlockingUserClientDelegate delegate = new BlockingUserClientDelegate();

    try {
      WaitHelper.getTestWaiter().performWait(new Runnable() {
        @Override
        public void run() {
          userClient.createAccount(email, password, delegate);
        }
      });
    } catch (WaitHelper.InnerError e) {
      if (e.getCause() instanceof UserClientUserAlreadyExistsException) {
        return false;
      }

      throw e;
    }

    return delegate.username.equals(delegate.body);
  }

  public String getNode(final String email) {
    final BlockingUserClientDelegate delegate = new BlockingUserClientDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        userClient.getNode(email, delegate);
      }
    });

    return delegate.body.trim();
  }

  protected static class BlockingUserClientDelegate implements UserClientDelegate {
    public String username;
    public String body;

    @Override
    public void handleSuccess(String username, String body) {
      this.body = body;
      this.username = username;

      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleFailure(UserClientException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }
}
