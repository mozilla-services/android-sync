package org.mozilla.android.sync;

public class NonObjectJSONException extends Exception {
  private static final long serialVersionUID = 435366246452253073L;
  Object obj;
  public NonObjectJSONException(Object object) {
    obj = object;
  }
}
