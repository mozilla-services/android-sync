#!/bin/sh

# Verify that HEAD headers are up to date.

exitcode=0

# first files commited but not merged to develop.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "testhelpers" "$@" `git diff --name-only develop HEAD | grep ^src/main/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only develop HEAD | grep /testhelpers/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only develop HEAD | grep ^src/test/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only develop HEAD | grep ^test/src/org/mozilla/.*\\.java$` || exitcode=$?

# second files we are modifying.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "testhelpers" "$@" `git ls-files --modified | grep ^src/main/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git ls-files --modified | grep /testhelpers/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git ls-files --modified | grep ^src/test/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git ls-files --modified | grep ^test/src/org/mozilla/.*\\.java$` || exitcode=$?

# third files that are new and are staged.
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_MPL" -x "testhelpers" "$@" `git diff --name-only --cached | grep ^src/main/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only --cached | grep /testhelpers/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only --cached | grep ^src/test/java/org/mozilla/.*\\.java$` || exitcode=$?
python "tools/check_headers.py" -l "package" -f "tools/COPYRIGHT_PD" "$@" `git diff --name-only --cached | grep ^test/src/org/mozilla/.*\\.java$` || exitcode=$?

exit $exitcode
