./build-manifest.sh
./build-strings.sh
mvn android:apk
mvn assembly:assembly
