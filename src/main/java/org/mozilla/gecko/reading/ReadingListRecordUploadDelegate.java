/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import org.mozilla.gecko.reading.ReadingListClient.ReadingListRecordResponse;
import org.mozilla.gecko.reading.ReadingListClient.ReadingListResponse;
import org.mozilla.gecko.sync.net.MozResponse;

public interface ReadingListRecordUploadDelegate {
  public void onInvalidUpload(ReadingListResponse response);
  public void onConflict(ReadingListResponse response);
  public void onSuccess(ReadingListRecordResponse response, ReadingListRecord record);
  public void onBadRequest(MozResponse response);
  public void onFailure(Exception ex);
  public void onFailure(MozResponse response);
}
