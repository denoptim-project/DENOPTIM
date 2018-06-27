#!/bin/bash

wrkDir=`pwd`

files=$(ls input/*.sdf )
numInp=$(ls -1 input/*.sdf | wc -l)
id=0

cd $DENOPTIMHomeDir/data/ru_catalysis/models
$RscriptDENOPTIM getmodel.R
mv plsmodel.RData $wrkDir
cp predictCE.R $wrkDir
cd $wrkDir
if [ "$sedSyntax" == "GNU" ]
then
    sed -i "s|OTF_WDIR|$wrkDir|g" predictCE.R
elif [ "$sedSyntax" == "BSD" ]
then
    sed -i '' "s|OTF_WDIR|$wrkDir|g" predictCE.R
fi
for f in $files
do
    # Set file names
    id=$(( $id + 1 ))
    inpSDF="$wrkDir/$f"
    fname=`basename $inpSDF .sdf | awk -F'_' '{print $1"_"$2}'`
    outSDF="$wrkDir/$fname""_FIT.sdf"
    logFile="$wrkDir/$fname.log"

    #report
    echo -ne ' progress: '$fname' / '$numInp'\r'

    #Run fitness provider
    $DENOPTIMHomeDir/data/ru_catalysis/Ru_14-el_fitness_provider.sh  $inpSDF $outSDF $wrkDir $id > $logFile 2>&1

    #Check output
    if [ ! -f $outSDF ]; then
	echo " "
        echo "ERROR! Somenthing went wrong while building molecule $fname:"
	echo "$outSDF not found!"
	echo "Check log file $logFile"
	echo " "
        exit 1
    fi
done

echo "                                                      "

exit 0

