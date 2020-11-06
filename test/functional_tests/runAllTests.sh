#!/bin/bash
################################################################################
#                                                                              #
#                                Run all tests                                 #
#                                                                              #
################################################################################
#
# Use this script to run all the tests.

#
# Usage:
#
# ./runAllTests.sh

#
# Settings:
#

# Path to TINKER executables
# Currently required only by t1 that will be skipped if the following is empty.
export tinkerPathDENOPTIM=""

# Directory created for running tests (must be shorter than 40 characters).
wDir="/tmp/denoptim_test"

# Environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"
export DENOPTIMJarFiles="$DENOPTIM_HOME/build"
if [ ! -f "$DENOPTIMJarFiles/DenoptimGA.jar" ]
then
    echo "Cannot find  $DENOPTIMJarFiles/DenoptimGA.jar"
    echo "Trying under dist folder"
    if [ ! -f "$DENOPTIMJarFiles/dist/DenoptimGA.jar" ]
    then
       echo "ERROR! Cannot find  $DENOPTIMJarFiles/dist/DenoptimGA.jar"
       exit -1
    fi
    export DENOPTIMJarFiles="$DENOPTIM_HOME/build/dist"
fi
echo "Using DENOPTIM from $DENOPTIMJarFiles"



################################################################################
#                                                                              #
#                    No need to change things below this line                  #
#                                                                              #
################################################################################


############################################
# Function for running a Test
############################################

function runTest() {
    testName="$1"
    echo "Running test '$testName'... (ctrl-z to stop all)"

    #generate the directory
    testDir="$wDir/$testName"
    mkdir "$testDir"
    if [ ! -d "$testDir" ]
    then
        echo " ERROR! Unabel to create directory $testDir"
        exit
    fi

    #prepare the files
    cd "$testDir"
    cp -r "$DENOPTIM_HOME/test/functional_tests/$testName/"* .

    #Run the test
    ./"run$testName.sh"

    #get exit value and, in case of error, stop
    status=$?
    if [ "$status" -ne 0 ]
    then
	echo "ERROR running test $testName! Find relevant files in $testDir"
	echo " "
	exit -1
    fi

    #Back home
    cd "$wDir"
}

############################################
# Main
############################################

# Detect the version of SED
if man sed | grep -q "BSD"
then
    export sedInPlace="-i ''"
else
    if man sed | grep -q "GNU"
    then
        export sedInPlace="-i"
    else
        echo " ERROR! Could not detect 'sed' version."
        echo " Expecting to find BSD or GNU."
        exit 1
    fi
fi

# Number of slave cores (NB: do not change)
export DENOPTIMslaveCores=4

# Make working directory
if [ -d "$wDir" ]
then
    echo " "
    echo "ERROR! Old $wDir exists already! Remove it to run new tests."
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

cd "$wDir"


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
# p7: exploration of fragment space that generates serialized graphs for t7
#
## runTest "p7" # Repaced by unit testing
#echo "t7 replaced by unit test"

#
# t7: conversion of serialized DENOPTIMgraph to SDF
#
## runTest "t7" # Replaced by unit testing

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
# runTest "t16"
echo "Skipping t16 until GraphEdit wildcard problem is solved."

#
# t17: Evolution based on random seletion of known graphs
#
runTest "t17"

#
# t18: comparison of graph lists
#
runTest "t18"

echo "All done!"
exit 0
