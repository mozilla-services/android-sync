/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;


public interface BackoffHandler {
  public long getEarliestNextRequest();
  public void setEarliestNextRequest(long next);
  public void extendEarliestNextRequest(long next);

  /**
   * Return the number of milliseconds until we're allowed to sync again,
   * or 0 if now is fine.
   */
  public long delayMilliseconds();
}