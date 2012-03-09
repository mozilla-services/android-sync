/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.net.ResourceDelegate;

import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

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
