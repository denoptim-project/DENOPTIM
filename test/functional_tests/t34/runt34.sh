#!/bin/bash
#
# This script is meant to be executed by runAllTests.sh.
# Usually, you should not run this script yourself.
#
# WARNING!
# To run these tests we depend on the Reinvent software 
# (https://github.com/MolecularAI/REINVENT4), which must be
# reachable through a socket server.
# 
# WARNING!
# We also depend on the availability of specific Reinvent models,
# which must be available in the socket server. The filenames
# of the models are specified in the reinvent_config_*.json files.
#

wrkDir=`pwd`
logFile="t34.log"
paramFile="t34.params"

# First JSON string value of key $1 in file $2. Portable on BSD and GNU sed.
jsonStringValue() {
    sed -n 's/.*"'"$1"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$2" | head -n 1
}

if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

# Check if Reinvent-specific dependencies are satisfied
reinventConfig=($(ls data/reinvent_config_*.json))
if [[ ${#reinventConfig[@]} -eq 0 ]]
then
    # This is not possible unless you temper with the repo, but it's a good check.
    echo " "
    echo "Test 't34' NOT PASSED: No Reinvent configuration files found"
    exit 1
fi
for config in "${reinventConfig[@]}"
do
    modelFilePathname=$(jsonStringValue model_file "$config")
    if [[ ! -f "$modelFilePathname" ]]
    then
        echo " "
        echo "Test 't34' SKIPPED: Reinvent model file not found: $modelFilePathname"
        exit 0
    fi
done

# Check if the Reinvent socket server is reachable
reinventHostname=$(grep -i 'REINVENTSERVERHOSTNAME' "$paramFile" | awk -F'=' '{print $2}')
reinventPort=$(grep -i 'REINVENTSERVERPORT' "$paramFile" | awk -F'=' '{print $2}')
if [[ -z "$reinventHostname" ]] || [[ -z "$reinventPort" ]]
then
    echo " "
    echo "Test 't34' NOT PASSED: Reinvent hostname or port not found in $paramFile"
    exit 1
fi
if ! nc -z $reinventHostname $reinventPort > /dev/null 2>&1
then
    echo " "
    echo "Test 't34' SKIPPED: Reinvent socket server ($reinventHostname:$reinventPort) not reachable"
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

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&-

runFolder=$(basename "$(ls -1dt "$wrkDir"/RUN*/ | head -n 1)")

grep -q 'DENOPTIM EA run completed' "$wrkDir"/$runFolder.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't34' NOT PASSED (symptom: completion msg not found)"
    exit 1
fi

totalCandidates=$(ls "$wrkDir/"RUN*/Gen*/*.sdf | wc -l)
expectedTotalCandidates=25
if [[ "$totalCandidates" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test 't34' NOT PASSED (Found $totalCandidates/$expectedTotalCandidates candidates)"
   exit 1
fi

nDeNovoFromReinvent=$(grep -l '"attribute":"Reinvent"' "$wrkDir/"RUN*/Gen*/*.sdf | wc -l)
if [[ "$nDeNovoFromReinvent" -lt "$expectedTotalCandidates" ]]
then
   echo " "
   echo "Test 't34' NOT PASSED (Found $nDeNovoFromReinvent/$expectedTotalCandidates de novo candidates from Reinvent)"
   exit 1
fi

echo "Test 't34' PASSED"
exit 0

