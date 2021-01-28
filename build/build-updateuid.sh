#!/bin/bash

# Building UpdateUID

find ../src/UpdateUID/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/commons-cli-1.3.1.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create UpdateUID.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: updateuid.UpdateUID" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/commons-cli-1.3.1.jar" >> manifest.mf
echo >> manifest.mf

jar cvfm UpdateUID.jar manifest.mf updateuid 

if [ "$?" = "0" ]; then
    rm -rf manifest.mf updateuid
else
	echo "Failed to create UpdateUID.jar."
    exit -1
fi

echo "--------------------- Done building UpdateUID.jar ---------------------"
