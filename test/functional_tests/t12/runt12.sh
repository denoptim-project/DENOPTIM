#!/bin/bash

wrkDir=`pwd`
logFile="t12.log"
paramFile="t12.params"

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
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles/DenoptimGA.jar" "$paramFile"
exec 1>&6 6>&-

#Check outcome
function isInUIDVector() {
    uid="$1"
    symUIDSet=("SLCXUQQDPJZJFU-PREDEBANNA-N" "XOBKSJJDNFUZPF-UHFFFAOYNA-N" "UWVUUAMXEHQIJL-PREDEBANNA-N" "GEGLCBTXYBXOJA-PREDEBANNA-N" "AAIOWJYNAVKNSR-UHFFFAOYNA-N" "ACGNRLPBAWDHSO-UHFFFAOYNA-N" "AHAFJAUUVOYKFU-UHFFFAOYNA-N" "AMOXORAHSJKEGP-UHFFFAOYNA-N" "APGQMGGYMOAPDL-UHFFFAOYNA-N" "ATUOYWHBWRKTHZ-UHFFFAOYNA-N" "AZHSSKPUVBVXLK-UHFFFAOYNA-N" "BECNQKRGALRPCX-UHFFFAOYNA-N" "BNGANSSGBJAGIW-UHFFFAOYNA-N" "BVEQRPPQVRLBQY-UHFFFAOYNA-N" "CIBMHJPPKCXONB-UHFFFAOYNA-N" "CKFGINPQOCXMAZ-UHFFFAOYNA-N" "CTJAKAQLCQKBTC-UHFFFAOYNA-N" "CZIDTSODKOFJNN-UHFFFAOYNA-N" "FDBMVGZOFRWXGW-UHFFFAOYNA-N" "FQYIEXPVXBCYRP-UHFFFAOYNA-N" "GZTYCIXSJQHVNS-UHFFFAOYNA-N" "HEWZVZIVELJPQZ-UHFFFAOYNA-N" "IXFCAQTZQUFQGU-UHFFFAOYNA-N" "JDLOJZBMTBIATO-UHFFFAOYNA-N" "KNYUPEIZCGMVFE-UHFFFAOYNA-N" "LSQZZMZYZPEYGY-UHFFFAOYNA-N" "NEVZTUDIKNCVNE-UHFFFAOYNA-N" "NGVTXINFTCZHGA-UHFFFAOYNA-N" "NKDDWNXOKDWJAK-UHFFFAOYNA-N" "NOSJIHNMTOMLHW-UHFFFAOYNA-N" "NPNPZTNLOVBDOC-UHFFFAOYNA-N" "OTMSDBZUPAUEDD-UHFFFAOYNA-N" "PNYFNXVHMKKELK-UHFFFAOYNA-N" "PRCNTEMWKQXDDM-UHFFFAOYNA-N" "PSZOHVCVJZCXKU-UHFFFAOYNA-N" "PZIGDQQHBLGNHP-UHFFFAOYNA-N" "QEDAGHAELKPMPV-UHFFFAOYNA-N" "RAMPIMNVBQYGOB-UHFFFAOYNA-N" "RDIXDWRSFIEIBW-UHFFFAOYNA-N" "RUUBIFVWPACNLY-UHFFFAOYNA-N" "SCYULBFZEHDVBN-UHFFFAOYNA-N" "SHTNYEVTKAFOPP-UHFFFAOYNA-N" "SIEPAMHQARKLSE-UHFFFAOYNA-N" "SOGYPWICVSCTBV-UHFFFAOYNA-N" "SPEUIVXLLWOEMJ-UHFFFAOYNA-N" "UTCVPSPEEGOCCY-UHFFFAOYNA-N" "UXODXOGFYYUYHJ-UHFFFAOYNA-N" "VNWKTOKETHGBQD-UHFFFAOYNA-N" "VOYLGRBFALRMGT-UHFFFAOYNA-N" "WIHMGGWNMISDNJ-UHFFFAOYNA-N" "WTBXHOLYBWGOJY-UHFFFAOYNA-N" "XDJBBXPTJIFTKO-UHFFFAOYNA-N" "YKFPUCGAXARQPK-UHFFFAOYNA-N" "ZDWMPMOONJIHAL-UHFFFAOYNA-N" "MMMMMMMMMMMMMM-UHFFFAOYSA-N" "PPPPPPPPPPPPPP-UHFFFAOYSA-N" "BFSUQRCCKXZXEX-UHFFFAOYNA-N" "This UID is not an InChi key but some text, with numbers 1234, and symbols #@._;" "This UID is also in the UIDFileIn, 12345, .;-_@*" "UID with some text, numberts 1223 456, and symbols *@._-:,%&§")
    for symUID in "${symUIDSet[@]}"
    do
        if [ "$symUID" == "$uid" ] ; then
            return 0 #true
        fi
    done
    return 1 #false
}
while IFS='' read -r uid || [[ -n "$uid" ]]; do
    if ! isInUIDVector "$uid" ; then
        echo " "
        echo "Test 't12' NOT PASSED (symptom: check unexpected UID '$uid'. If correct add it to the runt12.sh script)"
        exit 1
    fi
done < "$wrkDir"/RUN*/MOLUID.txt

uidA=$(grep -q 'SPEUIVXLLWOEMJ-UHFFFAOYNA-N' "$wrkDir"/RUN*/MOLUID.txt)
uidA=$(grep -q 'UID with some text, numberts 1223 456, and symbols *@._-:,%&§' "$wrkDir"/RUN*/MOLUID.txt)
prod=$((uidA * uidB))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: missing UID from initial popl. file)"
    exit 1
fi

uidA=$(grep -q 'MMMMMMMMMMMMMM-UHFFFAOYSA-N' "$wrkDir"/RUN*/MOLUID.txt)
uidB=$(grep -q 'This UID is not an InChi key but some text, with numbers 1234, and symbols #@._;' "$wrkDir"/RUN*/MOLUID.txt)
uidC=$(grep -q 'PPPPPPPPPPPPPP-UHFFFAOYSA-N' "$wrkDir"/RUN*/MOLUID.txt)
prod=$((uidA * uidB * uidC))
if [[ "$prod" != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: missing UID from UIDFileIn)"
    exit 1
fi

grep -q 'DENOPTIM EA run completed' "$wrkDir"/RUN*.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't12' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't12' PASSED"
fi
exit 0
