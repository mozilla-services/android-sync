DESTDIR=$1
ANDROID=$DESTDIR/mobile/android

if [ ! -d $ANDROID ]; then
  echo "No android dir."
  exit 1
fi

SYNC=$DESTDIR/mobile/android/sync
WARNING="These files are managed in the android-sync repo. Do not modify directly, or your changes will be lost."

if [ -d $SYNC ]; then
  echo "Sync directory already exists. Updating."
else
  echo "No Sync directory. Creating directory structure."
  mkdir -p $SYNC
fi

echo "Creating README.txt."
echo $WARNING > $SYNC/README.txt

echo "Copying manifests..."
rsync -a manifests $SYNC/

echo "Copying source..."
SOURCEDIR="src/main/java/org/mozilla/android/sync"
SOURCEFILES=$(find "$SOURCEDIR" -name '*.java' | sed "s,$SOURCEDIR/,,")
echo "Source files: \n  $SOURCEFILES"
rsync --include "*.java" -a $SOURCEDIR $ANDROID/base/
echo $WARNING > $ANDROID/base/sync/README.txt
echo $SOURCEFILES > $ANDROID/base/sync/JAVAFILES.in

echo "Copying resources..."
rsync -av res/ $ANDROID/app/android/

echo "Copying dependencies..."
DEPSDIR=$DESTDIR/other-licenses/android-sync-deps
rm -r $DEPSDIR
rsync -a external/*.jar $DEPSDIR/

# We need to inform the Fennec makefile that we're including:
# * Java dependencies (JAVA_CLASSPATH)
# * New icons and XML files (NSINSTALL stuff)
# * New code (JAVAFILES)
# * God knows what else.
