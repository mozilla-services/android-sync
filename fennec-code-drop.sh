DESTDIR=$1
if [ -z "$DESTDIR" ]; then
  echo "No destination directory specified."
  exit 1
fi
shift

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

mvn clean compile || { echo 'mvn clean compile failed'; exit 1; }

if [ "$1" = "notests" ]
then
  echo "Not running tests."
else
  echo "Running tests."
  mvn test || { echo 'mvn test failed'; exit 1; }
fi
shift

echo "Copying."
ANDROID="$ANDROID" SYNC="$SYNC" ./fennec-copy-code.sh
