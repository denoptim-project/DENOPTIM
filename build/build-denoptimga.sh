#!/bin/bash

# Building DenoptimGA
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create DenoptimGA.jar. Cannot locate DENOPTIM.jar in ../lib"
    exit -1
fi

find ../src/DenoptimGA/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/commons-io-2.4.jar:lib/commons-lang3-3.1.jar:lib/vecmath.jar:lib/commons-math3-3.6.1.jar:lib/DENOPTIM.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DenoptimGA.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimga.DenoptimGA" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/commons-io-2.4.jar lib/commons-lang3-3.1.jar lib/vecmath.jar lib/commons-math3-3.6.1.jar lib/DENOPTIM.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimGA.jar manifest.mf denoptimga 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf denoptimga
else
	echo "Failed to create DenoptimGA.jar."
    exit -1
fi

echo "--------------------- Done building DenoptimGA.jar ---------------------"
