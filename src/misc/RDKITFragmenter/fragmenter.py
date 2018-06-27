#!/usr/bin/env python

"""
Created on Thu Aug 14 11:42:29 2014

@author: vishwesv
"""

import sys
from rdkit import Chem
from rdkit.Chem import AllChem
from rdkit.Chem import BRICS


print 'Number of arguments:', len(sys.argv), 'arguments.'
print 'Argument List:', str(sys.argv)

if len(sys.argv ) == 3 :
    inp_sdf_file = sys.argv[1]
    out_sdf_file = sys.argv[2]
else:
   sys.exit ("Usage: fragmenter.py infile outfile")
   
   
try:
    suppl = Chem.SDMolSupplier(inp_sdf_file)
    catalog=set()
    for mol in suppl:
        if mol is None: continue
        print mol.GetNumAtoms() 
        #AllChem.Compute2DCoords(mol)
        pieces = BRICS.BRICSDecompose(mol)                   
        catalog.update(pieces)

    print('Generated: ', len(catalog), ' fragments.')
    ofile = Chem.SDWriter(out_sdf_file)    
    for frg in catalog: 
        cmol = Chem.MolFromSmiles(frg)
        AllChem.Compute2DCoords(cmol)
        ofile.write(cmol)        

            
except IOError:
   print >> sys.stderr, "Input file could not be opened"
   sys.exit(1)    
