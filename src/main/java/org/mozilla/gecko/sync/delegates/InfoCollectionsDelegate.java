/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.delegates;

import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.net.SyncServer11Response;

public interface InfoCollectionsDelegate {
  public void handleSuccess(InfoCollections global);
  public void handleFailure(SyncServer11Response response);
  public void handleError(Exception e);
}
