#!/bin/bash

#install SONR
echo "installing latest build of SONR"
echo "--------------------------------"
i=0
for foo in `adb devices | egrep 'device$' | cut -d ' ' -f1`
do
	echo "Device: ${foo}"
    adb -s $foo install -r $PWD/bin/SONR-release.apk
    let i+=1
done
echo "SONR deployed to ${i} devices"