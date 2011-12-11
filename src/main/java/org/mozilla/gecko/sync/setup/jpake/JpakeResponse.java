/* Ripped from SyncStorageResponse */

package org.mozilla.gecko.sync.setup.jpake;

import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;

public class JpakeResponse extends SyncResponse {
  public JpakeResponse(HttpResponse res) {
    super(res);
  }
}
