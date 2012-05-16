/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.android.sync.test.TestSynchronizer.TrackingWBORepository;
import org.mozilla.android.sync.test.helpers.BaseTestStorageRequestDelegate;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockRecord;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.Server11Repository;
import org.mozilla.gecko.sync.repositories.Server11RepositorySession;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.simpleframework.http.ContentType;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.HttpEntity;

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

  public class MockServer11RepositorySession extends Server11RepositorySession {
    public MockServer11RepositorySession(Repository repository) {
      super(repository);
    }

    public RecordUploadRunnable getRecordUploadRunnable() {
      // TODO: implement upload delegate in the class, too!
      return new RecordUploadRunnable(null, recordsBuffer, recordGuidsBuffer, byteCount);
    }

    public void enqueueRecord(Record r) {
      super.enqueue(r);
    }

    public HttpEntity getEntity() {
      return this.getRecordUploadRunnable().getBodyEntity();
    }
  }

  public class TestSyncStorageRequestDelegate extends
      BaseTestStorageRequestDelegate {
    @Override
    public void handleRequestSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      BaseResource.consumeEntity(res);
      data.stopHTTPServer();
    }
  }

  @Test
  public void test() throws URISyntaxException {

    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(new POSTMockServer());

    MockServer11RepositorySession session = new MockServer11RepositorySession(
        null);
    session.enqueueRecord(new MockRecord(Utils.generateGuid(), null, 0, false));
    session.enqueueRecord(new MockRecord(Utils.generateGuid(), null, 0, false));

    URI uri = new URI(LOCAL_REQUEST_URL);
    SyncStorageRecordRequest r = new SyncStorageRecordRequest(uri);
    TestSyncStorageRequestDelegate delegate = new TestSyncStorageRequestDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.post(session.getEntity());
  }

  protected TrackingWBORepository getLocal() {
    final TrackingWBORepository local = new TrackingWBORepository();
    BookmarkRecord[] outbounds = new BookmarkRecord[] {
        new BookmarkRecord("outboundFail1", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail2", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail3", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail4", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail5", "bookmarks", 1, false),
        new BookmarkRecord("outboundFail6", "bookmarks", 1, false),
    };
    for (BookmarkRecord outbound : outbounds) {
      local.wbos.put(outbound.guid, outbound);
    }
    return local;
  }

  @Test
  public void testErrors() throws Exception {
    final String COLLECTION = "test";
    Logger.LOG_TO_STDOUT = true;

    final TrackingWBORepository local = getLocal();
    MockServer mockServer = new MockServer(200, "{ success: [\"outboundFail1\"], failed: { outboundFail2: [] } }");

    final Server11Repository remote = new Server11Repository(TEST_SERVER, USERNAME, COLLECTION, this);
    KeyBundle collectionKey = new KeyBundle(USERNAME, SYNC_KEY);
    Crypto5MiddlewareRepository cryptoRepo = new Crypto5MiddlewareRepository(remote, collectionKey);
    cryptoRepo.recordFactory = new BookmarkRecordFactory();

    final Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = cryptoRepo;
    synchronizer.repositoryB = local;

    data.startHTTPServer(mockServer);
    assertNull(TestSynchronizer.doSynchronize(synchronizer));
    data.stopHTTPServer();
  }
}
