package org.mozilla.gecko.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.user.UserClientException.UserClientMalformedRequestException;
import org.mozilla.gecko.user.UserClientException.UserClientMalformedResponseException;
import org.mozilla.gecko.user.UserClientException.UserClientUserAlreadyExistsException;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class UserClient {
  public static final String LOG_TAG = "UserClient";

  protected final URI endpoint;

  public UserClient(URI endpoint) {
    this.endpoint = endpoint;
  }

  protected abstract class UserClientResourceDelegate extends BaseResourceDelegate {
    protected final UserClientDelegate delegate;

    public UserClientResourceDelegate(Resource resource, UserClientDelegate delegate) {
      super(resource);
      this.delegate = delegate;
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      Logger.warn(LOG_TAG, "Got protocol exception.", e);
      delegate.handleError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      Logger.warn(LOG_TAG, "Got IO exception.", e);
      delegate.handleError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      Logger.warn(LOG_TAG, "Got protocol exception.", e);
      delegate.handleError(e);
    }
  }

  protected void makeResourceDelegate(BaseResource r) {
  }

  public void isAvailable(final String email, final UserClientDelegate delegate) {
    final String username;
    URI uri;
    try {
      username = Utils.usernameFromAccount(email);

      uri = new URI(endpoint + "/user/1.0/" + username);
    } catch (Exception e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = new UserClientResourceDelegate(r, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        if (statusCode != 200) {
          delegate.handleFailure(new UserClientException("Expected status code 200 but got " + statusCode + "."));
          return;
        }

        String body;
        try {
          body = res.body().trim();
        } catch (Exception e) {
          delegate.handleFailure(new UserClientException(e));
          return;
        }

        delegate.handleSuccess(username, body);
      }
    };

    r.get();
  }

  public void getNode(final String email, final UserClientDelegate delegate) {
    final String username;
    URI uri;
    try {
      username = Utils.usernameFromAccount(email);

      uri = new URI(endpoint + "/user/1.0/" + username + "/node/weave");
    } catch (Exception e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = new UserClientResourceDelegate(r, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        if (statusCode != 200) {
          delegate.handleFailure(new UserClientException("Expected status code 200 but got " + statusCode + "."));
          return;
        }

        String body;
        try {
          body = res.body().trim();
        } catch (Exception e) {
          delegate.handleFailure(new UserClientException(e));
          return;
        }

        delegate.handleSuccess(username, body);
      }
    };

    r.get();
  }

  public void createAccount(String email, String password, UserClientDelegate delegate) {
    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("email", email);
    o.put("password", password);
//    o.put("captcha-challenge", "");
//    o.put("captcha-response", "");

    final String username;
    URI uri;
    try {
      username = Utils.usernameFromAccount(email);

      uri = new URI(endpoint + "/user/1.0/" + username);
    } catch (Exception e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = new UserClientResourceDelegate(r, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncStorageResponse res = new SyncStorageResponse(response);
        int statusCode = res.getStatusCode();

        String body;
        try {
          body = res.body().trim();
        } catch (Exception e) {
          delegate.handleFailure(new UserClientMalformedResponseException(e));
          return;
        }

        if (statusCode == 200) {
          delegate.handleSuccess(username, body);
          return;
        }

        String errorMessage;
        try {
          errorMessage = res.getErrorMessage();
        } catch (Exception e) {
          delegate.handleFailure(new UserClientMalformedResponseException(e));
          return;
        }

        if ("4".equals(body)) {
          delegate.handleFailure(new UserClientUserAlreadyExistsException(errorMessage));
        } else {
          delegate.handleFailure(new UserClientMalformedRequestException(errorMessage));
        }
      }
    };

    try {
      r.put(o.object);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
    }
  }
}
