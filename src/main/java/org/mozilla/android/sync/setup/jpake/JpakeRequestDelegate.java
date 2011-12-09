package org.mozilla.android.sync.setup.jpake;

import ch.boye.httpclientandroidlib.HttpResponse;

public interface JpakeRequestDelegate {
  public void onRequestFailure(HttpResponse response);
  public void onRequestSuccess(HttpResponse response);
  public void onRequestError(Exception e);
}
