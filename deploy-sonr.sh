#!/bin/bash

#get the latest source
echo "pulling latest build from git..."
git pull origin master

#build and sign from source
ant release

#install signed apk on all phones
sh install-sonr.sh