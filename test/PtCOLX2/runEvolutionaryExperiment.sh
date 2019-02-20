#!/bin/bash
#
# Script lauching an evolutionary experiment aiming to select ligand sets [X,X,L]
# that weacjen the C-O bond in Pt(CO)(L)(X)_2
#

# Setting the environment
export SHELL="/bin/bash"

# Run DENOPTIM	
echo " "
echo "Starting DenoptimGA (kill it with ctrl+c)"
echo " "
java -jar ../../build/DenoptimGA.jar input_parameters

# Goodbye
echo "All done. Thanks for using DENOPTIM!"
