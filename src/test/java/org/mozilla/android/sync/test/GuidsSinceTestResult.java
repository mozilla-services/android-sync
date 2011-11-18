package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.RepoStatusCode;

public class GuidsSinceTestResult {

  private RepoStatusCode statusCode;
  private String[] guids;

  public GuidsSinceTestResult(RepoStatusCode statusCode, String [] guids) {
   this.setStatusCode(statusCode);
   this.setGuids(guids);
  }

  public RepoStatusCode getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(RepoStatusCode statusCode) {
    this.statusCode = statusCode;
  }

  public String[] getGuids() {
    return guids;
  }

  public void setGuids(String[] guids) {
    this.guids = guids;
  }

}
