package org.mozilla.todo.test;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.todo.TodoClient;
import org.mozilla.todo.TodoClientDelegate;

public class BlockingTodoClient {
  protected final TodoClient client;

  public BlockingTodoClient(String serverURL) {
    client = new TodoClient(serverURL);
  }

  public static class BlockingTodoClientPermissionException extends Exception {
    private static final long serialVersionUID = 2740825718902870898L;
  }

  protected static class BlockingTodoDelegate implements TodoClientDelegate {
    public String body = null;

    @Override
    public void handleResponse(int statusCode, String body) {
      this.body = body;

      if (statusCode != 200) {
        WaitHelper.getTestWaiter().performNotify(new BlockingTodoClientPermissionException());
        return;
      }

      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  protected String status() throws Exception {
    final BlockingTodoDelegate delegate = new BlockingTodoDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.status(delegate);
      }
    });

    return ExtendedJSONObject.parseJSONObject(delegate.body).getString("logged_in_email");
  }

  protected void login(final String assertion) {
    final BlockingTodoDelegate delegate = new BlockingTodoDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.login(assertion, delegate);
      }
    });
  }

  protected void logout() {
    final BlockingTodoDelegate delegate = new BlockingTodoDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.logout(delegate);
      }
    });
  }

  protected void save(final JSONArray todos) {
    final BlockingTodoDelegate delegate = new BlockingTodoDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.save(todos, delegate);
      }
    });
  }

  protected JSONArray get() throws Exception {
    final BlockingTodoDelegate delegate = new BlockingTodoDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.get(delegate);
      }
    });

    return (JSONArray) new JSONParser().parse(delegate.body);
  }

  public String getSessionCookie() {
    return client.getSessionCookie();
  }
}
