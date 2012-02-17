#!/bin/sh

python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "test" "$@" src/main/java/org/mozilla/gecko/ src/main/java/org/mozilla/android
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" test/src/org/mozilla


