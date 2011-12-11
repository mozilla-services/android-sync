package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class MultipleRecordsForGuidException extends SyncException {

  private static final long serialVersionUID = 7426987323485324741L;

  public MultipleRecordsForGuidException(Exception ex) {
    super(ex);
  }

}
