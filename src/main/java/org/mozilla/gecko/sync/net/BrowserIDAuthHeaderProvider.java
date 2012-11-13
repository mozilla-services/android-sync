package org.mozilla.gecko.sync.net;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

public class BrowserIDAuthHeaderProvider implements AuthHeaderProvider {
  protected final String assertion;

  public BrowserIDAuthHeaderProvider(String assertion) {
    this.assertion = assertion;
  }

  @Override
  public Header getAuthHeader(HttpRequestBase request, BasicHttpContext context, DefaultHttpClient client) {
    Header header = new BasicHeader("Authorization", "Browser-ID " + assertion);

    return header;
  }
}
