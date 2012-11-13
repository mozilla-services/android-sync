=========================================
Developing android-services using Vagrant
=========================================

To make it easier for new contributors to contribute to Mozilla's
Android services, we've created a Vagrant configuration that uses
Puppet to set up a virtual machine.  You can then develop from within
the virtual machine environment without needing to configure your own
machine.  (Of course, if you prefer to configure your own machine, you
can do that instead.  Or you can do both!)

To build, test, and run Mozilla Android Services client software, you
need a fairly involved toolchain, including:

* Java;
* the Android SDK;
* Maven 3;
* the android-services repository;
* and supporting client libraries.

Install Virtualbox
==================

Virtualbox is a free, open source, virtualization product available
for most popular platforms.  Download it from
https://www.virtualbox.org/wiki/Downloads and install it.

Install Vagrant
===============

Vagrant is a free, open source product that uses VirtualBox to build
configurable, lightweight, and portable virtual machines dynamically.
Download it from http://downloads.vagrantup.com/ and install it as
well.

Download prerequisites
======================

Developing Android software requires Sun's Java JDK, but unfortunately
Sun's Java cannot be downloaded and distributed without each developer
agreeing to a license.  That means you'll have to download them
yourself; we can't have the virtual machine set up download them
automatically for you.

Since you already have to download some files, we're going to pile on
a few more that strictly speaking we could download automatically.
The advantage of not downloading them automatically is that if you
want to create more virtual machines, the files won't need to be
downloaded again.

1. Sun Java JDK

Go to
http://www.oracle.com/technetwork/java/javase/downloads/jdk7u9-downloads-1859576.html,
accept the license agreement, and download the **Linux x86** version
named **jdk-7u9-linux-i586.tar.gz**.  That file needs to go in the
directory ::

  puppet/modules/data/files/

2. Sun Java Cryptography Policy

We need the Java Cryptography Extension (JCE) Unlimited Strength
Jurisdiction Policy Files 7 to perform strong encryption.

Go to
http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html,
accept the license agreement, and download the file named
**UnlimitedJCEPolicyJDK7.zip**.  That file also needs to go in the
directory ::

  puppet/modules/data/files/

3. Google Android SDK, platform, and platform tools

Download the following three files:

http://dl.google.com/android/android-sdk_r20.0.3-linux.tgz

http://dl-ssl.google.com/android/repository/android-16_r03.zip

http://dl-ssl.google.com/android/repository/platform-tools_r14-linux.zip

They all need to go in the same directory ::

  puppet/modules/data/files/

This means you should have the following output ::

  archo:android-sync ncalexan$ ls -1 puppet/modules/data/files
  README.txt
  UnlimitedJCEPolicyJDK7.zip
  android-16_r03.zip
  android-sdk_r20.0.3-linux.tgz
  jdk-7u9-linux-i586.tar.gz
  platform-tools_r14-linux.zip

Run Vagrant to create virtual machine environment
=================================================

Now we should be able to create the virtual machine. ::

  archo:android-sync ncalexan$ vagrant up develop
  [default] Importing base box 'precise32'...
  [default] The guest additions on this VM do not match the install version of
  VirtualBox! This may cause things such as forwarded ports, shared
  folders, and more to not work properly. If any of those things fail on
  this machine, please update the guest additions and repackage the
  box.

  Guest Additions Version: 4.2.0
  VirtualBox Version: 4.2.4
  [default] Matching MAC address for NAT networking...
  [default] Clearing any previously set forwarded ports...
  [default] Forwarding ports...
  [default] -- 22 => 2222 (adapter 1)
  [default] Creating shared folders metadata...
  [default] Clearing any previously set network interfaces...
  [default] Booting VM...
  [default] Waiting for VM to boot. This can take a few minutes.
  [default] VM booted and ready for use!
  [default] Mounting shared folders...
  [default] -- v-root: /vagrant
  [default] -- manifests: /tmp/vagrant-puppet/manifests
  [default] -- v-pp-m0: /tmp/vagrant-puppet/modules-0
  [default] Running provisioner: Vagrant::Provisioners::Puppet...
  [default] Running Puppet with /tmp/vagrant-puppet/manifests/develop.pp...
  stdin: is not a tty
  info: Applying configuration version '1352165432'

  ...

  info: Creating state file /var/lib/puppet/state/state.yaml

  notice: Finished catalog run in 113.84 seconds

Access virtual machine environment
==================================

Now you should be able to connect to the virtual machine. ::

  archo:android-sync ncalexan$ vagrant ssh develop
  Welcome to Ubuntu 12.04 LTS (GNU/Linux 3.2.0-23-generic-pae i686)

   * Documentation:  https://help.ubuntu.com/
  Welcome to your Vagrant-built virtual machine.
  Last login: Fri Sep 14 06:22:31 2012 from 10.0.2.2
  vagrant@precise32:~$

Run the Android Services test suite
===================================

And, hopefully, you can run the Android Services test suite! It will
download the internet, but that should only happen once: ::

  vagrant@precise32:~$ cd /vagrant
  vagrant@precise32:/vagrant$ ./preprocess.sh && mvn clean test
  Using ANDROID_PACKAGE_NAME org.mozilla.fennec.
  Using ANDROID_CPU_ARCH armeabi-v7a.
  Using MOZ_UPDATE_CHANNEL default.
  Using MOZ_APP_DISPLAYNAME FxSync.
  Using MOZ_APP_VERSION 0.
  Using MOZ_ANDROID_SHARED_ID org.mozilla.fennec.sharedID.
  Using MOZ_ANDROID_SHARED_ACCOUNT_TYPE org.mozilla.fennec_sync_test.
  [INFO] Scanning for projects...
  Downloading: http://repo.maven.apache.org/maven2/com/jayway/maven/plugins/android/generation2/android-maven-plugin/3.1.0/android-maven-plugin-3.1.0.pom
  Downloaded: http://repo.maven.apache.org/maven2/com/jayway/maven/plugins/android/generation2/android-maven-plugin/3.1.0/android-maven-plugin-3.1.0.pom (21 KB at 42.7 KB/sec)
  ...
  Downloaded: http://repo.maven.apache.org/maven2/commons-lang/commons-lang/2.6/commons-lang-2.6.jar (278 KB at 393.7 KB/sec)
  [INFO] ------------------------------------------------------------------------
  [INFO] Reactor Build Order:
  [INFO]
  [INFO] Android Sync
  [INFO] Android Sync - App
  [INFO] Android Sync - Instrumentation
  [INFO]
  [INFO] ------------------------------------------------------------------------
  [INFO] Building Android Sync 1
  [INFO] ------------------------------------------------------------------------
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-clean-plugin/2.4.1/maven-clean-plugin-2.4.1.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-clean-plugin/2.4.1/maven-clean-plugin-2.4.1.jar (23 KB at 207.2 KB/sec)
  [INFO]
  [INFO] --- maven-clean-plugin:2.4.1:clean (default-clean) @ android-sync ---
  [INFO]
  [INFO] ------------------------------------------------------------------------
  [INFO] Building Android Sync - App 1
  [INFO] ------------------------------------------------------------------------
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-install-plugin/maven-metadata.xml
  ...
  Downloaded: http://repo.maven.apache.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar (12645 KB at 1164.1 KB/sec)
  [INFO]
  [INFO] --- maven-clean-plugin:2.4.1:clean (default-clean) @ android-sync-app ---
  [INFO] Deleting /vagrant/android-sync-app/target
  [INFO]
  [INFO] --- android-maven-plugin:3.1.0:generate-sources (default-generate-sources) @ android-sync-app ---
  [INFO] ANDROID-904-002: Found aidl files: Count = 0
  [INFO] ANDROID-904-002: Found aidl files: Count = 0
  [INFO] /usr/local/android-sdk-linux/platform-tools/aapt [package, -m, -J, /vagrant/android-sync-app/target/generated-sources/r, -M, /vagrant/android-sync-app/../AndroidManifest.xml, -S, /vagrant/android-sync-app/../res, --auto-add-overlay, -I, /usr/local/android-sdk-linux/platforms/android-16/android.jar]
  [INFO]     (skipping file '.DS_Store' due to ANDROID_AAPT_IGNORE pattern '.*')
  [INFO]
  [INFO] --- build-helper-maven-plugin:1.7:add-source (default) @ android-sync-app ---
  Downloading: http://repo.maven.apache.org/maven2/junit/junit/3.8.2/junit-3.8.2.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/1.5.8/plexus-utils-1.5.8.jar (262 KB at 924.3 KB/sec)
  [INFO] Source directory: /vagrant/external/httpclientandroidlib/httpclientandroidlib/src added.
  [INFO] Source directory: /vagrant/external/json-simple-1.1/src added.
  [INFO]
  [INFO] --- maven-resources-plugin:2.4.3:resources (default-resources) @ android-sync-app ---
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/shared/maven-filtering/1.0-beta-4/maven-filtering-1.0-beta-4.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/apache/maven/shared/maven-filtering/1.0-beta-4/maven-filtering-1.0-beta-4.jar (34 KB at 124.4 KB/sec)
  [INFO] Using 'UTF-8' encoding to copy filtered resources.
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-app/src/main/resources
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-app/target/generated-sources/extracted-dependencies/src/main/resources
  [INFO]
  [INFO] --- maven-compiler-plugin:2.3.2:compile (default-compile) @ android-sync-app ---
  [INFO] Compiling 738 source files to /vagrant/android-sync-app/target/classes
  [INFO]
  [INFO] --- android-maven-plugin:3.1.0:proguard (default-proguard) @ android-sync-app ---
  [INFO]
  [INFO] --- maven-resources-plugin:2.4.3:testResources (default-testResources) @ android-sync-app ---
  [INFO] Using 'UTF-8' encoding to copy filtered resources.
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-app/src/test/resources
  [INFO]
  [INFO] --- maven-compiler-plugin:2.3.2:testCompile (default-testCompile) @ android-sync-app ---
  [INFO] Compiling 65 source files to /vagrant/android-sync-app/target/test-classes
  [INFO]
  [INFO] --- maven-surefire-plugin:2.12:test (default-test) @ android-sync-app ---
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/surefire/surefire-junit47/2.12/surefire-junit47-2.12.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/apache/maven/surefire/common-junit4/2.12/common-junit4-2.12.jar (16 KB at 9.8 KB/sec)
  [INFO] Surefire report directory: /vagrant/android-sync-app/target/surefire-reports
  [INFO] Using configured provider org.apache.maven.surefire.junitcore.JUnitCoreProvider

  -------------------------------------------------------
   T E S T S
  -------------------------------------------------------
  Concurrency config is parallel='none', perCoreThreadCount=true, threadCount=2, useUnlimitedThreads=false
  Running org.mozilla.gecko.sync.stage.test.TestFetchMetaGlobalStage
  Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.828 sec
  ...
  Running org.mozilla.gecko.sync.crypto.test.TestPersistedCrypto5Keys
  Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 sec

  Results :

  Tests run: 209, Failures: 0, Errors: 0, Skipped: 0

  [INFO]
  [INFO] ------------------------------------------------------------------------
  [INFO] Building Android Sync - Instrumentation 1
  [INFO] ------------------------------------------------------------------------
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-surefire-plugin/maven-metadata.xml
  ...
  Downloaded: http://repo.maven.apache.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar (5746 KB at 1153.2 KB/sec)
  [INFO]
  [INFO] --- maven-clean-plugin:2.4.1:clean (default-clean) @ android-sync-instrumentation ---
  [INFO] Deleting /vagrant/android-sync-instrumentation/target
  [INFO]
  [INFO] --- android-maven-plugin:3.1.0:generate-sources (default-generate-sources) @ android-sync-instrumentation ---
  [INFO] ANDROID-904-002: Found aidl files: Count = 0
  [INFO] ANDROID-904-002: Found aidl files: Count = 0
  [INFO] /usr/local/android-sdk-linux/platform-tools/aapt [package, -m, -J, /vagrant/android-sync-instrumentation/target/generated-sources/r, -M, /vagrant/android-sync-instrumentation/../test/AndroidManifest.xml, -S, /vagrant/android-sync-instrumentation/../test/res, --auto-add-overlay, -I, /usr/local/android-sdk-linux/platforms/android-16/android.jar]
  [INFO]
  [INFO] --- maven-resources-plugin:2.4.3:resources (default-resources) @ android-sync-instrumentation ---
  [INFO] Using 'UTF-8' encoding to copy filtered resources.
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-instrumentation/src/main/resources
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-instrumentation/target/generated-sources/extracted-dependencies/src/main/resources
  [INFO]
  [INFO] --- maven-compiler-plugin:2.5.1:compile (default-compile) @ android-sync-instrumentation ---
  Downloading: http://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/1.5.1/plexus-utils-1.5.1.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-compiler-javac/1.9.1/plexus-compiler-javac-1.9.1.jar (14 KB at 128.2 KB/sec)
  [INFO] Compiling 62 source files to /vagrant/android-sync-instrumentation/target/classes
  [INFO]
  [INFO] --- android-maven-plugin:3.1.0:proguard (default-proguard) @ android-sync-instrumentation ---
  [INFO]
  [INFO] --- maven-resources-plugin:2.4.3:testResources (default-testResources) @ android-sync-instrumentation ---
  [INFO] Using 'UTF-8' encoding to copy filtered resources.
  [INFO] skip non existing resourceDirectory /vagrant/android-sync-instrumentation/src/test/resources
  [INFO]
  [INFO] --- maven-compiler-plugin:2.5.1:testCompile (default-testCompile) @ android-sync-instrumentation ---
  [INFO] No sources to compile
  [INFO]
  [INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ android-sync-instrumentation ---
  Downloading: http://repo.maven.apache.org/maven2/org/apache/maven/surefire/surefire-booter/2.12.4/surefire-booter-2.12.4.pom
  ...
  Downloaded: http://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar (309 KB at 372.5 KB/sec)
  [INFO] No tests to run.
  [INFO] ------------------------------------------------------------------------
  [INFO] Reactor Summary:
  [INFO]
  [INFO] Android Sync ...................................... SUCCESS [0.261s]
  [INFO] Android Sync - App ................................ SUCCESS [55.228s]
  [INFO] Android Sync - Instrumentation .................... SUCCESS [13.993s]
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time: 1:17.379s
  [INFO] Finished at: Tue Nov 06 01:49:50 UTC 2012
  [INFO] Final Memory: 23M/92M
  [INFO] ------------------------------------------------------------------------
