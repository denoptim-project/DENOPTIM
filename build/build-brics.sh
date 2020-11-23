#!/bin/bash

# Building SetupBRICS
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi


if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/


find ../src/misc/RDKITFragmenter/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/DENOPTIM-GUI.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create SetupBRICS.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: setupbrics.SetupBRICS" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/DENOPTIM-GUI.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm SetupBRICS.jar manifest.mf setupbrics 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf setupbrics 
else
	echo "Failed to create SetupBRICS.jar."
    exit -1
fi

echo "--------------------- Done building SetupBRICS.jar ---------------------"
