#!/usr/bin/python
###############################################################################
#
# Fitness provider script
# =======================
#
# This scripts recovers the fitness/mol_error from a list.
#
# @param $1 pathname of input SDF file: it must contain the graph
#        representation of the candidate system of which we are calculating
#        the fitness.
# @param $2 pathname of the output SDF file where to report the fitness
#        (possibly with some additional information attached) or the error/s
#        justifying rejecting of this candidate.
# @param $3 pathname of the working space, i.e., a directory.
# @param $4 numerical ID of the task this script is asked to perform.
# @param $5 pathname to the file collecting the candidates unique identifiers
#        collected up to the moment when this script is asked to evaluate a new
#        candidate
#
###############################################################################

import os
import sys

# Parse command line arguments
inpSDF=sys.argv[1]
outSDF=sys.argv[2]
wrkDir=sys.argv[3]
taskId=sys.argv[4]
UIDFILE=sys.argv[5]

molName=os.path.basename(inpSDF).replace("_inp.sdf","")
log=os.path.join(os.path.dirname(inpSDF), molName + "_FProvider.log")
preliminaryOutput=os.path.join(os.path.dirname(inpSDF), molName + "_preOut.sdf")

sys.stdout = open(log, 'w')
print("Log of fitness provider task for "+molName)
os.chdir(wrkDir)
print("cwd: "+os.getcwd());

# Pathnames to the sources of data
uidToAtomClash=os.path.join(os.path.dirname(__file__),"UIDsToAtomClash")
uidToFitness=os.path.join(os.path.dirname(__file__),"UIDsToFitness")
print("Using map UiD-to-Fitness:     "+uidToFitness);
print("Using map UiD-to-AtomClashes: "+uidToAtomClash);

if not os.path.isfile(inpSDF):
    print("Cannot find file '%s'" % filename)
    sys.stdout.close()
    sys.exit(1)

def copySDFandAddProperty(inputSDF, outputSDF, propName, propValue):
    infile = open(inputSDF,'r')
    outfile = open(outputSDF,'w')
    for line in infile:
        if "$$$$" in line:
            outfile.write("> <"+propName+">"+os.linesep)
            outfile.write(propValue+os.linesep)
            outfile.write(os.linesep)
        outfile.write(line)
    infile.close()
    outfile.close()

def exitWithError(msg, status):
    copySDFandAddProperty(inpSDF,preliminaryOutput,"MOL_ERROR",msg)
    os.rename(preliminaryOutput,outSDF)
    print("Exiting with error: "+msg)
    sys.stdout.close()
    sys.exit(status)

# Identify the candidate
import re
foundUid=False
with open(inpSDF, "r") as inputFile:
    for line in inputFile:
        if re.search("<InChi>", line):
            uid=next(inputFile).strip()
            foundUid=True
            break
if (not foundUid):
    exitWithError("#UID: not found",0)
print("Found candidate's UID: "+uid)

# Some candidate led to atom chalshes during molecularm odeling, so do not have a fitness
# Is this candidate one of them?
with open(uidToAtomClash, "r") as atomClashingUIDs:
    for line in atomClashingUIDs:
        if re.search(uid, line):
            exitWithError("#AtomClash: Found Atom Clashes",0)
            
# Recover the fitness of the candidate, if available.
foundFitness=False
with open(uidToFitness, "r") as knownUIDs:
    for line in knownUIDs:
        if re.search(uid, line):
            print("Found fitness value: "+line.strip())
            fitness=line.strip().split()[1]
            foundFitness=True
            copySDFandAddProperty(inpSDF,preliminaryOutput,"FITNESS",fitness)
            break

# If no fitness is found this candidate is ignored.
if (not foundFitness):
    exitWithError("#FITNESS: not found",0)

print("All done. Returning "+outSDF)
os.rename(preliminaryOutput, outSDF)
sys.stdout.close()
sys.exit(0)
