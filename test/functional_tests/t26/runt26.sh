#!/bin/bash

wrkDir=`pwd`
logFile="t26.log"
paramFile="t26.params"

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

#Run sub tests
nSubTests=30
elSymbols=('C  ' 'N  ' 'P  ' 'H  ' 'ATM' 'O  ' 'Si ' 'F  ' 'S  ')
totChecks=0
for i in $(seq 1 $nSubTests)
do
    "$javaDENOPTIM" -jar "$denoptimJar" -r GO "t26-$i.params" > "t26-$i.log" 2>&1 
    if ! grep -q 'Completed GeneOpsRunner' "t26-$i.log"
    then
        echo " "
        echo "Test 't26' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi

    if [ ! -f "graph_mut-$i.sdf" ]; then
        echo "Test 't26' NOT PASSED (symptom: file graph_mut-$i.sdf not found)"
        exit -1
    fi

    if [ ! -f "expected_output/graph_mut-$i.sdf" ]; then
        echo "Test 't26' NOT PASSED (symptom: cannot find file with expected results (expected_output/graph_mut-$i.sdf)"
        exit -1
    fi

    nChecks=0
    nEnd=$(grep -n "M *END" "graph_mut-$i.sdf" | awk -F':' '{print $1}')
    nEndRef=$(grep -n "M *END" "expected_output/graph_mut-$i.sdf" | awk -F':' '{print $1}')
    for el in "${elSymbols[@]}"
    do
        nEl=$(head -n "$nEnd" "graph_mut-$i.sdf" | grep -c " $el ")
        nElRef=$(head -n "$nEndRef" "expected_output/graph_mut-$i.sdf" | grep -c " $el ")
        if [ "$nEl" -ne "$nElRef" ]; then
            echo "Test 't26' NOT PASSED (symptom: wrong number of $el atoms in graph_mut-$i.sdf: $nEl, should be $nElRef)"
            exit -1
        fi
        nChecks=$((nChecks+1))
        #Only for debugging
        #echo "$i -> $el $nEl $nElRef"
    done

    if [ "$nChecks" -ne "${#elSymbols[@]}" ]
    then
        echo "Test 't26' NOT PASSED (sympton: wrong number of checks $nChecks in step $i)"
        exit -1
    fi

    # Check of graph features
    g=$(grep -A 1 "GraphENC" "graph_mut-$i.sdf" | tail -n 1)
    gRef=$(grep -A 1 "GraphENC" "expected_output/graph_mut-$i.sdf" | tail -n 1)
    string=('DENOPTIMRing' 'SymmetricVertexes')
    for s in "${string[@]}"
    do
        ng=$(echo "$g" | grep -c "$s")
        ngRef=$(echo "$gRef" | grep -c "$s")
        if [ "$ng" -ne "$ngRef" ]
        then
            echo "Test 't26' NOT PASSED (sympton: wrong number of $s in step $i: expected=$ngRef, actual=$ng)"
            exit -1
        fi
    done

    totChecks=$((totChecks+1))
done

if [ "$totChecks" -ne $nSubTests ]
then
    echo "Test 't26' NOT PASSED (sympton: wrong number of sub runs $totChecks)"
    exit -1
fi

echo "Test 't26' PASSED"
exit 0
