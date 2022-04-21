#!/bin/bash

wrkDir=`pwd`

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

#Run first step
exec 6>&1
exec > "t19.log_1"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/StringConverter.jar" t19.params_1
exec 1>&6 6>&- 

#Check outcome
if ! grep -q 'StringConverter run completed' "$wrkDir"/t19.log_1
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: completion msg not found - step 1)"
    exit 1
fi
if [ ! -f "intermediate.txt" ]
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: missing intermediate file, i.e., output of 1st step)"
    exit -1
fi

exec 6>&1
exec > "t19.log_2"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/StringConverter.jar" t19.params_2
exec 1>&6 6>&-

#Check outcome
if ! grep -q 'StringConverter run completed' "$wrkDir"/t19.log_2
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: completion msg not found - step 2)"
    exit 1
fi
if [ ! -f "final.sdf" ]
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: missing final sdf file)"
    exit -1
fi

naref=$(head -n 4 reference.sdf | tail -n 1 | awk '{print $1}')
nbref=$(head -n 4 reference.sdf | tail -n 1 | awk '{print $2}')
na=$(head -n 4 final.sdf | tail -n 1 | awk '{print $1}')
nb=$(head -n 4 final.sdf | tail -n 1 | awk '{print $2}')

if [[ "$na" != "$naref" ]] ; then
    echo "Test 't19' NOT PASSED (symptom: wrong number of atoms)."
    exit -1
fi
if [[ "$nb" != "$nbref" ]] ; then
    echo "Test 't19' NOT PASSED (symptom: wrong number of bonds)."
    exit -1
fi


nref=$(grep -n "^M *END" reference.sdf | awk -F':' '{print $1}')
n=$(grep -n "^M *END" final.sdf | awk -F':' '{print $1}')

head -n "$nref" reference.sdf | tail -n +5 > cnRef
head -n "$n" final.sdf | tail -n +5 > cn

head -n "$na" cn | cut -c 1-7,11-17,21-27,31-35 > atmLst
head -n "$naref" cnRef | cut -c 1-7,11-17,21-27,31-35 > atmLstRef
tail -n "$((nb+1))" cn > bndLst
tail -n "$((nbref+1))" cn > bndLstRef

differences=$(diff atmLst atmLstref)
if [ "$differences" != "" ]; then
    echo "Test 't19' NOT PASSED (symptom: different atom list)."
    echo "$differences"
    exit -1
fi
differences=$(diff bndLst bndLstref)
if [ "$differences" != "" ]; then
    echo "Test 't19' NOT PASSED (symptom: different bond list)."
    echo "$differences"
    exit -1
else
    echo "Test 't19' PASSED"
fi

exit 0
