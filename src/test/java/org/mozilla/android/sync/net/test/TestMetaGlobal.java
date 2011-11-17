package org.mozilla.android.sync.net.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.android.sync.net.MetaGlobal;
import org.mozilla.android.sync.net.MetaGlobalDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestMetaGlobal {
  public static final String USER_PASS = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  public static final String META_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  private TestResourceData data        = new TestResourceData();

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
      assertEquals(global.getSyncID(), "1234567");
      assertEquals(global.getEngines().toJSONString(), "{}");
      assertEquals(global.getStorageVersion(), new Long(5));
      data.stopHTTPServer(); 
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
  }

  public class MissingMetaGlobalServer extends TestServer {
    public void handle(Request request, Response response) {
      this.handle(request, response, 404, "{}");
    }
  }
  public class ExistingMetaGlobalServer extends TestServer {
    public void handle(Request request, Response response) {
      String body = "{\"syncID\": \"1234567\", \"engines\": {}, \"storageVersion\": 5}";
      this.handle(request, response, 200, body);      
    }
  }

  @Test
  public void testMetaGlobalMissingFetch() {
    data.startHTTPServer(new MissingMetaGlobalServer());
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertFalse(global.isModified);
    global.setSyncID("foobar");
    assertTrue(global.isModified);
    assertEquals(global.getSyncID(), "foobar");
    global.fetch(new MissingMetaGlobalFetchDelegate(global));
  }

  @Test
  public void testMetaGlobalExistingFetch() {
    data.startHTTPServer(new ExistingMetaGlobalServer());
    MetaGlobal global = new MetaGlobal(META_URL, USER_PASS);
    assertNull(global.getSyncID());
    assertNull(global.getEngines());
    assertNull(global.getStorageVersion());
    global.fetch(new ExistingMetaGlobalFetchDelegate(global));
  }
}
