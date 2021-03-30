#!/bin/bash

wrkDir=`pwd`
logFile="t20.log"
paramFile="t20.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/TestOperator.jar" "$paramFile"
exec 1>&6 6>&-


# Checking outcome
nChecks=0
for xoResult in 'male_xo.sdf' 'female_xo.sdf'
do
    if [ ! -f "$xoResult" ]; then
        echo "Test 't20' NOT PASSED (symptom: file $xoResult not found)"
        exit -1
    fi
    nEnd=$(grep -n "M *END" "$xoResult" | awk -F':' '{print $1}')
    nEndRef=$(grep -n "M *END" "expected_output/$xoResult" | awk -F':' '{print $1}')
    for el in 'C' 'N' 'P' 'H'
    do
        nEl=$(head -n "$nEnd" "$xoResult" | grep -c " $el   ")
        nElRef=$(head -n "$nEndRef" "expected_output/$xoResult" | grep -c " $el   ")
        if [ "$nEl" -ne "$nElRef" ]; then
            echo "Test 't20' NOT PASSED (symptom: wrong number of $el atoms in $xoResult"
            exit -1
        fi
        nChecks=$((nChecks+1))
    done
done

if [ "$nChecks" -eq 8 ]
then
    echo "Test 't20' PASSED"
    exit 0
else
    echo "Test 't20' NOT PASSED (sympton: wrong number of checks $nChecks)"
    exit -1
fi
