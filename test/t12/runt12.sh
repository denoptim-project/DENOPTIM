#!/bin/bash

wrkDir=`pwd`
logFile="t12.log"

mv data/* $wrkDir
rm -rf data

#Run it
exec 6>&1
exec > $logFile
exec 2>&1
$javaDENOPTIM -jar $DENOPTIMJarFiles/CheckpointReader.jar checkpoint.chk
exec 1>&6 6>&- 

#Check outcome
grep -q "\[level\=2\, unqVrtId\=734\, unqGraphId\=362\, unqMolId\=231\, graphId\=354\, rootId\=7\, nextIds\=\[2\, 0\, 0\, 2\, 0\, 0\, 0\, 0\, 1\]" $logFile
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED"
    exit -1
else
    echo "Test 't12' PASSED"
fi

exit 0

