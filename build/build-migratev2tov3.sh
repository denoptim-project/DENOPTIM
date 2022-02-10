#!/bin/bash

# Building MigrateV2ToV3

if [ ! -f DENOPTIM-GUI.jar ]; then
    echo "Failed to locate DENOPTIM-GUI.jar"
    exit -1
fi
cp DENOPTIM-GUI.jar lib/

find ../src/misc/MigrateV2ToV3/src/ -name *.java > javafiles.txt
javac -cp lib/cdk-2.3.jar:lib/jgrapht-core-1.4.0.jar:lib/DENOPTIM-GUI.jar:lib/gson-2.8.6.jar @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create MigrateV2ToV3.jar."
    exit -1
fi

rm javafiles.txt


echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: migratev2tov3.MigrateV2ToV3" >> manifest.mf
echo "Class-Path: lib/cdk-2.3.jar lib/jgrapht-core-1.4.0.jar lib/gson-2.8.6.jar lib/vecmath.jar lib/DENOPTIM-GUI.jar " >> manifest.mf
echo >> manifest.mf

jar cvfm MigrateV2ToV3.jar manifest.mf migratev2tov3 


if [ "$?" = "0" ]; then
     rm -rf manifest.mf migratev2tov3
else
	echo "Failed to create MigrateV2ToV3.jar."
    exit -1
fi

echo "--------------------- Done building MigrateV2ToV3.jar ---------------------"
