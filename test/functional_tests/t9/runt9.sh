#!/bin/bash

wrkDir=`pwd`
logFile="t9.log"
paramFile="t9.params"

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
if [[ "$nGraphs" != 185 ]]
then
    echo " "
    echo "Test 't9' NOT PASSED (symptom: wrong number of graphs $nGraphs)"
    exit -1
fi

nMols=$(ls -1 "$wrkDir"/*/M*out.sdf | wc -l | tr -d '[[:space:]]')
if [[ "$nMols" != 125 ]]
then
    echo " "
    echo "Test 't9' NOT PASSED (symptom: wrong number of output mols $nMols)"
    exit -1
fi

grep -q 'FragSpaceExplorer run completed' "$wrkDir"/FSE*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't9' NOT PASSED (symptom: completion msg not found)"
    exit -1
else
    echo "Test 't9' PASSED"
fi


exit 0

