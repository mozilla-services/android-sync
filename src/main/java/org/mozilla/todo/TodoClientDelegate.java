package org.mozilla.todo;

public interface TodoClientDelegate {
  void handleResponse(int statusCode, String body);
  void handleError(Exception e);
}