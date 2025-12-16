#!/bin/bash
#
# This script is meant to be executed by runAllTests.sh.
# Usually, you should not run this script yourself.
#
# WARNING!
# To run these tests we depend on an external socket server running the RingClosingMM server.
# This script will start such server and shutdown it after the tests are finished.
# Hovewer, for this to happen the environment must have the rc-optimizer command available.
#

if ! command -v rc-optimizer &> /dev/null
then
    echo " "
    echo "WARNING! Cannot find rc-optimizer command in the environment"
    echo "Skipping test 't1'."
    echo " "
    exit 0
fi

rc-optimizer --help > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo " "
    echo "WARNING! rc-optimizer command is not working"
    echo "Skipping test 't1'."
    echo " "
    exit 0
fi

function compareElementalAnalysis()
{
    res="$1"
    ref="$2"
    nEnd=$(grep -n "M *END" "$res" | awk -F':' '{print $1}')
    nEndRef=$(grep -n "M *END" "$ref" | awk -F':' '{print $1}')
    for el in ' C  ' ' N  ' ' P  ' ' H  ' ' Cl ' ' Ru ' ' B  ' ' V  ' ' Rh  ' ' O  ' ' W  '
    do
        nEl=$(head -n "$nEnd" "$res" | grep -c " $el ")
        nElRef=$(head -n "$nEndRef" "$ref" | grep -c " $el ")
        if [ "$nEl" -ne "$nElRef" ]; then
            echo "Test 't1' NOT PASSED (symptom: wrong number of $el atoms: $nEl, should be $nElRef)"
            exit -1
        fi
    done
}

HOST="${1:-localhost}"
PORT="${2:-59557}"

weOwnRCOServer=false
if ! rc-optimizer --server-status --host "${HOST}" --port "${PORT}" | grep -q "RUNNING" ; then
    rc-optimizer --server-start --host "${HOST}" --port "${PORT}" > server.log 2>&1 &
    sleep 3
    if ! rc-optimizer --server-status --host "${HOST}" --port "${PORT}" | grep -q "RUNNING" ; then
        echo "ERROR: failed to start RCO server"
        echo "See $wrkDir/server.log for details"
        exit -1
    else
        echo "RCO server started!"
    fi
    weOwnRCOServer=true
fi

wrkDir=`pwd`

files=$(ls input/*.sdf )
nFiles=$(ls input/*.sdf | wc -l | awk '{print $1}')

for f in $files
do
    # Set file names
    inpSDF="$wrkDir/$f"
    fname=`basename "$inpSDF" .sdf`
    outSDF="$wrkDir/$fname""_3Dbuilt.sdf"
    dnpParams="$wrkDir/$fname""_DnpCG.params"
    logFile="$wrkDir/$fname.log"

    #report
    echo -ne ' progress: '$fname' / '$nFiles' \r'

    #Prepare parameters
    echo "3DB-inpSDF=$inpSDF" > "$dnpParams"
    echo "3DB-outSDF=$outSDF" >> "$dnpParams"
    echo "FS-RotBondsDefFile=$DENOPTIM_HOME/src/main/resources/data/rotatableBonds-1.0" >> "$dnpParams"

    echo "3DB-workDir=$wrkDir" >> "$dnpParams"

    if [ "$fname" = "MOL000009" ]; then
       # Ring closing settings
        echo "RC-CloseRings"  >> "$dnpParams"
        echo "FS-rotConstrDefFile=input/MOL000009_tor_cnstr" >> "$dnpParams"
    fi

    #run builder
    "$javaDENOPTIM" -jar "$denoptimJar" -r B3D "$dnpParams" &> "$logFile"

    #Check output
    if [ -f "$logFile" ]; then
        if ! grep -q 'MolecularModelBuilder terminated normally' "$logFile"; then
            echo " "
            echo "ERROR! Something went wrong while building molecule $fname:"
            echo "Error was not interpreted. See $logFile!"
            echo " "
            exit 1
        fi
    else
        echo " "
        echo "ERROR! Something went wrong while building molecule $fname:"
        echo "$logFile not found!"
        echo " "
        exit 1
    fi
    if [ ! -f "$outSDF" ]; then
	    echo " "
        echo "ERROR! Something went wrong while building molecule $fname:"
        echo "$outSDF not found!"
        echo "Check log file $logFile"
        echo " "
        exit 1
    fi
    nl=$(wc -l < "$outSDF")
    if [ 4 -gt "$nl" ]; then
        echo "ERROR: Output file '$outSDF' is empty."
        exit 1
    fi

    # Check outcome
    expRes="$wrkDir/expected_results/$fname""_3Dbuilt.sdf"
    if [ ! -f "$expRes" ]; then
        echo "Cannot find expected result for $fname: $expRes"
        exit 1
    fi 
    compareElementalAnalysis "$outSDF" "$expRes"

    if [ "$fname" = "MOL000009" ]; then
        if ! grep -q 'Constraining dihedral along bond Ru3-C7' "$logFile"; then
            echo "Cannot find traces of constrained dihedral"
            exit 1
        fi
    fi    
done

if [ "$weOwnRCOServer" = true ]; then
    rc-optimizer --server-stop --host "${HOST}" --port "${PORT}"

    if [ $? -ne 0 ]; then
        echo "ERROR: failed to stop RCO server"
        echo "See $wrkDir/server.log for details"
        exit -1
    fi
fi

echo " "
echo "Test 't1' PASSED"
exit 0

