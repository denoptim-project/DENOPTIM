#!/bin/bash
#
# Runs the conversion tool and compares the actual results against the 
# expected ones.
#
# Usage:
# ======
#
#   ./run.sh
#
#
##############################################################################

testNames=('graph_w_vertex_w_sym_APs.json' 'graphs_w_vertex_w_sym_APs.sdf' 'graphs_w_sym_APs_and_sym_vertexes.sdf' 'vertex_w_template_w_sym_APs.sdf' 'graph_w_template_w_sym_APs.sdf' 'editing_task' 'vertex_w_sym_APs.sdf')

for testName in ${testNames[@]}
do
  pathname="${testName%.*}"
  extension=""
  if [[ "$testName" == *.* ]]; then
    extension=".${testName##*.}"
  fi
  
  # Run the conversion of this test case
  python ../convert_symmetric_sets_in_JSON.py -i "${testName}" -o "${pathname}_ACTUAL${extension}"
  if [ $? -ne 0 ]; then
    echo "Bad termination for case $testName."
    exit 1
  fi

  # Compare resutls with  expectations
  diff "${pathname}_ACTUAL${extension}" "${pathname}_EXPECTED${extension}"
  if [ $? -ne 0 ]; then
    echo "Actual results differ from expectations for case $testName."
    exit 1
  fi

  # Cleanup
  rm -f  "${pathname}_ACTUAL${extension}"
done

echo "Test passed!"
exit 0
