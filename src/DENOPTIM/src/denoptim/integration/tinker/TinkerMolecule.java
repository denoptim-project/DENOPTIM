/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.integration.tinker;

import java.io.Serializable;
import java.util.ArrayList;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class TinkerMolecule implements Serializable, Cloneable
{
    private ArrayList<TinkerAtom> lstAtoms;
    ArrayList<int[]> zdel;
    ArrayList<int[]> zadd;
    String molname;

    private boolean debug = false;


//------------------------------------------------------------------------------

    public TinkerMolecule()
    {
	lstAtoms = new ArrayList<TinkerAtom>();
	zdel = new ArrayList<int[]>();
	zadd = new ArrayList<int[]>();
	molname = "";
    }

//------------------------------------------------------------------------------

    public TinkerMolecule(String m_name, ArrayList<int[]> m_zadd,
                            ArrayList<int[]> m_zdel, ArrayList<TinkerAtom> m_atoms)
    {
        lstAtoms = m_atoms;
        molname = m_name;
        zdel = m_zdel;
        zadd = m_zadd;
    }

//------------------------------------------------------------------------------
    
    /**
     * Identify the list of connected atoms (only first neighbours are 
     * considered) to the atom at the specified 
     * position
     * @param m_pos
     * @return list of connected atoms 
     */

    public ArrayList<Integer> getConnectedAtoms(int m_pos)
    {
        ArrayList<Integer> lst = new ArrayList<>();

        TinkerAtom tatm = getAtom(m_pos);
        int[] d = tatm.getAtomNeighbours();
        if (d[0] != 0)
            lst.add(Integer.valueOf(d[0])); 

        int numberOfAtoms = lstAtoms.size();
        for (int i = 0; i<numberOfAtoms; i++)
        {
            tatm = lstAtoms.get(i);
            if (tatm.getXYZIndex() == m_pos)
            {
                continue;
            }
            d = tatm.getAtomNeighbours();
            if (d[0] == m_pos)
            {
                lst.add(Integer.valueOf(tatm.getXYZIndex()));
            }
        }
        

        return lst;
    }

//------------------------------------------------------------------------------
    
    /**
     * store 3d coordinates of the fragments
     * @param coords 
     */

    public void set3DCoordinates(ArrayList<double[]> coords)
    {
        int numberOfAtoms = coords.size();
        for (int i = 0; i<numberOfAtoms; i++)
        {
            TinkerAtom atom = lstAtoms.get(i);
            double[] x = coords.get(i);
            atom.moveTo(x);
        }
    }

//------------------------------------------------------------------------------

    public void setName(String m_name)
    {
        this.molname = m_name;
    }

//------------------------------------------------------------------------------

    public void setAtoms(ArrayList<TinkerAtom> m_atoms)
    {
        this.lstAtoms = m_atoms;
    }

//------------------------------------------------------------------------------

    public void setBondPairs(ArrayList<int[]> m_zdel, ArrayList<int[]> m_zadd)
    {
        this.zdel = m_zdel;
        this.zadd = m_zadd;
    }

//------------------------------------------------------------------------------

    public ArrayList<TinkerAtom> getAtoms()
    {
        return this.lstAtoms;
    }

//------------------------------------------------------------------------------

    public int getLastAtomNumber()
    {
        return lstAtoms.get(lstAtoms.size()-1).getXYZIndex();
    }

//------------------------------------------------------------------------------

    public String getName()
    {
        return this.molname;
    }

//------------------------------------------------------------------------------

    public ArrayList<int[]> getBondAdd()
    {
        return this.zadd;
    }

//------------------------------------------------------------------------------

    public ArrayList<int[]> getBondDel()
    {
        return this.zdel;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the atom which has the XYZ index set to m_pos
     * @param m_pos
     * @return the TinkerAtom at the specified position
     */

    public TinkerAtom getAtom(int m_pos)
    {
        for (int i=0; i<lstAtoms.size(); i++)
        {
            TinkerAtom atm = lstAtoms.get(i);
            if (atm.getXYZIndex() == m_pos)
            {
                return atm;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * This mehtod produces a shallow copy of the object
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

//------------------------------------------------------------------------------

    /**
     * This mehtod produces a deep copy of the object
     */
    public TinkerMolecule deepCopy()
    {
	String nMolName = this.molname;
	ArrayList<TinkerAtom> atms = new ArrayList<TinkerAtom>();
	for (int ia=0; ia<this.lstAtoms.size(); ia++)
	{
	    TinkerAtom oTa = this.lstAtoms.get(ia);
	    String str = oTa.getAtomString();
	    int at = oTa.getAtomType();
	    double[] oXyz = oTa.getXYZ();
	    double[] nXyz = new double[] {oXyz[0], oXyz[1], oXyz[2]};
	    int[] oNBnd = oTa.getAtomNeighbours();
	    int[] nNBnd = new int[] {oNBnd[0], oNBnd[1], oNBnd[2], oNBnd[3]};
            double[] oDiAng = oTa.getDistAngle();
            double[] nDiAng = new double[] {oDiAng[0], oDiAng[1], oDiAng[2]};
	    TinkerAtom nTa = new TinkerAtom(ia+1,str,at,nXyz,nNBnd,nDiAng);
	    int vId = oTa.getVertexId();
	    nTa.setVertexId(vId);
	    atms.add(nTa);
	}
	
	ArrayList<int[]> zadd = new ArrayList<int[]>();
	for (int ia=0; ia<this.zadd.size(); ia++)
	{
	    int[] oBnd = this.zadd.get(ia);
	    int[] nBnd = new int [] {oBnd[0], oBnd[1]};
	    zadd.add(nBnd);
	}

	ArrayList<int[]> zdel = new ArrayList<int[]>();
        for (int ia=0; ia<this.zdel.size(); ia++)
        {
            int[] oBnd = this.zdel.get(ia);
            int[] nBnd = new int [] {oBnd[0], oBnd[1]};
            zdel.add(nBnd);
        }
	
	TinkerMolecule nTMol = new TinkerMolecule(nMolName, zadd, zdel, atms);

	return nTMol;
    }
    
//------------------------------------------------------------------------------
    
    public boolean isConnected(int atm1, int atm2)
    {
        TinkerAtom A1 = getAtom(atm1);
        TinkerAtom A2 = getAtom(atm2);
        
        int[] d1 = A1.getAtomNeighbours();
        int[] d2 = A2.getAtomNeighbours();
        
        if (d1[0] == atm2)
            return true;
        if (d2[0] == atm1)
            return true;
        
        return false;
    }

//------------------------------------------------------------------------------

    public void addMolecule(TinkerMolecule tmol2)
    {
        lstAtoms.addAll(tmol2.getAtoms());

        // add zdel, zadd
        zadd.addAll(tmol2.getBondAdd());
        zdel.addAll(tmol2.getBondDel());
    }

//------------------------------------------------------------------------------

    /**
     * Add one atom to this molecule
     */

    public void addAtom(TinkerAtom ta)
    {
	lstAtoms.add(ta);
    }

//------------------------------------------------------------------------------          
    /**
     * Add one bond by appending a pair of indeces into the z-add section
     * @param a1 first atom index
     * @param a2 second atom index
     */

    public void addBond(int a1, int a2)
    {
	int[] newBnd = new int[2];
	newBnd[0] = a1;
	newBnd[1] = a2;
	zadd.add(newBnd);
    }

//------------------------------------------------------------------------------

    /**
     * Check if the pair of atoms has been used to define a proper torsion
     * @param t1
     * @param t2
     * @return
     * <code>true</code> if the atoms have been used already
     */
    public boolean isTorsionUsed(int t1, int t2)
    {
        for (TinkerAtom atm : lstAtoms)
        {
            int[] nbs = atm.getAtomNeighbours();

            if ((nbs[0] == t1) && (nbs[1] == t2) && atm.usesProperTorsion())
            {
                if (debug)
                    System.err.println("Already used atoms(a): " + t1 + " " + t2);
                return true;
            }
            else if ((nbs[0] == t2) && (nbs[1] == t1)
                                                    && atm.usesProperTorsion())
            {
                if (debug)
                    System.err.println("Already used atoms(b): " + t2 + " " + t1);
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------
    
    public void printIC()
    {
        int numatoms = lstAtoms.size();
        // write out the number of atoms and the title
        String line = "";
        line = String.format("%6d  %s%n", numatoms, molname);
        System.err.print(line);

        for (int i = 0; i<numatoms; i++)
        {
            TinkerAtom atom = lstAtoms.get(i);
            int[] d1 = atom.getAtomNeighbours();
            double[] d2 = atom.getDistAngle();
            // output of first three atoms is handled separately
            if (i==0)
            {
                line = String.format("%6d  %-3s%6d%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType());
                System.err.print(line);
            }
            else if (i == 1)
            {
                line = String.format("%6d  %-3s%6d%6d%10.5f%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType(), d1[0], d2[0]);
                System.err.print(line);
            }
            else if (i == 2)
            {
                line = String.format("%6d  %-3s%6d%6d%10.5f%6d%10.4f%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType(), d1[0], d2[0],
                                    d1[1], d2[1]);
                System.err.print(line);
            }
            // output the fourth through final atoms
            else
            {
                line = String.format("%6d  %-3s%6d%6d%10.5f%6d%10.4f%6d%10.4f%6d%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType(), d1[0], d2[0],
                                    d1[1], d2[1], d1[2], d2[2], d1[3]);
                System.err.print(line);
            }
        }
        
//        if (zadd.size() > 0 || zdel.size() > 0)
//        {
//            line = "\n";
//            System.err.print(line);
//
//            for (int i=0; i<zadd.size(); i++)
//            {
//                int[] z = zadd.get(i);
//                line =  String.format("%6d%6d%n", z[0], z[1]);
//                System.err.println(line);
//            }
//
//            if (zdel.size() > 0)
//            {
//                line = "\n";
//                System.err.print(line);
//
//                for (int i=0; i<zdel.size(); i++)
//                {
//                    int[] z = zdel.get(i);
//                    line =  String.format("%6d%6d%n", z[0], z[1]);
//                    System.err.println(line);
//                }
//            }
//        }
        
        System.err.println();
    }
    
//------------------------------------------------------------------------------    
    

}
