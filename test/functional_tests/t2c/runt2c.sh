#!/bin/bash

wrkDir=`pwd`
logFile="t2c.log"
paramFile="t2c.params"

wdToDenoptim="$wrkDir/"
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    wdToDenoptim="$(cd "$wrkDir" ; pwd -W | sed 's/\//\\\\/g')"
fi

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR\/|$wdToDenoptim\\\\|g" "$f"
    sed "$sedInPlace" "s|OTF_WDIR|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r FSE "$paramFile"
exec 1>&6 6>&- 

#Check outcome
nGraphs=$(cat "$wrkDir"/*/FSE-Level_*/F*.txt | wc -l | tr -d '[[:space:]]')
if [[ $nGraphs != 4 ]]
then
    echo " "
    echo "Test 't2c' NOT PASSED (symptom: wrong number of graphs $nGraphs)"
    exit -1
fi

n=0;n=$(grep -i -l MNDiff_a123 "$wrkDir"/FSE*/*out.sdf | wc -l | awk '{print $1}')
if [ $n != 3 ]
then
    echo " "
    echo "Test 't2c' NOT PASSED (symptom: some descriptors were not reported - found $n)"
    exit 1
fi

n=0;n=$(grep -l FITNESS "$wrkDir"/FSE*/*out.sdf | wc -l | awk '{print $1}')
if [ $n != 3 ]
then
    echo " "
    echo "Test 't2c' NOT PASSED (symptom: some fitness value were not reported - found $n)"
    exit 1
fi

grep -q 'FragSpaceExplorer run completed' "$wrkDir"/FSE*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't2c' NOT PASSED (symptom: completion msg not found)"
    exit -1
else
    echo "Test 't2c' PASSED"
fi

exit 0

