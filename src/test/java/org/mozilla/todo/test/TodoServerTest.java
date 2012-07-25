package org.mozilla.todo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONArray;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.persona.crypto.JWCrypto;
import org.mozilla.persona.crypto.RSAJWCrypto;
import org.mozilla.todo.TodoServer;

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
      certificate = JWCrypto.createMockMyIdCertificate(publicKeyToSign, USERNAME);
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
