package org.mozilla.gecko.user;

public interface UserClientDelegate {
  void handleSuccess(String username, String body);
  void handleFailure(UserClientException e);
  void handleError(Exception e);
}