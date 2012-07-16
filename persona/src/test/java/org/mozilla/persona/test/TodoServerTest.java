package org.mozilla.persona.test;

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

/**
 * Test 123done.org todo list app.
 * <p>
 * To generate new assertion, valid for 2 days:
 * <p>
 * <code>
 * ./gen_mockmyid.js -u test@mockmyid.com -a "http://123done.org"
 * </code>
 * <p>
 * Then copy value in <code>assertion.txt</code> to string assertion.
 */
public class TodoServerTest {
  public static final String SERVER_URL = "http://123done.org"; // Remember to update assertion if you change servers.
  // public static final String HOST = "http://localhost:8080"; // Remember to update assertion if you change servers.
  public static final String EMAIL = "test@mockmyid.com";
  public static final String ASSERTION = "eyJhbGciOiJSUzI1NiJ9.eyJwdWJsaWMta2V5Ijp7ImFsZ29yaXRobSI6IlJTIiwibiI6IjE5Mjg3MjQ4ODgyNTY5NzUwMTUyNjQ3MTEzMDY1MDg2ODA0OTg5MTQxMTk5NDMzMzUzMjM2NDEwNzAyNjEwMzE0MzY5MDAxNjE1MDgxMjA4ODA3NTQ1NjU0MDA0MDgwNzUwMTcyNTgwNTU0MzE0NTI5NjcxNTc0MTQzNjIwMjE1NjQ1NTY5ODQ4MjgxMjMyNDAyMTI2NzYwOTU1Mzg4MDMzNDIyNzY3NDg1NjA5MTQwNTA5NDk5MTIyNDg4MjkzMjA4MDI5MzA5Mjg4NTMzMDM1NzczNTExNjkyNjk5MTM5NDAyMzIyNjY1MDA5MTY0MjIxNDE0MDA2MzEwNDA5MTgyMjkxNjcwNjIzMjgxNTE4NDY2ODUxOTY3MzE0NzI0NDEzODE4OTQwNjY1Mzk5NzM1ODQ0MDk1NDQ0OTY1MTY3MjExODQyNjM2OTQyNjc3NzUxMDY2MTE4MjUwNjgyNzg4Nzg5MTI2MjU3NzE4NzYzMDYxMjQ5NTc2MDQwMjg4NzgxNjczODY4NjI4MjUxNzY1MzY1OTE2MjI3MTAxMDc3OTUyMzc5MTk0NDQ1OTk4MDc3MTcwNzQ2MTE4NjE4ODQ4MTIxMzcyMzMxNjE2Nzk0NzQxNjE0ODM3NDk4NjI4NzMwNzg2MzQ5OTc1NjA2MjAwNjA3Nzc3NTI3Njc5MzIxODcyMTc5MTMyNDUwMjk0NjUyNjU3NzU5MTA2MTkyODcyMDUwMjYwNjY5NTkzMjA1NTk4MDAzMjc2NTcwOTIyMzI3OTE4Njg1NzAyNTA4MTU2NzU3MTc4NDA4NDM4NzczMDQ3NzkwODExIiwiZSI6IjY1NTM3In0sInByaW5jaXBhbCI6eyJlbWFpbCI6InRlc3RAbW9ja215aWQuY29tIn0sImV4cCI6MTM0MjY0MjEzNjU0MiwiaXNzIjoibW9ja215aWQuY29tIn0.MUBbYK2JT9RAfaahFVXXeIWdfBlIC4zGxzE5E6ydIviUrHZta47UaOxI3g24Ohw-ofwAGN-4U49L4E0LvZ39XkvnVxDU0Ne7naY5aHtt5BJWRll_mrb4UFFOhTl6_qDbVc3saOEseO7o8eYa7fsm7MX-uhZhkXSKNNYNta52W0Zsbzd84I7Jweh9aDHOk3af3AHB06Ogt1l9-BxszqT3Sb5HyIdetDaKY6QHDKHr_Zn5Ea30ijH_qh0MvcIgOtaELcCO0TmSZ_ClCVDLfBLZLRqOnpTu9tOa57xa0dEQ5TudpUSRVvxH1Q2T1Op3q1aAwv8nK39sCdXb4vWCu1HCJw~eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEzNDI0NjkzMzY1NDgsImV4cCI6MTM0MjY0MjEzNjU0OCwiaXNzIjoiMTI3LjAuMC4xIiwiYXVkIjoiaHR0cDovLzEyM2RvbmUub3JnIn0.jp2ZUAzA6hkN61D1Pm2Q0hfyNy2Uqvl3WXjupRi3I-CIDxYkoTqxFW1vVfpDW_74YZkzMzBOzEUTZj_E9CMic9UeX-15wIpbkhIMCXziiyEIDTYwuT7NcRcqBVCw940pLxOyYl0hfpo1HqEJWctV0RfUNFLlOTizKm1PFG2vg6WKqe1ZB6V9oW3WIBWZF16Fq38dHElFaGRwV15q6-rSJT1exfDo_mrYxQYJFGUE527WDbIuOAcTNgUwdi1hQkg-10t_scPpctabXfV5KccqZtUWDYDkAAOms7jAWCWXPVFbG_Llm7mkHhK8x5SQcPVlQOcWGUmWgJ5w9I2ZmwYFag";

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
  public void testLoginLogout() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(ASSERTION);
    assertNotNull(todoServer.getUserLoggedIn());

    todoServer.logout();
    assertNull(todoServer.getUserLoggedIn());
  }

  @Test
  public void testMultipleLogins() throws Exception {
    final TodoServer todoServer = new TodoServer(SERVER_URL);

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(ASSERTION);
    assertNotNull(todoServer.getUserLoggedIn());

    final String cookie = todoServer.getSessionCookie();
    assertNotNull(cookie);

    todoServer.logout();
    assertNull(todoServer.getUserLoggedIn());

    assertNull(todoServer.getUserLoggedIn());
    todoServer.login(ASSERTION);
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

    todoServer.login(ASSERTION);
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

    todoServer.login(ASSERTION);
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
