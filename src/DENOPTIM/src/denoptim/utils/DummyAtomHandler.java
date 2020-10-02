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

package denoptim.utils;


import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 *
 * @author Marco Foscato
 * @author Vishwesh Venkatraman
 */
public class DummyAtomHandler
{
    private String elm = "";
    
    //Recursion flag for reporting infos
     int recNum = 1;

    //Amount of debug details printed on screen
    private int debugLevel = 0;
    
//------------------------------------------------------------------------------
    
    public DummyAtomHandler(String m_elm)
    {
        elm = m_elm;
    }    

//------------------------------------------------------------------------------

    /**
     * Returns true is the given string corresponds to an element symbol
     */

    public static boolean isElement(String s)
    {
        return DENOPTIMConstants.ALL_ELEMENTS.contains(s);
    }
    
//------------------------------------------------------------------------------

    /**
     * Removes all dummy atoms and the bonds connecting them to other atoms
     */

    public IAtomContainer removeDummy(IAtomContainer mol)
    {
        List<IAtom> dummiesList = new ArrayList<>();

        //Identify the target atoms to be treated
        for (IAtom atm : mol.atoms())
        {
            String symbol = atm.getSymbol();
            if (elm.equals(""))
            {
                if (!isElement(symbol))
                {
                    dummiesList.add(atm);
                }
            }
            else
            {
                if (symbol.equals(elm))
                {
                    dummiesList.add(atm);
                }
            }
        }

        if (debugLevel > 0)
            System.err.println("Found "+dummiesList.size()+" dummy atoms(2)");

        if (debugLevel > 2)
            for (IAtom du : dummiesList)
                System.err.println("DU LIST: " + getSDFAtomNumber(mol,du)+" size: "
                                                + dummiesList.size());

        //Delete dummy atoms and change connectivity
        for (IAtom du : dummiesList)
        {
            //Remove Du-[*] Bonds
            List<IAtom> nbrOfDu = mol.getConnectedAtomsList(du);
            int numOfTerms = nbrOfDu.size();
            for (IAtom nbr : nbrOfDu)
                mol.removeBond(du,nbr);

            //Remove Du atom
            mol.removeAtom(du);
            if (debugLevel > 2)
                System.err.println("NOTE! Atom Numbers change: dummy atom deleted");
        } //end loop over Du

        return mol;
    }    
    
//------------------------------------------------------------------------------
    
    public IAtomContainer removeDummyInHapto(IAtomContainer mol) 
                                                        throws DENOPTIMException
    {
        List<IAtom> dummiesList = new ArrayList<>();
        
        //Identify the target atoms to be treated
        for (IAtom atm : mol.atoms())
        {
            String symbol = atm.getSymbol();
            if (elm.equals(""))
            {
                if (!isElement(symbol))
                {
                    dummiesList.add(atm);
                }
            } 
            else 
            {
                if (symbol.equals(elm)) 
                {
                    dummiesList.add(atm);
                }
            }
        }

        if (debugLevel > 0)
            System.err.println("Found "+dummiesList.size()+" dummy atoms");

        if (debugLevel > 2)
        {
            for (IAtom du : dummiesList)
            {
                System.err.println("DU LIST: " + getSDFAtomNumber(mol,du) 
                                   + " size: " + dummiesList.size());
            }
        }

        //Delete dummy atoms and change connectivity
        for (IAtom du : dummiesList)
        {
            //Remove Du-[*] Bonds
            List<IAtom> nbrOfDu = mol.getConnectedAtomsList(du);
            int numOfTerms = nbrOfDu.size();
            for (IAtom nbr : nbrOfDu)
                mol.removeBond(du,nbr);

            //Identify atoms of ligand in mupltihapto system
            if (debugLevel > 2)
            {
                System.err.println(" DU: " + getSDFAtomNumber(mol,du) 
                                   + " size: "+nbrOfDu.size());
            }

            List<Boolean> found = getFlagsVector(numOfTerms);
            List<Set<IAtom>> goupsOfTerms = new ArrayList<>();
            List<Integer> hapticity = new ArrayList<>();
            for (int i=0; i<numOfTerms; i++)
            {
                //Was it found already?
                if (found.get(i))
                    continue;

                //Look for ligand starting from this term
                IAtom nbrI = nbrOfDu.get(i);
                Set<IAtom> ligOfNbrI = 
                                 exploreConnectedToAtom(nbrI,nbrOfDu,mol,found);
                if (!ligOfNbrI.isEmpty())
                {
                    goupsOfTerms.add(ligOfNbrI);
                    hapticity.add(ligOfNbrI.size());
                }

            } //end of loop over neighbours of Dummy

            if (debugLevel > 2)
            {
                System.err.println("There are "+goupsOfTerms.size()+" groups of terms");
                for (int i=0; i<goupsOfTerms.size(); i++)
                {
                    Set<IAtom> s = goupsOfTerms.get(i);
                    System.err.print(" Group "+i+" - Hapticity: "+hapticity.get(i)+" => ");
                    for (IAtom sa : s)
                        System.err.print((mol.getAtomNumber(sa)+1)+sa.getSymbol()+" ");
                    System.err.println(" ");
                }
            }

            // If Du is in between groups, connectivity has to be fixed
            if (goupsOfTerms.size() > 1)
            {
                //Idenfify the ligand corresponding to Du
                int ligandID = -1;
                boolean ligandFound = false;
                List<Point3d> allCandidates = new ArrayList<Point3d>();
                for (int i=0; i<goupsOfTerms.size(); i++)
                {
                    Set<IAtom> grp = goupsOfTerms.get(i);

                    //Identify center of the group of terms
                    Point3d candidateDuP3d = new Point3d();
                    for (IAtom atm : grp)
                    {
                        try 
                        {
                            Point3d ligP3d = atm.getPoint3d();
                            candidateDuP3d.x = candidateDuP3d.x + ligP3d.x;
                            candidateDuP3d.y = candidateDuP3d.y + ligP3d.y;
                            candidateDuP3d.z = candidateDuP3d.z + ligP3d.z;
                        } 
                        catch (Throwable t) 
                        {
                            Point2d ligP2d = atm.getPoint2d();
                            candidateDuP3d.x = candidateDuP3d.x + ligP2d.x;
                            candidateDuP3d.y = candidateDuP3d.y + ligP2d.y;
                            candidateDuP3d.z = 0.0000; 
                        }
                    }
                    allCandidates.add(candidateDuP3d);
                    candidateDuP3d.x = candidateDuP3d.x / (double) hapticity.get(i);
                    candidateDuP3d.y = candidateDuP3d.y / (double) hapticity.get(i);
                    candidateDuP3d.z = candidateDuP3d.z / (double) hapticity.get(i);

                    //Get coords of du
                    Point3d dummyP3d = new Point3d();
                    try 
                    {
                        Point3d du3d = du.getPoint3d();
                        dummyP3d.x = du3d.x;
                        dummyP3d.y = du3d.y;
                        dummyP3d.z = du3d.z;
                    } 
                    catch (Throwable t) 
                    {
                        Point2d du2d = du.getPoint2d();
                        dummyP3d.x = du2d.x;
                        dummyP3d.y = du2d.y;
                        dummyP3d.z = 0.0000;
                    }

                    //Check if Du correspond to the centroid of this group
                    // WARNING! Hard-coded threshold.
                    double dist = candidateDuP3d.distance(dummyP3d);
                    if (dist < 0.002)
                    {
                        if (ligandFound)
                        {
                            String msg = "More then one group of atoms may "
                                         + "correspond to the ligand. Not able "
                                         + "to identify the ligand!";
                            throw new DENOPTIMException(msg);
                        }
                        ligandID = i;
                        ligandFound = true;
                    }
                }

                //In case of no mathcing return the error
                if (!ligandFound)
                {
                    String msg = "Dummy atom does not seem to be placed at the "
                                 + "centroid of a multihapto ligand. "
                                 + "Du: " + du
                                 + "Candidates: " + allCandidates
                                 + "See current molecule in 'error.sdf'";
                    DenoptimIO.writeMolecule("error.sdf",mol,false);
                    throw new DENOPTIMException(msg);
                }
                
                //Connect every atom from the multihapto ligand whith
                // the cetral atom/atoms
                Set<IAtom> ligand = goupsOfTerms.get(ligandID);
                for (int i=0; i<goupsOfTerms.size(); i++)
                {
                    if (i == ligandID)
                        continue;

                    Set<IAtom> grp = goupsOfTerms.get(i);

                    for (IAtom centralAtm : grp)
                    {
                        for (IAtom ligandAtm : ligand)
                        {
                            if (debugLevel > 2)
                            {
                                System.err.println("Making a bond between: " + 
                                    mol.getAtomNumber(ligandAtm) + 
                                    ligandAtm.getSymbol() + " - " + 
                                    mol.getAtomNumber(centralAtm) + 
                                    centralAtm.getSymbol());
                            }
                            IBond bnd = new Bond(ligandAtm,centralAtm);
                            mol.addBond(bnd);
                        }
                    }
                }
            }

            //Remove Du atom
            mol.removeAtom(du);
            if (debugLevel > 2)
                System.err.println("NOTE! Atom Numbers change: dummy atom deleted");
        } //end loop over Du
        
        return mol;
    }

//------------------------------------------------------------------------------
    
    /**
     * Explore connected systems in a list of atoms and returns all the atoms
     * that can be reached starting from the seed atom by moving only along
     * connections between atoms in the initial list
     * @param seed the atom from which to start
     * @param inList list of atoms to be considered
     * @param mol the molecular object containing all atoms in inList
     * @param doneFlag vector of boolean flags
     * @return the group of atoms reachable via bonds between atoms in inList
     */
    private Set<IAtom> exploreConnectedToAtom(IAtom seed, List<IAtom> inList, 
                                IAtomContainer mol, List<Boolean> doneFlag)
    {
        Set<IAtom> outSet = new HashSet<>();

        //Deal with the seed
        int idx = inList.indexOf(seed);
        doneFlag.set(idx,true);
        outSet.add(seed);

        //Look for other atoms reachable from here
        List<IAtom> connToSeed = mol.getConnectedAtomsList(seed);
        connToSeed.retainAll(inList);
        for (IAtom nbr : connToSeed)
        {
            int idx2 = inList.indexOf(nbr);
            if (!doneFlag.get(idx2))
            {
                recNum++;
                Set<IAtom> recursiveOut =
                            exploreConnectedToAtom(nbr,inList,mol, doneFlag);
                recNum--;
                outSet.addAll(recursiveOut);
            }
        }
        return outSet;
    }

//------------------------------------------------------------------------------
    
    /**
     * Generates a vector of boolean flags. The size of the vector equals the 
     * number of atoms in the <code>IAtomContainer<code/>. 
     * All flags are initialized to <code>false<code/>.
     * @param size of vector of flags has to be generated.
     * @return a vector of flags.
     */
    private List<Boolean> getFlagsVector(int size)
    {
        //create a vector with false entries
        List<Boolean> flg = new ArrayList<>();
        for (int i = 0; i<size; i++) {
            flg.add(false);
        }
        return flg;
    }

//------------------------------------------------------------------------------

    private static int getSDFAtomNumber(IAtomContainer mol, IAtom atm)
    {
        return mol.getAtomNumber(atm) + 1;
    }
    
//------------------------------------------------------------------------------    
}
