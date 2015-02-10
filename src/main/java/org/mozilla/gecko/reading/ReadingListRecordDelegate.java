/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import org.mozilla.gecko.reading.ReadingListClient.ReadingListResponse;
import org.mozilla.gecko.sync.net.MozResponse;

public interface ReadingListRecordDelegate {
  void onRecordReceived(ReadingListRecord record);
  void onComplete(ReadingListResponse response);
  void onFailure(MozResponse response);
  void onFailure(Exception error);
}
