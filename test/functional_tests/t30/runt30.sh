#!/bin/bash

wrkDir=`pwd`
logFile="t30.log"
paramFile="t30.params"

mv data/* "$wrkDir"
rm -rf data

#Run sub tests
nSubTests=1
totChecks=0
expectedNoMissingAtomMols=(3)
expectedFixedMols=(0)
expectedFragments=(0)
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
            echo "Test 't30' NOT PASSED (symptom: wring number of structures without missing atoms: $n vs. ${expectedNoMissingAtomMols[$i-1]}."
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedFixedMols[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/structuresFixed* | wc -l)
        if [ "$n" -ne ${expectedFixedMols[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wring number of structures without missing atoms: $n vs. ${expectedFixedMols[$i-1]}."
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedFragments[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/Fragments* | wc -l)
        if [ "$n" -ne ${expectedFragments[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wring number of structures without missing atoms: $n vs. ${expectedFragments[$i-1]}."
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
