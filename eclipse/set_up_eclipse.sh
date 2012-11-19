#!/bin/sh

if [ ! -x ./preprocess.sh ];
then
    echo "set_up_eclipse.sh must be run from root of android-sync git repository."
    exit 1
fi

function usage
{
    echo "usage: set_up_eclipse.sh [-n | --no-maven] [-h | --help]"
}

MAVEN="yesmaven"

while [ "$1" != "" ]; do
  case $1 in
      -n | --no-maven )       MAVEN="nomaven"
                              ;;
      -h | --help )           usage
                              exit
                              ;;
      * )                     usage
                              exit 1
  esac
  shift
done

if [ "$MAVEN" = "nomaven" ]
then
  echo "Not running maven."
else
  echo "Running maven."

  "mvn clean site && mvn -DdownloadJavadocs=true -DdownloadSources=true eclipse:eclipse" || { echo 'mvn failed'; exit 1; }
fi

# now to preprocess and install Eclipse files.

PREPROCESSOR="python tools/Preprocessor.py"
DEFINITIONS="$DEFINITIONS -DM2_REPO=$HOME/.m2/repository"

set -x # provide user feedback.

# install Eclipse files for main android-sync-app project.
$PREPROCESSOR $DEFINITIONS ./eclipse/android-sync-app.project > .project
$PREPROCESSOR $DEFINITIONS ./eclipse/android-sync-app.classpath > .classpath

# install Eclipse files for testing android-sync-instrumentation project.
$PREPROCESSOR $DEFINITIONS ./eclipse/android-sync-instrumentation.project > ./test/.project
$PREPROCESSOR $DEFINITIONS ./eclipse/android-sync-instrumentation.classpath > ./test/.classpath

cat <<EOF

To finish configuring android-sync Eclipse projects, perform the
following steps in Eclipse:

1. Window > Preferences > Java > Build Path > Classpath Variables and
define M2_REPO to equal your local Maven repository (usually, that is
"$HOME/.m2/repository").

This lets Eclipse find dependencies installed by Maven.

See http://www.mkyong.com/maven/how-to-configure-m2_repo-variable-in-eclipse-ide/
for more details.

2. File > Import... > General > Existing Projects into Workspace, and
specify $PWD.

This should add the main android-sync-app project.

3. File > Import... > General > Existing Projects into Workspace, and
specify $PWD/test.

This should add the secondary android-sync-instrumentation project,
for running device tests.

4. Refresh everything.

You should have a correctly configured android-sync Eclipse workspace.
If not, email services-dev@mozilla.org.
EOF
