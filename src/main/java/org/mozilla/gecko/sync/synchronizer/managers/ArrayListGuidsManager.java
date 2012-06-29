/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.synchronizer.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mozilla.gecko.sync.synchronizer.FillingGuidsManager;

/**
 * For testing: maintains a collection of GUIDs to be fetched in memory.
 */
public class ArrayListGuidsManager implements FillingGuidsManager {
  public final ArrayList<String> guids = new ArrayList<String>();

  public int batchSize;

  public ArrayListGuidsManager(int batchSize) {
    this.batchSize = batchSize;
  }

  @Override
  public void addFreshGuids(final Collection<String> guids) throws Exception {
    this.guids.removeAll(guids);
    this.guids.addAll(guids);
  }

  @Override
  public List<String> nextGuids() throws Exception {
    if (guids.isEmpty()) {
      return new ArrayList<String>();
    }
    int end = guids.size();
    int beg = Math.max(0, end - batchSize);
    final List<String> next = guids.subList(beg, end);
    final ArrayList<String> ret = new ArrayList<String>(next);
    next.clear();
    return ret;
  }

  @Override
  public void removeGuids(final Collection<String> guids) throws Exception {
    for (String guid : guids) {
      this.guids.remove(guid);
    }
  }

  @Override
  public void retryGuids(final Collection<String> guids) throws Exception {
    // No effort to remove GUIDs that fail repeatedly.
    addFreshGuids(guids);
  }

  @Override
  public int numGuidsRemaining() {
    return guids.size();
  }
}
