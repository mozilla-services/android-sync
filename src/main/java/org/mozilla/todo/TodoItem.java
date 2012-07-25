package org.mozilla.todo;

public class TodoItem {
  public String text;
  public boolean done;

  public TodoItem() {
    text = "";
    done = false;
  }

  public TodoItem(final String text) {
    this();
    this.text = text;
  }

  public TodoItem(final String text, final boolean done) {
    this.text = text;
    this.done = done;
  }
}
