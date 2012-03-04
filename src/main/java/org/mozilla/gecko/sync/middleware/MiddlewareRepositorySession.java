/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.middleware;

import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;

public abstract class MiddlewareRepositorySession extends RepositorySession {

  protected RepositorySession inner;

  public MiddlewareRepositorySession(Repository repository) {
    super(repository);
  }

  @Override
  public void wipe(RepositorySessionWipeDelegate delegate) {
    inner.wipe(delegate);
  }

  public void begin(RepositorySessionBeginDelegate delegate) {
    inner.begin(delegate);
  }

  @Override
  public void finish(RepositorySessionFinishDelegate delegate) throws InactiveSessionException {
    inner.finish(delegate);
  }

  @Override
  public synchronized void ensureActive() throws InactiveSessionException {
    inner.ensureActive();
  }

  @Override
  public synchronized boolean isActive() {
    return inner.isActive();
  }

  @Override
  public synchronized SessionStatus getStatus() {
    return inner.getStatus();
  }

  @Override
  public synchronized void setStatus(SessionStatus status) {
    inner.setStatus(status);
  }

  @Override
  public synchronized void transitionFrom(SessionStatus from, SessionStatus to)
      throws InvalidSessionTransitionException {
    inner.transitionFrom(from, to);
  }

  @Override
  public void abort() {
    inner.abort();
  }

  @Override
  public void abort(RepositorySessionFinishDelegate delegate) {
    inner.abort(delegate);
  }

  @Override
  public void guidsSince(long timestamp, RepositorySessionGuidsSinceDelegate delegate) {
    // TODO: need to do anything here?
    inner.guidsSince(timestamp, delegate);
  }

  @Override
  public void storeDone() {
    inner.storeDone();
  }

  @Override
  public void storeDone(long storeEnd) {
    inner.storeDone(storeEnd);
  }

}