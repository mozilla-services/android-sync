package org.mozilla.gecko.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HMACAuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncServer2Client;
import org.mozilla.gecko.sync.net.SyncServer2Client.SyncServer2InfoCollectionsDelegate;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.gecko.tokenserver.test.BlockingTokenServerClient;

public class TestLocalSyncServer2Client {
  public static final String TEST_USERNAME = "testx";

  public static final String TEST_LOCAL_TOKEN_SERVER_URL = "http://localhost:5000";
  public static final String TEST_LOCAL_AUDIENCE = "http://localhost:5000"; // Default audience accepted by a local dev token server.
  public static final String TEST_LOCAL_SERVICE_URL = TEST_LOCAL_TOKEN_SERVER_URL + "/1.0/sync/2.0";

  protected SyncServer2Client client;

  protected AuthHeaderProvider authHeaderProvider;
  protected String assertion;
  protected TokenServerToken token;

  public TestLocalSyncServer2Client() {
    BaseResource.rewriteLocalhost = false;
  }

  @Before
  public void setUp() throws Exception {
    assertion = JWCrypto.createMockMyIdAssertion(TEST_USERNAME, TEST_LOCAL_AUDIENCE);

    BlockingTokenServerClient tokenServerClient = new BlockingTokenServerClient(new URI(TEST_LOCAL_SERVICE_URL));
    token = tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true);

    authHeaderProvider = new HMACAuthHeaderProvider(token.id, token.key);

    client = new SyncServer2Client(new URI(token.endpoint), authHeaderProvider);
  }

  protected ExtendedJSONObject getInfoCollections() {
    final List<ExtendedJSONObject> results = new ArrayList<ExtendedJSONObject>();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getInfoCollections(new SyncServer2InfoCollectionsDelegate() {
          @Override
          public void onSuccess(ExtendedJSONObject result) {
            results.add(result);

            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onRemoteFailure() {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException());
          }

          @Override
          public void onRemoteError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException(e));
          }

          @Override
          public void onLocalError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException(e));
          }
        });
      }
    });

    assertEquals(1, results.size());

    return results.get(0);
  }

  @Test
  public void testGetInfoCollections() throws Exception {
    ExtendedJSONObject result = getInfoCollections();

    assertEquals("{}", result.toString());
  }

  @Test
  public void testBadAuthorization() throws Exception {
    authHeaderProvider = new HMACAuthHeaderProvider("BAD TOKEN ID", token.key);

    client = new SyncServer2Client(new URI(token.endpoint), authHeaderProvider);

    try {
      getInfoCollections();

      fail("Expected exception.");
    } catch (WaitHelper.InnerError e) {
      assertEquals(RuntimeException.class, e.innerError.getClass());
    }
  }
}
