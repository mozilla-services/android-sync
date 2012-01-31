DESTDIR=$1
if [ -z "$DESTDIR" ]; then
  echo "No destination directory specified."
  exit 1
fi

ANDROID=$DESTDIR/mobile/android
if [ ! -d $ANDROID ]; then
  echo "No android dir."
  exit 1
fi

SYNC=$DESTDIR/mobile/android/sync

if [ -d $SYNC ]; then
  echo "Sync directory already exists. Updating."
else
  echo "No Sync directory. Creating directory structure."
  mkdir -p $SYNC
fi

echo "Preprocessing."
./preprocess.sh
./preprocess-fennec.sh

echo "Running tests."
mvn clean test

echo "Copying."
ANDROID="$ANDROID" SYNC="$SYNC" ./fennec-copy-code.sh
