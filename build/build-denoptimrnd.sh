#!/bin/bash

# Building DenoptimRND
if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create DenoptimRND.jar. Cannot locate DENOPTIM.jar in ../lib"
    exit -1
fi

find ../src/DenoptimRND/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/commons-io-2.4.jar:lib/commons-lang3-3.1.jar:lib/vecmath.jar:lib/commons-math3-3.6.1.jar:lib/DENOPTIM.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DenoptimRND.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimrnd.DenoptimRND" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/commons-io-2.4.jar lib/commons-lang3-3.1.jar lib/vecmath.jar lib/commons-math3-3.6.1.jar lib/DENOPTIM.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimRND.jar manifest.mf denoptimrnd 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf denoptimrnd
else
	echo "Failed to create DenoptimRND.jar."
    exit -1
fi

echo "--------------------- Done building DenoptimRND.jar ---------------------"
