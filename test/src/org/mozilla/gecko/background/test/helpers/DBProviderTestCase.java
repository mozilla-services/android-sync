/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.test.helpers;

import java.io.File;

import android.content.ContentProvider;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.mock.MockContentResolver;

/**
 * Because ProviderTestCase2 is unable to handle custom DB paths.
 */
public abstract class DBProviderTestCase<T extends ContentProvider> extends
    AndroidTestCase {

  Class<T> providerClass;
  String providerAuthority;

  protected File fakeProfileDirectory;
  private MockContentResolver resolver;
  private T provider;

  public DBProviderTestCase(Class<T> providerClass, String providerAuthority) {
    this.providerClass = providerClass;
    this.providerAuthority = providerAuthority;
  }

  public T getProvider() {
    return provider;
  }

  public MockContentResolver getMockContentResolver() {
    return resolver;
  }

  protected abstract String getCacheSuffix();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    File cache = getContext().getCacheDir();
    fakeProfileDirectory = new File(cache.getAbsolutePath() + getCacheSuffix());
    System.out.println("Test: Creating profile directory " + fakeProfileDirectory.getAbsolutePath());
    if (!fakeProfileDirectory.mkdir()) {
      throw new IllegalStateException("Could not create temporary directory.");
    }

    final Context context = getContext();
    assertNotNull(context);
    resolver = new MockContentResolver();
    provider = providerClass.newInstance();
    provider.attachInfo(context, null);
    assertNotNull(provider);
    resolver.addProvider(providerAuthority, getProvider());
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
