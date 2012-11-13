package org.mozilla.gecko.user;

public class UserClientException extends Exception {
  private static final long serialVersionUID = 3120860294319373976L;

  public UserClientException(String detailMessage) {
    super(detailMessage);
  }

  public UserClientException(Throwable throwable) {
    super(throwable);
  }

  public static class UserClientUserAlreadyExistsException extends UserClientException {
    private static final long serialVersionUID = 3120860294319373975L;

    public UserClientUserAlreadyExistsException(String detailMessage) {
      super(detailMessage);
    }
  }

  public static class UserClientMalformedRequestException extends UserClientException {
    private static final long serialVersionUID = 3120860294319373974L;

    public UserClientMalformedRequestException(String detailMessage) {
      super(detailMessage);
    }
  }

  public static class UserClientMalformedResponseException extends UserClientException {
    private static final long serialVersionUID = 3120860294319373973L;

    public UserClientMalformedResponseException(Throwable throwable) {
      super(throwable);
    }
  }
}