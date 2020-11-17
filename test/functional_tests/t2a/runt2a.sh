#!/bin/bash

wrkDir=`pwd`
logFile="t2a.log"
paramFile="t2a.params"

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
nGraphs=$(cat "$wrkDir"/*/FSE-Level_*/F*.txt | wc -l | tr -d '[[:space:]]')
#3
if [[ $nGraphs != 3 ]]
then
    echo " "
    echo "Test 't2a' NOT PASSED (symptom: wrong number of graphs $nGraphs)"
    exit -1
fi
n=0;n=$(grep -l "Ring" "$wrkDir"/*/FSE-Level_*/F*.txt | wc -l | awk '{print $1}')
if [[ $n != 1 ]]
then
    echo " "
    echo "Test 't2a' NOT PASSED (symptom: wrong number of generated rings - $n)"
    exit -1
fi

grep -q 'FragSpaceExplorer run completed' "$wrkDir"/FSE*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't2a' NOT PASSED (symptom: completion msg not found)"
    exit -1
else
    echo "Test 't2a' PASSED"
fi

exit 0

