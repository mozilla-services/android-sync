package org.mozilla.gecko.sync.jpake;

import ch.boye.httpclientandroidlib.HttpResponse;

public interface JPakeRequestDelegate {

  public void onRequestFailure(HttpResponse response);
  public void onRequestSuccess(HttpResponse response);
  public void onRequestError(Exception e);
}
