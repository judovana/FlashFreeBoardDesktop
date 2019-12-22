#!/bin/bash
set -ex
# have or dont have services all vars?
HOME=/home/app/
orig="$HOME/onboard.default"
current="$HOME/.config/dconf/user"
if [ -f $orig ] ; then
  killall onboard || true
  sleep 1 #wait for dconf to save ; see keyboard.service RestartSec
  cp -v $orig $current
  exit 133
else
  echo "$orig do not exists, creating new"
  cp -v $current $orig
  exit 122
fi
