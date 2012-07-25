package org.mozilla.todo.activities;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.persona.PersonaAccountAuthenticator;
import org.mozilla.todo.TodoItem;
import org.mozilla.todo.TodoServer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

public class TodoActivity extends Activity {
  public static final String LOG_TAG = "TodoActivity";

  // public static final String HOST = "123done.org";
  public static final String HOST = "localhost:8080";
  public static final String SERVER_URL = "http://" + HOST;

  protected Account[] accounts = null;
  protected final TodoServer todoServer = new TodoServer(SERVER_URL);

  public TodoActivity() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.todo_activity);

    final Spinner spinner = (Spinner) findViewById(R.id.todo_account_spinner);
    final OnItemSelectedListener spinnerListener = new AccountSelectedListener();
    spinner.setOnItemSelectedListener(spinnerListener);
  }

  protected void logout() {
    try {
      todoServer.logout();
    } catch (URISyntaxException e) {
      Logger.warn(LOG_TAG, "Got exception in logout.", e);
    }
  }

  protected void login(final Account account) {
    final AccountManager accountManager = AccountManager.get(this);

    Logger.warn(LOG_TAG, "before task");
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Logger.info(LOG_TAG, "doInBackground");
        try {
          final String assertion = accountManager.blockingGetAuthToken(account, SERVER_URL, true);
          Logger.info(LOG_TAG, "Assertion is " + assertion);
          todoServer.login(assertion);
          return true;
        } catch (Throwable e) {
          Logger.warn(LOG_TAG, "Got exception in logout.", e);
          return false;
        }
      }

      @Override
      protected void onPostExecute(final Boolean success) {
        Logger.warn(LOG_TAG, "Success? " + success);
        updateTodoList();
      }
    }.execute();
  }

  protected void updateAccount() {
    final Spinner spinner = (Spinner) findViewById(R.id.todo_account_spinner);
    final Account account = (Account) spinner.getSelectedItem();

    if (account == null) {
      Logger.info(LOG_TAG, "No account selected.");
      return;
    }

    final String selectedAccountName = account.name;
    Logger.info(LOG_TAG, "Account name is " + selectedAccountName + ".");
    login(account);
  }

  protected class AccountSelectedListener implements OnItemSelectedListener {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long rowInView) {
      updateAccount();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Logger.info(LOG_TAG, "onResume");

    // Set accounts.
    accounts = getPersonaAccounts(this);
    final Spinner spinner = (Spinner) findViewById(R.id.todo_account_spinner);

    final List<Account> as = new ArrayList<Account>();
    for (Account account : accounts) {
      as.add(account);
    }
    final TodoAccountAdapter adapter = new TodoAccountAdapter(this, as);
    spinner.setAdapter(adapter);

    updateAccount();
  }

  protected void updateTodoList() {
    Logger.info(LOG_TAG, "updateTodoList");

    final Activity activity = this;
    final ListView listView = (ListView) findViewById(R.id.todo_items_listView);

    new AsyncTask<Void, Void, JSONArray>() {

      @Override
      protected JSONArray doInBackground(Void... params) {
        Logger.info(LOG_TAG, "updateTodoList doInBackground");
        try {
          return todoServer.get();
        } catch (URISyntaxException e) {
          return null;
        }
      }

      @Override
      protected void onPostExecute(final JSONArray todoItems) {
        Logger.info(LOG_TAG, "updateTodoList onPostExecute");
        Logger.warn(LOG_TAG, "todoItems " + todoItems);
        final List<TodoItem> x = new ArrayList<TodoItem>();
        for (Object todoItem : todoItems) {
          JSONObject o = (JSONObject) todoItem;
          x.add(new TodoItem((String) o.get("v"),  false));
        }
        final ListAdapter adapter = new TodoItemsAdapter(activity, x);
        listView.setAdapter(adapter);
      }
    }.execute();
  }

  public static Account[] getPersonaAccounts(final Context context) {
    final AccountManager am = AccountManager.get(context);
    return am.getAccountsByType(PersonaAccountAuthenticator.ACCOUNT_TYPE_PERSONA);
  }
}
