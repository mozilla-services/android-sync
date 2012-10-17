DESTDIR=$1
if [ -z "$DESTDIR" ]; then
  echo "No destination directory specified."
  exit 1
fi
shift

ANDROID=$DESTDIR/mobile/android
if [ ! -d $ANDROID ]; then
  echo "No Android dir."
  exit 1
fi

SERVICES=$DESTDIR/mobile/android/services

if [ -d $SERVICES ]; then
  echo "Services directory already exists. Updating."
else
  echo "No Services directory. Creating directory structure."
  mkdir -p $SERVICES
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
ANDROID="$ANDROID" SERVICES="$SERVICES" ./fennec-copy-code.sh
