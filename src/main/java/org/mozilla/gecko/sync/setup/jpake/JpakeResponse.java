/* Ripped from SyncStorageResponse */

package org.mozilla.gecko.sync.setup.jpake;

import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;

public class JPakeResponse extends SyncResponse {

  public JPakeResponse(HttpResponse res) {
    super(res);
  }
}
