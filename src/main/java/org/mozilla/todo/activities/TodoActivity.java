package org.mozilla.todo.activities;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.R;
import org.mozilla.persona.PersonaAccountAuthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Spinner;

public class TodoActivity extends Activity {
  public static final String LOG_TAG = "TodoActivity";

  public TodoActivity() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.todo_activity);

    final Spinner spinner = (Spinner) findViewById(R.id.todo_account_spinner);
    final Account[] accounts = getPersonaAccounts(this);

    final List<Account> as = new ArrayList<Account>();
    for (Account account : accounts) {
      as.add(account);
    }
    final TodoAccountAdapter adapter = new TodoAccountAdapter(this, as);
    spinner.setAdapter(adapter);
  }

  public static Account[] getPersonaAccounts(final Context context) {
    final AccountManager am = AccountManager.get(context);
    return am.getAccountsByType(PersonaAccountAuthenticator.ACCOUNT_TYPE_PERSONA);
  }
}
