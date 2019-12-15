#!/bin/bash
set -ex
# have or dont have services all vars?
HOME=/home/app-data/
orig="$HOME/onboard.default"
current="$HOME/.config/dconf/user"
sleep 4 #see keyboard.service RestartSec
if [ -f $orig ] ; then
  killall onboard || true
  cp -v $orig $current
  exit 133
else
  echo "$orig do not exists, creating new"
  cp -v $current $orig
  exit 122
fi
