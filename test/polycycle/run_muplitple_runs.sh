#!/bin/bash
#
# Script lauching a set of parallel and independent EA experiments
#
#
# Usage:
#
# ./run_muplitple_runs.sh [-r]
#
# Options:
# -r           remove previously existing workspace.
# -n NN        number of parallel runs to start          

#Number of runs (max is 20. To go beyond store more ransom seeds)
nn=2

randomSeeds=(2624 19377 9563 26551 13997 19563 13082 17637 11771 26024 26992 23172 11765 23456 5109 16245 24077 6596 39487 24)

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

submitDir="$(pwd)"
wDir="/tmp/denoptim_polycycle_multirun"

# Process arguments
overwrite=1
args=($@)
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-r") overwrite=0 ;;
        "-n") if [ $((i+1)) -lt $# ];
              then
                  nn=${args[$((i+1))]}
              fi;;
    esac
done

if [ "$nn" -gt 19 ]
then
    echo "ERROR! Not enough random seeds. Please, edit the script to add more random seeds."
    exit -1
fi

sedSyntax=GNU
sed --version >/dev/null 2>&1
if [ $? -ne 0 ]; then
    if strings $(which sed) | grep -q -i BSD ; then
        sedSyntax="BSD"
    else
        echo " "
        echo "WARNING! Unable to understand the verion of 'sed'! Many things could go wrong because I cannot find out how to do in-place string replacements. I will go on, but should you see misfunctioning behavior, this is the first thing to look at and fix."
        echo " "
        sedSyntax=Other
        exit -1
    fi
fi
export sedSyntax="$sedSyntax"

# Copy to tmp space
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


for i in $(seq 1 $nn)
do
    rs=${randomSeeds[$i]}
    echo "Starting run $i (seed $rs)"
    runDir="$wDir/run_$i"
    mkdir "$runDir"
    cd "$run_Dir"

    echo "Copying file to $runDir"
    cp -r "$submitDir/data" "$runDir"
    cp "$submitDir/input_parameters" "$runDir/GA.params"
    if [ "$sedSyntax" == "GNU" ]
    then
        sed -i "s|RANDOMSEEDTOSET|$rs|g" "$runDir/GA.params"
        sed -i "s|RUNDIRTOSET|$runDir|g" "$runDir/GA.params"
    elif [ "$sedSyntax" == "BSD" ]
    then
        sed -i '' "s|RANDOMSEEDTOSET|$rs|g" "$runDir/GA.params"
        sed -i '' "s|RUNDIRTOSET|$runDir|g" "$runDir/GA.params"
    fi

    cd "$runDir"

    log="$runDir/run_$i.log"
    echo "Running DenoptimGA... (Use interface to stop it. PWD:$(pwd). Log in $log)"
    nohup "$javaDENOPTIM" -jar "$denoptimJar" -r GA -f GA.params > "$log" 2>&1&

    files=$(find "$runDir" -type f -wholename "*/interface/*")
    stopme=1
    for f in ${files[@]}
    do
        if grep -q STOP_GA "$f"
        then
            echo "STOPPING upon user request from $f"
            stopme=0
            break
        fi 
    done
    if [ 0 -eq "$stopme" ]
    then
        break
    fi
done

# Goodbye
echo "All sumbitted. Results will become available under $wDir"
exit 0
