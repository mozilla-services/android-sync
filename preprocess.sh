#!/bin/sh

# Or use the one in mozilla-central.
USERNAME=$(whoami)
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$($PREPROCESSOR -Fsubstitution -DUSERNAME=$USERNAME package-name.txt)

# For standalone use.
MOZ_UPDATE_CHANNEL="default"
MOZ_APP_DISPLAYNAME="FxSync"
MOZ_APP_VERSION="0"
ANDROID_CPU_ARCH="armeabi-v7a"

# Keep MOZ_ANDROID_SHARED_ID consistent with
# mobile/android/base/Makefile.in.  If the shared IDs are not the
# same, the Android Sync package's SyncAdapter and instrumentation
# tests will be denied access to the Fennec content provider.
MOZ_ANDROID_SHARED_ID="${ANDROID_PACKAGE_NAME}.sharedID"
# We intentionally change the MOZ_ANDROID_SHARED_ACCOUNT_TYPE.  In
# Fennec, this is:
#
# MOZ_ANDROID_SHARED_ACCOUNT_TYPE="${ANDROID_PACKAGE_NAME}_sync"
#
# Since we want to test without reference to the installed Fennec, we
# define our own testing Android Account type:
MOZ_ANDROID_SHARED_ACCOUNT_TYPE="${ANDROID_PACKAGE_NAME}_sync_test"

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."
echo "Using ANDROID_CPU_ARCH $ANDROID_CPU_ARCH."
echo "Using MOZ_UPDATE_CHANNEL $MOZ_UPDATE_CHANNEL."
echo "Using MOZ_APP_DISPLAYNAME $MOZ_APP_DISPLAYNAME."
echo "Using MOZ_APP_VERSION $MOZ_APP_VERSION."
echo "Using MOZ_ANDROID_SHARED_ID $MOZ_ANDROID_SHARED_ID."
echo "Using MOZ_ANDROID_SHARED_ACCOUNT_TYPE $MOZ_ANDROID_SHARED_ACCOUNT_TYPE."

ANNOUNCEMENTSCONSTANTS=src/main/java/org/mozilla/gecko/background/announcements/AnnouncementsConstants.java
GLOBALCONSTANTS=src/main/java/org/mozilla/gecko/sync/GlobalConstants.java
SYNCCONSTANTS=src/main/java/org/mozilla/gecko/sync/SyncConstants.java
BROWSERCONTRACT=src/main/java/org/mozilla/gecko/db/BrowserContract.java
MANIFEST=AndroidManifest.xml

# All definitions used at Android Services preprocess time must be
# present at Fennec build time.  You should never need to do this, but
# see Bug 795499 for an example of adding a preprocessor definition to
# Fennec.
DEFINITIONS=""
DEFINITIONS="$DEFINITIONS -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"
DEFINITIONS="$DEFINITIONS -DANDROID_CPU_ARCH=$ANDROID_CPU_ARCH"
DEFINITIONS="$DEFINITIONS -DMOZ_UPDATE_CHANNEL=$MOZ_UPDATE_CHANNEL"
DEFINITIONS="$DEFINITIONS -DMOZ_APP_VERSION=$MOZ_APP_VERSION"
DEFINITIONS="$DEFINITIONS -DMOZ_ANDROID_SHARED_ID=$MOZ_ANDROID_SHARED_ID"
DEFINITIONS="$DEFINITIONS -DMOZ_ANDROID_SHARED_ACCOUNT_TYPE=$MOZ_ANDROID_SHARED_ACCOUNT_TYPE"
DEFINITIONS="$DEFINITIONS -DMOZ_APP_DISPLAYNAME=$MOZ_APP_DISPLAYNAME"

$PREPROCESSOR $DEFINITIONS $ANNOUNCEMENTSCONSTANTS.in > $ANNOUNCEMENTSCONSTANTS
$PREPROCESSOR $DEFINITIONS $GLOBALCONSTANTS.in > $GLOBALCONSTANTS
$PREPROCESSOR $DEFINITIONS $SYNCCONSTANTS.in > $SYNCCONSTANTS
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST
$PREPROCESSOR $DEFINITIONS $BROWSERCONTRACT.in > $BROWSERCONTRACT

mkdir -p res/values res/xml
$PREPROCESSOR $DEFINITIONS strings/strings.xml.template > res/values/strings.xml
$PREPROCESSOR $DEFINITIONS sync_authenticator.xml.template > res/xml/sync_authenticator.xml
$PREPROCESSOR $DEFINITIONS sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
$PREPROCESSOR $DEFINITIONS sync_options.xml.template > res/xml/sync_options.xml

# Now do the test project.
TEST_MANIFEST=test/AndroidManifest.xml
$PREPROCESSOR $DEFINITIONS $TEST_MANIFEST.in > $TEST_MANIFEST
