#!/bin/bash

RUNNER=org.mozilla.gecko.background.tests/android.test.InstrumentationTestRunner

read CLASS COUNT <<< "$@"

for i in {1..$COUNT}; do
  adb shell am instrument -w -e class "$CLASS" "$RUNNER"
done
