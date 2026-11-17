#!/bin/bash
#
# This script is meant to be executed by runAllTests.sh.
# Usually, you should not run this script yourself.
#
# WARNING!
# Here we test functionality related with the integration with 
# the Reinvent software 
# (https://github.com/MolecularAI/REINVENT4). However, we do not
# call or require Reinvent to be available as pre-computed
# data will be read from file.
#

wrkDir=`pwd`

# First JSON string value of key $1 in file $2. Portable on BSD and GNU sed.
jsonStringValue() {
    sed -n 's/.*"'"$1"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$2" | head -n 1
}

if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

# Now, start the test
mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    # Handle sedInPlace which may contain spaces (BSD sed: -i '')
    if [[ "$sedInPlace" == *"''"* ]]; then
        sed -i '' "s|OTF_WDIR|$wrkDir|g" "$f"
        sed -i '' "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
    else
        sed $sedInPlace "s|OTF_WDIR|$wrkDir|g" "$f"
        sed $sedInPlace "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
    fi
done

#Run sub tests
elSymbols=('C  ' 'N  ' 'P  ' 'H  ' 'ATM' 'O  ' 'Si ' 'F  ' 'S  ')
graphFeatures=('DENOPTIMRing' 'SymmetricVertexes' 'CAP' 'UNDEFINED' 'vertexId' 'srcAPID')
totChecks=0
paramFiles=($(ls t*.params))
nSubTests=${#paramFiles[@]}
for paramFile in "${paramFiles[@]}"
do
    i=${paramFile%.params}
    testName="$i"
    i=${testName##*-}
    "$javaDENOPTIM" -jar "$denoptimJar" -r GO "$paramFile" > "$testName.log" 2>&1
    if ! grep -q 'Completed GeneOpsRunner' "$testName.log"
    then
        echo " "
        echo "Test 't35' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi

    if [ ! -f "graph_mut-$i.sdf" ]; then
        echo "Test 't35' NOT PASSED (symptom: file graph_mut-$i.sdf not found)"
        exit -1
    fi

    if [ ! -f "expected_output/graph_mut-$i.sdf" ]; then
        echo "Test 't35' NOT PASSED (symptom: cannot find file with expected results (expected_output/graph_mut-$i.sdf)"
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
            echo "Test 't35' NOT PASSED (symptom: wrong number of $el atoms in graph_mut-$i.sdf: $nEl, should be $nElRef)"
            exit -1
        fi
        nChecks=$((nChecks+1))
        #Only for debugging
        #echo "$i -> $el $nEl $nElRef"
    done

    if [ "$nChecks" -ne "${#elSymbols[@]}" ]
    then
        echo "Test 't35' NOT PASSED (sympton: wrong number of checks $nChecks in step $i)"
        exit -1
    fi
    # Check of graph features
    g=$(grep -A 1 "GraphENC" "graph_mut-$i.sdf" | tail -n 1)
    gRef=$(grep -A 1 "GraphENC" "expected_output/graph_mut-$i.sdf" | tail -n 1)
    for s in "${graphFeatures[@]}"
    do
        ng=$(echo "$g" | grep -c "$s")
        ngRef=$(echo "$gRef" | grep -c "$s")
        if [ "$ng" -ne "$ngRef" ]
        then
            echo "Test 't35' NOT PASSED (sympton: wrong number of $s in step $i: expected=$ngRef, actual=$ng)"
            exit -1
        fi
    done

    totChecks=$((totChecks+1))
done


if [ "$totChecks" -ne $nSubTests ]
then
    echo "Test 't35' NOT PASSED (sympton: wrong number of sub runs $totChecks)"
    exit -1
fi

echo "Test 't35' PASSED"
exit 0

#
# Run first
#
testName="t35-1"
logFile="$testName.log"
paramFile="$testName.params"
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile" > "$logFile" 2>&1
exec 1>&6 6>&-

runFolder="$(grep "Output files associated with the current run are located in" "$logFile" | awk -F'are located in ' '{print $2}')"

if ! grep -q 'DENOPTIM EA run completed' "$runFolder.log"
then
    echo " "
    echo "Test '$testName' NOT PASSED (symptom: completion msg not found in $runFolder.log)"
    exit 1
fi

totalCandidates=$(ls "$runFolder/"Gen*/*.sdf | wc -l)
expectedTotalCandidates=20
if [[ "$totalCandidates" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test '$testName' NOT PASSED (Found $totalCandidates/$expectedTotalCandidates candidates)"
   exit 1
fi

nDeNovoFromReinvent=$(grep -l '"attribute":"Reinvent"' "$runFolder/"Gen*/*.sdf | wc -l)
if [[ "$nDeNovoFromReinvent" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test '$testName' NOT PASSED (Found $nDeNovoFromReinvent/$expectedTotalCandidates de novo candidates from Reinvent)"
   exit 1
fi


#
# Run second
#
testName="t35-2"
logFile="$testName.log"
paramFile="$testName.params"
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile" > "$logFile" 2>&1
exec 1>&6 6>&-

runFolder="$(grep "Output files associated with the current run are located in" "$logFile" | awk -F'are located in ' '{print $2}')"

if ! grep -q 'DENOPTIM EA run completed' "$runFolder.log"
then
    echo " "
    echo "Test '$testName' NOT PASSED (symptom: completion msg not found)"
    exit 1
fi

totalCandidates=$(ls "$runFolder/"Gen*/*.sdf | wc -l)
expectedTotalCandidates=20
if [[ "$totalCandidates" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test '$testName' NOT PASSED (Found $totalCandidates/$expectedTotalCandidates candidates)"
   exit 1
fi

nDeNovoFromReinvent=$(grep -l '"attribute":"Reinvent"' "$runFolder"/Gen*/*.sdf | wc -l)
if [[ "$nDeNovoFromReinvent" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test '$testName' NOT PASSED (Found $nDeNovoFromReinvent/$expectedTotalCandidates de novo candidates from Reinvent)"
   exit 1
fi

echo "Test 't35' PASSED"
exit 0

