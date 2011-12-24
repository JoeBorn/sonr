#!/bin/bash

#get the latest source
git pull origin master

#build and sign from source
ant release

#install signed apk on all phones
sh install-sonr.sh