#!/bin/bash
################################################################################
#                                                                              #
#                             Run One Single Test                              #
#                                                                              #
################################################################################
#
# Use this script to run all the tests.

# 
# Usage:
#
# ./<script_name>.sh
#
# Settings:
#
# Path to TINKER executables
# Currently required only by t1 that will be skipped if the following is empty.
export tinkerPathDENOPTIM=""

# Directory created for running tests (must be shorter than 40 characters).
wDir="/tmp/dnTestTemplate"

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

#
# t97: under development - mutation involving templates
#
runTest "t97"

#
# t98: under development - crossover involving templates
#
runTest "t98"

#
# t99: under development - Meant for testing of the graph template stuff in GA run
#
runTest "t99"

echo "All done!"
exit 0
