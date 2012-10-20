package org.mozilla.gecko.fxaccount;

import java.net.URI;

import org.mozilla.gecko.aitc.DeviceRecord;
import org.mozilla.gecko.aitc.net.AppsClient;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientDelegate;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientException;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientObjectDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class FxAccountAvatarClient {
  public final static String LOG_TAG = FxAccountAvatarClient.class.getSimpleName();

  public final static String UUID = "AAAAZZZZ-BBBB-CCCC-DDDD-EEEEZZZZEEEE";

  protected final URI endpoint;

  public FxAccountAvatarClient(URI endpoint) {
    this.endpoint = URI.create(endpoint + "/1.0/aitc/1.0");
  }

  public interface AvatarDelegate {
    public void onSuccess(ExtendedJSONObject avatar);
    public void onFailure(Exception e);
    public void onError(Exception e);
  }

  public void putAvatar(String assertion, final ExtendedJSONObject avatar, final AvatarDelegate avatarDelegate) {
    TokenServerClient tokenServerClient = new TokenServerClient(endpoint);

    tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, new TokenServerClientDelegate() {
      @Override
      public void handleSuccess(TokenServerToken token) {
        AppsClient appsClient = new AppsClient(token);

        final ExtendedJSONObject apps = new ExtendedJSONObject();
        apps.put("avatar", avatar);
        DeviceRecord device = new DeviceRecord(UUID, "FxAccounts", "type", "layout", apps);

        appsClient.putDevice(device, new AppsClientDelegate() {
          @Override
          public void handleSuccess() {
            avatarDelegate.onSuccess(null);
          }

          @Override
          public void handleRemoteFailure(AppsClientException e) {
            avatarDelegate.onFailure(e);
          }

          @Override
          public void handleError(Exception e) {
            avatarDelegate.onError(e);
          }
        });
      }

      @Override
      public void handleFailure(TokenServerException e) {
        avatarDelegate.onFailure(e);
      }

      @Override
      public void handleError(Exception e) {
        avatarDelegate.onError(e);
      }
    });
  }

  public void getAvatar(String assertion, final AvatarDelegate avatarDelegate) {
    TokenServerClient tokenServerClient = new TokenServerClient(endpoint);

    tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, new TokenServerClientDelegate() {
      @Override
      public void handleSuccess(TokenServerToken token) {
        AppsClient appsClient = new AppsClient(token);

        final ExtendedJSONObject result = new ExtendedJSONObject();

        appsClient.getDevice(UUID, new AppsClientObjectDelegate() {
          @Override
          public void handleSuccess() {
            try {
              avatarDelegate.onSuccess(result.getObject("avatar"));
            } catch (NonObjectJSONException e) {
              avatarDelegate.onError(e);
            }
          }

          @Override
          public void handleRemoteFailure(AppsClientException e) {
            avatarDelegate.onFailure(e);
          }

          @Override
          public void handleError(Exception e) {
            avatarDelegate.onError(e);
          }

          @Override
          public void onObject(ExtendedJSONObject device) {
            try {
              result.put("avatar", device.getObject("apps").getObject("avatar"));
            } catch (NonObjectJSONException e) {
              // Ignore for now.
            }
          }
        });
      }

      @Override
      public void handleFailure(TokenServerException e) {
        avatarDelegate.onFailure(e);
      }

      @Override
      public void handleError(Exception e) {
        avatarDelegate.onError(e);
      }
    });
  }
}
