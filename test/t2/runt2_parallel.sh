#!/bin/bash

#numper of parallel processes
numproc=4

#Working directory for running tests
wDir="/scratch/mf/test"
#NOTE: path $wDir/$testName/ must be short. Tinker needs paths
#      shorter than 80 characters.

#DENOPTIM home
export DENOPTIMHomeDir="/scratch/mfo051_DENOPTIM/newdenoptim"

#Setting for the execution of DENOPTIM tools
export javaDENOPTIM="/Home/siv22/mfo051/softwares/JDK_SE7/jdk1.7.0_07/bin/java"
export DENOPTIMJarFiles="$DENOPTIMHomeDir/build"

#OPENBABEL
export obabelDENOPTIM="/scratch/mfo051_BIN/openbabel-2.3.2/bin/obabel"

#MOPAC
export mopacDENOPTIM="/scratch/mfo051_BIN/MOPAC/MOPAC2012.exe"

#TINKER
export tinkerPathDENOPTIM="/scratch/mfo051_DENOPTIM/bin"


export SHELL="/bin/bash"

##########################################

#generate the directory
if [ -d $wDir ]
then
    echo " ERROR! Folder $wDir exists"
    exit
fi
mkdir $wDir
testDir=$wDir/t2
mkdir $testDir
if [ ! -d $testDir ]
then
    echo " ERROR! Unabel to create directory $testDir"
    exit
fi

#prepare the files
cd $testDir
cp $DENOPTIMHomeDir/test/t2/* . -r

##########################################

#
#Prepare scripts for running in parallel
#
files=$(ls input/*.sdf )
tot=$(ls input/*.sdf | wc -l)
molPerProc=$(( $tot / $numproc ))
partial=0
scriptid=1
rm runTask-* -f

for f in $files
do
    partial=$(( $partial + 1 ))
    if [ $partial -gt $molPerProc ]
    then
        partial=1
	if [ $scriptid -ne $numproc ]
	then
            scriptid=$(( $scriptid + 1 ))
	fi
    fi
    scriptName="runTask-$scriptid.sh"
    echo "./runt2_fromFilename.sh $f 0" >> $scriptName
    echo "if [ \$? -ne 0 ]" >> $scriptName
    echo "then" >> $scriptName
    echo "    echo \"Processing of file $f returned non-zero exit value\"" >> $scriptName
    echo "    exit -1" >> $scriptName
    echo "fi" >> $scriptName
    echo " " >> $scriptName
done
chmod +x runTask-*

./runTask-1.sh &
./runTask-2.sh &
./runTask-3.sh &
./runTask-4.sh &

exit 0

