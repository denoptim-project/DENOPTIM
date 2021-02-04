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

if [ ! -d "$tinkerPathDENOPTIM" ]
then
    echo " "
    echo "WARNING! Cannot find Tinker executables in \$tinkerPathDENOPTIM"
    echo "If Tinker is installed, please set valiable \$tinkerPathDENOPTIM "
    echo "in runAllTests.sh and run it again."
    echo "The value should be set to be the pathname of "
    echo "the bin folder containing all the executables of Tinker."
    echo "The installation of Tinker must be configured with "
    echo "'maxval' >= 12 (see sizes.i in Tinker's source code)."
    echo "Skipping test 't1'."
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

wrkDir=`pwd`

files=$(ls input/*.sdf )

for f in $files
do
    # Set file names
    inpSDF="$wrkDir/$f"
    fname=`basename "$inpSDF" .sdf`
    tmpOptSDFFile="$wrkDir/$fname""_3Dbuilt.sdf"
    tinkerparFile="$wrkDir/$fname""_tinker.par"
    logFile="$wrkDir/$fname.log"

    #report
    echo -ne ' progress: '$fname' / 5\r'

    #Prepare parameters
    echo "CG-inpSDF=$inpSDF" > "$tinkerparFile"
    echo "CG-outSDF=$tmpOptSDFFile" >> "$tinkerparFile"
    echo "FS-ScaffoldLibFile=$wrkDir/scaff.sdf" >> "$tinkerparFile"
    echo "FS-FragmentLibFile=$wrkDir/frags.sdf" >> "$tinkerparFile"
    echo "FS-CappingFragmentLibFile=$wrkDir/cap.sdf" >> "$tinkerparFile"
    echo "FS-CompMatrixFile=$wrkDir/CPMap.par" >> "$tinkerparFile"
    echo "FS-RotBondsDefFile=$DENOPTIM_HOME/src/DenoptimCG/data/rotatableBonds-1.0" >> "$tinkerparFile"

    echo "CG-workDir=$wrkDir" >> "$tinkerparFile"
    # location of the TINKER tools
    echo "CG-toolPSSROT=$tinkerPathDENOPTIM/pssrot" >> "$tinkerparFile"
    echo "CG-toolXYZINT=$tinkerPathDENOPTIM/xyzint" >> "$tinkerparFile"
    echo "CG-toolINTXYZ=$tinkerPathDENOPTIM/intxyz" >> "$tinkerparFile"
    # param file used by Tinker
    echo "CG-ForceFieldFile=$DENOPTIM_HOME/src/DenoptimCG/data/uff_vdw.prm" >> "$tinkerparFile"
    # key file to be used by tinker with PSSROT
    # this file is copied and edited for every molecule
    echo "CG-KEYFILE=$DENOPTIM_HOME/src/DenoptimCG/data/build_uff.key" >> "$tinkerparFile"
    # parameters used by PSSROT
    # this file is copied and edited for every molecule
    echo "CG-PSSROTPARAMS=$DENOPTIM_HOME/src/DenoptimCG/data/submit_pssrot" >> "$tinkerparFile"


    #run builder
    "$javaDENOPTIM" -jar "$DENOPTIMJarFiles/DenoptimCG.jar" "$tinkerparFile" &> "$logFile"

    #Check output
    if [ ! -f "$tmpOptSDFFile" ]; then
	echo " "
        echo "ERROR! Something went wrong while building molecule $fname:"
	echo "$tmpOptSDFFile not found!"
	echo "Check log file $logFile"
	echo " "
        exit 1
    fi
done
echo " "

# Check outcome (only size of SDF files: #atoms+#bonds+props+headers)
n=$(wc -l *3Dbuilt.sdf | tail -n 1 | awk '{print $1}')
if [[ "$n" != 579 ]]
then
    echo " "
    echo "Test 't1' NOT PASSED (symptom: )"
    exit 1
else
    echo "Test 't1' PASSED"
fi

exit 0

