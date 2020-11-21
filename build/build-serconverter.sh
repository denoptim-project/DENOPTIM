#!/bin/bash

# Building SerConverter
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar in ../lib"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/SerConverter/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.4.19.jar:lib/DENOPTIM-GUI.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create SerConverter.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: serconverter.SerConverter" >> manifest.mf
echo "Class-Path: lib/cdk-1.4.19.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm SerConverter.jar manifest.mf serconverter 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf serconverter
else
	echo "Failed to create SerConverter.jar."
    exit -1
fi

echo "--------------------- Done building SerConverter.jar ---------------------"
