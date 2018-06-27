#!/bin/bash

f=$1
id=$2
wrkDir=`pwd`

# Set file names
inpSDF="$wrkDir/$f"
fname=`basename $inpSDF .sdf | awk -F'_' '{print $1"_"$2}'`
outSDF="$wrkDir/$fname""_FIT.sdf"
logFile="$wrkDir/$fname.log"

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

exit 0
