#!/bin/bash

wrkDir=`pwd`
logFile="t31.log"
paramFile="t31.params"

mv data/* "$wrkDir"
rm -rf data

# Start denoptim's server
"$javaDENOPTIM" -jar "$denoptimJar" -r PY4J &
SERVER_PID=$!

# Wait for the server to be replying
for i in 1, 2, 3, 4, 5
do
  sleep 0.5
  python client-1.py > tmp 2>&1 
  ES=$?
  if [ "$ES" -eq 0 ]
  then
    break;
  fi
done

kill "$SERVER_PID"

if [ "$ES" -ne 0 ]
then
    echo "Test 't31' NOT PASSED (symptom: non-zero exit status from python)"
    exit -1
fi

echo "Test 't31' PASSED"
exit 0
