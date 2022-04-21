#!/bin/bash
#
# Script lauching an evolutionary experiment aiming to select ligand sets [X,X,L]
# that weacken the C-O bond in Pt(CO)(L)(X)_2
#
#
# Usage:
#
# ./scriptName.sh [-r]
#
# Options:
# -r           remove previously existing workspace.
#

overwrite=1
args=($@)
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-r") overwrite=0 ;;
    esac
done

# Setting the environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"
export denoptimJar=$(find "$DENOPTIM_HOME/target" -name "denoptim*-jar-with-dependencies.jar")

if [ ! -f "$denoptimJar" ]
then
    echo "Cannot find DENOPTIM's jar. Make sure you have built the project by running 'mvn package' and ensuring its successful completion."
    exit -1
fi
echo "Using DENOPTIM jar: $denoptimJar"

# Copy to tmp space
wDir="/tmp/denoptim_PtCO"
if [ -d "$wDir" ]
then
    if [ $overwrite -eq 0 ]
    then
        rm -fr "$wDir"
    else
        echo " "
        echo "ERROR! Old $wDir exists already! Remove it to run a new test."
        echo " "
        exit 
    fi
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

# Adapt to Windows OS
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    sed -i 's/\//\\/g' input_parameters
fi


# Run DENOPTIM	
echo " "
echo "Starting DenoptimGA (ctrl+c to kill)"
echo " "
"$javaDENOPTIM" -jar "$denoptimJar" -r GA input_parameters

# Goodbye
echo "All done. See results under $wDir"
echo "Thanks for using DENOPTIM!"

