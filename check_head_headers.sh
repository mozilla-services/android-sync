#!/bin/sh

# Verify that HEAD headers are up to date.

python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "test" "$@" `git diff --name-only develop HEAD | grep ^src/main/java/org/mozilla/`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only develop HEAD | grep ^src/test/java/org/mozilla`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only develop HEAD | grep ^test/src/org/mozilla`
