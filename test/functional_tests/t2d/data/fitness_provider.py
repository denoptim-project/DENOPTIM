#!/usr/bin/python
# 
# example of fitness provider running on python
#


import sys

# Parse command line arguments
# Input with graph representation
inpSDF=sys.argv[1]
# Output: we are going to create (with Fitness/Error)
outSDF=sys.argv[2]
# Working directory
wrkDir=sys.argv[3]
# Task ID
taskId=sys.argv[4]
# Location of the with unique identifiers of previously encountered molecules)
UIDFILE=sys.argv[5]

import os
if not os.path.isfile(inpSDF):
  print("Cannot find file '%s'" % filename)
  sys.exit()

# our fitness is a random number :D
import random
infile = open(inpSDF,'r')
outfile = open(outSDF,'w')
for line in infile:
  if "$$$$" in line:
    outfile.write("> <FITNESS>\n")
    outfile.write("%.5f\n" % random.random())
    outfile.write("\n")
  outfile.write(line)

infile.close()
outfile.close()

sys.exit(0)
