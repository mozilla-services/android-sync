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

echo "Copying resources..."
# I'm guessing these go here.
#rsync -av res/drawable $ANDROID/app/android/
#rsync -av res/drawable-hdpi $ANDROID/app/android/
#rsync -av res/drawable-mdpi $ANDROID/app/android/
#rsync -av res/drawable-ldpi $ANDROID/app/android/
#rsync -av res/layout/*.xml $ANDROID/app/android/layout/

echo "Creating README.txt."
echo $WARNING > $SYNC/README.txt

echo "Copying manifests..."
rsync -a manifests $SYNC/

echo "Copying dependencies..."
DEPSDIR=$DESTDIR/other-licenses/android-sync-deps
rm -r $DEPSDIR
rsync -a target/android-sync-android-sync.jar $DEPSDIR/android-sync-deps.jar

echo "Copying jar..."
rsync -a target/android-sync.jar $SYNC/android-sync.jar
