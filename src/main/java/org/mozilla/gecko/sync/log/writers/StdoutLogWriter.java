/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.log.writers;

public class StdoutLogWriter extends LogWriter {
  @Override
  public void error(String tag, String message, Throwable error) {
    System.out.println("E :: " + tag + " :: " + message);
    if (error != null) {
      error.printStackTrace();
    }
  }

  @Override
  public void warn(String tag, String message, Throwable error) {
    System.out.println("W :: " + tag + " :: " + message);
    if (error != null) {
      error.printStackTrace();
    }
  }

  @Override
  public void info(String tag, String message, Throwable error) {
    System.out.println("I :: " + tag + " :: " + message);
    if (error != null) {
      error.printStackTrace();
    }
  }

  @Override
  public void debug(String tag, String message, Throwable error) {
    System.out.println("D :: " + tag + " :: " + message);
    if (error != null) {
      error.printStackTrace();
    }
  }

  @Override
  public void trace(String tag, String message, Throwable error) {
    System.out.println("V :: " + tag + " :: " + message);
    if (error != null) {
      error.printStackTrace();
    }
  }

  @Override
  public void close() {
    // Nothing to close;
  }

  @Override
  public boolean shouldLogVerbose(String tag) {
    return true;
  }
}
