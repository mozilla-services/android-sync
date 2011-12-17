#!/bin/sh

# Or use the one in mozilla-central.
PREPROCESSOR=tools/Preprocessor.py

ANDROID_PACKAGE=org.mozilla.gecko

AUTHORITIES=src/main/java/org/mozilla/gecko/sync/repositories/android/Authorities.java
MANIFEST=AndroidManifest.xml

python $PREPROCESSOR -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE $AUTHORITIES.in > $AUTHORITIES
python $PREPROCESSOR -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE $MANIFEST.in > $MANIFEST

python $PREPROCESSOR -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE strings.xml.template > res/values/strings.xml
python $PREPROCESSOR -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE sync_syncadapter.xml.template > res/xml/sync_syncadapter.xml
