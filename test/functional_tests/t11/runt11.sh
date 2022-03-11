#!/bin/bash

wrkDir=`pwd`
logFile="t11.log"
paramFile="t11.params"

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
function isInUIDVector() {
    uid="$1"
    symUIDSet=("AHAFJAUUVOYKFU-UHFFFAOYNA-N" "APGQMGGYMOAPDL-UHFFFAOYNA-N" "ATUOYWHBWRKTHZ-UHFFFAOYNA-N" "BECNQKRGALRPCX-UHFFFAOYNA-N" "CIBMHJPPKCXONB-UHFFFAOYNA-N" "CKFGINPQOCXMAZ-UHFFFAOYNA-N" "FDBMVGZOFRWXGW-UHFFFAOYNA-N" "GZTYCIXSJQHVNS-UHFFFAOYNA-N" "HEWZVZIVELJPQZ-UHFFFAOYNA-N" "IXFCAQTZQUFQGU-UHFFFAOYNA-N" "JDLOJZBMTBIATO-UHFFFAOYNA-N" "LSQZZMZYZPEYGY-UHFFFAOYNA-N" "NEVZTUDIKNCVNE-UHFFFAOYNA-N" "NKDDWNXOKDWJAK-UHFFFAOYNA-N" "PRCNTEMWKQXDDM-UHFFFAOYNA-N" "PZIGDQQHBLGNHP-UHFFFAOYNA-N" "QEDAGHAELKPMPV-UHFFFAOYNA-N" "VNWKTOKETHGBQD-UHFFFAOYNA-N" "XDJBBXPTJIFTKO-UHFFFAOYNA-N" "ZDWMPMOONJIHAL-UHFFFAOYNA-N")
    for symUID in "${symUIDSet[@]}"
    do
        if [ "$symUID" == "$uid" ] ; then
            return 0 #true
        fi
    done
    return 1 #false
}

while IFS='' read -r udi || [[ -n "$udi" ]]; do
    if [ -z "$uid" ] ; then
        continue
    fi
    if ! isInUIDVector "$uid" ; then
        echo " "
        echo "Test 't11' NOT PASSED (symptom: unexpected UID '$uid')"
        exit 1
    fi
done < "$wrkDir"/RUN*/MOLUID.txt

echo "Test 't11' PASSED"
exit 0

