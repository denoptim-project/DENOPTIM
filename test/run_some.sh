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

runTest "t20"

echo "All done!"
exit 0
