/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;

import android.util.Log;

public class ExpectBeginFailDelegate extends DefaultBeginDelegate {

  @Override
  public void onBeginFailed(Exception ex) {
    Log.i("ExpectBeginFailDelegate", "Got onBeginFailed, as expected.");
    if (!(ex instanceof InvalidSessionTransitionException)) {
      performNotify("Expected InvalidSessionTransititionException but got ", ex);
    }
  }
}
