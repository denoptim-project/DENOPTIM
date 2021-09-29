#!/bin/bash

wrkDir=`pwd`
logFile="t15.log"
paramFile="t15.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/TestOperator.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if [ ! -f "$wrkDir/male_xo.sdf" ] || [ ! -f "$wrkDir/female_xo.sdf" ]
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: output file/s not found)"
    exit 1
fi

exec 6>&1
exec > "${logFile}_check-1"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/GraphListsHandler.jar t15-check-1.params
exec 1>&6 6>&-

if ! grep -q "#Matches in list A: 1/1" "${logFile}_check-1"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph differs in male_xo.sdf)"
    exit 1
fi
if ! grep -q "#Matches in list B: 1/1" "${logFile}_check-1"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph differs in male_xo.sdf)"
    exit 1
fi

exec 6>&1
exec > "${logFile}_check-2"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/GraphListsHandler.jar t15-check-2.params
exec 1>&6 6>&-

if ! grep -q "#Matches in list A: 1/1" "${logFile}_check-2"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph differs in female_xo.sdf)"
    exit 1
fi
if ! grep -q "#Matches in list B: 1/1" "${logFile}_check-2"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph differs in female_xo.sdf)"
    exit 1
fi

if ! grep -q 'TestOperator run completed' "$wrkDir"/t15.log
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't15' PASSED"
fi
exit 0
