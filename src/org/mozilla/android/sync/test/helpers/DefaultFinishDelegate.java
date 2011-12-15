package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;

public class DefaultFinishDelegate extends DefaultDelegate implements RepositorySessionFinishDelegate {

  @Override
  public void onFinishFailed(Exception ex) {
    sharedFail("Finish failed");
  }

  @Override
  public void onFinishSucceeded(RepositorySession session, RepositorySessionBundle bundle) {
    sharedFail("Hit default finish delegate");
  }

  @Override
  public RepositorySessionFinishDelegate deferredFinishDelegate() {
    return new RepositorySessionFinishDelegate() {
      final RepositorySessionFinishDelegate self = this;
      @Override
      public void onFinishSucceeded(final RepositorySession session,
                                    final RepositorySessionBundle bundle) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onFinishSucceeded(session, bundle);
          }}).start();
      }

      @Override
      public void onFinishFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onFinishFailed(ex);
          }}).start();
      }

      @Override
      public RepositorySessionFinishDelegate deferredFinishDelegate() {
        return this;
      }
    };
  }
}
