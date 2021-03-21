#!/bin/bash

# Building PrepareFitnessOutput

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/PrepareFitnessOutput/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/commons-cli-1.3.1.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .


if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create PrepareFitnessOutput.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: preparefitnessoutput.PrepareFitnessOutput" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/commons-cli-1.3.1.jar lib/DENOPTIM-GUI.jar lib/gson-2.8.6.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm PrepareFitnessOutput.jar manifest.mf preparefitnessoutput 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf preparefitnessoutput
else
	echo "Failed to create PrepareFitnessOutput.jar."
    exit -1
fi

echo "--------------------- Done building PrepareFitnessOutput.jar ---------------------"
