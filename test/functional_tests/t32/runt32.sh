#!/bin/bash

# Setup working directory and file names
wrkDir=`pwd`
logFile="t32.log"
paramFile="t32.params"

# Skip test on Windows
if [[ "$(uname)" == CYGWIN* ]] || [[ "$(uname)" == MINGW* ]] || [[ "$(uname)" == MSYS* ]]
then
    echo "Test SKIPPED on Windows"
    exit 0
fi

# Move initial data and cleanup
mv data/* "$wrkDir"
rm -rf data

# Run the test
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$denoptimJar" -r GA "$paramFile"
exec 1>&6 6>&-

# Assuming the directory structure and file location
sdfFile=$(find "$wrkDir" -type f -name "M00000001_out.sdf" -print -quit)

# Use awk to count the total number of vertices and ensure it equals 5
count=$(awk '
BEGIN { depth=0; capture=0; count=0; }
/"symVertices": \[/ { capture=1; depth=1; next; }
capture && /\[/ { depth++; }
capture && /\]/ {
    depth--;
    if (depth == 0) {
        capture=0;
    }
}
capture {
    while (match($0, /[0-9]+/)) {
        count++;
        $0 = substr($0, RSTART + RLENGTH);
    }
}
END { print count }
' "$sdfFile")

# Check if the count is correct
if [[ "$count" -ne 5 ]]; then
    echo "Test NOT PASSED: The expected number of symmetric vertices (5) is different from the one found ($count)."
    exit -1
else
    echo "Test t32 PASSED: Correct number of symmetric vertices found ($count)."
fi

exit 0