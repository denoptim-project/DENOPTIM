#!/bin/bash

wrkDir=`pwd`
logFile="t12c.log"
paramFile="t12c.params"

wdToDenoptim="$wrkDir/"
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    wdToDenoptim="$(cd "$wrkDir" ; pwd -W | sed 's/\//\\\\/g')"
fi

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR\/|$wdToDenoptim\\\\|g" "$f"
    sed "$sedInPlace" "s|OTF_WDIR|$wdToDenoptim|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&- 

#Check outcome
uidA=$(grep -q 'SPEUIVXLLWOEMJ-UHFFFAOYNA-N' "$wrkDir"/MOLUID.txt)
uidA=$(grep -q 'UID with some text, numberts 1223 456, and symbols *@._-:,%&§' "$wrkDir"/MOLUID.txt)
prod=$((uidA * uidB))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12c' NOT PASSED (symptom: missing UID from initial popl. file)"
    exit 1
fi

uidA=$(grep -q 'MMMMMMMMMMMMMM-UHFFFAOYSA-N' "$wrkDir"/MOLUID.txt)
uidB=$(grep -q 'This UID is not an InChi key but some text, with numbers 1234, and symbols #@._;' "$wrkDir"/MOLUID.txt)
uidC=$(grep -q 'PPPPPPPPPPPPPP-UHFFFAOYSA-N' "$wrkDir"/MOLUID.txt)
prod=$((uidA * uidB * uidC))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12c' NOT PASSED (symptom: missing UID from UIDFileIn)"
    exit 1
fi

n=0;n=$(grep -i -l Zagreb "$wrkDir"/RUN*/*/*out.sdf | wc -l | awk '{print $1}')
if [ 15 -gt $n ]
then
    echo " "
    echo "Test 't12c' NOT PASSED (symptom: some descriptors were not reported - found $n)"
    exit 1
fi

n=0;n=$(grep -l FITNESS "$wrkDir"/RUN*/*/*out.sdf | wc -l | awk '{print $1}')
if [ 15 -gt $n ]
then
    echo " "
    echo "Test 't12c' NOT PASSED (symptom: some fitness value were not reported - found $n)"
    exit 1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12c' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12c' PASSED"
fi
exit 0

