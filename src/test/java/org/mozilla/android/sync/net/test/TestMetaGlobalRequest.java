/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.MetaGlobalRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class TestMetaGlobalRequest {
  private static final String TEST_SYNC_ID = "foobar";
  private static final int TEST_STORAGE_VERSION = 117;
  private static final String TEST_META_GLOBAL_RESPONSE = "{\"id\":\"global\",\"payload\":" +
      "\"{\\\"syncID\\\":\\\"" + TEST_SYNC_ID + "\\\",\\\"storageVersion\\\":" + TEST_STORAGE_VERSION + "," +
      "\\\"engines\\\":{\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"},\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"NNaQr6_F-9dm\\\"},\\\"forms\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"GXF29AFprnvc\\\"},\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"},\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"},\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"},\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\",\"username\":\"5817483\",\"modified\":1.32046073744E9}";

  private static final int    TEST_PORT    = 15325;
  private static final String TEST_SERVER  = "http://localhost:" + TEST_PORT;

  private static final String USER_PASS = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  private static final String META_URL  = TEST_SERVER + "/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  private HTTPServerTestHelper data     = new HTTPServerTestHelper(TEST_PORT);

  @Before
  public void setUp() {
    BaseResource.enablePlainHTTPConnectionManager();
    BaseResource.rewriteLocalhost = false;
  }

  public class MockMetaGlobalFetchDelegate implements MetaGlobalDelegate {
    boolean             successCalled     = false;
    MetaGlobal          successGlobal     = null;
    SyncStorageResponse successResponse   = null;
    boolean             failureCalled     = false;
    SyncStorageResponse failureResponse   = null;
    boolean             errorCalled       = false;
    Exception           errorException    = null;
    boolean             missingCalled     = false;
    SyncStorageResponse missingResponse   = null;
    boolean             malformedCalled   = false;
    SyncStorageResponse malformedResponse = null;

    public void handleSuccess(MetaGlobal global, SyncStorageResponse response) {
      successCalled = true;
      successGlobal = global;
      successResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }

    public void handleFailure(SyncStorageResponse response) {
      failureCalled = true;
      failureResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }

    public void handleError(Exception e) {
      errorCalled = true;
      errorException = e;
      WaitHelper.getTestWaiter().performNotify();
    }

    public void handleMissing(SyncStorageResponse response) {
      missingCalled = true;
      missingResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }

    public void handleMalformed(SyncStorageResponse response) {
      malformedCalled = true;
      malformedResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }
  }

  public MockMetaGlobalFetchDelegate doFetch(MockServer server) {
    final MetaGlobalRequest request = new MetaGlobalRequest(META_URL, USER_PASS);
    final MockMetaGlobalFetchDelegate delegate = new MockMetaGlobalFetchDelegate();

    data.startHTTPServer(server);
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        request.fetch(delegate);
      }
    }));
    data.stopHTTPServer();

    return delegate;
  }

  @Test
  public void testMetaGlobalMissingFetch() {
    MockServer missingMetaGlobalServer = new MockServer(404, "");

    final MockMetaGlobalFetchDelegate delegate = doFetch(missingMetaGlobalServer);

    assertTrue(delegate.missingCalled);
    assertEquals(404, delegate.missingResponse.getStatusCode());
  }

  @Test
  public void testMetaGlobalExistingFetch() {
    MockServer existingMetaGlobalServer = new MockServer(200, TEST_META_GLOBAL_RESPONSE);

    final MockMetaGlobalFetchDelegate delegate = doFetch(existingMetaGlobalServer);

    assertTrue(delegate.successCalled);
    assertEquals(200, delegate.successResponse.getStatusCode());
    MetaGlobal mg = delegate.successGlobal;
    assertNotNull(mg);
    assertEquals(TEST_SYNC_ID, mg.syncID);
    assertEquals(TEST_STORAGE_VERSION, mg.storageVersion);
    EngineSettings engineSettings = mg.getEngineSettings("clients");
    assertNotNull(engineSettings);
    assertEquals("fDg0MS5bDtV7", engineSettings.syncID);
    assertEquals(1, engineSettings.version);
  }

  @Test
  public void testMetaGlobalMalformedFetch() {
    MockServer server = new MockServer(200, "{2:2:2:2!}");

    final MockMetaGlobalFetchDelegate delegate = doFetch(server);

    assertTrue(delegate.malformedCalled);
    assertNotNull(delegate.malformedResponse);
    assertNull(delegate.successGlobal);
  }
}
