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
