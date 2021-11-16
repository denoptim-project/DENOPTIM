#!/usr/bin/env python3
'''
Script to generate 3D fragment for the 20 most common amino acids.
Execute it without options/arguments.

@author: Marco Foscato
'''


# This is where the resulting fragment will be written
outPathName="amino_acid_3D-fragments.sdf"

print('Starting generation of amino acid fragments...')

from rdkit import Chem
from rdkit.Chem import AllChem
from operator import itemgetter

fileWriter = Chem.SDWriter("amino_acid_3D-fragments.sdf")
print('Writing to '+outPathName)

aminoacids={'VAL':'V', 'ILE':'I', 'LEU':'L', 'GLU':'E', 'GLN':'Q', \
'ASP':'D', 'ASN':'N', 'HIS':'H', 'TRP':'W', 'PHE':'F', 'TYR':'Y',    \
'ARG':'R', 'LYS':'K', 'SER':'S', 'THR':'T', 'MET':'M', 'ALA':'A',    \
'GLY':'G', 'PRO':'P', 'CYS':'C'}

ctermAPClass = 'peptide:0'
ntermAPClass = 'peptide:1'

params = AllChem.ETKDGv2()

for aaa, a in aminoacids.items():
    print('Making fragment for ', aaa, '/',a)
    mol = Chem.rdmolfiles.MolFromSequence('g'+a+'g')
    mol.SetProp(key="_Name", val=aaa)
    mol = AllChem.AddHs(mol)
    conformationIDs = AllChem.EmbedMultipleConfs(mol, numConfs=20)
    result_and_energies = AllChem.MMFFOptimizeMoleculeConfs(mol)
    lowestEnergyConfId = min(enumerate(result_and_energies), key=itemgetter(1))[0]
    
    cterm = Chem.MolFromSmarts('CN([#1])C([#1])([#1])C(=O)O[#1]')
    ctermAtoms = mol.GetSubstructMatch(cterm)
    ctermAPCoords = mol.GetConformer(lowestEnergyConfId).GetPositions()[ctermAtoms[1]]
    ctermAPSrcID = ctermAtoms[0]
    
    nterm = Chem.MolFromSmarts('NC(=O)C([#1])([#1])N([#1])[#1]')
    ntermAtoms = mol.GetSubstructMatch(nterm)
    ntermAPCoords = mol.GetConformer(lowestEnergyConfId).GetPositions()[ntermAtoms[1]]
    ntermAPSrcID = ntermAtoms[0]
    
    mmol = Chem.RWMol(mol)
    for i in sorted(ctermAtoms[1:] + ntermAtoms[1:], reverse=True):
        mmol.RemoveAtom(i)
        if (i<ctermAPSrcID):
            ctermAPSrcID-=1
        if (i<ntermAPSrcID):
            ntermAPSrcID-=1
    
    mol = mmol.GetMol()
    
    ctermAPSrcID+=1 #Reporting with 1-based indexing
    ntermAPSrcID+=1 #Reporting with 1-based indexing
    cTermAP = f"{ctermAPSrcID}#{ctermAPClass}:{ctermAPCoords[0]:.4f}%{ctermAPCoords[1]:.4f}%{ctermAPCoords[2]:.4f}"
    nTermAP = f"{ntermAPSrcID}#{ntermAPClass}:{ntermAPCoords[0]:.4f}%{ntermAPCoords[1]:.4f}%{ntermAPCoords[2]:.4f}"

    propertyClass = nTermAP+" "+cTermAP
    propertyAP = str(ntermAPSrcID)+":1 "+str(ctermAPSrcID)+":1"
    if ntermAPSrcID > ctermAPSrcID:
        propertyClass= cTermAP+" "+nTermAP
        propertyAP = str(ctermAPSrcID)+":1 "+str(ntermAPSrcID)+":1"
    
    mol.SetProp(key='ATTACHMENT_POINTS', val=propertyClass)
    fileWriter.write(mol, confId=conformationIDs[lowestEnergyConfId])

fileWriter.close()

print('All done! Se file '+outPathName)
