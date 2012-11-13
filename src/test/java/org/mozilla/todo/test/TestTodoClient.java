package org.mozilla.todo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.log.writers.LogWriter;
import org.mozilla.todo.test.BlockingTodoClient.BlockingTodoClientPermissionException;

/**
 * Test 123done.org todo list API.
 */
public class TestTodoClient {
  public static final String HOST = "123done.org";
  // public static final String HOST = "localhost:8080";
  public static final String SERVER_URL = "http://" + HOST;
  public static final String USERNAME = "test";
  public static final String EMAIL = USERNAME + "@mockmyid.com";

  protected final LogWriter logWriter = null; // For debugging: new StdoutLogWriter();

  protected final String assertion;
  protected BlockingTodoClient client;

  public TestTodoClient() {
    try {
      assertion = JWCrypto.createMockMyIdAssertion(USERNAME, SERVER_URL);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    if (logWriter != null) {
      Logger.startLoggingTo(logWriter);
    }

    client = new BlockingTodoClient(SERVER_URL);
  }

  @After
  public void tearDown() {
    if (logWriter != null) {
      Logger.stopLoggingTo(logWriter);
    }
  }

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

  @Test
  public void testStatusWhileLoggedOut() throws Exception {
    String loggedInEmail = client.status();
    assertNull(loggedInEmail);
  }

  @Test
  public void testLogoutWhileLoggedOut() throws Exception {
    try {
      client.logout();
      fail("Expected permission exception.");
    } catch (WaitHelper.InnerError e) {
      assertTrue(e.innerError instanceof BlockingTodoClientPermissionException);
    }

    String loggedInEmail = client.status();
    assertNull(loggedInEmail);
  }

  @Test
  public void testLoginLogout() throws Exception {
    client.login(assertion);
    assertNotNull(client.status());

    client.logout();
    assertNull(client.status());
  }

  @Test
  public void testMultipleLogins() throws Exception {
    assertNull(client.status());
    client.login(assertion);
    assertNotNull(client.status());

    final String cookie = client.getSessionCookie();
    assertNotNull(cookie);

    client.logout();
    assertNull(client.status());

    assertNull(client.status());
    client.login(assertion);
    assertNotNull(client.status());

    final String cookie2 = client.getSessionCookie();
    assertNotNull(cookie2);
    assertFalse(cookie.equals(cookie2));

    client.logout();
  }

  @Test
  public void testGetSave() throws Exception {
    client.login(assertion);
    assertNotNull(client.status());

    final int NUM = 3;
    final JSONArray out = newTestTodoList(NUM);
    client.save(out);

    final JSONArray in = client.get();
    assertTrue(Utils.sameArrays(out, in));
    assertEquals(NUM, out.size());

    client.logout();
  }

  @Test
  public void testGetWhileLoggedOut() throws Exception {
    try {
      client.get();
      fail("Expected permission exception.");
    } catch (WaitHelper.InnerError e) {
      assertTrue(e.innerError instanceof BlockingTodoClientPermissionException);
    }
  }

  @Test
  public void testAfterLogout() throws Exception {
    client.login(assertion);
    assertNotNull(client.status());
    client.logout();
    assertNull(client.status());

    try {
      client.save(this.newTestTodoList(4));
      fail("Expected permission exception.");
    } catch (WaitHelper.InnerError e) {
      assertTrue(e.innerError instanceof BlockingTodoClientPermissionException);
    }
  }
}
