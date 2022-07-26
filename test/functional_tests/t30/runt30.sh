#!/bin/bash

wrkDir=`pwd`
logFile="t30.log"
paramFile="t30.params"

mv data/* "$wrkDir"
rm -rf data

#Run sub tests
expectedNoMissingAtomMols=(3 1 0 0 0)
expectedPreFiltered=(0 0 2 0 0)  
expectedFragments=(0 0 0 20 12)
nSubTests=${#expectedNoMissingAtomMols[@]}
totChecks=0
for i in $(seq 1 $nSubTests)
do
    "$javaDENOPTIM" -jar "$denoptimJar" -r FRG "t30-$i.params" > "t30-$i.log" 2>&1 
    if ! grep -q 'Completed Fragmenter' "t30-$i.log"
    then
        echo " "
        echo "Test 't30' NOT PASSED (symptom: completion msg not found - step $i)"
        exit 1
    fi

    output="$(grep "Output files" "t30-$i.log" | cut -c 61-)"
    if [ ! -d "$output" ]; then
        echo "Test 't30' NOT PASSED (symptom: could not find output folder '$output')"
        exit -1
    fi
    
    if [ 0 -ne ${expectedNoMissingAtomMols[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/structuresNoMissingAtoms* | wc -l)
        if [ "$n" -ne ${expectedNoMissingAtomMols[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of structures without missing atoms: $n vs. ${expectedNoMissingAtomMols[$i-1]}."
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedPreFiltered[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/structuresPreFiltered* | wc -l)
        if [ "$n" -ne ${expectedPreFiltered[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of prefiltered structures : $n vs. ${expectedPreFiltered[$i-1]}."
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedFragments[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/Fragments* | wc -l)
        if [ "$n" -ne ${expectedFragments[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of fragments peoduced: $n vs. ${expectedFragments[$i-1]}."
            exit -1
        fi
    fi

    totChecks=$((totChecks+1))
done

if [ "$totChecks" -ne $nSubTests ]
then
    echo "Test 't30' NOT PASSED (sympton: wrong number of sub runs $totChecks)"
    exit -1
fi

echo "Test 't30' PASSED"
exit 0
