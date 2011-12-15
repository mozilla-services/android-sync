package org.mozilla.gecko.sync.setup.jpake;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.ResourceDelegate;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;

public class JpakeRequest implements Resource {
  private static String LOG_TAG = "JPAKE_REQUEST";

  private BaseResource resource;
  public JPakeRequestDelegate delegate;

  public JpakeRequest(String uri, ResourceDelegate delegate) throws URISyntaxException {
    this(new URI(uri), delegate);
  }

  public JpakeRequest(URI uri, ResourceDelegate delegate) {
    this.resource = new BaseResource(uri);
    this.resource.delegate = delegate;
    Log.d(LOG_TAG, "new uri: " + uri);
  }

  @Override
  public void get() {
    this.resource.get();
  }

  @Override
  public void delete() {
    this.resource.delete();
  }

  @Override
  public void post(HttpEntity body) {
    this.resource.post(body);
  }

  @Override
  public void put(HttpEntity body) {
    this.resource.put(body);
  }
}
