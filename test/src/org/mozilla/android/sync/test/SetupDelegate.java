/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertNotNull;

import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;

import junit.framework.AssertionFailedError;

/**
 * Stores created session in AndroidBookmarksTest.
 *
 * @author rnewman
 *
 */
public class SetupDelegate extends DefaultSessionCreationDelegate {
  public void onSessionCreated(RepositorySession sess) {
    try {
      assertNotNull(sess);
      AndroidBrowserRepositoryTest.session = (AndroidBrowserRepositorySession) sess;
      AndroidBrowserRepositoryTest.performNotify();
    } catch (AssertionFailedError e) {
      AndroidBrowserRepositoryTest.performNotify(e);
    }
  }
}