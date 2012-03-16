/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.stage.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.AssertionFailedError;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockResourceDelegate;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.ServerSyncStage;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import android.content.Context;

/**
 * All tests request a <code>BaseResource</code> which the
 * <code>ServerSyncStage</code> should observe. This request may or may not set
 * backoff headers; the stage may or may not continue after backoff; and the
 * synchronizer may succeed, fail, or abort.
 * <p>
 * We test for all these conditions, and we verify that the
 * <code>Synchronizer</code> is only saved on success.
 */
public class TestServerSyncStage {
  private int          TEST_PORT                = 15325;
  private final String TEST_CLUSTER_URL         = "http://localhost:" + TEST_PORT;
  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";
  private final long   TEST_BACKOFF_IN_SECONDS  = 2401;

  private HTTPServerTestHelper data = new HTTPServerTestHelper(TEST_PORT);

  public static boolean calledSave;

  @Before
  public void setUp() {
    BaseResource.enablePlainHTTPConnectionManager();
    BaseResource.rewriteLocalhost = false;
    calledSave = false;
  }

  /**
   * A <code>ServerSyncStage</code> that does not override <code>advance</code>
   * and returns the given synchronizer.
   *
   * Set <code>synchronizer</code> before use.
   */
  public class MockServerSyncStage extends ServerSyncStage {
    public Synchronizer synchronizer;

    public MockServerSyncStage() {
    }

    @Override
    protected String getCollection() {
      return null;
    }

    @Override
    protected Repository getLocalRepository() {
      return null;
    }

    @Override
    protected String getEngineName() {
      return null;
    }

    @Override
    protected RecordFactory getRecordFactory() {
      return null;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    public Synchronizer getConfiguredSynchronizer(GlobalSession session) throws NoCollectionKeysSetException, URISyntaxException, NonObjectJSONException, IOException, ParseException {
      return synchronizer;
    }
  }

  public class MockContinueServerSyncStage extends MockServerSyncStage {
    @Override
    protected boolean continueAfterBackoff(long backoff) {
      return true;
    }
  }

  /**
   * A <code>Synchronizer</code> that makes a <code>BaseResource</code> request
   * before finishing synchronization.
   *
   * Override <code>finishSynchronize</code> in subclasses.
   */
  public abstract class MockSynchronizer extends Synchronizer {
    public final WaitHelper innerWaitHelper = new WaitHelper();

    @Override
    public void synchronize(Context context, SynchronizerDelegate delegate) {
      // We should have installed our HTTP response observer before starting the sync.
      assertNotNull(BaseResource.getHttpResponseObserver());

      innerWaitHelper.performWait(new Runnable() {
        @Override
        public void run() {
          try {
            final BaseResource r = new BaseResource(TEST_CLUSTER_URL);
            r.delegate = new MockResourceDelegate(innerWaitHelper);
            r.get();
          } catch (URISyntaxException e) {
            innerWaitHelper.performNotify(e);
          }
        }
      });

      finishSynchronize(delegate);

      // We should have uninstalled our HTTP response observer when the sync is terminated.
      assertNull(BaseResource.getHttpResponseObserver());
    }

    @Override
    public SynchronizerConfiguration save() {
      calledSave = true;
      return null;
    }

    // Override in subclasses.
    public abstract void finishSynchronize(SynchronizerDelegate delegate);
  }

  public MockGlobalSessionCallback doTestWith(final MockServer server, final MockServerSyncStage stage, final MockSynchronizer synchronizer) throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    stage.synchronizer = synchronizer;
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback) {
      @Override
      protected void prepareStages() {
        super.prepareStages();
        stages.put(Stage.syncBookmarks, stage);
      }
    };

    data.startHTTPServer(server);
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      public void run() {
        try {
          session.start();
        } catch (Exception e) {
          final AssertionFailedError error = new AssertionFailedError();
          error.initCause(e);
          WaitHelper.getTestWaiter().performNotify(error);
        }
      }
    }));
    data.stopHTTPServer();

    return callback;
  }

  public static enum Backoff {
    YES,
    NO
  };

  public static enum Continue {
    YES,
    NO
  };

  public static enum Synchronize {
    SUCCEED,
    FAIL,
    ABORT,
  };

  public MockGlobalSessionCallback doTest(Backoff backoff, Continue cont, Synchronize sync) throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    MockServer server;
    if (backoff == Backoff.YES) {
      server = new MockServer() {
        public void handle(Request request, Response response) {
          response.set("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS));
          super.handle(request, response);
        }
      };
    } else {
      server = new MockServer();
    }

    MockServerSyncStage stage;
    if (cont == Continue.YES) {
      stage = new MockContinueServerSyncStage();
    } else {
      stage = new MockServerSyncStage();
    }

    MockSynchronizer synchronizer;
    if (sync == Synchronize.SUCCEED) {
      synchronizer = new MockSynchronizer() {
        @Override
        public void finishSynchronize(SynchronizerDelegate delegate) {
          delegate.onSynchronized(this);
        }
      };
    } else if (sync == Synchronize.FAIL) {
      synchronizer = new MockSynchronizer() {
        @Override
        public void finishSynchronize(SynchronizerDelegate delegate) {
          delegate.onSynchronizeFailed(this, new RuntimeException(), "for no reason");
        }
      };
    } else {
      synchronizer = new MockSynchronizer() {
        @Override
        public void finishSynchronize(SynchronizerDelegate delegate) {
          delegate.onSynchronizeAborted(this);
        }
      };
    }

    return doTestWith(server, stage, synchronizer);
  }

  @Test
  public void testNoBackoff() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.NO, Continue.NO, Synchronize.SUCCEED);

    assertFalse(callback.calledRequestBackoff);
    assertTrue(callback.calledSuccess);
    assertTrue(calledSave);
  }

  @Test
  public void testNoBackoffContinues() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.NO, Continue.YES, Synchronize.SUCCEED);

    assertFalse(callback.calledRequestBackoff);
    assertTrue(callback.calledSuccess);
    assertTrue(calledSave);
  }

  @Test
  public void testNoBackoffFailed() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.NO, Continue.NO, Synchronize.FAIL);

    assertFalse(callback.calledRequestBackoff);
    assertTrue(callback.calledError);
    assertFalse(calledSave);
  }

  @Test
  public void testNoBackoffAborted() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.NO, Continue.NO, Synchronize.ABORT);

    assertFalse(callback.calledRequestBackoff);
    assertTrue(callback.calledError);
    assertFalse(calledSave);
  }

  @Test
  public void testBackoff() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.YES, Continue.NO, Synchronize.SUCCEED);

    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
    assertTrue(callback.calledError);
    assertFalse(calledSave);
  }

  @Test
  public void testBackoffContinues() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.YES, Continue.YES, Synchronize.SUCCEED);

    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
    assertTrue(callback.calledSuccess);
    assertTrue(calledSave);
  }

  @Test
  public void testBackoffFailed() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.YES, Continue.NO, Synchronize.FAIL);

    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
    assertTrue(callback.calledError);
    assertFalse(calledSave);
  }

  @Test
  public void testBackoffAborted() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTest(Backoff.YES, Continue.NO, Synchronize.ABORT);

    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
    assertTrue(callback.calledError);
    assertFalse(calledSave);
  }
}
