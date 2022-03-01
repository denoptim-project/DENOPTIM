#!/bin/bash

wrkDir=`pwd`

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run and check outcome of each part

totChecks=0
for i in $(seq 1 4)
do
    "$javaDENOPTIM" -jar "$denoptimJar" -r GI -f "t24-$i.params" > "t24-$i.log" 2>&1 
    if ! grep -q 'Isomorphism run completed' "t24-$i.log"
    then
        echo " "
        echo "Test 't24' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi
    if ! grep -q 'Graphs are DENOPTIM-isomorphic!' "t24-$i.log"
    then
        echo " "
        echo "Test 't24' NOT PASSED (symptom: failed isomorphism detection - step $i)"
        exit 1
    fi
    totChecks=$((totChecks+1))
done

for i in $(seq 6 9)
do
    "$javaDENOPTIM" -jar "$denoptimJar" -r GI -f "t24-$i.params" > "t24-$i.log" 2>&1
    if ! grep -q 'Isomorphism run completed' "t24-$i.log"
    then
        echo " "
        echo "Test 't24' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi
    if ! grep -q 'No DENOPTIM-isomorphism found.' "t24-$i.log"
    then
        echo " "
        echo "Test 't24' NOT PASSED (symptom: failed isomorphism detection - step $i)"
        exit 1
    fi
    totChecks=$((totChecks+1))
done

if [ "$totChecks" -eq 8 ] ; then
    echo "Test 't24' PASSED"
    exit 0
else
    echo "Test 't24' NOT PASSED (symptom: wrong number of succesful steps $totChecks/8)"
    exit -1
fi
