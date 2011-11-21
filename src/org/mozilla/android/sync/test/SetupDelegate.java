/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertNotNull;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
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
      AndroidBookmarksTestHelper.session = (BookmarksRepositorySession) sess;
      AndroidBookmarksTestHelper.testWaiter.performNotify();
    } catch (AssertionError e) {  
      AndroidBookmarksTestHelper.testWaiter.performNotify(e);
    }
  }
}