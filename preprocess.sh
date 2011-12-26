#!/bin/sh

# Or use the one in mozilla-central.
PREPROCESSOR="python tools/Preprocessor.py"

ANDROID_PACKAGE_NAME=$(cat package-name.txt)

echo "Using ANDROID_PACKAGE_NAME $ANDROID_PACKAGE_NAME."

MANIFEST=AndroidManifest.xml
DEFINITIONS="-DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE_NAME"
$PREPROCESSOR $DEFINITIONS $MANIFEST.in > $MANIFEST
