Installing a Firefox package to your Android device
===================================================

Fennec exposes data stores (bookmarks, history, passwords, etc) to the
Android services test suite, so the test suite always needs a Fennec
(Firefox for Android) package to talk to.  In addition, you will want
to install Android services to the device in order to develop and test
functionality.  There are two different ways to install Android
services to the device: *re-packaged configuration* and *own-package
configuration*.


The re-packaged configuration is best for new developers and for
Android Services specific work.  The own-package configuration is best
for existing Fennec developers and for Android Services work that
needs to integrate closely with Fennec.  Both can require a deep
understanding of how Android Services, Fennec, and the Android system
interact.  We recommend starting with a re-packaged configuration and
transitioning to an own-package configuration if and when you need to
integrate more deeply with Fennec.

Re-packaged configuration
-------------------------

In the *re-packaged configuration* the tools download a current Fennec
Nightly and "re-package" it -- sign and align it -- for development
purposes.  This changes the Android permissions to let the test suite
access the Fennec data stores.

The re-packaged configuration is very good for rapid development of
Android Services since it keeps most Services things away from Fennec
(except the data stores, which always live within Fennec).  This
configuration gives Android Services its own Firefox Sync account type
(*org.mozilla.fennec_sync_test*), which lets you create test Android
Sync Accounts and sync them independently of Fennec.  The
Authenticator (and, indeed, the sync) run in the *org.mozilla.gecko*
Android package.

The re-packaged configuration does not require each developer to build
Fennec.  We trade download time for build environment set up and
compilation time.  Since building Fennec is a long and complicated
process, new developers should prefer this configuration.  The trade
off is that you cannot change Fennec itself, or test some kinds deep
integration with Fennec.

Getting started with a re-packaged configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We have tools to download and install a developer version of Fennec
Nightly.  You'll need your Android device connected and enabled for
development, or an ARMv6 emulator running ::

  archo:android-sync ncalexan$ ./tools/fennec_installer.py dil
  Downloading https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk...
  Downloading https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk... done.
  Downloaded /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpIPQEaZ.
  Re-packaging /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpIPQEaZ to gecko.apk.
  Created temporary directory /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpd0fR5o.
  deleting: META-INF/MANIFEST.MF
  deleting: META-INF/CERT.SF
  deleting: META-INF/CERT.RSA
  Re-packaged /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpd0fR5o/gecko.apk.
  Wrote to gecko.apk.
  Deleted temporary directory /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpd0fR5o.
  Installing gecko.apk...
    3480 KB/s (26633169 bytes in 7.471s)
    pkg: /data/local/tmp/gecko.apk
  Success
  Installing gecko.apk... done.
  Launching org.mozilla.fennec/.App...
  Starting: Intent { act=android.intent.action.MAIN cmp=org.mozilla.fennec/.App }
  Launched org.mozilla.fennec/.App.

Finally, build and run the Android Services test suite: ::

  archo:android-sync ncalexan$ ./preprocess.py && mvn clean integration-test
  src/main/java/org/mozilla/gecko/background/common/GlobalConstants.java
  src/main/java/org/mozilla/gecko/sync/SyncConstants.java
  src/main/java/org/mozilla/gecko/db/BrowserContract.java
  AndroidManifest.xml
  test/AndroidManifest.xml
  res/values/strings.xml
  res/xml/sync_options.xml
  res/xml/sync_syncadapter.xml
  res/xml/sync_authenticator.xml
  [INFO] Scanning for projects...
  [INFO] ------------------------------------------------------------------------
  [INFO] Reactor Build Order:
  [INFO]
  [INFO] Android Sync
  [INFO] Android Sync - App
  [INFO] Android Services - Bagheera Client Test
  [INFO] Android Sync - Instrumentation
  ...

Own-package configuration
-------------------------

The *own-package configuration* is so-called because each developer is
responsible for building her own Fennec Nightly (Android package
*org.mozilla.fennec_\@USERNAME\@*).

Each development iteration, the developer updates the code integrated
into Fennec and then redeploys Fennec as a whole.  This alternative
configuration is the traditional configuration and makes the most
sense for existing Fennec developers.

You'll want to update ``preprocess.ini`` to specify the Android
package name for the test suite to talk to.
