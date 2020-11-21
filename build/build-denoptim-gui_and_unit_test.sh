#!/bin/bash

# Building GUI
if [ -d ../lib ]; then
    if [ -d lib ]; then
        cp -r ../lib/*.jar lib/
    else
        cp -r ../lib .        
    fi 
fi
cp -r ../src/GUI/images images

find ../src/DENOPTIM/src/ ../src/DenoptimGA/src ../src/FragSpaceExplorer/src ../src/GUI/src -name *.java > javafiles.txt

jarsColumnSeparated=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/:/g' | sed 's/\:$//g')

javac -cp "$jarsColumnSeparated" @javafiles.txt -encoding utf-8 -d .

if [ "$?" != "0" ]; then
    rm javafiles.txt
	echo "Failed to create GUI.jar."
    exit -1
fi

rm javafiles.txt

jars=$(ls -1 lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
echo JARS: $jars
jarsAndImages="$jars images"
echo JARS AND IMAGES: $jarsAndImages

echo "Manifest-Version: 1.0" > manifest.mf
#echo "Main-Class: gui.GUI" >> manifest.mf
echo "Class-Path: $(echo $jarsAndImages | fold -w58 | awk '{print " "$0}')" >> manifest.mf
echo >> manifest.mf

jar cvfm DENOPTIM-GUI.jar manifest.mf denoptim denoptimga fragspaceexplorer gui images

if [ "$?" = "0" ]; then
    rm -rf manifest.mf gui denoptim denoptimga fragspaceexplorer images
else
	echo "Failed to create DENOPTIM-GUI.jar."
    exit -1
fi

echo "--------------------- Done building DENOPTIM-GUI.jar ---------------------"

## Run all JUnit tests (including CDK's)
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar --scan-classpath --details=tree

# To run only DENOPTIM's tests
java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar -p denoptim


# To run a specific test
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar -c denoptim.fitness.DescriptorUtilsTest
