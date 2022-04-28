#!/bin/bash

wrkDir=`pwd`
logFile="t12.log"
paramFile="t12.params"

if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

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
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&-

#Check outcome

runFolder=$(basename $(ls -lrtd "$wrkDir"/RUN*/ | tail -n 1 | awk '{print $NF}'))

uidA=$(grep -q 'SPEUIVXLLWOEMJ-UHFFFAOYNA-N' "$wrkDir"/$runFolder/MOLUID.txt)
uidA=$(grep -q 'UID with some text, numberts 1223 456, and symbols *@._-:,%&§' "$wrkDir"/$runFolder/MOLUID.txt)
prod=$((uidA * uidB))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: missing UID from initial popl. file)"
    exit 1
fi

uidA=$(grep -q 'MMMMMMMMMMMMMM-UHFFFAOYSA-N' "$wrkDir"/$runFolder/MOLUID.txt)
uidB=$(grep -q 'This UID is not an InChi key but some text, with numbers 1234, and symbols #@._;' "$wrkDir"/$runFolder/MOLUID.txt)
uidC=$(grep -q 'PPPPPPPPPPPPPP-UHFFFAOYSA-N' "$wrkDir"/$runFolder/MOLUID.txt)
prod=$((uidA * uidB * uidC))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: missing UID from UIDFileIn)"
    exit 1
fi

# NB: set this to true in case you have to update the expected results. This
# is needed in case of changes to the GA algorithms that make the sequence of
# generated candidates differe from version to version. For example, when
# introducing new or modifying existing genetic operators.
prepare_expected=false
# The lines beginning with M00... should then be processed to remove duplicates 
# and written into the data/expected_results file

for genFolder in Gen0 Gen1 Gen2 Gen3 Final
do
    if [ ! -d "$wrkDir/$runFolder/$genFolder" ]; then
        echo " "
        echo "Test 't12' NOT PASSED (Lack of reproducibility - symptom: missing folder $genFolder)"
        exit 1
    fi

    find "$wrkDir/$runFolder/$genFolder" -name "*_out.sdf" | while read outSDF 
    do
        molName=$(basename "$outSDF" _out.sdf)
        
        endMolDef=$(grep -n "M  END" "$outSDF" | awk -F':' '{print $1}')
        nC=$(head -n "$endMolDef" "$outSDF"  | grep -c "[0-9] C   [0-9]")
        nH=$(head -n "$endMolDef" "$outSDF"  | grep -c "[0-9] H   [0-9]")
        nCl=$(head -n "$endMolDef" "$outSDF" | grep -c "[0-9] Cl  [0-9]")
        nF=$(head -n "$endMolDef" "$outSDF"  | grep -c "[0-9] F   [0-9]")
        nO=$(head -n "$endMolDef" "$outSDF"  | grep -c "[0-9] O   [0-9]")
        nAtms=$(head -n 4 "$outSDF" | tail -n 1 | cut -c 1-3 | sed 's/ //g')
        nBnds=$(head -n 4 "$outSDF" | tail -n 1 | cut -c 4-6 | sed 's/ //g')
        invariant="C${nC}H${nH}Cl${nCl}F${nF}O${nO}_nAtms${nAtms}_nBnds$nBnds"
        
        if $prepare_expected ; then
            echo "$molName $invariant"
        else
            if ! grep -q "$molName" "$wrkDir/expected_results"; then
                echo " "
                echo "Test 't12' NOT PASSED (Lack of reproducibility - symptom: could not find mol name '$molName' in expected results)"
                exit 1
            fi
            expected=$(grep "$molName" "$wrkDir/expected_results" | awk '{print $2}')
            if [[ "$expected" != "$invariant" ]]; then
                echo " "
                echo "Test 't12' NOT PASSED (Lack of reproducibility - symptom: inconsistent invariant for $molName)"
                echo "Found:    _${invariant}_"
                echo "Expected: _${expected}_"
                exit 1
            fi
        fi
    done
done

if $prepare_expected ; then
    echo " "
    echo "Test 't12' RUN IN PREPARATION MODE Change value of 'prepare_expected'!"
    exit 1
fi


grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12' PASSED"
fi
exit 0
