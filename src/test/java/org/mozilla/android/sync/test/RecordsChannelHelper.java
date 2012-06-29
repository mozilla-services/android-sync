/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectSuccessRepositorySessionFinishDelegate;
import org.mozilla.android.sync.test.helpers.WBORepository;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.synchronizer.RecordsChannel;
import org.mozilla.gecko.sync.synchronizer.RecordsChannelDelegate;

public class RecordsChannelHelper {

  protected WBORepository remote;
  protected WBORepository local;

  protected RepositorySession source;
  protected RepositorySession sink;
  protected RecordsChannelDelegate rcDelegate;

  protected AtomicInteger numFlowFetchFailed;
  protected AtomicInteger numFlowStoreFailed;
  protected AtomicInteger numFlowCompleted;
  protected AtomicBoolean flowBeginFailed;
  protected AtomicBoolean flowFinishFailed;

  protected RecordsChannel newRecordsChannel(final RepositorySession source, final RepositorySession sink, final RecordsChannelDelegate rcDelegate) {
    return new RecordsChannel(source,  sink, rcDelegate);
  }

  public void doFlow(final Repository remote, final Repository local) throws Exception {
    doFlow(0, remote, local);
  }

  public void doFlow(final long lastSyncTimestamp, final Repository remote, final Repository local) throws Exception {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        remote.createSession(new ExpectSuccessRepositorySessionCreationDelegate(WaitHelper.getTestWaiter()) {
          @Override
          public void onSessionCreated(RepositorySession session) {
            source = session;
            source.lastSyncTimestamp = lastSyncTimestamp;
            local.createSession(new ExpectSuccessRepositorySessionCreationDelegate(WaitHelper.getTestWaiter()) {
              @Override
              public void onSessionCreated(RepositorySession session) {
                sink = session;
                WaitHelper.getTestWaiter().performNotify();
              }
            }, null);
          }
        }, null);
      }
    });

    assertNotNull(source);
    assertNotNull(sink);

    numFlowFetchFailed = new AtomicInteger(0);
    numFlowStoreFailed = new AtomicInteger(0);
    numFlowCompleted = new AtomicInteger(0);
    flowBeginFailed = new AtomicBoolean(false);
    flowFinishFailed = new AtomicBoolean(false);

    rcDelegate = new RecordsChannelDelegate() {
      @Override
      public void onFlowFetchFailed(RecordsChannel recordsChannel, Exception ex) {
        numFlowFetchFailed.incrementAndGet();
      }

      @Override
      public void onFlowStoreFailed(RecordsChannel recordsChannel, Exception ex, String recordGuid) {
        numFlowStoreFailed.incrementAndGet();
      }

      @Override
      public void onFlowFinishFailed(RecordsChannel recordsChannel, Exception ex) {
        flowFinishFailed.set(true);
        WaitHelper.getTestWaiter().performNotify();
      }

      @Override
      public void onFlowCompleted(RecordsChannel recordsChannel, long fetchEnd, long storeEnd) {
        numFlowCompleted.incrementAndGet();
        try {
          sink.finish(new ExpectSuccessRepositorySessionFinishDelegate(WaitHelper.getTestWaiter()) {
            @Override
            public void onFinishSucceeded(RepositorySession session, RepositorySessionBundle bundle) {
              try {
                source.finish(new ExpectSuccessRepositorySessionFinishDelegate(WaitHelper.getTestWaiter()) {
                  @Override
                  public void onFinishSucceeded(RepositorySession session, RepositorySessionBundle bundle) {
                    performNotify();
                  }
                });
              } catch (InactiveSessionException e) {
                WaitHelper.getTestWaiter().performNotify(e);
              }
            }
          });
        } catch (InactiveSessionException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }

      @Override
      public void onFlowBeginFailed(RecordsChannel recordsChannel, Exception ex) {
        flowBeginFailed.set(true);
        WaitHelper.getTestWaiter().performNotify();
      }
    };

    final RecordsChannel rc = newRecordsChannel(source,  sink, rcDelegate);
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          rc.beginAndFlow();
        } catch (InvalidSessionTransitionException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
  }
}
