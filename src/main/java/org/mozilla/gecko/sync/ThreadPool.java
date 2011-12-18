package org.mozilla.gecko.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
  public static ExecutorService executorService = Executors.newCachedThreadPool();
  public static void run(Runnable runnable) {
    executorService.submit(runnable);
  }
}
