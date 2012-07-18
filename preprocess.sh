#!/bin/sh

# Or use the one in mozilla-central.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)

# For standalone use.
MOZ_APP_DISPLAYNAME="FxSync"
MOZ_APP_VERSION="0"

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."
echo "Using MOZ_APP_DISPLAYNAME $MOZ_APP_DISPLAYNAME."
echo "Using MOZ_APP_VERSION $MOZ_APP_VERSION."

CONSTANTS=src/main/java/org/mozilla/gecko/sync/GlobalConstants.java
BROWSERCONTRACT=src/main/java/org/mozilla/gecko/db/BrowserContract.java
MANIFEST=AndroidManifest.xml

DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME -DMOZ_APP_VERSION=$MOZ_APP_VERSION"
DISPLAYNAME_DEF="-DMOZ_APP_DISPLAYNAME=$MOZ_APP_DISPLAYNAME"
$PREPROCESSOR $DEFINITIONS "$DISPLAYNAME_DEF" $CONSTANTS.in > $CONSTANTS
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST
$PREPROCESSOR $DEFINITIONS $BROWSERCONTRACT.in > $BROWSERCONTRACT

$PREPROCESSOR $DEFINITIONS strings/strings.xml.template > res/values/strings.xml
$PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
$PREPROCESSOR $DEFINITIONS sync_options.xml.template > res/xml/sync_options.xml

# Now do the test project.
TEST_MANIFEST=test/AndroidManifest.xml
$PREPROCESSOR $DEFINITIONS $TEST_MANIFEST.in > $TEST_MANIFEST
