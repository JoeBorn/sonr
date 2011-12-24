#!/bin/bash

#install SONR
echo "installing latest build of SONR"
echo "--------------------------------"
for foo in `adb devices | egrep 'device$' | cut -d ' ' -f1`
do
	echo "Device: ${foo}"
    adb -s $foo install -r $PWD/bin/SONR-release.apk
done
echo "finished installing SONR"