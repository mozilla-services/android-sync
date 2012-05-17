/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

public interface SynchronizerSessionDelegate {
  public void onInitialized(SynchronizerSession session);

  public void onSynchronizedSession(SynchronizerSession session);
  public void onSynchronizeSessionFailed(SynchronizerSession session, Exception lastException, String reason);
  public void onSynchronizeSessionSkipped(SynchronizerSession session);

  public void onFetchError(Exception e);
  public void onSessionError(Exception e);

  public void notifyLocalRecordStoreFailed(Exception e, String recordGuid);
  public void notifyRemoteRecordStoreFailed(Exception e, String recordGuid);
}
