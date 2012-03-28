/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
import org.mozilla.android.sync.test.helpers.MockServerSyncStage;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

public class TestGlobalSession {
  private int          TEST_PORT                = 15325;
  private final String TEST_CLUSTER_URL         = "http://localhost:" + TEST_PORT;
  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";
  private final long   TEST_BACKOFF_IN_SECONDS  = 2401;

  public WaitHelper getTestWaiter() {
    return WaitHelper.getTestWaiter();
  }

  /**
   * A mock GlobalSession that fakes a 503 on info/collections and
   * sets X-Weave-Backoff header to the specified number of seconds.
   */
  public class MockBackoffGlobalSession extends MockGlobalSession {

    public MockSharedPreferences prefs;
    public long backoffInSeconds = -1;

    public MockBackoffGlobalSession(long backoffInSeconds,
        String clusterURL, String username, String password,
        KeyBundle syncKeyBundle, GlobalSessionCallback callback)
            throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
      super(clusterURL, username, password, syncKeyBundle, callback);
      this.backoffInSeconds = backoffInSeconds;
    }

    public class MockBackoffFetchInfoCollectionsStage extends FetchInfoCollectionsStage {
      @Override
      public void execute(GlobalSession session) {
        final HttpResponse response = new BasicHttpResponse(
          new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));

        response.addHeader("X-Weave-Backoff", Long.toString(backoffInSeconds)); // Backoff given in seconds.
        session.handleHTTPError(new SyncStorageResponse(response), "Failure fetching info/collections.");
      }
    }

    @Override
    protected void prepareStages() {
      super.prepareStages();
      stages.put(Stage.fetchInfoCollections, new MockBackoffFetchInfoCollectionsStage());
    }
  }

  /**
   * Test that handleHTTPError does in fact backoff.
   */
  @Test
  public void testBackoffCalledByHandleHTTPError() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));
      response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          session.handleHTTPError(new SyncStorageResponse(response), "Illegal method/protocol");
        }
      }));

      assertEquals(false, callback.calledSuccess);
      assertEquals(true,  callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true,  callback.calledRequestBackoff);
      assertEquals(TEST_BACKOFF_IN_SECONDS * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }


  /**
   * Test that a trivially successful GlobalSession does not fail or backoff.
   */
  @Test
  public void testSuccessCalledAfterStages() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch (Exception e) {
            final AssertionFailedError error = new AssertionFailedError();
            error.initCause(e);
            getTestWaiter().performNotify(error);
          }
        }
      }));

      assertEquals(true,  callback.calledSuccess);
      assertEquals(false, callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(false, callback.calledRequestBackoff);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  /**
   * Test that a failing GlobalSession does in fact fail and back off.
   */
  @Test
  public void testBackoffCalledInStages() {
    try {
      final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
      final GlobalSession session = new MockBackoffGlobalSession(TEST_BACKOFF_IN_SECONDS, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

      getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
        public void run() {
          try {
            session.start();
          } catch (Exception e) {
            final AssertionFailedError error = new AssertionFailedError();
            error.initCause(e);
            getTestWaiter().performNotify(error);
          }
        }
      }));

      assertEquals(false, callback.calledSuccess);
      assertEquals(true,  callback.calledError);
      assertEquals(false, callback.calledAborted);
      assertEquals(true,  callback.calledRequestBackoff);
      assertEquals(TEST_BACKOFF_IN_SECONDS * 1000, callback.weaveBackoff); // Backoff returned in milliseconds.
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }
  }

  private HTTPServerTestHelper data = new HTTPServerTestHelper(TEST_PORT);

  @Before
  public void setUp() {
    BaseResource.enablePlainHTTPConnectionManager();
    BaseResource.rewriteLocalhost = false;
  }

  public void doRequest() {
    // We should have installed our HTTP response observer before starting the sync.
    assertNotNull(BaseResource.getHttpResponseObserver());

    final WaitHelper innerWaitHelper = new WaitHelper();
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
  }

  public MockGlobalSessionCallback doTestSuccess(final boolean stageShouldBackoff, final boolean stageShouldAdvance) throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    MockServer server = new MockServer() {
      @Override
      public void handle(Request request, Response response) {
        if (stageShouldBackoff) {
          response.set("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS));
        }
        super.handle(request, response);
      }
    };

    final MockServerSyncStage stage = new MockServerSyncStage() {
      @Override
      public void execute(GlobalSession session) {
        doRequest();
        if (stageShouldAdvance) {
          session.advance();
          return;
        }
        session.abort(null,  "Stage intentionally failed.");
      }
    };

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

    // We should have uninstalled our HTTP response observer when the session is terminated.
    assertNull(BaseResource.getHttpResponseObserver());

    return callback;
  }

  @Test
  public void testOnSuccessBackoffAdvanced() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(true, true);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
  }

  @Test
  public void testOnSuccessBackoffAborted() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(true, false);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
  }

  @Test
  public void testOnSuccessNoBackoffAdvanced() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(false, true);

    assertTrue(callback.calledSuccess);
    assertFalse(callback.calledRequestBackoff);
  }

  @Test
  public void testOnSuccessNoBackoffAborted() throws SyncConfigurationException,
      IllegalArgumentException, NonObjectJSONException, IOException,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(false, false);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertFalse(callback.calledRequestBackoff);
  }
}
