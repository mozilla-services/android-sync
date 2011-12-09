/* Ripped from SyncStorageResponse */

package org.mozilla.android.sync.setup.jpake;

import org.mozilla.android.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;

public class JpakeResponse extends SyncResponse {
  public JpakeResponse(HttpResponse res) {
    super(res);
  }
}
