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

# TODO: consider using a fake Reinvent server that only answers with 
# pre-computed responses. This would speed up and ensure reproducibility.

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

# Check if Reinvent-specific dependencies are satisfied
reinventConfigFiles=($(ls data/reinvent_config_*.json))
if [[ ${#reinventConfigFiles[@]} -eq 0 ]]
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
paramFiles=($(ls t*.params))
for paramFile in "${paramFiles[@]}"
    do
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
done

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

#
# Strings searched in the candidate SDF files, and the minimum number of
# matches expected for t34-1, t34-2, and t34-3. To add a check, append one
# entry to each array, in the same position.
#
expectedTotalCandidates=(20 20 20)

queryPatterns=(
    '"source":"CONSTRUCTION".*"attribute":"Reinvent (with scaffolds)"'
    '"source":"CONSTRUCTION".*"attribute":"Reinvent (de novo)"'
    '"source":"INPUT"'
    '"source":"MUTATION".*"mutationType":"CHANGEBRANCH"'
    '"source":"MUTATION".*"mutationType":"EXTEND"'
    '"source":"MUTATION".*"mutationType":"CHANGELINK"'
    '"source":"MUTATION".*"mutationType":"ADDLINK"'
)

#t34-x x:1   2   3   4   5   6
queryExpected=(
       "20   0   0   0   0   0"  # construction from scaffold
        "0  15   0   0   0   0"  # construction de novo
        "0   5  10  10  10  10"  # input
        "0   0  10   0   0   0"  # change branch
        "0   0   0  10   0   0"  # extend
        "0   0   0   0  10   0"  # change link
        "0   0   0   0   0  10"  # add link
)


#
# Run the tests and check outcomes
#
for ((testNumber=1; testNumber<=${#expectedTotalCandidates[@]}; testNumber++))
do
    idx=$((testNumber-1))
    testName="t34-$testNumber"
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

    # Check the number of candidates generated
    totalCandidates=$(ls "$runFolder/"Gen*/*.sdf | wc -l)
    if [[ "$totalCandidates" -lt "${expectedTotalCandidates[$idx]}" ]]
    then
        echo " "
        echo "Test '$testName' NOT PASSED (Found $totalCandidates/${expectedTotalCandidates[$idx]} candidates)"
        exit 1
    fi

    for q in "${!queryPatterns[@]}"
    do
        nFound=$(grep -l "${queryPatterns[$q]}" "$runFolder/"Gen*/*.sdf | wc -l)
        expectedForQuery=(${queryExpected[$q]})
        if [[ "$nFound" -ne "${expectedForQuery[$idx]}" ]]
        then
            echo " "
            echo "Test '$testName' NOT PASSED (Found $nFound/${expectedForQuery[$idx]} for query '${queryPatterns[$q]}')"
            exit 1
        fi
    done

done

echo "Test 't34' PASSED"
exit 0

