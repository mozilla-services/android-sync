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
