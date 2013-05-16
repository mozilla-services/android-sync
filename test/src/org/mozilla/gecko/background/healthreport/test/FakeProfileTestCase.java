/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

public abstract class FakeProfileTestCase extends ActivityInstrumentationTestCase2<Activity> {

  protected Context context;
  protected File fakeProfileDirectory;

  public FakeProfileTestCase() {
    super(Activity.class);
  }

  abstract String getCacheSuffix();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    File cache = context.getCacheDir();
    fakeProfileDirectory = new File(cache.getAbsolutePath() + getCacheSuffix());
    System.out.println("Test: Creating profile directory " + fakeProfileDirectory.getAbsolutePath());
    if (!fakeProfileDirectory.mkdir()) {
      throw new IllegalStateException("Could not create temporary directory.");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    // We don't check return values.
    System.out.println("Test: Cleaning up " + fakeProfileDirectory.getAbsolutePath());
    for (File child : fakeProfileDirectory.listFiles()) {
      child.delete();
    }
    fakeProfileDirectory.delete();
    super.tearDown();
  }
}
