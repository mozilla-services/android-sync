package org.mozilla.gecko.fxaccount;

public class FxAccountCreationException extends Exception {
  private static final long serialVersionUID = -1213303602029069238L;

  public FxAccountCreationException(String string) {
    super(string);
  }

  public FxAccountCreationException(Throwable throwable) {
    super(throwable);
  }

}
