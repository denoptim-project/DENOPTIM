#!/bin/bash

###############################################################
#                                                             #
#                        Run all tests                        #
#                                                             #
###############################################################

#Working directory for running tests
wDir="/Users/mfo051/scratch"
#NOTE: path $wDir/$testName/ must be short. Tinker needs paths
#      shorter than 80 characters.

#DENOPTIM home
export DENOPTIMHomeDir="/Users/mfo051/tools/DENOPTIM"
export DENOPTIM_HOME="/Users/mfo051/tools/DENOPTIM"

#Setting for the execution of DENOPTIM tools
export javaDENOPTIM="/usr/bin/java"
export DENOPTIMJarFiles="$DENOPTIMHomeDir/build"

#OPENBABEL
export obabelDENOPTIM="/usr/local/bin"

#ChemAxon tools
export chemaxonDENOPTIM="/Applications/ChemAxon/MarvinBeans/bin"

#TINKER
export tinkerPathDENOPTIM="/Users/mfo051/tools/tinker_with_extensions/TinkerLFMM_RCP_2/bin"

#R
export RscriptDENOPTIM="/usr/bin/Rscript"

# Version of 'sed' (either GNU or BSD)
export sedSyntax="BSD"

#shell
export SHELL="/bin/bash"

#Hardware specific params
export DENOPTIMslaveCores=4


############################################
#                                          #
# No need to change things below this line #
#                                          #
############################################


############################################ 
# Function for running a Test
############################################ 

function runTest() {
    testName=$1
    echo "Running test '$testName'... (ctrl-z to kill all)"

    #generate the directory
    testDir=$wDir/$testName
    mkdir $testDir
    if [ ! -d $testDir ]
    then
        echo " ERROR! Unabel to create directory $testDir"
        exit
    fi

    #prepare the files
    cd $testDir
    cp -r $DENOPTIMHomeDir/test/$testName/* .

    #Run the test
    ./run$testName.sh

    #get exit value and, in case of error, stop
    status=$?
    if [ $status -ne 0 ]
    then
	echo "ERROR running test $testName! Find relevant files in $testDir"
	echo " "
	exit -1
    fi

    #Back home
    cd $wDir
}

############################################
# Main
############################################

# Make working directory
if [ -d "$wDir" ]
then
    echo " "
    echo "ERROR! $wDir exists already!"
    echo " "
    exit
fi
mkdir "$wDir"
if [ ! -d "$wDir" ]
then
    echo " "
    echo "ERROR! Unable to create working directory."
    echo " "
    exit
fi

cd $wDir


# Run tests

#
# t1: use DenoptimCG to build 1000 molecules. Dummy atoms and various elements are included in the molecules.
#
#TODO runTest "t1"

#
# t2: run fitness provider for Ru 14-el complexes (3D builder + fitness evaluation)
#
#TODO:update keywords
#echo " "
#echo " Skipping t2: scripts need to be updated!"
#echo " "
#runTest "t2"

#
# t3: build diamonds - from sp3 C and H
#
#TODO runTest "t3"

#
# t4: parallel combinatorial builder from frag. space definition (i.e., build starting from scaffold)
#
#TODO runTest "t4"

#
# t5: parallel combinatorial builder from given root (default format: STRING)
#
runTest "t5"

#
# t6: parallel combinatorial builder with enforced symmetry
#
runTest "t6"

#
# t7: parallel combinatorial builder from STRING-like user-provided root graph and enforced symmetry
#
runTest "t7"

#
# t8: parallel combinatorial building til exhaustion of space
#
runTest "t8"

#
# p9: partial exploration of fragment space that generates checkpoint and serialized graphs for other tests.
#
runTest "p9"

#
# t9: restart FragSpaceExplorer from checkpoint file; non-first, non-last iteration
#
runTest "t9"

#
# t10: restart FragSpaceExplorer from checkpoint file; end of level
#
echo "SKIPPING t10: work in progress..."
#runTest "t10"

#
# t11: conversion of serialized DENOPTIMgraph to SDF
#
echo "SKIPPING t11: work in progress..."
#runTest "t11"

#
# t12: conversion of serialized checkpoint file to human readable string
#
runTest "t12"

#
# t13: FragSpaceExplorer with symmetry constraints
#
runTest "t13"

#
# t14: FragSpaceExplorer with symmetry constraints and enforced symmetry
#
runTest "t14"

#
# t15: Genetic algorithm running with enforced symmetry.
#
runTest "t15"

#
# t16: Genetic algorithm running with symmetry constraints.
#
runTest "t16"

#
# t17: DenoptimGA recovering symmetry from SDF field with graph representation
#
runTest "t17"

#
# t18: test crossover operator
#
runTest "t18"

#
# t19: test crossover operator on molecules with symmetry and rings
#
runTest "t19"

#
# t20: GraphEditor 
#
runTest "t20"


echo "All done!"
exit 0
