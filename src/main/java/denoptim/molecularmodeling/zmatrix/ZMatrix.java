package denoptim.molecularmodeling.zmatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.utils.ConnectedLigand;
import denoptim.utils.ConnectedLigandComparator;
import denoptim.utils.MathUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RotationalSpaceUtils;


/**
 * Representation of an atom container's geometry with internal coordinates.
 */
public class ZMatrix
{ 
    /**
     * Identifier of this ZMatrix
     */
    private String id;

    /**
     * All atoms and pseudoAtoms mentioned in the Zmatrix
     */
    private List<ZMatrixAtom> lstAtoms;
    
    /**
     * All bonds in the system, whether included or not in the Z-matrix 
     * intrinsic connections
     */
    private List<ZMatrixBond> lstBonds;

//------------------------------------------------------------------------------

    /**
     * Constructor for ZMatrix
     */
    public ZMatrix()
    {
        lstAtoms = new ArrayList<>();
        lstBonds = new ArrayList<>();
    }

//------------------------------------------------------------------------------

    /**
     * Add an atom to the ZMatrix
     * @param atom The atom to add
     */
    public void addAtom(ZMatrixAtom atom)
    {
        lstAtoms.add(atom);
    }

//------------------------------------------------------------------------------

    /**
     * Remove an atom from the ZMatrix
     * @param atm The atom to remove
     */
    public void removeAtom(ZMatrixAtom atm)
    {
        lstAtoms.remove(atm);
        List<ZMatrixBond> bondsToRemove = new ArrayList<>();
        for (ZMatrixBond bond : lstBonds)
        {
            if (bond.getAtm1() == atm || bond.getAtm2() == atm)
            {
                bondsToRemove.add(bond);
            }
        }
        lstBonds.removeAll(bondsToRemove);
    }

//------------------------------------------------------------------------------

    /**
     * Get the number of atoms in the ZMatrix
     * @return The number of atoms in the ZMatrix
     */
    public int getAtomCount()
    {
        return lstAtoms.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get the number of bonds in the ZMatrix
     * @return The number of bonds in the ZMatrix
     */
    public int getBondCount()
    {
        return lstBonds.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get the bond data for the ZMatrix
     * @return The bond data for the ZMatrix
     */
    public List<int[]> getBondData()
    {
        List<int[]> bondData = new ArrayList<>();
        for (ZMatrixBond bond : lstBonds)
        {
            bondData.add(new int[] {bond.getAtm1().getId(), 
                bond.getAtm2().getId()});
        }
        return bondData;
    }

//------------------------------------------------------------------------------

    /**
     * Get the id of the ZMatrix
     * @return The id of the ZMatrix
     */
    public String getId()
    {
        return id;
    }

//------------------------------------------------------------------------------

    /**
     * Set the id of the ZMatrix
     * @param id The id to set
     */
    public void setId(String id)
    {
        this.id = id;
    }

//------------------------------------------------------------------------------

    /**
     * Get the bonds to add to the Z-matrix
     * @return A list of bonds to add defined by the 0-basedindex of the atoms
     * in the ZMatrix. 
     */
    public List<int[]> getBondsToAdd()
    {
        List<int[]> bondsToAdd = new ArrayList<>();
        for (ZMatrixBond bond : lstBonds)
        {
            boolean found = false;
            for (ZMatrixAtom atm : lstAtoms)
            {
                
                if (atm.getBondRefAtom() == null)
                    continue;
                
                if ((atm == bond.getAtm1() && atm.getBondRefAtom() == bond.getAtm2()) ||
                    (atm == bond.getAtm2() && atm.getBondRefAtom() == bond.getAtm1()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                bondsToAdd.add(new int[] {bond.getAtm1().getId(), 
                    bond.getAtm2().getId()});
            }
        }
        return bondsToAdd;
    }

//------------------------------------------------------------------------------

    /**
     * Get the bonds to delete from the ZMatrix
     * @return The bonds to delete from the ZMatrix
     */
    public List<int[]> getBondsToDel()
    {
        List<int[]> bondsToDel = new ArrayList<>();
        for (int i = 0; i < lstAtoms.size(); i++)
        {
            ZMatrixAtom atm = lstAtoms.get(i);
            
            if (atm.getBondRefAtom() == null)
                continue;
            
            boolean found = false;
            for (ZMatrixBond bond : lstBonds)
            {
                if ((atm == bond.getAtm1() && atm.getBondRefAtom() == bond.getAtm2()) ||
                    (atm == bond.getAtm2() && atm.getBondRefAtom() == bond.getAtm1()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                bondsToDel.add(new int[] {atm.getId(), atm.getBondRefAtom().getId()});
            }
        }
        return bondsToDel;
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the atom at the given index
     * @param index The index of the atom
     * @return The atom at the given index
     */
    public ZMatrixAtom getAtom(int index)
    {
        return lstAtoms.get(index);
    }

//------------------------------------------------------------------------------

    /**
     * Get the atoms in the ZMatrix
     * @return The atoms in the ZMatrix
     */
    public List<ZMatrixAtom> getAtoms()
    {
        return lstAtoms;
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the index of the atom
     * @param atm The atom
     * @return The index of the atom
     */
    public int getIndex(ZMatrixAtom atm)
    {
        return lstAtoms.indexOf(atm);
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the bond reference atom for the atom at the given index
     * @param index The index of the atom
     * @return The bond reference atom or null if not set
     */
    public ZMatrixAtom getBondRefAtom(int index)
    {
        return getAtom(index).getBondRefAtom();
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the index of the bond reference atom for the atom at the given index
     * @param index The index of the atom
     * @return The index of the bond reference atom or -1 if not set
     */
    public int getBondRefAtomIndex(int index)
    {
        ZMatrixAtom atm = getBondRefAtom(index);
        if (atm == null)
            return -1;
        return lstAtoms.indexOf(atm);
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the angle reference atom for the atom at the given index
     * @param index The index of the atom
     * @return The angle reference atom or null if not set
     */
    public ZMatrixAtom getAngleRefAtom(int index)
    {
        return getAtom(index).getAngleRefAtom();
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the index of the angle reference atom for the atom at the given index
     * @param index The index of the atom
     * @return The index of the angle reference atom or -1 if not set
     */
    public int getAngleRefAtomIndex(int index)
    {
        ZMatrixAtom atm = getAngleRefAtom(index);
        if (atm == null)
            return -1;
        return lstAtoms.indexOf(atm);
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the second angle reference atom for the atom at the given index
     * @param index The index of the atom
     * @return The second angle reference atom or null if not set
     */
    public ZMatrixAtom getAngle2RefAtom(int index)
    {
        return getAtom(index).getAngle2RefAtom();
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the index of the second angle reference atom for the atom at the 
     * given index
     * @param index The index of the atom
     * @return The index of the second angle reference atom or -1 if not set
     */
    public int getAngle2RefAtomIndex(int index)
    {
        ZMatrixAtom atm = getAngle2RefAtom(index);
        if (atm == null)
            return -1;
        return lstAtoms.indexOf(atm);
    }

//------------------------------------------------------------------------------
    /**
     * Get the bond length for the atom at the given index
     * @param index The index of the atom
     * @return The bond length or null if not set
     */
    public Double getBondLength(int index)
    {
        return getAtom(index).getBondLength();
    }

//------------------------------------------------------------------------------

    /**
     * Get the bond angle for the atom at the given index
     * @param index The index of the atom
     * @return The bond angle or null if not set
     */
    public Double getAngleValue(int index)
    {
        return getAtom(index).getAngleValue();
    }

//------------------------------------------------------------------------------

    /**
     * Get the angle2 angle for the atom at the given index
     * @param index The index of the atom
     * @return The bond angle or null if not set
     */
    public Double getAngle2Value(int index)
    {
        return getAtom(index).getAngle2Value();
    }

//------------------------------------------------------------------------------

    /**
     * Get the chiral flag for the atom at the given index
     * @param index The index of the atom
     * @return The chiral flag or null if not set
     */
    public Integer getChiralFlag(int index)
    {
        return getAtom(index).getChiralFlag();
    }

//------------------------------------------------------------------------------
    
    /**
     * Check if the dihedral between the two atoms at the given indices
     * uses proper torsion
     * @param idx1 The index of the first atom
     * @param idx2 The index of the second atom
     * @return True if the dihedral uses proper torsion, false otherwise
     */
    public boolean usesProperDihedral(int idx1, int idx2)
    {
        for (int ia=0; ia<lstAtoms.size(); ia++)
        {
            int idxJ = getBondRefAtomIndex(ia);
            int idxK = getAngleRefAtomIndex(ia);
            if ((idxJ==idx1 && idxK==idx2) || (idxJ==idx2 && idxK==idx1))
            {
                Integer c = getChiralFlag(ia);
                if (c != null)
                {
                    return c == 0;
                    
                }
            }
        }
        return false;
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the bond at the given index
     * @param index The index of the bond
     * @return The bond
     */
    public ZMatrixBond getBond(int index)
    {
        return lstBonds.get(index);
    }

//------------------------------------------------------------------------------

    /**
     * Delete the bond between the two atoms at the given indices
     * @param a1 The index of the first atom
     * @param a2 The index of the second atom
     */
    public void delBond(int a1, int a2)
    {
        delBond(lstAtoms.get(a1), lstAtoms.get(a2));
    }

//------------------------------------------------------------------------------

    /**
     * Delete the bond between the two atoms
     * @param a1 The first atom
     * @param a2 The second atom
     */
    public void delBond(ZMatrixAtom a1, ZMatrixAtom a2)
    {
        List<ZMatrixBond> bondsToDel = new ArrayList<>();
        for (ZMatrixBond bond : lstBonds)
        {
            if ((bond.getAtm1() == a1 && bond.getAtm2() == a2) ||
                (bond.getAtm1() == a2 && bond.getAtm2() == a1))
            {
                bondsToDel.add(bond);
                break;
            }
        }
        lstBonds.removeAll(bondsToDel);
    }

//------------------------------------------------------------------------------

    /**
     * Add a bond between the two atoms at the given indices
     * @param a1 The index of the first atom
     * @param a2 The index of the second atom
     * @param order The order of the bond
     */
    public void addBond(int a1, int a2)
    {
        addBond(lstAtoms.get(a1), lstAtoms.get(a2));
    }

//------------------------------------------------------------------------------

    /**
     * Add a bond between the two atoms
     * @param a1 The first atom
     * @param a2 The second atom
     * @param order The order of the bond
     */
    public void addBond(ZMatrixAtom a1, ZMatrixAtom a2)
    {
        lstBonds.add(new ZMatrixBond(a1, a2));
    }

//------------------------------------------------------------------------------

    /**
     * Clone the ZMatrix
     * @return The cloned ZMatrix
     */
    public ZMatrix clone()        
    {
        ZMatrix clone = new ZMatrix();
        // First pass: create all atoms with null references
        for (ZMatrixAtom atom : lstAtoms)
        {
            clone.addAtom(new ZMatrixAtom(
                atom.getId(), atom.getSymbol(), atom.getType(),
                null, null, null,
                atom.getBondLength(), 
                atom.getAngleValue(), 
                atom.getAngle2Value(),
                atom.getChiralFlag()));
        }
        // Second pass: set up references using indices from original
        for (int i = 0; i < lstAtoms.size(); i++)
        {
            ZMatrixAtom origAtom = lstAtoms.get(i);
            ZMatrixAtom cloneAtom = clone.lstAtoms.get(i);
            
            if (origAtom.getBondRefAtom() != null)
            {
                int refIdx = getIndex(origAtom.getBondRefAtom());
                cloneAtom.setBondRefAtom(clone.lstAtoms.get(refIdx));
            }
            
            if (origAtom.getAngleRefAtom() != null)
            {
                int refIdx = getIndex(origAtom.getAngleRefAtom());
                cloneAtom.setAngleRefAtom(clone.lstAtoms.get(refIdx));
            }
            
            if (origAtom.getAngle2RefAtom() != null)
            {
                int refIdx = getIndex(origAtom.getAngle2RefAtom());
                cloneAtom.setAngle2RefAtom(clone.lstAtoms.get(refIdx));
            }
        }
        // Third pass: add bonds
        for (ZMatrixBond bond : lstBonds)
        {
            clone.addBond(
                clone.lstAtoms.get(getIndex(bond.getAtm1())), 
                clone.lstAtoms.get(getIndex(bond.getAtm2())));
        }
        return clone;
    }
    
//-----------------------------------------------------------------------------
 
    /**
     * Convert {@link IAtomContainer} to {@link ZMatrix}.
     * Supports only containers where all atoms are reachable following
     * the connectivity and starting from any other atom in the container.
     * Atom types, if any are read from the atom property 
     * {@link DENOPTIMConstants#ATMPROPATOMTYPE}.
     * @param mol the {@link IAtomContainer} to convert
     * @return the molecule represented by internal coordinates
     */

    public static ZMatrix getZMatrixFromIAC(IAtomContainer mol)  throws DENOPTIMException
    {
        boolean debug = false; //only for development
        ZMatrix zmat = new ZMatrix();
        String doneBnd = "visitedBond";
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            ZMatrixAtom bndRef = null;
            ZMatrixAtom angleRef = null;
            ZMatrixAtom angle2Ref = null;
            Double bondLength = null;
            Double angleValue = null;
            Double angle2Value = null;
            Integer chiralFlag = null;

            IAtom atmI = mol.getAtom(i);
            if (debug)
            {
                System.err.println("Atom to IC: "
                        + MoleculeUtils.getSymbolOrLabel(atmI)+" "+i);
            }

            // define the bond length
            if (i>0)
            {
                i2 = getFirstRefAtomId(i,mol);
                bondLength = atmI.getPoint3d().distance(mol.getAtom(i2).getPoint3d());
                mol.getBond(atmI,mol.getAtom(i2)).setProperty(doneBnd,"T");
                bndRef = zmat.getAtom(i2);
            }

            // define the angle reference atom
            if (i>1)
            {
                i3 = getSecondRefAtomId(i,i2,mol);
                angleRef = zmat.getAtom(i3);
                angleValue = MathUtils.angle(atmI.getPoint3d(),
                                            mol.getAtom(i2).getPoint3d(),
                                            mol.getAtom(i3).getPoint3d());
            }

            // decide on dihedral or second angle
            if (i>2)
            {
                ObjectPair op = getThirdRefAtomId(i,i2,i3,mol,zmat);
                i4 = (int) op.getFirst();
                i5 = (int) op.getSecond();
                if (i5==1)
                {
                    angle2Value = MathUtils.angle(atmI.getPoint3d(),
                                                mol.getAtom(i2).getPoint3d(),
                                                mol.getAtom(i4).getPoint3d()); 
                    angle2Ref = zmat.getAtom(i4);
                    double sign = MathUtils.torsion(
                                                atmI.getPoint3d(),
                                                mol.getAtom(i2).getPoint3d(),
                                                mol.getAtom(i3).getPoint3d(),
                                                mol.getAtom(i4).getPoint3d());
                    if (sign > 0.0)
                    {
                        i5 = -1;
                    }
                } else {
                    IAtom atmJ = mol.getAtom(i2);
                    IAtom atmK = mol.getAtom(i3);
                    IAtom atmL = mol.getAtom(i4);

                    angle2Value = MathUtils.torsion(
                        atmI.getPoint3d(), atmJ.getPoint3d(),
                        atmK.getPoint3d(), atmL.getPoint3d());
                    double valueIJKL = angle2Value;

                    angle2Ref = zmat.getAtom(i4);
                    
                    IBond bnd = mol.getBond(atmJ, atmK);
                    if (bnd!=null)
                    {
                        Object cnstrDefObj = bnd.getProperty(
                                RotationalSpaceUtils.PROPERTY_ROTDBDCSTR_DEF);
                        if (cnstrDefObj!=null)
                        {
                            // For clarity, IJKL are the atoms identifying the
                            // dihedral used in the internal coordinates,
                            // while ABCD are the atoms used to define the 
                            // constrained dihedral angle.
                            IAtom[] atomsABCD = (IAtom[]) cnstrDefObj;
                            double cnstrABCD = (double) bnd.getProperty(
                                    RotationalSpaceUtils.PROPERTY_ROTDBDCSTR_VALUE);
                            if (atmJ==atomsABCD[2] && atmK==atomsABCD[1])
                            {
                                // We ensure consistent orfer in the definitions
                                // of the two dihedral angles
                                atomsABCD = new IAtom[]{
                                        atomsABCD[3], atomsABCD[2],
                                        atomsABCD[1], atomsABCD[0]};
                                if (cnstrABCD>0)
                                {
                                    cnstrABCD = 360.0 - cnstrABCD;
                                } else {
                                    cnstrABCD = cnstrABCD - 360.0;
                                }
                            }

                            double valueABCD = MathUtils.torsion(
                                    atomsABCD[0].getPoint3d(), 
                                    atomsABCD[1].getPoint3d(),
                                    atomsABCD[2].getPoint3d(), 
                                    atomsABCD[3].getPoint3d());
                            double correctionABCD = cnstrABCD - valueABCD;
                            
                            angle2Value = angle2Value + correctionABCD;
                            
                            if (angle2Value > 360)
                            {
                                angle2Value = angle2Value - 360;
                            } else if (angle2Value < -360) {
                                angle2Value = angle2Value + 360;
                            }

                            if (debug)
                            {
                                System.err.println(" dihedral constrain along "
                                        + i2 + "-" + i3 + ": " 
                                        + valueIJKL + " -> " + angle2Value
                                        + " (changed by " + correctionABCD + ")");
                            }
                        }
                    }
                }
                chiralFlag = i5;
                if (debug)
                {
                    System.err.println(" i4 = "+ i4 + " angle2Value: " + angle2Value + " " + i5);
                }
            }

            String symb = MoleculeUtils.getSymbolOrLabel(atmI);
            String atyp = "0";
            Object atypObj = atmI.getProperty(DENOPTIMConstants.ATMPROPATOMTYPE);
            if (atypObj != null)
                atyp = atypObj.toString();

            ZMatrixAtom zatm = new ZMatrixAtom(i, symb, atyp,
                    bndRef, angleRef, angle2Ref,
                    bondLength, angleValue, angle2Value, 
                    chiralFlag);
            zmat.addAtom(zatm);
        }

        // Add bonds not visited
        for (IBond b : mol.bonds())
        {
            zmat.addBond(mol.indexOf(b.getAtom(0)),
                           mol.indexOf(b.getAtom(1)));
        }

        // Due to the assumption that all atoms are part of the same 
        // connected network, no bond has to be deleted
        
        return zmat;
    }

//----------------------------------------------------------------------------
   
    private static int getFirstRefAtomId(int i1, IAtomContainer mol)
    {
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        for (IAtom nbr : mol.getConnectedAtomsList(mol.getAtom(i1)))
        {
            if (mol.indexOf(nbr) < i1)
            {
                ConnectedLigand cl = new ConnectedLigand(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        int i2 = mol.indexOf(candidates.get(0).getAtom());
        return i2;
    }

//----------------------------------------------------------------------------

    private static int getSecondRefAtomId(int i1, int i2, IAtomContainer mol)
    {
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        for (IAtom nbr : mol.getConnectedAtomsList(mol.getAtom(i2)))
        {
            if ((mol.indexOf(nbr) < i1) && (nbr != mol.getAtom(i1)))
            {
                ConnectedLigand cl = new ConnectedLigand(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        int i3 = mol.indexOf(candidates.get(0).getAtom());
        return i3;
    }

//----------------------------------------------------------------------------

    private static ObjectPair getThirdRefAtomId(int i1, int i2, int i3, 
               IAtomContainer mol, ZMatrix zmat) throws DENOPTIMException
    {
        boolean debug = false; //only for development
        int i5 = 0;
        IAtom atmI1 = mol.getAtom(i1);
        IAtom atmI2 = mol.getAtom(i2);
        IAtom atmI3 = mol.getAtom(i3);
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        if (zmat.usesProperDihedral(i2, i3) || 
            countPredefinedNeighbours(i1,atmI3,mol)==1)
        {
            i5 = 1;
            for (IAtom nbr : mol.getConnectedAtomsList(atmI2))
            {
                if (debug)
                {
                   System.err.println("  Eval. 3rd (ANG): " + 
                           MoleculeUtils.getSymbolOrLabel(nbr)
                   + mol.indexOf(nbr) + " " 
                   + (mol.indexOf(nbr) < i1) + " "
                   + (nbr != atmI1) + " " 
                   + (nbr != atmI3));
                }
                if ((mol.indexOf(nbr) < i1) && (nbr != atmI1) && 
                                                        (nbr != atmI3))
                {
                    double dbcAng = MathUtils.angle(nbr.getPoint3d(),
                                                          atmI2.getPoint3d(),
                                                          atmI3.getPoint3d());
                    if(dbcAng > 1.0)
                    {
                        ConnectedLigand cl = new ConnectedLigand(nbr,1);
                        candidates.add(cl);
                    }
                    else
                    {
                        if (debug)
                        {
                            System.err.println("  ...but collinear with "
                                    + MoleculeUtils.getSymbolOrLabel(atmI3) + i3
                                               + " (i4-i2-i3: " + dbcAng 
                                               + ")");
                        }
                    }
                }
            }
        }
        else
        {
            i5 = 0;
            for (IAtom nbr : mol.getConnectedAtomsList(atmI3))
            {
                if (debug)
                {
                   System.err.println("  Eval. 3rd (TOR): " 
                            + MoleculeUtils.getSymbolOrLabel(nbr)
                   + mol.indexOf(nbr) + " "
                   + (mol.indexOf(nbr) < i1) + " "
                   + (nbr != atmI1) + " "
                   + (nbr != atmI2));
                }
                if ((mol.indexOf(nbr) < i1) && (nbr != atmI1) &&
                                                        (nbr != atmI2))
                {
                    ConnectedLigand cl = new ConnectedLigand(nbr,1);
                    candidates.add(cl);
                }
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        if (candidates.size() == 0)
        {
            String msg = "Unable to make internal coordinates. Please, "
                         + "consider the use of dummy atoms in proximity "
                         + "of atom " + zmat.getAtom(i1+1);
            throw new DENOPTIMException(msg);
        }
        int i4 = mol.indexOf(candidates.get(0).getAtom());

        ObjectPair op = new ObjectPair(i4,i5);
        
        return op;
    }

//------------------------------------------------------------------------------

    private static int countPredefinedNeighbours(int i, IAtom a, IAtomContainer mol)
    {
        int tot = 0;
        for (IAtom nbr : mol.getConnectedAtomsList(a))
        {
            if (mol.indexOf(nbr) < i)
                tot++;
        }
        return tot;
    }
    

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
       
        if (o.getClass() != getClass())
            return false;
        
        ZMatrix other = (ZMatrix) o;
        
        // Compare id
        if (this.id == null ? other.id != null : !this.id.equals(other.id))
            return false;
        
        // Compare atom counts
        if (this.lstAtoms.size() != other.lstAtoms.size())
            return false;
        
        // Compare atoms (by equals method)
        for (int i = 0; i < this.lstAtoms.size(); i++)
        {
            if (!this.lstAtoms.get(i).equals(other.lstAtoms.get(i)))
                return false;
        }
        
        // Compare bond counts
        if (this.lstBonds.size() != other.lstBonds.size())
            return false;
        
        // Compare bonds (by equals method, order-independent for undirected bonds)
        // Create sets of bond IDs for comparison
        Set<String> thisBondIds = new HashSet<>();
        for (ZMatrixBond bond : this.lstBonds)
        {
            int id1 = bond.getAtm1().getId();
            int id2 = bond.getAtm2().getId();
            // Store in canonical form (smaller ID first)
            String bondId = Math.min(id1, id2) + "-" + Math.max(id1, id2);
            thisBondIds.add(bondId);
        }
        
        Set<String> otherBondIds = new HashSet<>();
        for (ZMatrixBond bond : other.lstBonds)
        {
            int id1 = bond.getAtm1().getId();
            int id2 = bond.getAtm2().getId();
            String bondId = Math.min(id1, id2) + "-" + Math.max(id1, id2);
            otherBondIds.add(bondId);
        }
        
        return thisBondIds.equals(otherBondIds);
    }

//------------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + lstAtoms.size();
        for (ZMatrixAtom atom : lstAtoms)
        {
            result = 31 * result + atom.hashCode();
        }
        result = 31 * result + lstBonds.size();
        for (ZMatrixBond bond : lstBonds)
        {
            result = 31 * result + bond.hashCode();
        }
        return result;
    }

//------------------------------------------------------------------------------
}

