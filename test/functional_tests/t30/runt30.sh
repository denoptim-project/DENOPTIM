#!/bin/bash

wrkDir=`pwd`
logFile="t30.log"
paramFile="t30.params"

mv data/* "$wrkDir"
rm -rf data

# Here we define the expected results
#           sub test ID: 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21
expctdNoMissingAtomMols=(9  1  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0)
    expectedPreFiltered=(0  0  2  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0)  
      expectedFragments=(0  0  0 20 12  8  6 11 70  0  0  0  0  0  9  0  4  2  7  7  3)
expectedIsomorphicFrags=(0  0  0  0  0  0 30 11  0  0  0  0  0  0 96  0  0  0  0  0  0)
        expectedResults=(9  1  2  0  0  0  0  0  0  4  2  2  2  3  0  2  0  0  0  0  0)

nSubTests=${#expctdNoMissingAtomMols[@]}

#Run sub tests
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
        echo "Test 't30' NOT PASSED (symptom: could not find output folder '$output' - step $i)"
        exit -1
    fi
    
    if [ 0 -ne ${expctdNoMissingAtomMols[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/structuresNoMissingAtoms* | wc -l)
        if [ "$n" -ne ${expctdNoMissingAtomMols[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of structures without missing atoms: $n vs. ${expctdNoMissingAtomMols[$i-1]}. Step $i"
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedPreFiltered[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/structuresPreFiltered* | wc -l)
        if [ "$n" -ne ${expectedPreFiltered[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of prefiltered structures : $n vs. ${expectedPreFiltered[$i-1]}. Step $i"
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedFragments[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/Fragments.sdf | wc -l)
        if [ "$n" -ne ${expectedFragments[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of fragments produced: $n vs. ${expectedFragments[$i-1]}. Step $i"
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedIsomorphicFrags[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/MW*All* | wc -l)
        if [ "$n" -ne ${expectedIsomorphicFrags[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of fragments produced: $n vs. ${expectedIsomorphicFrags[$i-1]}. Step $i"
            exit -1
        fi
    fi

    if [ 0 -ne ${expectedResults[$i-1]} ]
    then
        n=$(grep "\$\$\$\$" "$output"/Results.sdf | wc -l)
        if [ "$n" -ne ${expectedResults[$i-1]} ]
        then
            echo "Test 't30' NOT PASSED (symptom: wrong number of structures in 'Results' file: $n vs. ${expectedResults[$i-1]}. Strp $i"
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
