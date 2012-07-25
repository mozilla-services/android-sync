package org.mozilla.todo.activities;

import java.util.List;

import org.mozilla.gecko.R;

import android.accounts.Account;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class TodoAccountAdapter extends BaseAdapter implements SpinnerAdapter {
  private Activity activity;
  private List<Account> accounts; 

  public TodoAccountAdapter(Activity activity, final List<Account> accounts) {
      this.activity = activity;
      this.accounts = accounts;
  }

  public int getCount() {
      return accounts.size();
  }

  public Object getItem(int position) {
      return accounts.get(position);
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = activity.getLayoutInflater();
      View spinView = inflater.inflate(R.layout.todo_account_row, null);
      TextView t1 = (TextView) spinView.findViewById(R.id.account_name);
      t1.setText(String.valueOf(accounts.get(position).name));
//      TextView t2 = (TextView) spinView.findViewById(R.id.field2);
//      t2.setText(list_bsl.get(position).getName());
      return spinView;
  }
}
