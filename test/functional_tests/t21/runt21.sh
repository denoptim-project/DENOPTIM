#!/bin/bash

wrkDir=`pwd`
logFile="t21.log"
paramFile="t21.params"

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
"$javaDENOPTIM" -jar "$denoptimJar" -r GO "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if ! grep -q 'TestOperator run completed' "$wrkDir"/t21.log
then
    echo " "
    echo "Test 't21' NOT PASSED (symptom: completion msg not found)"
    exit 1
fi

if [ ! -f "graph_mut.sdf" ]; then
    echo "Test 't21' NOT PASSED (symptom: file graph_mut.sdf not found)"
    exit -1
fi

nChecks=0
nEnd=$(grep -n "M *END" "graph_mut.sdf" | awk -F':' '{print $1}')
nEndRef=$(grep -n "M *END" "expected_output/graph_mut.sdf" | awk -F':' '{print $1}')
for el in 'C  ' 'N  ' 'P  ' 'H  ' 'ATM'
do
    nEl=$(head -n "$nEnd" "graph_mut.sdf" | grep -c " $el ")
    nElRef=$(head -n "$nEndRef" "expected_output/graph_mut.sdf" | grep -c " $el ")
    if [ "$nEl" -ne "$nElRef" ]; then
        echo "Test 't21' NOT PASSED (symptom: wrong number of $el atoms in graph_mut.sdf: $nEl, should be $nElRef)"
        exit -1
    fi
    nChecks=$((nChecks+1))
done

if [ "$nChecks" -eq 5 ]
then
    echo "Test 't21' PASSED"
    exit 0
else
    echo "Test 't21' NOT PASSED (sympton: wrong number of checks $nChecks)"
    exit -1
fi

echo "Test 't21' PASSED"
exit 0
