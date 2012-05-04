/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.stage.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.net.test.TestMetaGlobal;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.FreshStartDelegate;
import org.mozilla.gecko.sync.delegates.KeyUploadDelegate;
import org.mozilla.gecko.sync.delegates.WipeServerDelegate;
import org.mozilla.gecko.sync.stage.FetchMetaGlobalStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestFetchMetaGlobalStage {
  @SuppressWarnings("unused")
  private static final String  LOG_TAG          = "TestMetaGlobalStage";

  private static final int     TEST_PORT        = 15325;
  private static final String  TEST_SERVER      = "http://localhost:" + TEST_PORT + "/";
  private static final String  TEST_CLUSTER_URL = TEST_SERVER + "cluster/";
  static String                TEST_NW_URL      = TEST_SERVER + "/1.0/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/node/weave"; // GET https://server/pathname/version/username/node/weave
  private HTTPServerTestHelper data             = new HTTPServerTestHelper(TEST_PORT);

  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";

  private final String TEST_INFO_COLLECTIONS_JSON = "{}";

  private static final String  TEST_SYNC_ID         = "testSyncID";
  private static final long    TEST_STORAGE_VERSION = GlobalSession.STORAGE_VERSION;

  private InfoCollections infoCollections;
  private KeyBundle syncKeyBundle;
  private MockGlobalSessionCallback callback;
  private GlobalSession session;

  private boolean calledRequiresUpgrade = false;
  private boolean calledProcessMissingMetaGlobal = false;
  private boolean calledFreshStart = false;
  private boolean calledWipeServer = false;
  private boolean calledUploadKeys = false;

  @Before
  public void setUp()
      throws IllegalStateException, NonObjectJSONException, IOException,
      ParseException, CryptoException, SyncConfigurationException, IllegalArgumentException, URISyntaxException {
    calledRequiresUpgrade = false;
    calledProcessMissingMetaGlobal = false;
    calledFreshStart = false;
    calledWipeServer = false;
    calledUploadKeys = false;

    // Set info collections to not have crypto.
    infoCollections = new InfoCollections(null, null);
    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_INFO_COLLECTIONS_JSON));

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);
    callback = new MockGlobalSessionCallback();
    session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
      syncKeyBundle, callback) {
      @Override
      protected void prepareStages() {
        super.prepareStages();
        Map<Stage, GlobalSyncStage> stages = new HashMap<Stage, GlobalSyncStage>(this.stages);
        stages.put(Stage.fetchMetaGlobal, new FetchMetaGlobalStage(this));
        this.stages = stages;
      }

      @Override
      public void requiresUpgrade() {
        calledRequiresUpgrade = true;
        this.abort(null, "Requires upgrade");
      }

      @Override
      public void processMissingMetaGlobal(MetaGlobal mg) {
        calledProcessMissingMetaGlobal = true;
        this.abort(null, "Missing meta/global");
      }

      // Don't really uploadKeys.
      @Override
      public void uploadKeys(CollectionKeys keys, KeyUploadDelegate keyUploadDelegate) {
        calledUploadKeys = true;
        keyUploadDelegate.onKeysUploaded();
      }

      // On fresh start completed, just stop.
      @Override
      public void freshStart() {
        calledFreshStart = true;
        freshStart(this, new FreshStartDelegate() {
          @Override
          public void onFreshStartFailed(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void onFreshStart() {
            WaitHelper.getTestWaiter().performNotify();
          }
        });
      }

      // Don't really wipeServer.
      @Override
      protected void wipeServer(final CredentialsSource credentials, final WipeServerDelegate wipeDelegate) {
        calledWipeServer = true;
        wipeDelegate.onWiped(System.currentTimeMillis());
      }
    };
    session.config.setClusterURL(new URI(TEST_CLUSTER_URL));
    session.config.infoCollections = infoCollections;
  }

  protected void doSession(MockServer server) {
    data.startHTTPServer(server);
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      public void run() {
        try {
          session.start();
        } catch (AlreadySyncingException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    }));
    data.stopHTTPServer();
  }

  @Test
  public void testFetchRequiresUpgrade() throws Exception {
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setSyncID(TEST_SYNC_ID);
    mg.setStorageVersion(new Long(TEST_STORAGE_VERSION + 1));

    MockServer server = new MockServer(200, mg.asCryptoRecord().toJSONString());
    doSession(server);

    assertEquals(true, callback.calledError);
    assertTrue(calledRequiresUpgrade);
  }

  @Test
  public void testFetchSuccess() throws Exception {
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setSyncID(TEST_SYNC_ID);
    mg.setStorageVersion(new Long(TEST_STORAGE_VERSION));

    MockServer server = new MockServer(200, mg.asCryptoRecord().toJSONString());
    doSession(server);

    assertEquals(true, callback.calledSuccess);
    assertEquals(TEST_SYNC_ID, session.config.metaGlobal.getSyncID());
    assertEquals(TEST_STORAGE_VERSION, session.config.metaGlobal.getStorageVersion().longValue());
  }

  @Test
  public void testFetchMissing() throws Exception {
    MockServer server = new MockServer(404, "missing");
    doSession(server);

    assertEquals(true, callback.calledError);
    assertTrue(calledProcessMissingMetaGlobal);
  }

  /**
   * Empty payload object has no syncID or storageVersion and should call freshStart.
   * @throws Exception
   */
  @Test
  public void testFetchEmptyPayload() throws Exception {
    MockServer server = new MockServer(200, TestMetaGlobal.TEST_META_GLOBAL_EMPTY_PAYLOAD_RESPONSE);
    doSession(server);

    assertTrue(calledFreshStart);
  }

  /**
   * No payload means no syncID or storageVersion and therefore we should call freshStart.
   * @throws Exception
   */
  @Test
  public void testFetchNoPayload() throws Exception {
    MockServer server = new MockServer(200, TestMetaGlobal.TEST_META_GLOBAL_NO_PAYLOAD_RESPONSE);
    doSession(server);

    assertTrue(calledFreshStart);
  }

  /**
   * Malformed payload is a server response issue, not a meta/global record
   * issue. This should error out of the sync.
   * @throws Exception
   */
  @Test
  public void testFetchMalformedPayload() throws Exception {
    MockServer server = new MockServer(200, TestMetaGlobal.TEST_META_GLOBAL_MALFORMED_PAYLOAD_RESPONSE);
    doSession(server);

    assertEquals(true, callback.calledError);
    assertEquals(ParseException.class, callback.calledErrorException.getClass());
  }

  protected void doFreshStart(MockServer server) {
    data.startHTTPServer(server);
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      public void run() {
        session.freshStart();
      }
    }));
    data.stopHTTPServer();
  }

  @Test
  public void testFreshStart() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    final AtomicBoolean mgUploaded = new AtomicBoolean(false);
    final AtomicBoolean mgDownloaded = new AtomicBoolean(false);
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
        if (mgUploaded.get()) {
          mgDownloaded.set(true);
          try {
            CryptoRecord rec = uploadedMg.asCryptoRecord();
            rec.keyBundle = syncKeyBundle;
            rec.encrypt();
            this.handle(request, response, 200, rec.toJSONString());
            return;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        this.handle(request, response, 404, "missing");
      }
    };
    doFreshStart(server);

    assertTrue(this.calledFreshStart);
    assertTrue(this.calledWipeServer);
    assertTrue(this.calledUploadKeys);
    assertTrue(mgUploaded.get());
    assertTrue(mgDownloaded.get());
    assertEquals(GlobalSession.STORAGE_VERSION, uploadedMg.getStorageVersion().longValue());
  }
}
