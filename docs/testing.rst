Testing
=======

There are two test suites: a unit test suite that runs locally on your
development machine and an integration test suite that runs via your Android
device.

Remember that any changes to preprocessed source files will need
``./preprocess.py`` to be run before any of the commands below.

Unit testing
------------

The source files for the JUnit 4 unit test suite may be found in
``./src/test/java/``.  The test suite can be run with the following command: ::

  mvn clean test

Remote debugging
~~~~~~~~~~~~~~~~

The test suite can open a port for a remote debugger and wait for a connection
with the following command (tested with Maven 3.0.5, Arch Linux): ::

  mvn -Dmaven.surefire.debug clean test

Any remote debugger can be attached to this open port (by default port 5005).
For example, you can attach jdb by specifying the port and the associated
source directories: ::

  jdb -attach 5005 -sourcepath "./src/main/java/:./src/test/java/"

Eclipse
~~~~~~~

For information on how to run the unit tests with Eclipse, please see the
`unit testing section of the Mozilla Wiki`_.

.. _`unit testing section of the Mozilla Wiki`: https://wiki.mozilla.org/Services/NativeSync#To_run_the_unit_test_suite_under_Eclipse

Integration testing
-------------------

The source files for the JUnit 3 integration test suite, also known as the
"Android instrumentation" test suite, may be found in ``./test``.

TODO: Add additional information to this section. Tests may still be run from
Eclipse (see below).

Eclipse
~~~~~~~

For information on how to run the integration tests with Eclipse, please see
the `integration testing section of the Mozilla Wiki`_.

.. _`integration testing section of the Mozilla Wiki`: https://wiki.mozilla.org/Services/NativeSync#To_run_the_integration_test_suite_under_Eclipse
