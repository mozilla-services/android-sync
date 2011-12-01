/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertNotNull;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;

/**
 * Pokes field in AndroidBookmarksTestHelper.
 *
 * @author rnewman
 *
 */
public class SetupDelegate extends DefaultSessionCreationDelegate {
  public void onSessionCreated(RepositorySession sess) {
    try {
      assertNotNull(sess);
      AndroidRepositoryTestHelper.session = (AndroidBrowserRepositorySession) sess;
      AndroidRepositoryTestHelper.testWaiter.performNotify();
    } catch (AssertionError e) {  
      AndroidRepositoryTestHelper.testWaiter.performNotify(e);
    }
  }
}