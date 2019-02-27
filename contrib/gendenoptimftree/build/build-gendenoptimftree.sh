#!/bin/bash

# Building GenDENOPTIMFTree
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



if [[ ! -f lib/cdk-1.5.13.jar || ! -f  lib/commons-cli-1.3.1.jar || ! -f  lib/indigo-inchi.jar || ! -f lib/indigo.jar || ! -f lib/indigo-renderer.jar || ! -f lib/jna.jar ]]; then
	echo "Failed to create GenDENOPTIMFTree.jar. Cannot locate necessary jar files in lib"
    exit -1
fi

find ../src/ -name *.java > javafiles.txt
javac -cp lib/cdk-1.5.13.jar:lib/commons-cli-1.3.1.jar:lib/indigo-inchi.jar:lib/indigo.jar:lib/indigo-renderer.jar:lib/jna.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GenDENOPTIMFTree.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: gendenoptimftree.GenDENOPTIMFTree" >> manifest.mf
echo "Class-Path: lib/cdk-1.5.13.jar lib/commons-cli-1.3.1.jar lib/indigo-inchi.jar lib/indigo.jar lib/indigo-renderer.jar lib/jna.jar " >> manifest.mf
echo >> manifest.mf

jar cvfm GenDENOPTIMFTree.jar manifest.mf gendenoptimftree 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf gendenoptimftree
else
	echo "Failed to create GenDENOPTIMFTree.jar."
    exit -1
fi

if [ ! -d ../dist ]; then
    mkdir ../dist
fi
cp -r lib ../dist
cp -r GenDENOPTIMFTree.jar ../dist
rm -rf lib GenDENOPTIMFTree.jar 


echo "--------------------- Done building GenDENOPTIMFTree.jar ---------------------"
