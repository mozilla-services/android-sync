package org.mozilla.android.sync;

import java.security.GeneralSecurityException;

public class CryptoException extends Exception { 
  public GeneralSecurityException cause;
  public CryptoException(GeneralSecurityException e) {
    this();
    this.cause = e;
  }
  public CryptoException() {
    
  }
  private static final long serialVersionUID = -5219310989960126830L;
}
