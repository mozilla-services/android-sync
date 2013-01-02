#!/bin/sh
#
# An example hook script to check license headers are correct for
# files about to be committed.  Called by "git commit" with no
# arguments.  The hook should exit with non-zero status after issuing
# an appropriate message if it wants to stop the commit.
#
# To enable this hook, rename this file to ".git/hooks/pre-commit".

./check_head_headers.sh -d
exitcode=$?

if test $exitcode -ne 0
then
echo "Check headers!"
fi

exit $exitcode
