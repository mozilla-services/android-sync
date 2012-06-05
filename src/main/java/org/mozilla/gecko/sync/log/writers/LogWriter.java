/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.log.writers;

public abstract class LogWriter {
  public abstract void error(String tag, String message, Throwable error);
  public abstract void warn(String tag, String message, Throwable error);
  public abstract void info(String tag, String message, Throwable error);
  public abstract void debug(String tag, String message, Throwable error);
  public abstract void trace(String tag, String message, Throwable error);
  public abstract void close();
  public abstract boolean shouldLogVerbose(String tag);
}
