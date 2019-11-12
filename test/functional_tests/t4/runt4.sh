#!/bin/bash

wrkDir=`pwd`
logFile="t4.log"
paramFile="t4.params"

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/FragSpaceExplorer.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
nGraphs=$(cat $wrkDir/*/FSE-Level_*/F*.txt | wc -l | tr -d '[[:space:]]')
#28
if [[ $nGraphs != 27 ]]
then
    echo " "
    echo "Test 't4' NOT PASSED (symptom: wrong number of graphs $nGraphs)"
    exit -1
fi

grep -q 'FragSpaceExplorer run completed' "$wrkDir"/FSE*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't4' NOT PASSED (symptom: completion msg not found)"
    exit -1
else
    echo "Test 't4' PASSED"
fi

exit 0

