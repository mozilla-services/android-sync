package org.mozilla.gecko.fxaccount;

public class FxAccountCreationException extends Exception {
  private static final long serialVersionUID = -1213303602029069238L;

  public FxAccountCreationException(String detailMessage) {
    super(detailMessage);

    if (detailMessage == null) {
      throw new IllegalArgumentException("detailMessage must not be null.");
    }
  }
}
