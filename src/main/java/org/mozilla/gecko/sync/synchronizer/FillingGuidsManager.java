/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer;

import java.util.Collection;
import java.util.List;

/**
 * Persist a collection of GUIDs that need to be fetched in the future.
 */
public interface FillingGuidsManager {
  /**
   * Persist GUIDs that are fresher than all existing GUIDs.
   * <p>
   * If possible, order GUIDs from stalest to freshest.
   *
   * @param guids fresh GUIDs.
   * @throws Exception
   */
  public void addFreshGuids(Collection<String> guids) throws Exception;

  /**
   * Get GUIDs to fetch next.
   * <p>
   * If possible, order GUIDs from freshest to stalest.
   *
   * @return GUIDs or null if there are none to fetch.
   * @throws Exception
   */
  public List<String> nextGuids() throws Exception;

  /**
   * Mark GUIDs as no longer needing to be fetched.
   *
   * @param guids array of GUIDs.
   * @throws Exception
   */
  public void removeGuids(Collection<String> guids) throws Exception;

  /**
   * Mark GUIDs as needing to be re-fetched.
   * <p>
   * This allows to purge GUIDs that repeatedly fail to be fetched.
   *
   * @param guids array of GUIDs.
   * @throws Exception
   */
  public void retryGuids(Collection<String> guids) throws Exception;

  /**
   * Get the number of GUIDs still to be fetched.
   *
   * @returns number of GUIDs.
   */
  public int numGuidsRemaining();
}
