#!/bin/sh

# Verify that HEAD headers are up to date.

# first files commited but not merged to develop.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "test" "$@" `git diff --name-only develop HEAD | grep ^src/main/java/org/mozilla/`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git diff --name-only develop HEAD | grep ^src/test/java/org/mozilla`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git diff --name-only develop HEAD | grep ^test/src/org/mozilla`

# second files we are modifying.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "test" "$@" `git ls-files --modified | grep ^src/main/java/org/mozilla/`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git ls-files --modified | grep ^src/test/java/org/mozilla`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git ls-files --modified | grep ^test/src/org/mozilla`

# third files that are new and are staged.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "test" "$@" `git diff --name-only --cached | grep ^src/main/java/org/mozilla/`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git diff --name-only --cached | grep ^src/test/java/org/mozilla`
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@"            `git diff --name-only --cached | grep ^test/src/org/mozilla`
