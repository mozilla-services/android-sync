/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

public interface RecordsChannelDelegate {
  public void onFlowCompleted(RecordsChannel recordsChannel, long fetchEnd, long storeEnd);
  public void onFlowBeginFailed(RecordsChannel recordsChannel, Exception ex);
  public void onFlowFinishFailed(RecordsChannel recordsChannel, Exception ex);

  /**
   * Called if fetching fails.
   *
   * @param recordsChannel the active channel.
   * @param ex the error causing the failure.
   */
  public void onFlowFetchFailed(RecordsChannel recordsChannel, Exception ex);

  public void notifyFlowRecordStoreFailed(RecordsChannel recordsChannel, Exception ex, String recordGuid);
}