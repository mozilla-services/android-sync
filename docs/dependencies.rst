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
