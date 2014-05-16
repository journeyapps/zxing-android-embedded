#!/bin/sh

if [ "$#" -ne 1 ]
then
  echo "Usage: ./release.sh \"Commit message\""
  exit 1
fi



rm -rf mvn-clone
git clone -b mvn-repo . mvn-clone
./gradlew uploadArchives
cd mvn-clone
git add maven-repository
git commit -m "$1"
git push origin mvn-repo:mvn-repo
cd ..
