/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import android.util.Log;

public class TestMetaGlobal {
  public static Object monitor = new Object();

  public static final String USER_PASS = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  public static final String META_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
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

  public class MissingMetaGlobalFetchDelegate implements MetaGlobalDelegate {
    MetaGlobal global;
    public MissingMetaGlobalFetchDelegate(MetaGlobal g) {
      global = g;
    }

    public void handleSuccess(MetaGlobal global) {
      fail("Fetch should 404, not succeed.");
    }

    public void handleFailure(SyncStorageResponse response) {
      fail("handleMissing should be invoked.");
    }

    public void handleError(Exception e) {
      e.printStackTrace();
      fail("Fetch should 404, not error.");
    }

    public void handleMissing(MetaGlobal global) {
      assertEquals(global.getResponse().getStatusCode(), 404);
      assertTrue(global.isModified);
      assertEquals(global.getSyncID(), "foobar");
      data.stopHTTPServer();
      synchronized (monitor) {
        monitor.notify();
      }
    }

    @Override
    public MetaGlobalDelegate deferred() {
      return this;
    }
  }

  public class ExistingMetaGlobalFetchDelegate implements MetaGlobalDelegate {
    MetaGlobal global;
    public ExistingMetaGlobalFetchDelegate(MetaGlobal g) {
      global = g;
    }

    public void handleSuccess(MetaGlobal global) {
      assertEquals(global.getResponse().getStatusCode(), 200);
      assertFalse(global.isModified);
      assertEquals(global.getSyncID(), "zPSQTm7WBVWB");
      assertTrue(global.getEngines() instanceof ExtendedJSONObject);
      assertEquals(global.getStorageVersion(), new Long(5));
      data.stopHTTPServer();
      synchronized (monitor) {
        monitor.notify();
      }
    }

    public void handleFailure(SyncStorageResponse response) {
      fail("Fetch should succeed.");
    }

    public void handleError(Exception e) {
      e.printStackTrace();
      fail("Fetch should succeed.");
    }

    public void handleMissing(MetaGlobal global) {
    }

    @Override
    public MetaGlobalDelegate deferred() {
      return this;
    }
  }

  public class MissingMetaGlobalServer extends MockServer {
    public void handle(Request request, Response response) {
      this.handle(request, response, 404, "{}");
    }
  }
  public class ExistingMetaGlobalServer extends MockServer {
    public void handle(Request request, Response response) {
      String body = "{\"id\":\"global\",\"payload\":\"{\\\"syncID\\\":\\\"zPSQTm7WBVWB\\\",\\\"storageVersion\\\":5,\\\"engines\\\":{\\\"clients\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"fDg0MS5bDtV7\\\"},\\\"bookmarks\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"NNaQr6_F-9dm\\\"},\\\"forms\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"GXF29AFprnvc\\\"},\\\"history\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"av75g4vm-_rp\\\"},\\\"passwords\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"LT_ACGpuKZ6a\\\"},\\\"prefs\\\":{\\\"version\\\":2,\\\"syncID\\\":\\\"-3nsksP9wSAs\\\"},\\\"tabs\\\":{\\\"version\\\":1,\\\"syncID\\\":\\\"W4H5lOMChkYA\\\"}}}\",\"username\":\"5817483\",\"modified\":1.32046073744E9}";
      this.handle(request, response, 200, body);
    }
  }

  @Test
  public void testMetaGlobalMissingFetch() throws InterruptedException {
    data.startHTTPServer(new MissingMetaGlobalServer());
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertFalse(global.isModified);
    global.setSyncID("foobar");
    assertTrue(global.isModified);
    assertEquals(global.getSyncID(), "foobar");
    synchronized (monitor) {
      global.fetch(new MissingMetaGlobalFetchDelegate(global));
      monitor.wait();
    }
  }

  @Test
  public void testMetaGlobalExistingFetch() throws InterruptedException {
    data.startHTTPServer(new ExistingMetaGlobalServer());
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertNull(global.getSyncID());
    assertNull(global.getEngines());
    assertNull(global.getStorageVersion());
    synchronized (monitor) {
      global.fetch(new ExistingMetaGlobalFetchDelegate(global));
      monitor.wait();
    }
  }

  // TODO: upload test.
}
