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
    echo "Running test '$testName'... (ctrl-z to stop all)"

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
    cp -r $DENOPTIMHomeDir/test/functional_tests/$testName/* .

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
# t1: use DenoptimCG to build funny molecules.
#
runTest "t1"

#
# t2: parallel combinatorial builder from given root (default format: STRING)
#
runTest "t2"

#
# t3: parallel combinatorial builder with enforced symmetry
#
runTest "t3"

#
# t4: parallel combinatorial builder from STRING-like user-provided root graph and enforced symmetry
#
runTest "t4"

#
# t5: parallel combinatorial building til exhaustion of space
#
runTest "t5"

#
# p6: partial exploration of fragment space that generates checkpoint and serialized graphs for other tests.
#
runTest "p6"

#
# t6: restart FragSpaceExplorer from checkpoint file; non-first, non-last iteration
#
runTest "t6"

#
# t7: conversion of serialized DENOPTIMgraph to SDF
#
runTest "t7"

#
# t8: conversion of serialized checkpoint file to human readable string
#
runTest "t8"

#
# t9: FragSpaceExplorer with symmetry constraints
#
runTest "t9"

#
# t10: FragSpaceExplorer with symmetry constraints and enforced symmetry
#
runTest "t10"

#
# t11: Genetic algorithm running with enforced symmetry.
#
runTest "t11"

#
# t12: Genetic algorithm running with symmetry constraints.
#
runTest "t12"

#
# t13: DenoptimGA recovering symmetry from SDF field with graph representation
#
runTest "t13"

#
# t14: test crossover operator
#
runTest "t14"

#
# t15: test crossover operator on molecules with symmetry and rings
#
runTest "t15"

#
# t16: GraphEditor 
#
runTest "t16"

#
# t17: Evolution based on random seletion of known graphs
#
runTest "t17"


echo "All done!"
exit 0
