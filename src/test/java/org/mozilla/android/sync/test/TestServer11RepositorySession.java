/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.mozilla.android.sync.test.SynchronizerHelpers.TrackingWBORepository;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.gecko.sync.repositories.FetchFailedException;
import org.mozilla.gecko.sync.repositories.Server11Repository;
import org.mozilla.gecko.sync.repositories.StoreFailedException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.gecko.sync.synchronizer.ServerLocalSynchronizer;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.simpleframework.http.ContentType;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestServer11RepositorySession implements CredentialsSource {

  public class POSTMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      try {
        String content = request.getContent();
        System.out.println("Content:" + content);
      } catch (IOException e) {
        e.printStackTrace();
      }
      ContentType contentType = request.getContentType();
      System.out.println("Content-Type:" + contentType);
      super.handle(request, response, 200, "{success:[]}");
    }
  }

  private static final int    TEST_PORT   = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT + "/";
  static final String LOCAL_REQUEST_URL   = TEST_SERVER + "1.1/n6ec3u5bee3tixzp2asys7bs6fve4jfw/storage/bookmarks";

  // Corresponds to rnewman+atest1@mozilla.com, local.
  static final String USERNAME          = "n6ec3u5bee3tixzp2asys7bs6fve4jfw";
  static final String USER_PASS         = "n6ec3u5bee3tixzp2asys7bs6fve4jfw:password";
  static final String SYNC_KEY          = "eh7ppnb82iwr5kt3z3uyi5vr44";

  @Override
  public String credentials() {
    return USER_PASS;
  }

  private HTTPServerTestHelper data     = new HTTPServerTestHelper(TEST_PORT);

  protected TrackingWBORepository getLocal(int numRecords) {
    final TrackingWBORepository local = new TrackingWBORepository();
    for (int i = 0; i < numRecords; i++) {
      BookmarkRecord outbound = new BookmarkRecord("outboundFail" + i, "bookmarks", 1, false);
      local.wbos.put(outbound.guid, outbound);
    }
    return local;
  }

  protected Exception doSynchronize(MockServer server) throws Exception {
    final String COLLECTION = "test";

    final TrackingWBORepository local = getLocal(100);
    final Server11Repository remote = new Server11Repository(TEST_SERVER, USERNAME, COLLECTION, this);
    KeyBundle collectionKey = new KeyBundle(USERNAME, SYNC_KEY);
    Crypto5MiddlewareRepository cryptoRepo = new Crypto5MiddlewareRepository(remote, collectionKey, new BookmarkRecordFactory());

    final Synchronizer synchronizer = new ServerLocalSynchronizer();
    synchronizer.repositoryA = cryptoRepo;
    synchronizer.repositoryB = local;

    data.startHTTPServer(server);
    try {
      Exception e = TestServerLocalSynchronizer.doSynchronize(synchronizer);
      return e;
    } finally {
      data.stopHTTPServer();
    }
  }

  @Test
  public void testFetchFailure() throws Exception {
    MockServer server = new MockServer(404, "error");
    Exception e = doSynchronize(server);
    assertNotNull(e);
    assertEquals(FetchFailedException.class, e.getClass());
  }

  @Test
  public void testStorePostSuccessWithFailingRecords() throws Exception {
    MockServer server = new MockServer(200, "{ modified: \" + " + Utils.millisecondsToDecimalSeconds(System.currentTimeMillis()) + ", " +
        "success: []," +
        "failed: { outboundFail2: [] } }");
    Exception e = doSynchronize(server);
    assertNotNull(e);
    assertEquals(StoreFailedException.class, e.getClass());
  }

  @Test
  public void testStorePostFailure() throws Exception {
    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        if (request.getMethod().equals("POST")) {
          this.handle(request, response, 404, "missing");
        }
        this.handle(request, response, 200, "success");
        return;
      }
    };

    Exception e = doSynchronize(server);
    assertNotNull(e);
    assertEquals(StoreFailedException.class, e.getClass());
  }
}
