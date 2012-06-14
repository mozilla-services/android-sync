#!/bin/sh

# Or use the one in mozilla-central.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)

# For standalone use.
MOZ_APP_DISPLAYNAME="FxSync"
MOZ_APP_VERSION="0"

# Consistent with mobile/android/base/Makefile.in.
MOZ_ANDROID_SHARED_ID="${ANDROID_PACKAGE_NAME}.sharedID"
MOZ_ANDROID_SHARED_ACCOUNT_TYPE="${ANDROID_PACKAGE_NAME}_sync"

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."
echo "Using MOZ_APP_DISPLAYNAME $MOZ_APP_DISPLAYNAME."
echo "Using MOZ_APP_VERSION $MOZ_APP_VERSION."
echo "Using MOZ_ANDROID_SHARED_ID $MOZ_ANDROID_SHARED_ID."
echo "Using MOZ_ANDROID_SHARED_ACCOUNT_TYPE $MOZ_ANDROID_SHARED_ACCOUNT_TYPE."

CONSTANTS=src/main/java/org/mozilla/gecko/sync/GlobalConstants.java
BROWSERCONTRACT=src/main/java/org/mozilla/gecko/db/BrowserContract.java
MANIFEST=AndroidManifest.xml

DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME -DMOZ_APP_VERSION=$MOZ_APP_VERSION -DMOZ_ANDROID_SHARED_ID=$MOZ_ANDROID_SHARED_ID -DMOZ_ANDROID_SHARED_ACCOUNT_TYPE=$MOZ_ANDROID_SHARED_ACCOUNT_TYPE"
DISPLAYNAME_DEF="-DMOZ_APP_DISPLAYNAME=$MOZ_APP_DISPLAYNAME"
$PREPROCESSOR $DEFINITIONS "$DISPLAYNAME_DEF" $CONSTANTS.in > $CONSTANTS
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST
$PREPROCESSOR $DEFINITIONS $BROWSERCONTRACT.in > $BROWSERCONTRACT

$PREPROCESSOR $DEFINITIONS strings/strings.xml.template > res/values/strings.xml
$PREPROCESSOR $DEFINITIONS sync_authenticator.xml.template > res/xml/sync_authenticator.xml
$PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
$PREPROCESSOR $DEFINITIONS apps_syncadapter.xml.template > res/xml/apps_syncadapter.xml
$PREPROCESSOR $DEFINITIONS sync_options.xml.template > res/xml/sync_options.xml

# Now do the test project.
TEST_MANIFEST=test/AndroidManifest.xml
$PREPROCESSOR $DEFINITIONS $TEST_MANIFEST.in > $TEST_MANIFEST
