#!/bin/bash

wrkDir=`pwd`

files=$(ls input/*.sdf )
 
for f in $files
do
    # Set file names
    inpSDF="$wrkDir/$f"
    fname=`basename $inpSDF .sdf`
    tmpOptSDFFile="$wrkDir/$fname""_3Dbuilt.sdf"
    tinkerparFile="$wrkDir/$fname""_tinker.par"
    logFile="$wrkDir/$fname.log"

    #report
    echo -ne ' progress: '$fname' / 1000\r'

    #Prepare parameters
    echo "CG-inpSDF=$inpSDF" > $tinkerparFile
    echo "CG-outSDF=$tmpOptSDFFile" >> $tinkerparFile
    echo "FS-ScaffoldLibFile=$wrkDir/scaff.sdf" >> $tinkerparFile
    echo "FS-FragmentLibFile=$wrkDir/frags.sdf" >> $tinkerparFile
    echo "FS-CappingFragmentLibFile=$wrkDir/cap.sdf" >> $tinkerparFile
    echo "FS-CompMatrixFile=$wrkDir/CPMap.par" >> $tinkerparFile
    echo "FS-RotBondsDefFile=$DENOPTIMHomeDir/src/DenoptimCG/data/rotatableBonds-1.0" >> $tinkerparFile

    echo "CG-wrkDir=$wrkDir" >> $tinkerparFile
    # location of the TINKER tools
    echo "CG-toolPSSROT=$tinkerPathDENOPTIM/pssrot" >> $tinkerparFile
    echo "CG-toolXYZINT=$tinkerPathDENOPTIM/xyzint" >> $tinkerparFile
    echo "CG-toolINTXYZ=$tinkerPathDENOPTIM/intxyz" >> $tinkerparFile
    # param file used by Tinker
    echo "CG-ForceFieldFile=$DENOPTIMHomeDir/src/DenoptimCG/data/uff_vdw.prm" >> $tinkerparFile
    # key file to be used by tinker with PSSROT
    # this file is copied and edited for every molecule
    echo "CG-KEYFILE=$DENOPTIMHomeDir/src/DenoptimCG/data/build_uff.key" >> $tinkerparFile
    # parameters used by PSSROT
    # this file is copied and edited for every molecule
    echo "CG-PSSROTPARAMS=$DENOPTIMHomeDir/src/DenoptimCG/data/submit_pssrot" >> $tinkerparFile


    #run builder
    $javaDENOPTIM -jar $DENOPTIMJarFiles/DenoptimCG.jar $tinkerparFile &> $logFile

    #Check output
    if [ ! -f $tmpOptSDFFile ]; then
	echo " "
        echo "ERROR! Something went wrong while building molecule $fname:"
	echo "$tmpOptSDFFile not found!"
	echo "Check log file $logFile"
	echo " "
        exit 1
    fi
done

exit 0

