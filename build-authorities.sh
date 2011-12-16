ANDROID_PACKAGE=org.mozilla.gecko
AUTHORITIES=src/main/java/org/mozilla/gecko/sync/repositories/android/Authorities.java
python tools/Preprocessor.py -DANDROID_PACKAGE_NAME=$ANDROID_PACKAGE $AUTHORITIES.in > $AUTHORITIES
