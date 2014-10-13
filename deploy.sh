#!/bin/zsh

make clean
make
gradle uploadArchives
