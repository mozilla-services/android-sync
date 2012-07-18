package org.mozilla.persona.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.json.simple.JSONArray;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

/**
 * Test 123done.org todo list API.
 */
public class TodoServerTest {
  public static final String HOST = "123done.org";
  // public static final String HOST = "localhost:8080";
  public static final String SERVER_URL = "http://" + HOST;
  public static final String USERNAME = "testx";
  public static final String EMAIL = USERNAME + "@mockmyid.com";

  protected String assertion;

  @SuppressWarnings("unchecked")
  protected JSONArray newTestTodoList(final int numTodos) {
    final JSONArray todos = new JSONArray();

    for (int i = 0; i < numTodos; i++) {
      final ExtendedJSONObject todo = new ExtendedJSONObject();
      todo.put("v", "todo " + (i + 1));
      todo.put("done", false);
      todos.add(todo.object);
    }

    return todos;
  }

  /**
   * Sign a public key asserting ownership of username@mockmyid.com with mockmyid.com's private key.
   *
   * @param publicKeyToSign public key to sign.
   * @param username sign username@mockmyid.com
   * @return Java Web Signature.
   * @throws Exception
   */
  protected String createMockMyIdCertificate(final ExtendedJSONObject publicKeyToSign, final String username) throws Exception {
    final ExtendedJSONObject mockMyIdSecretKey = new ExtendedJSONObject();
    mockMyIdSecretKey.put("n", new BigInteger("15498874758090276039465094105837231567265546373975960480941122651107772824121527483107402353899846252489837024870191707394743196399582959425513904762996756672089693541009892030848825079649783086005554442490232900875792851786203948088457942416978976455297428077460890650409549242124655536986141363719589882160081480785048965686285142002320767066674879737238012064156675899512503143225481933864507793118457805792064445502834162315532113963746801770187685650408560424682654937744713813773896962263709692724630650952159596951348264005004375017610441835956073275708740239518011400991972811669493356682993446554779893834303").toString(10));
    mockMyIdSecretKey.put("d", new BigInteger("6539906961872354450087244036236367269804254381890095841127085551577495913426869112377010004955160417265879626558436936025363204803913318582680951558904318308893730033158178650549970379367915856087364428530828396795995781364659413467784853435450762392157026962694408807947047846891301466649598749901605789115278274397848888140105306063608217776127549926721544215720872305194645129403056801987422794114703255989202755511523434098625000826968430077091984351410839837395828971692109391386427709263149504336916566097901771762648090880994773325283207496645630792248007805177873532441314470502254528486411726581424522838833").toString(10));

    final long issuedAt = System.currentTimeMillis();
    final long expiresAt = issuedAt + 60 * 60 * 1000;
    return RSAJWCrypto.certificate(publicKeyToSign, username + "@mockmyid.com", "mockmyid.com", issuedAt, expiresAt, mockMyIdSecretKey);
  }

  /**
   * Generate a BrowserID assertion.
   *
   * @return assertion.
   * @throws Exception
   */
  protected String getAssertion() throws Exception {
    if (assertion != null) {
      return assertion;
    }

    final ExtendedJSONObject pair = RSAJWCrypto.generateKeypair(2048);
    final ExtendedJSONObject publicKeyToSign = pair.getObject("publicKey");
    final ExtendedJSONObject privateKeyToSignWith = pair.getObject("privateKey");

    String certificate;
    try {
      certificate = createMockMyIdCertificate(publicKeyToSign, USERNAME);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    final String issuer = "127.0.0.1";
    final String audience = SERVER_URL;
    assertion = RSAJWCrypto.assertion(privateKeyToSignWith, certificate, issuer, audience);
    return assertion;
  }

  @Test
  public void testLoginLogout() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(getAssertion());
    assertNotNull(todoServer.getUserLoggedIn());

    todoServer.logout();
    assertNull(todoServer.getUserLoggedIn());
  }

  @Test
  public void testMultipleLogins() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(getAssertion());
    assertNotNull(todoServer.getUserLoggedIn());

    final String cookie = todoServer.getSessionCookie();
    assertNotNull(cookie);

    todoServer.logout();
    assertNull(todoServer.getUserLoggedIn());

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(getAssertion());
    assertNotNull(todoServer.getUserLoggedIn());

    final String cookie2 = todoServer.getSessionCookie();
    assertNotNull(cookie2);
    assertFalse(cookie.equals(cookie2));

    final int NUM = 3;
    final JSONArray out = newTestTodoList(NUM);
    todoServer.save(out);

    final JSONArray in = todoServer.get();
    assertTrue(Utils.sameArrays(out, in));
    assertEquals(NUM, out.size());

    todoServer.logout();
  }

  @Test
  public void testGetSave() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    todoServer.login(getAssertion());
    assertNotNull(todoServer.getUserLoggedIn());

    final int NUM = 3;
    final JSONArray out = newTestTodoList(NUM);
    todoServer.save(out);

    final JSONArray in = todoServer.get();
    assertTrue(Utils.sameArrays(out, in));
    assertEquals(NUM, out.size());

    todoServer.logout();
  }

  @Test
  public void testNotLoggedIn() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    try {
      todoServer.get();
      fail("Expected exception.");
    } catch (WaitHelper.InnerError e) {
      // Do nothing, we expected this exception.
    }
  }


  @Test
  public void testAfterLogout() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    todoServer.login(getAssertion());
    assertNotNull(todoServer.getUserLoggedIn());
    todoServer.logout();
    assertNull(todoServer.getUserLoggedIn());

    try {
      todoServer.get();
      fail("Expected exception.");
    } catch (WaitHelper.InnerError e) {
      // Do nothing, we expected this exception.
    }
  }
}
