package org.mozilla.gecko.tokenserver;

public class TokenServerToken {
  public final String id;
  public final String key;
  public final String uid;
  public final String endpoint;

  public TokenServerToken(String id, String key, String uid, String endpoint) {
    this.id = id;
    this.key = key;
    this.uid = uid;
    this.endpoint = endpoint;
  }
}