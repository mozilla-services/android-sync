/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.util.Log;

public class TestMetaGlobal {
  public static Object monitor = new Object();

  private static final int    TEST_PORT    = 15325;
  private static final String TEST_SERVER  = "http://localhost:" + TEST_PORT;
  private static final String TEST_SYNC_ID = "foobar";

  public static final String USER_PASS = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  public static final String META_URL  = TEST_SERVER + "/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  private HTTPServerTestHelper data    = new HTTPServerTestHelper();

  @Before
  public void setUp() {
    Log.i("TestMetaGlobal", "Faking SSL context.");
    BaseResource.enablePlainHTTPConnectionManager();
    Log.i("TestResource", "Disabling URI rewriting.");
    BaseResource.rewriteLocalhost = false;
  }

  @Test
  public void testMetaGlobalModified() {
    MetaGlobal g = new MetaGlobal(META_URL, USER_PASS);
    assertFalse(g.isModified);
    g.setSyncID("foobar");
    assertTrue(g.isModified);
    assertEquals(g.getSyncID(), "foobar");
  }

  public class MockMetaGlobalFetchDelegate implements MetaGlobalDelegate {
    boolean             successCalled   = false;
    MetaGlobal          successGlobal   = null;
    SyncStorageResponse successResponse = null;
    boolean             failureCalled   = false;
    SyncStorageResponse failureResponse = null;
    boolean             errorCalled     = false;
    Exception           errorException  = null;
    boolean             missingCalled   = false;
    MetaGlobal          missingGlobal   = null;
    SyncStorageResponse missingResponse = null;

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
      WaitHelper.getTestWaiter().performNotify(e);
    }

    public void handleMissing(MetaGlobal global, SyncStorageResponse response) {
      missingCalled = true;
      missingGlobal = global;
      missingResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public MetaGlobalDelegate deferred() {
      return this;
    }
  }

  public MockMetaGlobalFetchDelegate doFetch(final MetaGlobal global) {
    final MockMetaGlobalFetchDelegate delegate = new MockMetaGlobalFetchDelegate();
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        global.fetch(delegate);
      }
    }));

    return delegate;
  }

  @Test
  public void testMetaGlobalMissingFetch() {
    MockServer missingMetaGlobalServer = new MockServer(404, "{}");
    final MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertFalse(global.isModified);
    global.setSyncID(TEST_SYNC_ID);
    assertTrue(global.isModified);
    assertEquals(TEST_SYNC_ID, global.getSyncID());

    data.startHTTPServer(missingMetaGlobalServer);
    final MockMetaGlobalFetchDelegate delegate = doFetch(global);
    data.stopHTTPServer();

    assertTrue(delegate.missingCalled);
    assertEquals(404, delegate.missingResponse.getStatusCode());
    assertTrue(delegate.missingGlobal.isModified);
    assertEquals(TEST_SYNC_ID, delegate.missingGlobal.getSyncID());
  }

  @Test
  public void testMetaGlobalExistingFetch() {
    String body = "{\"id\":\"global\",\"payload\":\"{\\\"syncID\\\":\\\"zPSQTm7WBVWB\\\",\\\"storageVersion\\\":5,\\\"engines\\\":{\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"},\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"NNaQr6_F-9dm\\\"},\\\"forms\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"GXF29AFprnvc\\\"},\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"},\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"},\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"},\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\",\"username\":\"5817483\",\"modified\":1.32046073744E9}";
    MockServer existingMetaGlobalServer = new MockServer(200, body);
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertNull(global.getSyncID());
    assertNull(global.getEngines());
    assertNull(global.getStorageVersion());

    data.startHTTPServer(existingMetaGlobalServer);
    final MockMetaGlobalFetchDelegate delegate = doFetch(global);
    data.stopHTTPServer();

    assertTrue(delegate.successCalled);
    assertEquals(200, delegate.successResponse.getStatusCode());
    assertFalse(global.isModified);
    assertEquals("zPSQTm7WBVWB", global.getSyncID());
    assertTrue(global.getEngines() instanceof ExtendedJSONObject);
    assertEquals(new Long(5), global.getStorageVersion());
  }

  // TODO: upload test.
}
