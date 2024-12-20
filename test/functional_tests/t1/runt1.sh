#!/bin/bash
#
# This script is meant to be executed by runAllTests.sh.
# Usually, you should not run this script yourself.
#
# WARNING!
# To run these tests we depend on software Tinker.
# Make sure the location of Tinker's executables is defined
# in the environment as $tinkerPathDENOPTIM
#

if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

if [ ! -d "$tinkerPathDENOPTIM" ]
then
    echo " "
    echo "WARNING! Cannot find Tinker executables in \$tinkerPathDENOPTIM"
    echo "If Tinker is installed, please set valiable \$tinkerPathDENOPTIM "
    echo "in runTests.sh and run it again."
    echo "The value should be set to be the pathname of "
    echo "the bin folder containing all the executables of Tinker."
    echo "Skipping test 't1'."
    echo " "
    echo "WARNING! Some peculiar test cases require this configuration of Tinker:"
    echo "  -> 'maxval' >= 12 in sizes.i for Tinker version <7.x or sizes.f for >7.x"
    echo "  -> 'maxtors' = 22 * n in torsions.f for Tinker versions > 7.x)"
    echo "  -> 'maxbitor' = 65 * n in bitors.f for Tinker versions > 7.x)"
    echo "Test t1 can be run with a default installation of Tinker, ignoring "
    echo "the warnings about need of customized settings."
    echo " "
    exit 0
else
    if [ ! -f "$tinkerPathDENOPTIM/pssrot" ]
    then
        echo " "
        echo "WARNING! Cannot find 'pssrot' in \$tinkerPathDENOPTIM "
	echo "Skipping test 't1'."
        echo " "
        exit 0
    fi
    if [ ! -f "$tinkerPathDENOPTIM/xyzint" ]
    then
        echo " "
        echo "WARNING! Cannot find 'xyzint' in \$tinkerPathDENOPTIM "
        echo "Skipping test 't1'."
        echo " "
        exit 0
    fi
    if [ ! -f "$tinkerPathDENOPTIM/intxyz" ]
    then
        echo " "
        echo "WARNING! Cannot find 'intxyz' in \$tinkerPathDENOPTIM "
        echo "Skipping test 't1'."
        echo " "
        exit 0
    fi
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
    echo "FS-ScaffoldLibFile=$wrkDir/scaff.sdf" >> "$dnpParams"
    echo "FS-FragmentLibFile=$wrkDir/frags.sdf" >> "$dnpParams"
    echo "FS-CappingFragmentLibFile=$wrkDir/cap.sdf" >> "$dnpParams"
    echo "FS-CompMatrixFile=$wrkDir/CPMap.par" >> "$dnpParams"
    echo "FS-RotBondsDefFile=$DENOPTIM_HOME/src/main/resources/data/rotatableBonds-1.0" >> "$dnpParams"

    echo "3DB-workDir=$wrkDir" >> "$dnpParams"
    # location of the TINKER tools
    echo "3DB-toolPSSROT=$tinkerPathDENOPTIM/pssrot" >> "$dnpParams"
    echo "3DB-toolXYZINT=$tinkerPathDENOPTIM/xyzint" >> "$dnpParams"
    echo "3DB-toolINTXYZ=$tinkerPathDENOPTIM/intxyz" >> "$dnpParams"
    # param file used by Tinker
    echo "3DB-ForceFieldFile=$DENOPTIM_HOME/src/main/resources/data/uff_vdw.prm" >> "$dnpParams"
    # key file to be used by tinker with PSSROT
    # this file is copied and edited for every molecule
    echo "3DB-KEYFILE=$DENOPTIM_HOME/src/main/resources/data/build_uff.key" >> "$dnpParams"
    # parameters used by PSSROT
    # this file is copied and edited for every molecule
    echo "3DB-PSSROTPARAMS=$DENOPTIM_HOME/src/main/resources/data/submit_pssrot" >> "$dnpParams"

    #run builder
    "$javaDENOPTIM" -jar "$denoptimJar" -r B3D "$dnpParams" &> "$logFile"

    #Check output
    if [ -f "$logFile" ]; then
        if ! grep -q 'MolecularModelBuilder terminated normally' "$logFile"; then
            if grep -q 'Tinker failed on task' "$logFile"; then
                # In this case, Denoptim detected a problem in the Tinker job
                # and cannot proceed. This kind of error is most ofted due to
                # the limited defaults of Tinker not allowing for the 
                # complexity that is included in some of the tests for DENOPTIM.
                # We, therefore, print a warning and move on: this is most 
                # likely not a problem due to DENOPTIM.
                echo " "
                echo "WARNING: Tinker job failed for molecule $fname."
                n=$(grep -n 'Tinker failed on task' "$logFile" | awk -F':' '{print $1}')
                nn=$(wc -l "$logFile" | awk '{print $1}')
                tail -n $((nn - n)) "$logFile"
                continue
            else
                echo " "
                echo "ERROR! Something went wrong while building molecule $fname:"
                echo "Error was not interpreted. See $logFile!"
                echo " "
                exit 1
            fi
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
done
echo " "
echo "Test 't1' PASSED"
exit 0

