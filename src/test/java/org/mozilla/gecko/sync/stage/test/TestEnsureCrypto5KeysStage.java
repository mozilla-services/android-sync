/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.stage.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.stage.EnsureCrypto5KeysStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestEnsureCrypto5KeysStage {
  private int          TEST_PORT                = 15325;
  private final String TEST_CLUSTER_URL         = "http://localhost:" + TEST_PORT;
  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";

  private final String TEST_JSON_NO_CRYPTO =
      "{\"history\":1.3319567131E9}";
  private final String TEST_JSON_OLD_CRYPTO =
      "{\"history\":1.3319567131E9,\"crypto\":1.1E9}";
  private final String TEST_JSON_NEW_CRYPTO =
      "{\"history\":1.3319567131E9,\"crypto\":3.1E9}";

  private HTTPServerTestHelper data = new HTTPServerTestHelper(TEST_PORT);

  private InfoCollections infoCollections;
  private KeyBundle syncKeyBundle;
  private MockGlobalSessionCallback callback;
  private GlobalSession session;

  private boolean calledResetStages;
  private Collection<String> stagesReset;

  @Before
  public void setUp()
      throws IllegalStateException, NonObjectJSONException, IOException,
      ParseException, CryptoException, SyncConfigurationException, IllegalArgumentException, URISyntaxException {

    // Set info collections to not have crypto.
    infoCollections = new InfoCollections(null, null);
    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_JSON_NO_CRYPTO));

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);
    callback = new MockGlobalSessionCallback();
    session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
      syncKeyBundle, callback) {
      @Override
      protected void prepareStages() {
        super.prepareStages();
        Map<Stage, GlobalSyncStage> stages = new HashMap<Stage, GlobalSyncStage>(this.stages);
        stages.put(Stage.ensureKeysStage, new EnsureCrypto5KeysStage(this));
        this.stages = stages;
      }

      @Override
      public void resetStagesByEnum(Collection<Stage> stages) {
        calledResetStages = true;
        stagesReset = new ArrayList<String>();
        for (Stage stage : stages) {
          stagesReset.add(stage.name());
        }
      }

      @Override
      public void resetStagesByName(Collection<String> names) {
        calledResetStages = true;
        stagesReset = names;
      }
    };
    session.config.setClusterURL(new URI(TEST_CLUSTER_URL));
    session.config.infoCollections = infoCollections;
    calledResetStages = false;
    stagesReset = null;
  }

  public void doSession(MockServer server) {
    data.startHTTPServer(server);
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          session.start();
        } catch (AlreadySyncingException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
    data.stopHTTPServer();
  }

  @Test
  public void testUploadKeysFails() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException, URISyntaxException {
    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        if (request.getMethod().equals("PUT")) {
          this.handle(request, response, 400, "denied");
          return;
        }
        this.handle(request, response, 404, "not found");
      }
    };
    doSession(server);

    assertTrue(callback.calledError);
    assertTrue(callback.calledErrorException instanceof HTTPFailureException);
  }

  @Test
  public void testUploadKeysSucceeds() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException, URISyntaxException {
    assertNull(session.config.collectionKeys);

    final AtomicBoolean keysUploaded = new AtomicBoolean(false);
    final AtomicBoolean keysDownloaded = new AtomicBoolean(false);
    final CollectionKeys uploadedKeys = new CollectionKeys();

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        if (request.getMethod().equals("PUT")) {
          try {
            ExtendedJSONObject body = ExtendedJSONObject.parseJSONObject(request.getContent());
            System.out.println(body.toJSONString());
            assertTrue(body.containsKey("payload"));
            assertFalse(body.containsKey("default"));

            CryptoRecord rec = CryptoRecord.fromJSONRecord(body);
            uploadedKeys.setKeyPairsFromWBO(rec, syncKeyBundle); // Decrypt.
            keysUploaded.set(true);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          this.handle(request, response, 200, "success");
          return;
        }
        if (keysUploaded.get()) {
          keysDownloaded.set(true);
          try {
            CryptoRecord rec = uploadedKeys.asCryptoRecord();
            rec.keyBundle = syncKeyBundle;
            rec.encrypt();
            this.handle(request, response, 200, rec.toJSONString());
            return;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        this.handle(request, response, 404, "not found");
      }
    };

    doSession(server);

    assertTrue(callback.calledSuccess);
    assertTrue(keysUploaded.get());
    assertTrue(keysDownloaded.get());
    assertNotNull(session.config.collectionKeys);
    assertTrue(session.config.collectionKeys.equals(uploadedKeys));
  }

  @Test
  public void testDownloadUsesPersisted() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException, URISyntaxException {
    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_JSON_OLD_CRYPTO));
    session.config.persistedCryptoKeys().persistLastModified(System.currentTimeMillis());

    assertNull(session.config.collectionKeys);
    final CollectionKeys keys = CollectionKeys.generateCollectionKeys();
    keys.setDefaultKeyBundle(syncKeyBundle);
    session.config.persistedCryptoKeys().persistKeys(keys);

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        this.handle(request, response, 404, "should not be called!");
      }
    };

    doSession(server);

    assertTrue(callback.calledSuccess);
    assertNotNull(session.config.collectionKeys);
    assertTrue(CollectionKeys.differences(session.config.collectionKeys, keys).isEmpty());
  }

  @Test
  public void testDownloadFetchesNew()
      throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException,
      IOException, ParseException, CryptoException, URISyntaxException, NoCollectionKeysSetException {
    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_JSON_NEW_CRYPTO));
    session.config.persistedCryptoKeys().persistLastModified(System.currentTimeMillis());

    assertNull(session.config.collectionKeys);
    final CollectionKeys keys = CollectionKeys.generateCollectionKeys();
    keys.setDefaultKeyBundle(syncKeyBundle);
    session.config.persistedCryptoKeys().persistKeys(keys);

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        try {
          CryptoRecord rec = keys.asCryptoRecord();
          rec.keyBundle = syncKeyBundle;
          rec.encrypt();
          this.handle(request, response, 200, rec.toJSONString());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    doSession(server);

    assertTrue(callback.calledSuccess);
    assertNotNull(session.config.collectionKeys);
    assertTrue(session.config.collectionKeys.equals(keys));
  }

  /**
   * Change the default key but keep one collection key the same. Should reset
   * all but that one collection.
   */
  @Test
  public void testDownloadResetsOnDifferentDefaultKey()
      throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException,
      IOException, ParseException, CryptoException, URISyntaxException, NoCollectionKeysSetException {
    String TEST_COLLECTION = "bookmarks";

    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_JSON_NEW_CRYPTO));
    session.config.persistedCryptoKeys().persistLastModified(System.currentTimeMillis());

    KeyBundle keyBundle = KeyBundle.withRandomKeys();
    assertNull(session.config.collectionKeys);
    final CollectionKeys keys = CollectionKeys.generateCollectionKeys();
    keys.setKeyBundleForCollection(TEST_COLLECTION, keyBundle);
    session.config.persistedCryptoKeys().persistKeys(keys);
    keys.setDefaultKeyBundle(syncKeyBundle); // Change the default key bundle, but keep "bookmarks" the same.

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        try {
          CryptoRecord rec = keys.asCryptoRecord();
          rec.keyBundle = syncKeyBundle;
          rec.encrypt();
          this.handle(request, response, 200, rec.toJSONString());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    doSession(server);

    assertTrue(calledResetStages);
    Collection<String> allButCollection = new ArrayList<String>();
    for (Stage stage : Stage.getNamedStages()) {
      allButCollection.add(stage.getRepositoryName());
    }
    allButCollection.remove(TEST_COLLECTION);
    assertTrue(stagesReset.containsAll(allButCollection));
    assertTrue(allButCollection.containsAll(stagesReset));
    assertTrue(callback.calledError);
  }

  @Test
  public void testDownloadResetsEngineOnDifferentKey()
      throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException,
      IOException, ParseException, CryptoException, URISyntaxException, NoCollectionKeysSetException {
    final String TEST_COLLECTION = "history";

    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject(TEST_JSON_NEW_CRYPTO));
    session.config.persistedCryptoKeys().persistLastModified(System.currentTimeMillis());

    assertNull(session.config.collectionKeys);
    final CollectionKeys keys = CollectionKeys.generateCollectionKeys();
    session.config.persistedCryptoKeys().persistKeys(keys);
    keys.setKeyBundleForCollection(TEST_COLLECTION, syncKeyBundle); // Change one key bundle.

    MockServer server = new MockServer() {
      public void handle(Request request, Response response) {
        try {
          CryptoRecord rec = keys.asCryptoRecord();
          rec.keyBundle = syncKeyBundle;
          rec.encrypt();
          this.handle(request, response, 200, rec.toJSONString());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    doSession(server);

    assertTrue(calledResetStages);
    assertNotNull(stagesReset);
    assertEquals(1, stagesReset.size());
    assertTrue(stagesReset.contains(TEST_COLLECTION));
    assertTrue(callback.calledError);
  }
}
