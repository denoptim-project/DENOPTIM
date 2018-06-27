#!/bin/bash

# Building GetMOPACCoordinates
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create GetMOPACCoordinates.jar. Cannot locate DENOPTIM.jar in lib"
    exit -1
fi

find ../src/misc/GetMOPACCoordinates/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/vecmath.jar:lib/DENOPTIM.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GetMOPACCoordinates.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: getmopaccoordinates.GetMOPACCoordinates" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/vecmath.jar lib/DENOPTIM.jar " >> manifest.mf
echo >> manifest.mf

jar cvfm GetMOPACCoordinates.jar manifest.mf getmopaccoordinates 


if [ "$?" = "0" ]; then
    rm -rf manifest.mf getmopaccoordinates
else
	echo "Failed to create GetMOPACCoordinates.jar."
    exit -1
fi

echo "--------------------- Done building GetMOPACCoordinates.jar ---------------------"
