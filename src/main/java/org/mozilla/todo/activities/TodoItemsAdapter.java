package org.mozilla.todo.activities;

import java.util.List;

import org.mozilla.gecko.R;
import org.mozilla.todo.TodoItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class TodoItemsAdapter extends BaseAdapter implements SpinnerAdapter {
  private Activity activity;
  private List<TodoItem> todoItems;

  public TodoItemsAdapter(Activity activity, final List<TodoItem> todoItems) {
    this.activity = activity;
    this.todoItems = todoItems;
  }

  public int getCount() {
    return todoItems.size();
  }

  public Object getItem(int position) {
    return todoItems.get(position);
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = activity.getLayoutInflater();
    View v = inflater.inflate(R.layout.todo_item_row, null);
    TextView tv = (TextView) v.findViewById(R.id.todo_item_textView);
    tv.setText(String.valueOf(todoItems.get(position).text));
    return v;
  }
}
