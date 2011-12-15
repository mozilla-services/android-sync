/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;

public class DefaultBeginDelegate extends DefaultDelegate implements RepositorySessionBeginDelegate {

  @Override
  public void onBeginFailed(Exception ex) {
    sharedFail("Shouldn't fail.");
  }

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    sharedFail("Default begin delegate hit.");
  }

  @Override
  public RepositorySessionBeginDelegate deferredBeginDelegate() {
    final RepositorySessionBeginDelegate self = this;
    return new RepositorySessionBeginDelegate() {

      @Override
      public void onBeginSucceeded(final RepositorySession session) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onBeginSucceeded(session);
          }}).start();
      }

      @Override
      public void onBeginFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onBeginFailed(ex);
          }}).start();
      }

      @Override
      public RepositorySessionBeginDelegate deferredBeginDelegate() {
        return this;
      }
    };
  }
}
