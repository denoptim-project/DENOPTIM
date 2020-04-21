#!/bin/bash

# build script for DENOPTIM
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi



find ../src/DENOPTIM/src/ -name *.java > javafiles.txt

#jarsColumnSeparated=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/:/g' | sed 's/\:$//g')
jarsColumnSeparated="lib/apiguardian-api-1.1.0.jar:lib/cdk-1.4.19.jar:lib/commons-io-2.4.jar:lib/commons-lang3-3.1.jar:lib/commons-math3-3.6.1.jar:lib/junit-jupiter-5.5.2.jar:lib/junit-jupiter-api-5.5.2.jar:lib/junit-jupiter-engine-5.5.2.jar:lib/junit-jupiter-migrationsupport-5.5.2.jar:lib/junit-jupiter-params-5.5.2.jar:lib/vecmath.jar"
javac -cp "$jarsColumnSeparated" @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create DENOPTIM.jar."
    exit -1
fi

rm javafiles.txt

#jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')

jars="lib/apiguardian-api-1.1.0.jar lib/cdk-1.4.19.jar lib/commons-io-2.4.jar lib/commons-lang3-3.1.jar lib/commons-math3-3.6.1.jar lib/junit-jupiter-5.5.2.jar lib/junit-jupiter-api-5.5.2.jar lib/junit-jupiter-engine-5.5.2.jar lib/junit-jupiter-migrationsupport-5.5.2.jar lib/junit-jupiter-params-5.5.2.jar lib/vecmath.jar"
echo "Manifest-Version: 1.0" > manifest.mf
echo "Class-Path: $jars" >> manifest.mf
echo >> manifest.mf

jar cvfm DENOPTIM.jar manifest.mf denoptim

if [ "$?" = "0" ]; then
    rm manifest.mf
    rm -rf denoptim 
    mv DENOPTIM.jar lib/
    cp lib/DENOPTIM.jar ../lib
else
    echo "Failed to create DENOPTIM.jar."
    exit -1
fi

echo "--------------------- Done building DENOPTIM.jar ---------------------"

## Run all JUnit tests (including CDK's)
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp lib/DENOPTIM.jar:../lib/cdk-1.4.19.jar --scan-classpath --details=tree

# To run a specific test
java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp lib/DENOPTIM.jar:../lib/cdk-1.4.19.jar -p denoptim


# To run a specific test
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp lib/DENOPTIM.jar:../lib/cdk-1.4.19.jar -c denoptim.fragspace.FragmentSpaceTest

