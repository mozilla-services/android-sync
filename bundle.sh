mvn assembly:assembly -DdescriptorId=jar-with-dependencies
tar cf everything.tar target/android-sync.apk target/android-sync-jar-with-dependencies.jar
