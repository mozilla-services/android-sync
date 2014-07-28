===========================================
Background Services for Firefox for Android
===========================================

This repository is the development home for the background services
that ship with Firefox for Android.  These include, or will include:

* Android Sync;
* Android Firefox Health Report components.

and formerly included:

* Product Announcements.

.. contents:: :local:

Configuring your development environment
========================================

First, clone the git repository: ::

  archo:temp ncalexan$ git clone https://github.com/mozilla-services/android-sync
  Cloning into 'android-sync'...
  remote: Counting objects: 39508, done.
  remote: Compressing objects: 100% (11663/11663), done.
  remote: Total 39508 (delta 18085), reused 39220 (delta 17827)
  Receiving objects: 100% (39508/39508), 7.48 MiB | 374 KiB/s, done.
  Resolving deltas: 100% (18085/18085), done.

Licensing
---------

Android Services code that is shipped as part of Firefox for Mobile
must be licensed `Mozilla Public License 2.0`_.  Code that is not
shipped (for example, test code, command line scripts, and utilities)
is usually explicitly stated as being in the public domain.

.. _`Mozilla Public License 2.0`: http://www.mozilla.org/MPL

The top-level scripts ``./check_headers.sh`` and
``./check_head_headers.sh`` check, respectively, that all source and
test files, and uncommitted source and test files, have correct headers.
(Internally they both use ``./tools/check_headers.py``.)

git hooks
---------

The top-level script ``./pre-commit.sh`` is a git pre-commit hook that
will stop you committing files with incorrect headers.  To install it,
run ::

  archo:android-sync ncalexan$ ln ./pre-commit.sh .git/hooks/pre-commit

.. note:

  If the pre-commit hook is failing, check that
  ``.git/hooks/pre-commit`` exists and is executable.

To temporarily ignore this pre-commit hook, use ``git commit --no-verify``.

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

Testing
=======

There are two test suites: a unit test suite that runs locally on your
development machine and an integration test suite that runs on your Android
device.

Remember that any changes to preprocessed source files will need
``./preprocess.py`` to be run before any of the commands below, and you may
want to ``mvn clean`` to ensure all artifacts are up-to-date.

Unit testing
------------

The source files for the JUnit 4 unit test suite may be found in
``src/test/java/``.  The unit test suite can be run with the following
command: ::

  mvn test

Running the unit test suite under Eclipse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run the unit test suite under Eclipse:

1. first configure the test suite launcher: under ``Preferences > Run/Debug >
   Launching > Default Launchers``, set the Debug and Run launchers to
   ``Android JUnit Test Launcher``;
2. select the ``android-sync`` project and execute ``Run > Run As ... > JUnit
   Test``.

You can debug under Eclipse using ``Debug > Debug As ... > JUnit Test.``.

Debugging the unit test suite with jdb
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The test suite can open a port for a remote debugger and wait for a connection
with the following command (tested with Maven 3.0.5, Arch Linux): ::

  mvn -Dmaven.surefire.debug test

Any remote debugger can be attached to this open port (by default port 5005).
For example, you can attach jdb by specifying the port and the associated
source directories: ::

  jdb -attach 5005 -sourcepath "src/main/java/:src/test/java/"

Integration testing
-------------------

The source files for the JUnit 3 integration test suite, also known as the
"Android instrumentation" test suite, may be found in ``test``. Before running
the test suite you must have installed Fennec, configured ``./preprocess.ini``
to point to this particular installation, and **have launched this installation
at least once** (see `Bug 777846`_).

The integration test suite can be run with the following command: ::

  mvn integration-test

.. _`Bug 777846`: https://bugzilla.mozilla.org/show_bug.cgi?id=777846

Generating an HTML report
~~~~~~~~~~~~~~~~~~~~~~~~~

After running the test suite via the command line, if you would like
to generate an HTML report from the most recent test results (which are
otherwise found in your shell's output and as an XML file), you can use
the following command: ::

  mvn surefire-report:report-only

The resulting file will be stored at
``./android-sync-instrumentation/target/site/surefire-report.html``. This
file is overwritten each time this command is run so be sure to copy the
file out if you would like to keep it.

More information can be found via the `report plugin's homepage`_.

.. _`report plugin's homepage`: http://maven.apache.org/surefire/maven-surefire-report-plugin/

Running the integration test suite under Eclipse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run the integration test suite under Eclipse:

1. add the ``test`` subdirectory as a sub-project using ``File > Import >
   Existing project``;
2. refresh and clean everything;
3. select the ``test`` project and execute ``Run > Run As ... > Android JUnit
   Test``.

You can debug under Eclipse using ``Debug > Debug As ... > Android JUnit
Test.``.

Shared test code
----------------

Some test code is shared between both the unit test suite
and the integration test suite. This code may be found in:
``src/main/java/org/mozilla/gecko/background/testhelpers``. This
directory is exported to
``<path-to-mozilla-central>/mobile/android/tests/background/junit3/src/testhelpers``
by the ``fennec-copy-code.sh`` script. This shared code should be kept as
minimal as possible.

Landing code: getting your changes committed to android-sync and mozilla-inbound
================================================================================

Since Android services are developed in a repository external to the
main Mozilla repositories, landing code is a two-step process.  We
first land on the branch ``develop`` of git repository
``android-sync``, and then we land on ``mozilla-inbound`` (or any
other Mozilla repository).

Merging to develop
------------------

We use a gitflow_-like development process.  All new work is developed
on a branch that is continually rebased to ``develop``.  We prefer to
name branches like ``username/bug-NUMBER-description``, e.g.,
``nalexander/bug-844347-logger``.  We always open a GitHub pull request
to get review before merging.

We always rebase our branches onto ``develop`` to keep our history easy
to read, and so that GitHub will automatically close our pull requests
after merge.  We include bug numbers at the start of every commit
message (this helps when parsing ``git blame``).  After rebasing, your
git log should look something like: ::

  2babb1b * nalexander/bug-844347-logger Bug 844347: move Logger and log writers to org.mozilla.gecko.background.common.log package.
  d868215 * Bug 844347: move org.mozilla.gecko.sync.GlobalConstants to org.mozilla.gecko.background.common.GlobalConstants.
  1c24220 * Bug 844347: fold BackgroundConstants.java into GlobalConstants.java.in.
  319879b * Bug 844347: separate Sync-specific from common pieces in {SyncConstants,GlobalConstants}.java.in.
  e19f136 * origin/develop develop Bug 845080 - Extract BackgroundService superclass. r=rnewman

Commit ``e19f136`` is ``develop``; the other four commits are the commits
to be merged into ``develop``.  To merge: ::

  $ git checkout develop
  $ git merge --no-ff -m "Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman" nalexander/bug-844347-logger

Note the ``--no-ff`` flag; we always want merge commits.  This is
partly because we only put the ``r=reviewer`` tag on the merge
commits.  By rebasing and merging in this way, it is easy to tell who
did what and how development proceeded.  Most of the time, all of the
changes you just merged to ``develop`` will be landed as a single
patch on ``mozilla-inbound``.  Therefore, your merge commit should say
what you did (not how you did it) and should reference the product you
modified (in this case, Sync).  At this point, your git log should
look something like: ::

  f1f41af *   develop Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman
          |\
  2babb1b | * origin/nalexander/bug-844347-logger nalexander/bug-844347-logger Bug 844347: move Logger and log writers to org.mozilla.gecko.background.common.log package.
  d868215 | * Bug 844347: move org.mozilla.gecko.sync.GlobalConstants to org.mozilla.gecko.background.common.GlobalConstants.
  1c24220 | * Bug 844347: fold BackgroundConstants.java into GlobalConstants.java.in.
  319879b | * Bug 844347: separate Sync-specific from common pieces in {SyncConstants,GlobalConstants}.java.in.
          |/
  e19f136 *   origin/develop Bug 845080 - Extract BackgroundService superclass. r=rnewman

Finally, you need to push your changes back upstream: ::

  $ git push origin develop

.. _gitflow: http://nvie.com/posts/a-successful-git-branching-model/

Merging to mozilla-inbound
--------------------------

Let's assume your working directories look like ::

  $ ls
  android-sync
  mozilla-inbound

First, create a new mq_ patch.  Ensure that you're correctly carrying
over the author from the commits merged into ``develop``. ::

  $ cd mozilla-inbound
  $ hg qnew -m "Bug 844347 - Factor logging code that is not Sync-specific out of org.mozilla.gecko.sync. r=rnewman" --user "Nick Alexander <nalexander@mozilla.com>" 844347.patch

Run the Android services code-drop script, targeting the correct
Mozilla repository.  (You can also use copy-code, which does not
verify that the code builds and the unit test suite passes.) ::

  $ cd android-sync
  $ ./fennec-code-drop.sh ../mozilla-inbound

These scripts copy pieces of the ``android-sync`` repository into
``mobile-inbound/mobile/android/``.  Now you need to refresh the
patch.  Be sure to add and remove files, and be aware that renamed
files require special care [#hgaddremove]_: ::

  $ cd mozilla-inbound
  $ hg status
  $ hg add any-missing-files.java
  $ hg rm anything-removed.java
  $ hg qref

Check that the patch is what you want to commit.  You are responsible
for anything that you land in the tree, so it behooves you to make
sure you get this right. ::

  $ less .hg/patches/844347.patch

Finally, ensure that everything builds and runs.  Assuming your object
directory is ``objdir-droid``: ::

  $ make -C objdir-droid
  $ make -C objdir-droid package install

You can now finish your patch, verify what you're going to send, and
push it upstream: ::

  $ hg qfinish tip
  $ hg outgoing
  $ hg push

.. _mq: http://mercurial.selenic.com/wiki/MqExtension

.. [#hgaddremove] See
   http://hgtip.com/tips/advanced/2009-09-30-detecting-renames-automatically/.
   Consider using the argument ``--similarity 95`` (not 100, since
   moving Java code often changes at least the package name).

Updating Bugzilla
-----------------

This is not Android services specific, but we'll call it out anyway.
You need to:

1. set the Bugzilla ticket status as ASSIGNED to the author of the commits;
2. add the changeset URL that ``hg push`` reports to the Bugzilla
   ticket;
3. and set the target milestone.

Stub/unstub dependencies and the android-sync GitHub-hosted Maven repository
============================================================================

Android Sync stubs and unstubs several Android modules as part of our
testing framework.  The ``android-sync`` Maven project automatically
downloads the JAR files for these dependencies and caches them locally
(so they should only be downloaded once).  This makes our "first run"
set up shorter, since new contributors don't need to manually fetch
and install these modules.

These stub module dependencies are separate git projects, hosted at

* https://github.com/rnewman/sharedpreferences-stub
* https://github.com/rnewman/log-unstub
* https://github.com/rnewman/base64-unstub

The Maven artifacts (JAR files) of these are uploaded to the
``mvn-repo`` branch of ``github.com/mozilla-services/android-sync``.

Modifying dependencies
----------------------

In the unlikely event you need to modify these dependencies, the
source is delivered as git submodules.  Use ::

  git submodule init
  git submodule update

to populate the ``external`` directory with this source code.  You
will need to commit any changes you make to ``external/DEPENDENCY`` to
the appropriate git repository, and you will need to ``mvn deploy``
the updated JAR files to the GitHub Maven repository; see the
documentation of each dependency project for more information.

.. ## Old notes -- mostly still correct but not all up to date

.. * You need Maven 3.  Homebrew is possibly easiest:

..   brew update
..   brew install maven

.. * You need to prepare the repo before you can use it.
..   * Use `preprocess.py` to build manifests etc. to keep Eclipse happy. The output
..     is ignored by Git, and the Fennec merge script skips them, too.
..   * The `fennec-code-drop.sh` script does this for you, as well as running tests.
..   * To run Android tests, you'll need to create .project and .classpath in `test`,
..     too.

.. * To make changes to generated files.
..   * strings.xml doesn't exist. Modify strings.xml.in.
..   * AndroidManifest.xml doesn't exist. Modify the file fragments in manifests/.
..   * The same goes for other files that are produced by preprocess.py.
..   * If you want to alter a value _to affect our build only_, put it in
..     AndroidManifest.xml.in.

.. * To run the unit and integration test suites:
..   * `mvn test`
..   * `mvn integration-test` with an emulator running or a device connected.

.. * To merge to mozilla-central:

..   MC=~/moz/hg/mozilla-central
..   pushd $MC
..   hg qpop --all
..   hg pull -u
..   hg qnew -m "Android Sync code drop." code-drop
..   popd
..   ./fennec-code-drop.sh $MC
..   pushd $MC
..   # hg add any files that have been added. Removing files that have been
..   # removed is an exercise for the reader.
..   hg qrefresh

.. * If you *know* all tests pass, or you're in an environment that prevents you
..   from running them, you can invoke `fennec-copy-code.sh` directly (with the
..   appropriate environment variables.)

.. * To build mozilla-central:

..   # You can do a partial build if you know what you're doing.
..   make -f client.mk
..   make -C objdir-fennec package
..   adb install -r objdir-fennec/dist/fennec*.apk

.. * You'll need subgit to work with external dependency clones.

..   http://rustyklophaus.com/articles/20100124-SubmodulesAndSubreposDoneRight.html

