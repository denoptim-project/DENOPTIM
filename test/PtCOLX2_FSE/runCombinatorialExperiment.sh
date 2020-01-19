#!/bin/bash
#
# Script lauching an exploration of the fragment space experiment that 
# aims to select ligand sets [X,X,L] that weaken the C-O bond in Pt(CO)(L)(X)_2
#

# Setting the environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"
export DENOPTIMJarFiles="$DENOPTIM_HOME/build"
if [ ! -f "$DENOPTIMJarFiles/FragSpaceExplorer.jar" ]
then
    echo "Cannot find  $DENOPTIMJarFiles/FragSpaceExplorer.jar"
    echo "Trying under dist folder"
    if [ ! -f "$DENOPTIMJarFiles/dist/FragSpaceExplorer.jar" ]
    then
       echo "ERROR! Cannot find  $DENOPTIMJarFiles/dist/FragSpaceExplorer.jar"
       exit -1
    fi
    export DENOPTIMJarFiles="$DENOPTIM_HOME/build/dist"
fi
echo "Using DENOPTIM from $DENOPTIMJarFiles"

# Copy to tmp space
wDir="/tmp/denoptim_PtCO_FSE"
if [ -d "$wDir" ]
then
    echo " "
    echo "ERROR! Old $wDir exists already! Remove it to run a new test."
    echo " "
    exit
fi
mkdir "$wDir"
if [ ! -d "$wDir" ]
then
    echo " "
    echo "ERROR! Unable to create working directory $wDir"
    echo " "
    exit
fi
echo "Copying file to $wDir"
cp -r * "$wDir"
cd "$wDir"

# Run DENOPTIM	
echo " "
echo "Starting FragSpaceExplorer (ctrl+c to kill)"
echo " "
java -jar "$DENOPTIMJarFiles/FragSpaceExplorer.jar" input_parameters

# Goodbye
echo "All done. See results under $wDir"
echo "Thanks for using DENOPTIM!"

