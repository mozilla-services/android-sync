/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertNotNull;

import org.mozilla.gecko.sync.repositories.RepositorySession;

import android.util.Log;

public class ExpectBeginDelegate extends DefaultBeginDelegate {

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    Log.d("ExpectBeginDelegate", "onBeginSucceeded.");
    assertNotNull(session);
  }
}
