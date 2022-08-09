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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Fragment;
import denoptim.io.DenoptimIO;

/**
 * Toll to remove dummy atoms from linearities of multi-hapto sites.
 * 
 * @author Marco Foscato
 * @author Vishwesh Venkatraman
 */
public class DummyAtomHandler
{
    private String elm = "";
    
    //Recursion flag for reporting infos
    int recNum = 1;

    /**
     * Program-specific logger
     */
    private Logger logger;
    
    private final static String NL = DENOPTIMConstants.EOL;
     
    
//------------------------------------------------------------------------------
    
    public DummyAtomHandler(String elm, Logger logger)
    {
        this.elm = elm;
        this.logger = logger;
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
            String symbol = MoleculeUtils.getSymbolOrLabel(atm);
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

        if (logger.isLoggable(Level.FINEST))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Found "+dummiesList.size()+" dummy atoms:");
            for (IAtom du : dummiesList)
            {
                sb.append(" " + getSDFAtomNumber(mol,du)+" size: "
                        + dummiesList.size() + NL);
            }
            logger.log(Level.FINEST,sb.toString());
        }
        
        //Delete dummy atoms and change connectivity
        for (IAtom du : dummiesList)
        {
            //Remove Du-[*] Bonds
            List<IAtom> nbrOfDu = mol.getConnectedAtomsList(du);
            for (IAtom nbr : nbrOfDu)
                mol.removeBond(du,nbr);

            //Remove Du atom
            mol.removeAtom(du);
            logger.log(Level.FINEST, "NOTE! Atom Numbers change: "
                    + "dummy atom deleted");
        }
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
            String symbol = MoleculeUtils.getSymbolOrLabel(atm);
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
        
        if (logger.isLoggable(Level.FINEST))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Found "+dummiesList.size()+" dummy atoms:");
            for (IAtom du : dummiesList)
            {
                sb.append(" " + getSDFAtomNumber(mol,du)+" size: "
                        + dummiesList.size() + NL);
            }
            logger.log(Level.FINEST,sb.toString());
        }

        //Delete dummy atoms and change connectivity
        for (IAtom du : dummiesList)
        {
            //Remove Du-[*] Bonds
            List<IAtom> nbrOfDu = mol.getConnectedAtomsList(du);
            int numOfTerms = nbrOfDu.size();
            for (IAtom nbr : nbrOfDu)
                mol.removeBond(du,nbr);

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

            if (logger.isLoggable(Level.FINEST))
            {
                StringBuilder sb = new StringBuilder();
                sb.append("There are "+goupsOfTerms.size()+" groups of terms");
                for (int i=0; i<goupsOfTerms.size(); i++)
                {
                    Set<IAtom> s = goupsOfTerms.get(i);
                    sb.append(" Group "+i+" - Hapticity: "+hapticity.get(i)
                        +" => "+NL);
                    for (IAtom sa : s)
                    {
                        sb.append((mol.indexOf(sa)+1)+
                                MoleculeUtils.getSymbolOrLabel(sa)+" ");
                    }
                    sb.append(NL);
                }
                logger.log(Level.FINEST, sb.toString());
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

                //In case of no matching return the error
                if (!ligandFound)
                {
                    String msg = "Dummy atom does not seem to be placed at the "
                                 + "centroid of a multihapto ligand. "
                                 + "Du: " + du
                                 + "Candidates: " + allCandidates
                                 + "See current molecule in 'error.sdf'";
                    DenoptimIO.writeSDFFile("error.sdf",mol,false);
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
                            logger.log(Level.FINEST, "Making a bond between: "
                                    + mol.indexOf(ligandAtm)  
                                    + MoleculeUtils.getSymbolOrLabel(
                                            ligandAtm) + " - " 
                                    + mol.indexOf(centralAtm)  
                                    + MoleculeUtils.getSymbolOrLabel(
                                            centralAtm));
                            IBond bnd = new Bond(ligandAtm,centralAtm);
                            mol.addBond(bnd);
                        }
                    }
                }
            }

            //Remove Du atom
            mol.removeAtom(du);
            logger.log(Level.FINEST, "NOTE! Atom Numbers change: "
                    + "dummy atom deleted");
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
        return mol.indexOf(atm) + 1;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Append dummy atoms on otherwise linear arrangements of atoms. This
     * to allow the use of internal coordinates when doing 3d-modeling.
     * Dummy atoms are connected to the central atom of a linear 
     * (or close-to-linear) bend. Attachment points are also taken into account
     * @param frag the fragment to be modified.
     * @param angLim the upper limit for an angle before we consider it flat, 
     * i.e., 180 DEG angle.
     */
    public static void addDummiesOnLinearities(Fragment frag, double angLim)
    {
        for (IAtom atm : frag.atoms())
        {
            Point3d pC = MoleculeUtils.getPoint3d(atm);
            List<IAtom> nbrs = frag.getConnectedAtomsList(atm);
            List<AttachmentPoint> nbrAP = frag.getAPsFromAtom(atm);
            loopOVerNeigborsOfOneAtom:
            for(int i=0; i<(nbrs.size()+nbrAP.size()); i++)
            {
                Point3d pL = null;
                if (i<nbrs.size())
                    pL = MoleculeUtils.getPoint3d(nbrs.get(i));
                else {
                    pL = nbrAP.get(i-nbrs.size()).getDirectionVector();
                    if (pL == null)
                        continue;
                }
                for (int j=i+1; j<(nbrs.size()+nbrAP.size()); j++)
                {
                    Point3d pR = null;
                    if (j<nbrs.size())
                        pR = MoleculeUtils.getPoint3d(nbrs.get(j));
                    else {
                        pR = nbrAP.get(j-nbrs.size()).getDirectionVector();
                        if (pR == null)
                            continue;
                    }
                    double angle = MathUtils.angle(pL, pC, pR);
                    if (angle > angLim)
                    {
                        // APs' heads are locations to avoid
                        List<Point3d> placesToAvoid = new ArrayList<Point3d>();
                        for (AttachmentPoint ap : nbrAP)
                            placesToAvoid.add(ap.getDirectionVector());
                        
                        // Create dummy atom
                        IAtom dummyAtm = getDummyInSafeDirection(atm, pR, 
                                frag.getIAtomContainer(), placesToAvoid);
                        frag.addAtom(dummyAtm);

                        // Connect dummy atom
                        IBond dummyBnd = new Bond(dummyAtm,atm);
                        frag.addBond(dummyBnd);
                        break loopOVerNeigborsOfOneAtom;
                    }
                }
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Generates a dummy atom 0.1 nm from atom A in a place that is safe for
     * a dummy atom. The be safe the position of the dummy has to respect 2 
     * criteria:<ol> 
     * <li>be not too close to an existing atom connected to A,</li>
     * <li>be not too close to be opposite to an atom connected to A, which
     * means, do not create linearities.</li>
     * </ol>
     * The method try to use 90 and 45 DEG with respect to vector 
     * departing from atom A in direction given by point B.
     * @param atmA first atom used to place the dummy
     * @param pB point defining the direction of interest when placing the 
     * dummy atom.
     * @param mol the molecular system
     * @param placesToAvoid points from which the new dummy should stay away.
     * @return the dummy atom
     */
    public static IAtom getDummyInSafeDirection(IAtom atmA, Point3d pB, 
            IAtomContainer mol, List<Point3d> placesToAvoid)
    {
        Point3d pA = MoleculeUtils.getPoint3d(atmA);

        // Get the forbidden areas: those in proximity of
        // atoms connected to A, or opposite (180 deg) to them
        List<Vector3d> allBusyDirections = new ArrayList<Vector3d>();
        List<IAtom> nbrs = mol.getConnectedAtomsList(atmA);
        for (IAtom nbr : nbrs)
        {
            Point3d pLoc = MoleculeUtils.getPoint3d(nbr);
            Vector3d vLoc = CartesianSpaceUtils.getVectorFromTo(pA, pLoc);
            vLoc.normalize();
            allBusyDirections.add(vLoc);
            
            Vector3d vLocOpposite = new Vector3d(vLoc.x * -1.0, vLoc.y * -1.0, 
                    vLoc.z * -1.0);
            vLocOpposite.normalize();
            allBusyDirections.add(vLocOpposite);
        }
        for (Point3d pOther : placesToAvoid)
        {
            Vector3d vLoc = CartesianSpaceUtils.getVectorFromTo(pA, pOther);
            vLoc.normalize();
            allBusyDirections.add(vLoc);
            
            Vector3d vLocOpposite = new Vector3d(vLoc.x * -1.0, vLoc.y * -1.0, 
                    vLoc.z * -1.0);
            vLocOpposite.normalize();
            allBusyDirections.add(vLocOpposite);
        }
        
        //Get vector perpendicular (normal) to AB
        Vector3d vAB = CartesianSpaceUtils.getVectorFromTo(pA, pB);
        vAB.normalize();
        Vector3d vNorm = CartesianSpaceUtils.getNormalDirection(vAB);
        vNorm.normalize();

        // Rotate the normal vector to check different angles
        ArrayList<Vector3d> allAttempts = new ArrayList<Vector3d>();
        ArrayList<Double> allAttemptsMinVal = new ArrayList<Double>();
        double angStep = 22.0;
        double maxStepD =  360.0 / angStep;
        int maxStep = (int) maxStepD;
        double forbiddenRadius = 0.2;
        for(int j=1; j<3; j++)
        {
            for(int step = 0; step<maxStep; step++)
            {
                Vector3d vADuTry = new Vector3d();
                if (j==1)
                {
                    vADuTry = new Vector3d(vNorm.x,vNorm.y,vNorm.z);
                } else if (j==2) {
                    vADuTry = CartesianSpaceUtils.getSumOfVector(vAB,
                            new Vector3d(vNorm.x*(Math.sqrt(2.0)),
                                    vNorm.y*(Math.sqrt(2.0)),
                                    vNorm.z*(Math.sqrt(2.0))));
                }
                vADuTry.normalize();
    
                // Get the new candidate position by rotation
                double ang = angStep * step;
                CartesianSpaceUtils.rotatedVectorWAxisAngle(vADuTry,vAB,ang);
            
                // Check if the candidate position is too close to a forbidden
                // place. Use distance since they are all originating from A and
                // normalized.
                boolean skip = false;
                double min = 10.0;
                for (int i=0; i<allBusyDirections.size(); i++)
                {
                    Vector3d busyDir = allBusyDirections.get(i);
                    Vector3d diffVec = CartesianSpaceUtils.getDiffOfVector(
                            vADuTry, busyDir);
                    double l = diffVec.length();
                    if (l < forbiddenRadius)
                    {
                        skip = true;
                        break;
                    }
                    if (l<min)
                        min = l;
                }

                if (skip)
                    continue;

                // Store the surviving ones:
                // those that are not too close to forbidden regions;
                allAttempts.add(vADuTry);
                allAttemptsMinVal.add(min);
            } //end of loop over angles around AB (torsion of AB)
        } //end of loop over angle with AB

        // Find the best candidate: the most distant from forbidden areas
        double max = Collections.max(allAttemptsMinVal);
        int best = allAttemptsMinVal.indexOf(max);
        Vector3d vADu = new Vector3d();
        vADu = allAttempts.get(best);

        // Create the dummy in the best position found
        CartesianSpaceUtils.translateOrigin(vADu,pA);
        Point3d duP3dB = new Point3d(vADu.x, vADu.y, vADu.z);
        IAtom dummyAtm = new PseudoAtom(DENOPTIMConstants.DUMMYATMSYMBOL, duP3dB);

        return dummyAtm;
    }
    
//------------------------------------------------------------------------------    
}
