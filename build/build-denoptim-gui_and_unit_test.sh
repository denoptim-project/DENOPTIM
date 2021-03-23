#!/bin/bash

echo "Building DENOPTIM_GUI.jar from source..."
bash build-denoptim-gui.sh

echo "Starting JUnit tests..."
## Run all JUnit tests (including CDK's)
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar --scan-classpath --details=tree

# To run only DENOPTIM's tests
java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar -p denoptim


# To run a specific test
#java -jar ../test/junit/junit-platform-console-standalone-1.5.2.jar -cp DENOPTIM-GUI.jar -c denoptim.io.DenoptimIOTest
