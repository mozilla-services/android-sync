/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.AssertionFailedError;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockAbstractNonRepositorySyncStage;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockPrefsGlobalSession;
import org.mozilla.android.sync.test.helpers.MockResourceDelegate;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.MockServerSyncStage;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.domain.VersionConstants;
import org.mozilla.gecko.sync.stage.AndroidBrowserBookmarksServerSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.NoSuchStageException;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

public class TestGlobalSession {
  public static final String LOG_TAG = TestGlobalSession.class.getSimpleName();

  private int          TEST_PORT                = HTTPServerTestHelper.getTestPort();
  private final String TEST_CLUSTER_URL         = "http://localhost:" + TEST_PORT;
  private final String TEST_USERNAME            = "johndoe";
  private final String TEST_PASSWORD            = "password";
  private final String TEST_SYNC_KEY            = "abcdeabcdeabcdeabcdeabcdea";
  private final long   TEST_BACKOFF_IN_SECONDS  = 2401;

  public static WaitHelper getTestWaiter() {
    return WaitHelper.getTestWaiter();
  }

  protected Set<Class<? extends Object>> getClasses(Collection<? extends Object> set) {
    Set<Class<? extends Object>> newSet = new HashSet<Class<? extends Object>>();
    for (Object o : set) {
      newSet.add(o.getClass());
    }
    return newSet;
  }

  @Test
  public void testGetSyncStagesBy() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException, NoSuchStageException {

    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    GlobalSession s = new MockPrefsGlobalSession(SyncConfiguration.DEFAULT_USER_API, TEST_CLUSTER_URL,
                                                 TEST_USERNAME, TEST_PASSWORD, null,
                                                 new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY),
                                                 callback, /* context */ null, null, null);

    assertTrue(s.getSyncStageByEnum(Stage.syncBookmarks) instanceof AndroidBrowserBookmarksServerSyncStage);

    final Set<String> empty = new HashSet<String>();

    final Set<String> bookmarksAndTabsNames = new HashSet<String>();
    bookmarksAndTabsNames.add("bookmarks");
    bookmarksAndTabsNames.add("tabs");

    final Set<GlobalSyncStage> bookmarksAndTabsSyncStages = new HashSet<GlobalSyncStage>();
    GlobalSyncStage bookmarksStage = s.getSyncStageByName("bookmarks");
    GlobalSyncStage tabsStage = s.getSyncStageByEnum(Stage.syncTabs);
    assertNotNull(bookmarksStage);
    assertNotNull(tabsStage);
    bookmarksAndTabsSyncStages.add(bookmarksStage);
    bookmarksAndTabsSyncStages.add(tabsStage);

    final Set<Stage> bookmarksAndTabsEnums = new HashSet<Stage>();
    bookmarksAndTabsEnums.add(Stage.syncBookmarks);
    bookmarksAndTabsEnums.add(Stage.syncTabs);

    assertTrue(s.getSyncStagesByName(empty).isEmpty());

    // This is a little odd: global sessions create stage instances on demand,
    // so we won't get identical instances.
    assertEquals(getClasses(bookmarksAndTabsSyncStages), getClasses(s.getSyncStagesByName(bookmarksAndTabsNames)));
    assertEquals(getClasses(bookmarksAndTabsSyncStages), getClasses(s.getSyncStagesByEnum(bookmarksAndTabsEnums)));
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

      // Stage fakes a 503 and sets X-Weave-Backoff header to the given seconds.
      final GlobalSyncStage stage = new MockAbstractNonRepositorySyncStage() {
        @Override
        public void execute() {
          final HttpResponse response = new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 503, "Illegal method/protocol"));

          response.addHeader("X-Weave-Backoff", Long.toString(TEST_BACKOFF_IN_SECONDS)); // Backoff given in seconds.
          session.handleHTTPError(new SyncStorageResponse(response), "Failure fetching info/collections.");
        }
      };

      // Session installs fake stage to fetch info/collections.
      final GlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
          new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback)
      .withStage(Stage.fetchInfoCollections, stage);

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

  private HTTPServerTestHelper data = new HTTPServerTestHelper();

  @SuppressWarnings("static-method")
  @Before
  public void setUp() {
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

  public MockGlobalSessionCallback doTestSuccess(MockServer server, final GlobalSyncStage[] outerStages) throws Exception {
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final MockGlobalSession session = new MockGlobalSession(TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback);

    Stage index = Stage.values()[1];
    for (GlobalSyncStage stage : outerStages) {
      session.withStage(index, stage);
      index = GlobalSession.nextStage(index);
    }

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

  public MockGlobalSessionCallback doTestSuccess(final boolean stageShouldBackoff, final boolean stageShouldAdvance) throws Exception {
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
      public void execute() {
        doRequest();
        if (stageShouldAdvance) {
          session.advance();
          return;
        }
        session.abort(null,  "Stage intentionally failed.");
      }
    };

    return doTestSuccess(server, new MockServerSyncStage[] { stage });
  }

  @Test
  public void testOnSuccessBackoffAdvanced() throws Exception,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(true, true);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
  }

  @Test
  public void testOnSuccessBackoffAborted() throws Exception,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(true, false);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertTrue(callback.calledRequestBackoff);
    assertEquals(1000 * TEST_BACKOFF_IN_SECONDS, callback.weaveBackoff);
  }

  @Test
  public void testOnSuccessNoBackoffAdvanced() throws Exception,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(false, true);

    assertTrue(callback.calledSuccess);
    assertFalse(callback.calledRequestBackoff);

    callback.stagesCompleted.add(Stage.completed);
    assertEquals(Arrays.asList(Stage.values()), callback.stagesCompleted);
  }

  @Test
  public void testOnSuccessNoBackoffAborted() throws Exception,
      ParseException, CryptoException {
    MockGlobalSessionCallback callback = doTestSuccess(false, false);

    assertTrue(callback.calledError); // TODO: this should be calledAborted.
    assertFalse(callback.calledRequestBackoff);
  }

  @Test
  public void testGenerateNewMetaGlobalNonePersisted() throws Exception {
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockPrefsGlobalSession(SyncConfiguration.DEFAULT_USER_API, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD, null,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback, null, null, null);

    // Verify we fill in all of our known engines when none are persisted.
    session.config.enabledEngineNames = null;
    MetaGlobal mg = session.generateNewMetaGlobal();
    assertEquals(Long.valueOf(GlobalSession.STORAGE_VERSION), mg.getStorageVersion());
    assertEquals(VersionConstants.BOOKMARKS_ENGINE_VERSION, mg.getEngines().getObject("bookmarks").getIntegerSafely("version").intValue());
    assertEquals(VersionConstants.CLIENTS_ENGINE_VERSION, mg.getEngines().getObject("clients").getIntegerSafely("version").intValue());

    List<String> namesList = new ArrayList<String>(mg.getEnabledEngineNames());
    Collections.sort(namesList);
    String[] names = namesList.toArray(new String[namesList.size()]);
    String[] expected = new String[] { "bookmarks", "clients", "forms", "history", "passwords", "tabs" };
    assertArrayEquals(expected, names);
  }

  @Test
  public void testGenerateNewMetaGlobalSomePersisted() throws Exception {
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockPrefsGlobalSession(SyncConfiguration.DEFAULT_USER_API, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD, null,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback, null, null, null);

    // Verify we preserve engines with version 0 if some are persisted.
    session.config.enabledEngineNames = new HashSet<String>();
    session.config.enabledEngineNames.add("bookmarks");
    session.config.enabledEngineNames.add("clients");
    session.config.enabledEngineNames.add("addons");
    session.config.enabledEngineNames.add("prefs");

    MetaGlobal mg = session.generateNewMetaGlobal();
    assertEquals(Long.valueOf(GlobalSession.STORAGE_VERSION), mg.getStorageVersion());
    assertEquals(VersionConstants.BOOKMARKS_ENGINE_VERSION, mg.getEngines().getObject("bookmarks").getIntegerSafely("version").intValue());
    assertEquals(VersionConstants.CLIENTS_ENGINE_VERSION, mg.getEngines().getObject("clients").getIntegerSafely("version").intValue());
    assertEquals(0, mg.getEngines().getObject("addons").getIntegerSafely("version").intValue());
    assertEquals(0, mg.getEngines().getObject("prefs").getIntegerSafely("version").intValue());

    List<String> namesList = new ArrayList<String>(mg.getEnabledEngineNames());
    Collections.sort(namesList);
    String[] names = namesList.toArray(new String[namesList.size()]);
    String[] expected = new String[] { "addons", "bookmarks", "clients", "prefs" };
    assertArrayEquals(expected, names);
  }

  @Test
  public void testUploadUpdatedMetaGlobal() throws Exception {
    // Set up session with meta/global.
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockPrefsGlobalSession(SyncConfiguration.DEFAULT_USER_API, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD, null,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback, null, null, null);
    session.config.metaGlobal = session.generateNewMetaGlobal();
    session.enginesToUpdate.clear();

    // Set enabledEngines in meta/global, including a "new engine."
    String[] origEngines = new String[] { "bookmarks", "clients", "forms", "history", "tabs", "new-engine" };

    ExtendedJSONObject origEnginesJSONObject = new ExtendedJSONObject();
    for (String engineName : origEngines) {
      EngineSettings mockEngineSettings = new EngineSettings(Utils.generateGuid(), Integer.valueOf(0));
      origEnginesJSONObject.put(engineName, mockEngineSettings);
    }
    session.config.metaGlobal.setEngines(origEnginesJSONObject);

    // Engines to remove.
    String[] toRemove = new String[] { "bookmarks", "tabs" };
    for (String name : toRemove) {
      session.removeEngineFromMetaGlobal(name);
    }

    // Engines to add.
    String[] toAdd = new String[] { "passwords" };
    for (String name : toAdd) {
      String syncId = Utils.generateGuid();
      session.recordForMetaGlobalUpdate(name, new EngineSettings(syncId, Integer.valueOf(1)));
    }

    // Update engines.
    session.uploadUpdatedMetaGlobal();

    // Check resulting enabledEngines.
    Set<String> expected = new HashSet<String>();
    for (String name : origEngines) {
      expected.add(name);
    }
    for (String name : toRemove) {
      expected.remove(name);
    }
    for (String name : toAdd) {
      expected.add(name);
    }
    assertEquals(expected, session.config.metaGlobal.getEnabledEngineNames());
  }

  @Test
  public void testNextStage() {
    assertEquals(GlobalSession.nextStage(Stage.idle), Stage.checkPreconditions);
    assertEquals(GlobalSession.nextStage(Stage.completed), Stage.idle);
  }

  /**
   * Verify that we can restart a sync part way through.
   *
   * @throws Exception
   */
  @Test
  public void testRestart() throws Exception {
    final AtomicLong ran1 = new AtomicLong(0);
    final AtomicLong ran2 = new AtomicLong(0);
    final AtomicLong ran3 = new AtomicLong(0);

    GlobalSyncStage stage1 = new MockServerSyncStage() {
      @Override
      public void execute(GlobalSession session) {
        long value = ran1.incrementAndGet();
        Logger.info(LOG_TAG, "Running stage 1 for the nth (n = " + value + ") time");

        session.advance();
      }
    };

    GlobalSyncStage stage2 = new MockServerSyncStage() {
      @Override
      public void execute(GlobalSession session) {
        long value = ran2.incrementAndGet();
        Logger.info(LOG_TAG, "Running stage 2 for the nth (n = " + value + ") time");

        if (value == 1) {
          try {
            session.restart();
          } catch (AlreadySyncingException e) {
            session.abort(e, "Session failed.");
          }
          return;
        }
        session.advance();
      }
    };

    GlobalSyncStage stage3 = new MockServerSyncStage() {
      @Override
      public void execute(GlobalSession session) {
        long value = ran3.incrementAndGet();
        Logger.info(LOG_TAG, "Running stage 3 for the nth (n = " + value + ") time");

        session.advance();
      }
    };

    GlobalSyncStage[] stages = new GlobalSyncStage[] { stage1, stage2, stage3 };

    MockGlobalSessionCallback callback = doTestSuccess(new MockServer(), stages);

    if (callback.calledErrorException != null) {
      throw callback.calledErrorException;
    }
    assertTrue(callback.calledSuccess);

    assertEquals(2, ran1.get());
    assertEquals(2, ran2.get());
    assertEquals(1, ran3.get());
  }
}
