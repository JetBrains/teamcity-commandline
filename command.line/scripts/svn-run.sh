#!/bin/sh

#
# check parameters passed
#
if test -z "$3"
then
 echo "Usage: remoterun.sh <configurationid> <comment> <path_to_directory_which_contains_changes>"
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
  echo "The JAVA_HOME environment variable is not defined"
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
if [[ $3 == /* ]]
then
 #absolute
 svn status $3 |  awk '$1~/.*/ {print ($2)}'> $LIST
else
 #relative
 svn status $3 |  awk --assign PWD=$PWD '$1~/.*/ {print (PWD "/" $2)}'> $LIST
fi
#
# run remoterun
#
$JAVA_HOME/java -jar tcc.jar run -c $1 -m $2 @$LIST


