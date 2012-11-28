===========================================
Getting started developing Android Services
===========================================

Re-packaged configuration
=========================

We label this configuration *re-packaged* since the tools download a
current Fennec Nightly and re-package it (sign and align it) for
development purposes.

The re-packaged configuration is very good for rapid development of
Android Services since it keeps most Services things away from Fennec
(with the exception of the ContentProviders, which live within
Fennec).  This configuration gives Android Services its own Firefox
Sync account type (*org.mozilla.fennec_sync_test*), which lets you
create test Android Sync Accounts and sync them independently of
Fennec.  The Authenticator (and, indeed, the sync) run in the
*org.mozilla.gecko* Android package.  The trade off is that you cannot
easily test deep integration with Fennec itself.

**This configuration is best for new developers and for Android
Services specific work, but can require a deep understanding of how
Android Services, Fennec, and the Android system interact.**

Own-package configuration
=========================

We label this configuration *own-package* since each developer is
responsible for building her own Fennec Nightly (Android package
*org.mozilla.fennec_@USERNAME@*).

Each development iteration, the developer updates the code integrated
into Fennec and then redeploys Fennec as a whole.  This alternative
configuration is the traditional configuration.

**This configuration is best for existing Fennec developers and for
Android Services work that needs to integrate closely with Fennec, and
can also require a deep understanding of how Android Services, Fennec,
and the Android system interact.**

Getting started with a re-packaged configuration
================================================

First, clone the git repository: ::

  archo:temp ncalexan$ git clone https://github.com/mozilla-services/android-sync
  Cloning into 'android-sync'...
  remote: Counting objects: 39508, done.
  remote: Compressing objects: 100% (11663/11663), done.
  remote: Total 39508 (delta 18085), reused 39220 (delta 17827)
  Receiving objects: 100% (39508/39508), 7.48 MiB | 374 KiB/s, done.
  Resolving deltas: 100% (18085/18085), done.

Second, download and install a developer version of Fennec Nightly.
You'll need your Android device connected and enabled for development,
or an ARMv6 emulator running ::

  archo:temp ncalexan$ cd android-sync/
  archo:android-sync ncalexan$ ./tools/fennec_installer.py -v
  Downloading https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk...
  Downloading https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk... done.
  Downloaded /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpJxhrZh.
  Re-packaging /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmpJxhrZh to gecko.apk.
  Created temporary directory /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmppQpV3_.
  Re-packaged /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmppQpV3_/gecko.apk.
  Wrote to gecko.apk.
  Deleted temporary directory /var/folders/70/w4jt0cv5141cw8c6nxmzydkc0000gn/T/tmppQpV3_.
  Installing gecko.apk...
  3480 KB/s (26633169 bytes in 7.471s)
    pkg: /data/local/tmp/gecko.apk
  Success
  Installing gecko.apk... done.
  Launching org.mozilla.fennec/.App...
  Starting: Intent { act=android.intent.action.MAIN cmp=org.mozilla.fennec/.App }
  Launched org.mozilla.fennec/.App.

Finally, build and run the Android Services test suite: ::

  archo:android-sync ncalexan$ ./preprocess.sh && mvn clean integration-test
  Using ANDROID_PACKAGE_NAME org.mozilla.fennec.
  Using ANDROID_CPU_ARCH armeabi-v7a.
  Using MOZ_UPDATE_CHANNEL default.
  Using MOZ_APP_DISPLAYNAME FxSync.
  Using MOZ_APP_VERSION 0.
  Using MOZ_ANDROID_SHARED_ID org.mozilla.fennec.sharedID.
  Using MOZ_ANDROID_SHARED_ACCOUNT_TYPE org.mozilla.fennec_sync_test.
  [INFO] Scanning for projects...
  ...
