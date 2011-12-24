#!/bin/bash

#install SONR
echo "installing latest build of SONR"
for foo in `adb devices | egrep 'device$' | cut -d ' ' -f1`
do
    adb -s $foo install -r $PWD/bin/SONR-release.apk
done