package org.mozilla.android.sync.net.test;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mozilla.android.sync.net.ResourceDelegate;

/**
 * Implement a default set of delegate roles, such as 0 timeouts.
 */
public abstract class BaseResourceDelegate implements ResourceDelegate {

  @Override
  public String getCredentials() {
    return null;
  }

  @Override
  public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
    return;
  }

  @Override
  public int connectionTimeout() {
    return 0;
  }

  @Override
  public int socketTimeout() {
    return 0;
  }

}
