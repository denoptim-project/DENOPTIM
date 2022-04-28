#!/bin/bash

wrkDir=`pwd`
logFile="t28.log"
paramFile="t28.params"

wdToDenoptim="$wrkDir/"
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    wdToDenoptim="$(cd "$wrkDir" ; pwd -W | sed 's/\//\\\\/g')\\\\"
fi

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR\/|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_WDIR|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

for i in 1 2 3
do
    #Run sub tests
    "$javaDENOPTIM" -jar "$denoptimJar" -r GO "t28-$i.params" > "t28-$i.log" 2>&1 
    
    # Checking outcome
    nChecks=0
    for xoResult in "male-${i}_xo.sdf" "female-${i}_xo.sdf"
    do
        if [ ! -f "$xoResult" ]; then
            echo "Test 't28' NOT PASSED (symptom: file $xoResult not found)"
            exit -1
        fi
        nEnd=$(grep -n "M *END" "$xoResult" | awk -F':' '{print $1}')
        nEndRef=$(grep -n "M *END" "expected_output/$xoResult" | awk -F':' '{print $1}')
        for el in 'C' 'N' 'P' 'H' 'O' 'As'
        do
            nEl=$(head -n "$nEnd" "$xoResult" | grep -c " $el   ")
            nElRef=$(head -n "$nEndRef" "expected_output/$xoResult" | grep -c " $el   ")
            if [ "$nEl" -ne "$nElRef" ]; then
                echo "Test 't28' NOT PASSED (symptom: wrong number of $el atoms in $xoResult: $nEl, should be $nElRef)"
                exit -1
            fi
            nChecks=$((nChecks+1))
        done
        nRings=$(grep -c "\"gRings\": \[$" "$xoResult")
        nRingsRef=$(grep -c "\"gRings\": \[$" "expected_output/$xoResult")
        if [ "$nRings" -ne "$nRingsRef" ]; then
            echo "Test 't28' NOT PASSED (symptom: wrong number of rings in $xoResult: $nRings, should be $nRingsRef)"
            exit -1
        fi
    done
    
    if [ "$nChecks" -ne 12 ]
    then
        echo "Test 't28-$i' NOT PASSED (sympton: wrong number of checks $nChecks)"
        exit -1
    fi
done

echo "Test 't28' PASSED"
exit 0
