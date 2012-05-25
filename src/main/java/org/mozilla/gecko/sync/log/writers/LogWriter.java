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
