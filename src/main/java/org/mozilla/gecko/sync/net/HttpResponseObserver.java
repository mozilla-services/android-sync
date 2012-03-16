package org.mozilla.gecko.sync.net;

import ch.boye.httpclientandroidlib.HttpResponse;

public interface HttpResponseObserver {
  /**
   * Observe an HTTP response.
   *
   * @param response
   *          The <code>HttpResponse</code> to observe.
   */
  public void observeHttpResponse(HttpResponse response);
}
