#!/bin/bash

# Building DenoptimGA
if [ ! -f lib/DENOPTIM.jar ]; then
	echo "Failed to create DenoptimGA.jar. Cannot locate DENOPTIM.jar in ../lib"
    exit -1
fi

find ../src/DenoptimGA/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/commons-io-2.4.jar:lib/commons-lang3-3.1.jar:lib/vecmath.jar:lib/commons-math3-3.6.1.jar:lib/apiguardian-api-1.1.0.jar:lib/junit-jupiter-5.5.2.jar:lib/junit-jupiter-api-5.5.2.jar:lib/junit-jupiter-engine-5.5.2.jar:lib/junit-jupiter-migrationsupport-5.5.2.jar:lib/junit-jupiter-params-5.5.2.jar:lib/DENOPTIM.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DenoptimGA.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: denoptimga.DenoptimGA" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/commons-io-2.4.jar lib/commons-lang3-3.1.jar lib/vecmath.jar lib/commons-math3-3.6.1.jar lib/apiguardian-api-1.1.0.jar lib/junit-jupiter-5.5.2.jar lib/junit-jupiter-api-5.5.2.jar lib/junit-jupiter-engine-5.5.2.jar lib/junit-jupiter-migrationsupport-5.5.2.jar lib/junit-jupiter-params-5.5.2.jar lib/DENOPTIM.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm DenoptimGA.jar manifest.mf denoptimga 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf denoptimga
else
	echo "Failed to create DenoptimGA.jar."
    exit -1
fi

echo "--------------------- Done building DenoptimGA.jar ---------------------"