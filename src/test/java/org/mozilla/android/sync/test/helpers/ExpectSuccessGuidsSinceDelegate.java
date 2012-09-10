/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.ArrayList;

import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;

public class ExpectSuccessGuidsSinceDelegate extends ExpectSuccessDelegate implements RepositorySessionGuidsSinceDelegate {
  public final ArrayList<String> guids = new ArrayList<String>();

  public ExpectSuccessGuidsSinceDelegate(final WaitHelper waitHelper) {
    super(waitHelper);
  }

  @Override
  public void onGuidsSinceFailed(final Exception ex) {
    waitHelper.performNotify(ex);
  }

  @Override
  public void onGuidsSinceSucceeded(final String[] guids) {
    for (String guid : guids) {
      this.guids.add(guid);
    }
    waitHelper.performNotify();
  }
}
