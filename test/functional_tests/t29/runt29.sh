#!/bin/bash

wrkDir=`pwd`
logFile="t29.log"
paramFile="t29.params"

mv data/* "$wrkDir"
rm -rf data

#Run sub tests
nSubTests=10
elSymbols=('C  ' 'N  ' 'P  ' 'H  ' 'ATM' 'O  ' 'Si ' 'F  ' 'S  ' 'Cl ' 'Ru ')
totChecks=0
for i in $(seq 1 $nSubTests)
do
    "$javaDENOPTIM" -jar "$denoptimJar" -r GE "t29-$i.params" > "t29-$i.log" 2>&1 
    if ! grep -q 'Completed GraphEditor' "t29-$i.log"
    then
        echo " "
        echo "Test 't29' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi

    if [ ! -f "outGraphs-$i.sdf" ]; then
        echo "Test 't29' NOT PASSED (symptom: file outGraphs-$i.sdf not found)"
        exit -1
    fi

    if [ ! -f "expected_output/outGraphs-$i.sdf" ]; then
        echo "Test 't29' NOT PASSED (symptom: cannot find file with expected results (expected_output/outGraphs-$i.sdf)"
        exit -1
    fi

    nChecks=0
    nEnd=$(grep -n "M *END" "outGraphs-$i.sdf" | awk -F':' '{print $1}')
    nEndRef=$(grep -n "M *END" "expected_output/outGraphs-$i.sdf" | awk -F':' '{print $1}')
    for el in "${elSymbols[@]}"
    do
        nEl=$(head -n "$nEnd" "outGraphs-$i.sdf" | grep -c " $el ")
        nElRef=$(head -n "$nEndRef" "expected_output/outGraphs-$i.sdf" | grep -c " $el ")
        if [ "$nEl" -ne "$nElRef" ]; then
            echo "Test 't29' NOT PASSED (symptom: wrong number of $el atoms in outGraphs-$i.sdf: $nEl, should be $nElRef)"
            exit -1
        fi
        nChecks=$((nChecks+1))
        #Only for debugging
        #echo "$i -> $el $nEl $nElRef"
    done

    if [ "$nChecks" -ne "${#elSymbols[@]}" ]
    then
        echo "Test 't29' NOT PASSED (sympton: wrong number of checks $nChecks in step $i)"
        exit -1
    fi

    # Check of graph features
    g=$(grep -A 1 "GraphENC" "outGraphs-$i.sdf" | tail -n 1)
    gRef=$(grep -A 1 "GraphENC" "expected_output/outGraphs-$i.sdf" | tail -n 1)
    string=('DENOPTIMRing' 'SymmetricSet')
    for s in "${string[@]}"
    do
        ng=$(echo "$g" | grep -c "$s")
        ngRef=$(echo "$gRef" | grep -c "$s")
        if [ "$ng" -ne "$ngRef" ]
        then
            echo "Test 't29' NOT PASSED (sympton: wrong number of $s in step $i)"
            exit -1
        fi
    done

    totChecks=$((totChecks+1))
done

if [ "$totChecks" -ne $nSubTests ]
then
    echo "Test 't29' NOT PASSED (sympton: wrong number of sub runs $totChecks)"
    exit -1
fi

echo "Test 't29' PASSED"
exit 0
