/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import android.util.Log;

public class TestMetaGlobal {
  public static Object monitor = new Object();

  private static final int    TEST_PORT    = 15325;
  private static final String TEST_SERVER  = "http://localhost:" + TEST_PORT;
  private static final String TEST_SYNC_ID = "foobar";

  public static final String USER_PASS = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  public static final String META_URL  = TEST_SERVER + "/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  private HTTPServerTestHelper data    = new HTTPServerTestHelper(TEST_PORT);

  public static final String TEST_META_GLOBAL_RESPONSE = "{\"id\":\"global\",\"payload\":\"{\\\"syncID\\\":\\\"zPSQTm7WBVWB\\\",\\\"storageVersion\\\":5,\\\"engines\\\":{\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"},\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"NNaQr6_F-9dm\\\"},\\\"forms\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"GXF29AFprnvc\\\"},\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"},\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"},\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"},\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\",\"username\":\"5817483\",\"modified\":1.32046073744E9}";

  @Before
  public void setUp() {
    Log.i("TestMetaGlobal", "Faking SSL context.");
    BaseResource.enablePlainHTTPConnectionManager();
    Log.i("TestResource", "Disabling URI rewriting.");
    BaseResource.rewriteLocalhost = false;
  }

  @Test
  public void testMetaGlobalSyncID() {
    MetaGlobal g = new MetaGlobal(META_URL, USER_PASS);
    g.setSyncID("foobar");
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
    global.setSyncID(TEST_SYNC_ID);
    assertEquals(TEST_SYNC_ID, global.getSyncID());

    data.startHTTPServer(missingMetaGlobalServer);
    final MockMetaGlobalFetchDelegate delegate = doFetch(global);
    data.stopHTTPServer();

    assertTrue(delegate.missingCalled);
    assertEquals(404, delegate.missingResponse.getStatusCode());
    assertEquals(TEST_SYNC_ID, delegate.missingGlobal.getSyncID());
  }

  @Test
  public void testMetaGlobalExistingFetch() {
    MockServer existingMetaGlobalServer = new MockServer(200, TEST_META_GLOBAL_RESPONSE);
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertNull(global.getSyncID());
    assertNull(global.getEngines());
    assertNull(global.getStorageVersion());

    data.startHTTPServer(existingMetaGlobalServer);
    final MockMetaGlobalFetchDelegate delegate = doFetch(global);
    data.stopHTTPServer();

    assertTrue(delegate.successCalled);
    assertEquals(200, delegate.successResponse.getStatusCode());
    assertEquals("zPSQTm7WBVWB", global.getSyncID());
    assertTrue(global.getEngines() instanceof ExtendedJSONObject);
    assertEquals(new Long(5), global.getStorageVersion());
  }


  @Test
  public void testMetaGlobalSetFromRecord() throws IllegalStateException, NonObjectJSONException, IOException, ParseException {
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setFromRecord(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    assertEquals("zPSQTm7WBVWB", mg.getSyncID());
    assertTrue(mg.getEngines() instanceof ExtendedJSONObject);
    assertEquals(new Long(5), mg.getStorageVersion());
  }

  @Test
  public void testMetaGlobalAsCryptoRecord() throws IllegalStateException, NonObjectJSONException, IOException, ParseException {
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setFromRecord(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    CryptoRecord rec = mg.asCryptoRecord();
    assertEquals("global", rec.guid);
    mg.setFromRecord(rec);
    assertEquals("zPSQTm7WBVWB", mg.getSyncID());
    assertTrue(mg.getEngines() instanceof ExtendedJSONObject);
    assertEquals(new Long(5), mg.getStorageVersion());
  }

  @Test
  public void testMetaGlobalGetEnabledEngineNames() throws IllegalStateException, NonObjectJSONException, IOException, ParseException {
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setFromRecord(CryptoRecord.fromJSONRecord(TEST_META_GLOBAL_RESPONSE));
    assertEquals("zPSQTm7WBVWB", mg.getSyncID());
    final Set<String> actual = mg.getEnabledEngineNames();
    final Set<String> expected = new HashSet<String>();
    for (String name : new String[] { "bookmarks", "clients", "forms", "history", "passwords", "prefs", "tabs" }) {
      expected.add(name);
    }
    assertEquals(expected, actual);
  }

  public MockMetaGlobalFetchDelegate doUpload(final MetaGlobal global) {
    final MockMetaGlobalFetchDelegate delegate = new MockMetaGlobalFetchDelegate();
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      @Override
      public void run() {
        global.upload(delegate);
      }
    }));

    return delegate;
  }

  @Test
  public void testMetaGlobalUpload() {
    long TEST_STORAGE_VERSION = 111;
    String TEST_SYNC_ID = "testSyncID";
    MetaGlobal mg = new MetaGlobal(META_URL, USER_PASS);
    mg.setSyncID(TEST_SYNC_ID);
    mg.setStorageVersion(new Long(TEST_STORAGE_VERSION));

    final AtomicBoolean mgUploaded = new AtomicBoolean(false);
    final MetaGlobal uploadedMg = new MetaGlobal(null, null);

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        if (request.getMethod().equals("PUT")) {
          try {
            ExtendedJSONObject body = ExtendedJSONObject.parseJSONObject(request.getContent());
            System.out.println(body.toJSONString());
            assertTrue(body.containsKey("payload"));
            assertFalse(body.containsKey("default"));

            CryptoRecord rec = CryptoRecord.fromJSONRecord(body);
            uploadedMg.setFromRecord(rec);
            mgUploaded.set(true);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          this.handle(request, response, 200, "success");
          return;
        }
        this.handle(request, response, 404, "missing");
      }
    };

    data.startHTTPServer(server);
    final MockMetaGlobalFetchDelegate delegate = doUpload(mg);
    data.stopHTTPServer();

    assertTrue(delegate.successCalled);
    assertTrue(mgUploaded.get());
    assertEquals(TEST_SYNC_ID, uploadedMg.getSyncID());
    assertEquals(TEST_STORAGE_VERSION, uploadedMg.getStorageVersion().longValue());
  }
}
