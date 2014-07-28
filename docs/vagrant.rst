Developing in a virtual machine environment built by Vagrant and Puppet
=======================================================================

To build, test, and run Mozilla Android Services client software, you
need a fairly involved toolchain, including:

* Java;
* the Android SDK;
* Maven 3 (note that `v3.1.0 is broken`_);
* the android-sync repository.

.. _`v3.1.0 is broken`: https://code.google.com/p/maven-android-plugin/issues/detail?can=2&q=395&colspec=ID%20Type%20Component%20OpSys%20Status%20Priority%20Milestone%20Owner%20Summary&id=395

To make it easier for new contributors to contribute to Mozilla's
Android services, we've created a Vagrant configuration that uses
Puppet to set up a virtual machine.  You can then develop from within
the virtual machine environment without needing to configure your own
machine.  (Of course, if you prefer to configure your own machine, you
can do that instead.  Or you can do both!)

We suggest developers who work in Windows develop in such a virtual
machine environment, since developing Android software on Microsoft
Windows can be especially difficult, due to the differences in the
Windows toolchain and Google's Android tools.

Install Virtualbox
------------------

Virtualbox is a free, open source, virtualization product available
for most popular platforms.  Download it from
https://www.virtualbox.org/wiki/Downloads and install it.

Install Vagrant
---------------

Vagrant is a free, open source product that uses VirtualBox to build
configurable, lightweight, and portable virtual machines dynamically.
Download it from http://downloads.vagrantup.com/ and install it as
well.

Download prerequisites
----------------------

Developing Android software requires Sun's Java Development Kit, but
unfortunately Sun's Java cannot be downloaded and distributed without
each developer agreeing to a license.  That means you'll have to
download it yourself; we can't have the virtual machine download it
automatically for you.

Since you already have to download some files, we're going to pile on
a few more that strictly speaking we could download automatically.
The advantage of not downloading them automatically is that if you
want to create more virtual machines, the files won't need to be
downloaded again.

1. Sun Java JDK

   Go to
   http://www.oracle.com/technetwork/java/javase/downloads/index.html
   select the latest version, accept the license agreement, and
   download the **Linux x86** version named
   ``jdk-VERSION-linux-i586.tar.gz``.  That file needs to go in the
   directory ``puppet/modules/data/files/``.

2. Sun Java Cryptography Policy

   We need the Java Cryptography Extension (JCE) Unlimited Strength
   Jurisdiction Policy Files 7 to perform strong encryption.

   Go to
   http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
   accept the license agreement, and download the file named
   ``UnlimitedJCEPolicyJDK7.zip``.  That file also needs to go in the
   directory ``puppet/modules/data/files/``.

3. Google Android SDK, platform, and platform tools

   Download the following files:

   * http://dl.google.com/android/android-sdk_r21.1-linux.tgz
   * http://dl-ssl.google.com/android/repository/android-17_r02.zip
   * http://dl-ssl.google.com/android/repository/platform-tools_r16.0.2-linux.zip

   They all need to go in the same directory ``puppet/modules/data/files/``.

At the end, you should have the following files: ::

  archo:android-sync ncalexan$ ls -1 puppet/modules/data/files
  README.txt
  UnlimitedJCEPolicyJDK7.zip
  android-17_r02.zip
  android-sdk_r21.1-linux.tgz
  jdk-7u17-linux-i586.tar.gz
  platform-tools_r16.0.2-linux.zip

Create the virtual machine environment
--------------------------------------

Now we should be able to create the virtual machine using Vagrant.
The command is ``vagrant up develop``: ::

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
----------------------------------

Now you should be able to connect to the virtual machine using SSH.
The command is ``vagrant ssh develop``: ::

  archo:android-sync ncalexan$ vagrant ssh develop
  Welcome to Ubuntu 12.04 LTS (GNU/Linux 3.2.0-23-generic-pae i686)

   * Documentation:  https://help.ubuntu.com/
  Welcome to your Vagrant-built virtual machine.
  Last login: Fri Sep 14 06:22:31 2012 from 10.0.2.2
  vagrant@precise32:~$ cd /vagrant
  vagrant@precise32:/vagrant$ ls

  ...

  android-sync-app
  android-sync-instrumentation

  ...

Run the test suite
------------------

And now, hopefully, you can run the Android Services test suite! It will
download the internet, but that should only happen on the first run,
and then it should run all the tests and report success: ::

  vagrant@precise32:~$ cd /vagrant
  vagrant@precise32:/vagrant$ ./preprocess.py && mvn clean test
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

  [INFO] Reactor Summary:
  [INFO]
  [INFO] Android Sync ...................................... SUCCESS [0.071s]
  [INFO] Android Sync - App ................................ SUCCESS [37.702s]
  [INFO] Android Services - Bagheera Client Test ........... SUCCESS [3.510s]
  [INFO] Android Sync - Instrumentation .................... SUCCESS [5.750s]
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time: 47.545s
  [INFO] Finished at: Thu Mar 14 00:25:49 UTC 2013
  [INFO] Final Memory: 30M/88M
  [INFO] ------------------------------------------------------------------------
