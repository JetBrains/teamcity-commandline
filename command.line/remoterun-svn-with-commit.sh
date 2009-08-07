#!/bin/sh

#
# check path passed
#
if test -z "$2"
then
 echo "Usage: remoterun.sh <path_to_directory_which_contains_changes> <comment>"
 exit -1
fi
#
# set JAVA_HOME
#
#
if test -z "$JAVA_HOME" 
then
 JAVA_HOME=`which java 2>/dev/null`
 JAVA_HOME=`dirname $JAVA_HOME 2>/dev/null`
 if [ -z "$JAVA_HOME" ]; then
  echo "The JAVA_HOME environment variable is defined"
  echo "It's required for application running"
  exit 1
 fi
fi
# create list of changed files. NOTE: path should be full qualified. 
# 
PWD=`pwd`
LIST=$PWD/changes.`date +%Y-%m-%d.%T`.list
#
# build fullpath for changes according to passed path to directory
#
if [[ $1 == /* ]]
then
 #absolute
 svn status $1 |  awk '$1~/.*/ {print ($2)}'> $LIST
else
 #relative
 svn status $1 |  awk --assign PWD=$PWD '$1~/.*/ {print (PWD "/" $2)}'> $LIST
fi
#
# run remoterun
#
$JAVA_HOME/java -jar tcc-5.0.0.49.jar remoterun -c bt2 -m $2 @$LIST
#
# check return code & try to commit changes
#
if [[ $? -eq 0 ]] 
then
 svn commit --message $2 --targets $LIST
fi


