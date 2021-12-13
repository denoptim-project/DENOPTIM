/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptimcg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.integration.tinker.TinkerAtom;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.rings.RingClosingAttractor;
import denoptim.rings.RingClosure;
import denoptim.rings.RingClosureParameters;
import denoptim.task.ProcessHandler;
import denoptim.utils.GenUtils;
import denoptim.utils.ObjectPair;

/**
 * Toolkit to perform ring closure with 3D fragments
 *
 * @author Marco Foscato 
 */

public class RingClosureTool
{

    /**
     * Verbosity level
     */
    private static int verbosity = CGParameters.getVerbosity();

    /**
     * Iteration counter (for future use)
     */
    private int itn = 0;

    /**
     * File separator
     */
    final String fsep = System.getProperty("file.separator");

//------------------------------------------------------------------------------

    /**
     * Construct an empty RingClosureTool
     */
    public RingClosureTool()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Conversion to tinker IC may not always have the necessary atom types.
     * In order to fix this, we add user defined atom types.
     *
     * TODO: this method (from DENOPTIM3DMoleculeBuilder.java) should be made 
     * public and moved to a more sensible location (i.e., a class of utilities
     * for tinker methods)
     *
     * @param tmol The tinker IC representation
     * @throws DENOPTIMException
     */
    private static void setTinkerTypes(TinkerMolecule tmol) throws DENOPTIMException
    {

        ArrayList<TinkerAtom> lstAtoms = tmol.getAtoms();
        int numberOfAtoms = lstAtoms.size();
        for (int i = 0; i < numberOfAtoms; i++)
        {
            TinkerAtom tatom = lstAtoms.get(i);

            String st = tatom.getAtomString().trim();
            if (!CGParameters.getTinkerMap().containsKey(st))
            {
                String msg = "Unable to assign atom type to atom '" + st +"'.";
                throw new DENOPTIMException(msg);
            }
            Integer val = CGParameters.getTinkerMap().get(st);
            if (val != null)
            {
                tatom.setAtomType(val.intValue());
                if (verbosity > 3)
                    System.err.println("Set parameter for " + st);
            } 
            else
            {
                String msg = "No valid Tinker atom type for atom " + st;
                throw new DENOPTIMException(msg);
            }
        }
    }

//------------------------------------------------------------------------------    
    /**
     *
     * TODO: this method (from DENOPTIM3DMoleculeBuilder.java) should be made 
     * public and moved to a more sensible location (i.e., a class of utilities
     * for tinker methods)
     *
     */

    private String getNameLastCycleFile(String fname, String tinkerresfile)
                                                        throws DENOPTIMException
    {
        int lastIdx = CGUtils.countLinesWKeywordInFile(tinkerresfile,
                                " Final Function Value and Deformation");
        String workDir = CGParameters.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." +
                                 GenUtils.getPaddedString(3, lastIdx - 1);
        return xyzfile;
    }

//------------------------------------------------------------------------------    
    /**
     *
     * TODO: this method (from DENOPTIM3DMoleculeBuilder.java) should be made 
     * public and moved to a more sensible location (i.e., a class of utilities
     * for tinker methods)
     */

    private ArrayList<double[]> getTinkerLastCycleFile(String fname, String tinkerresfile)
                                                        throws DENOPTIMException
    {
        int lastIdx = CGUtils.countLinesWKeywordInFile(tinkerresfile,
                                " Final Function Value and Deformation");
        String workDir = CGParameters.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." +
                                 GenUtils.getPaddedString(3, lastIdx - 1);

        ArrayList<double[]> coords = TinkerUtils.readTinkerXYZ(xyzfile);

        // remove all the files filename.### (including the last one)
        for (int i = 0; i < lastIdx; i++)
        {
            xyzfile = workDir + fsep + fname + "."
                    + GenUtils.getPaddedString(3, i);
        }
        return coords;
    }

//------------------------------------------------------------------------------

    /*
     *
     * TODO: this method (from DENOPTIM3DMoleculeBuilder.java) should be made 
     * public and moved to a more sensible location (i.e., a class of utilities
     * for tinker methods)
     */

    private ArrayList<double[]> getTinkerCoords(String fname, String tinkerresfile)
                                                        throws DENOPTIMException
    {
        ArrayList<Double> energies = TinkerUtils.readPSSROTOutput(tinkerresfile);
        if (energies.isEmpty())
        {
            return null;
        }

        int idx = -1;
        double eng = Double.MAX_VALUE;
        for (int i = 0; i < energies.size(); i++)
        {
            double val = energies.get(i).doubleValue();
            if (val < eng)
            {
                idx = i;
                eng = val;
            }
        }

        String workDir = CGParameters.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." + GenUtils.getPaddedString(3, idx);
        ArrayList<double[]> coords = TinkerUtils.readTinkerXYZ(xyzfile);
        return coords;
    }

//------------------------------------------------------------------------------
    
    /**
     * Performs one or more attempts to close rings by conformational adaptation.
     * The number of attempts (i.e, different set of rings), is given by the 
     * information collected into the Molecule3DBuilder object provided as input.
     * If no ring closure is possible, returns an empty array.
     * @param mol the input molecular system 
     * @return the list of generated molecules, if any.
     * @throws DENOPTIMException
     */

    public ArrayList<Molecule3DBuilder> attemptAllRingClosures(
                                 Molecule3DBuilder mol) throws DENOPTIMException
    {
        ArrayList<Molecule3DBuilder> rcMols = new ArrayList<Molecule3DBuilder>();
        int i = 0;
        for (Set<ObjectPair> rcaComb : mol.getRCACombinations())
        {
            i++;
            if (verbosity > 1)
            {
                String s = "";
                for (ObjectPair p : rcaComb)
                {
                    s = s + p.getFirst() + ":" + p.getSecond() + " ";
                }
                System.out.println("Attempting Ring Closure with RCA "
                                        + "Combination (" + i + "): " + s);
            }

            // Try to create new molecule
            Molecule3DBuilder rcMol = attemptRingClosure(mol,rcaComb);

    	    // If some ring remains open, report in the MOL_ERROR field
    	    int newRingClosed = rcMol.getNewRingClosures().size();
    	    if (newRingClosed < rcaComb.size())
            {
        	    String err = "#RingClosureTool: uncomplete closure (closed "
    				+ newRingClosed + "/" + rcaComb.size() + ")";
        	    rcMol.getIAtomContainer().setProperty(
        	            DENOPTIMConstants.MOLERRORTAG,err);
            }
            rcMols.add(rcMol);
        }

        // Sort
        Collections.sort(rcMols, new RingClosedMolComparator());

        return rcMols;
    }

//------------------------------------------------------------------------------

    /**
     * Compares the Molecule3DBuilder afters ring closing-biased conformational
     * adaptation. Criteria for the evaluation are (i) the number of closed 
     * rings, (ii) the sum of the overall quality of all new RingClosures,
     * and (iii) the evaluation of atom proximity (dummy atoms excluded).
     * Note that this comparator sorts in DESCENDING order.
     */

    private class RingClosedMolComparator implements Comparator<Molecule3DBuilder>
    {
	public int compare(Molecule3DBuilder mA, Molecule3DBuilder mB)
	{
            final int FIRST = 1;
            final int EQUAL = 0;
            final int LAST = -1;

	    // First criterion is the number of RingClosures
	    int nRcA = mA.getNewRingClosures().size();
	    int nRcB = mB.getNewRingClosures().size();
	    if (nRcA < nRcB) // The HIGHER the number, the BETTER
	    {
		return FIRST;
	    }
	    else if (nRcA > nRcB)
	    {
		return LAST;
	    }

	    // Second, the quality (closeness to perfection) of RingClosures
	    double scoreA = mA.getNewRingClosuresQuality();
            double scoreB = mB.getNewRingClosuresQuality();
	    double dist = scoreA - scoreB;
	    double trsh = Math.min(scoreA,scoreB) * 0.05;
	    if (dist > 0.0 && dist > trsh) // The LOWER the score, the BETTER
            {
                return FIRST;
            }
	    if (dist < 0.0 && Math.abs(dist) > trsh)
            {
                return LAST;
            }

	    // Third, atom proximity
            double proxScoreA = mA.getAtomOverlapScore();
            double proxScoreB = mB.getAtomOverlapScore();
            if (proxScoreA < proxScoreB)  // The HIGHER the proxScore, the BETTER
            {
                return FIRST;
            }
            else if (proxScoreA > proxScoreB)
            {
                return LAST;
            }

	    return EQUAL;
	}
    }

//------------------------------------------------------------------------------

    /**
     * Attempts to close rings by finding the conformation that allows to
     * join heads and tails of atom specific chains. To this end a Potential
     * Smoothing and Search conformational search in ROTational space (PSSROT)
     * is performed under the effect of the Ring Closing potential,
     * that defines which atoms (chain head/tails) attract each other. 
     * The PSSROT engine is provided by an ad hoc modified version of Tinker
     * (call to external tool).
     * @param molIn the input molecular system 
     * @param rcaCombination the combination of RingClosingAttractors. This
     * object defines which pairs of RingClosingAttractors will we try to join
     * in this attempt.
     * @return a new molecular system with the freshly closed rings, if any,
     * otherwise an empty molecule.
     * @throws DENOPTIMException
     */

    public Molecule3DBuilder attemptRingClosure(Molecule3DBuilder molIn,
                      Set<ObjectPair> rcaCombination) throws DENOPTIMException
    {

        Molecule3DBuilder rcMol3d = molIn.deepcopy();
        IAtomContainer fmol = rcMol3d.getIAtomContainer();
        TinkerMolecule tmol = rcMol3d.getTinkerMolecule();
        String workDir = CGParameters.getWorkingDirectory();
        String molName = rcMol3d.getName();

        // Increment iteration number (to make unique file names)
        itn++;

        if (verbosity > 0)
        {
            System.err.println("Attempting Ring Closure via conformational"
                                + " adaptation for " + molName
                                + " (PSSROT - Iteration: " + itn + ")");
        }

        // Prepare dummy space for text and coordinates
        ArrayList<double[]> coords = new ArrayList<>();
        for (int i=0; i<fmol.getAtomCount(); i++)
        {
            double[] f = new double[3];
            f[0] = fmol.getAtom(i).getPoint3d().x;
            f[1] = fmol.getAtom(i).getPoint3d().y;
            f[2] = fmol.getAtom(i).getPoint3d().z;
        }
        
        // Prepare Tinker INT file (Internal coordinates)
        String rsIntFile = workDir + fsep + molName + "_rs" + itn + ".int";
        TinkerUtils.writeIC(rsIntFile, tmol);

        //Prepare Tinker KEY file (keywords, definition of potential)
        String rsKeyFile = workDir + fsep + molName + "_rs" + itn + ".key";
        StringBuilder rsSbKey = new StringBuilder(512);
        // Molecule-independent keywords
        for (String line : CGParameters.getRSKeyFileParams())
        {
            rsSbKey.append(line).append("\n");
        }
        // Active pairs of RCAs
        for (ObjectPair op : rcaCombination)
        {
            int iRcaA = ((Integer) op.getFirst()).intValue();
            int iRcaB = ((Integer) op.getSecond()).intValue();
            int iTnkAtmRcaA = rcMol3d.getTnkAtmIdOfRCA(
                                                rcMol3d.getAttractor(iRcaA));
            int iTnkAtmRcaB = rcMol3d.getTnkAtmIdOfRCA(
                                                rcMol3d.getAttractor(iRcaB));
            rsSbKey.append("RC-PAIR");
            rsSbKey.append(" " + iTnkAtmRcaA + " " + iTnkAtmRcaB + "\n");
        }
        // Definition of RingClosingPotential
        String rcStrategy = RingClosureParameters.getRCStrategy();

        if (rcStrategy.equals("BONDOVERLAP"))
        {
            if (verbosity > 1)
                System.out.println("Using BONDOVERLAP RC-strategy.");

            rsSbKey.append("RC11BNDTERM\n");
            rsSbKey.append("RC12BNDTERM NONE\n");
            for (ObjectPair op : rcaCombination)
            {
                int iRcaA = ((Integer) op.getFirst()).intValue();
                int iRcaB = ((Integer) op.getSecond()).intValue();
                RingClosingAttractor rca0 = rcMol3d.getAttractor(iRcaA);
                RingClosingAttractor rca1 = rcMol3d.getAttractor(iRcaB);
                int s0t = fmol.getAtomNumber(rca0.getSrcAtom()) + 1;
                int s1t = fmol.getAtomNumber(rca1.getSrcAtom()) + 1;
                int i0t = rcMol3d.getTnkAtmIdOfRCA(rca0);
                int i1t = rcMol3d.getTnkAtmIdOfRCA(rca1);

                double parA = 0.0;
                double parB = 0.0;
                parA = (rca0.getParamA11() + rca1.getParamA11()) / 2.0;
                parB = (rca0.getParamB11() + rca1.getParamB11()) / 2.0;

                rsSbKey.append("RC-11-PAIRS " + i0t + " " +s1t + " "
                               + parA + " " + parB + "\n");

                rsSbKey.append("RC-11-PAIRS " + s0t + " " + i1t + " "
                               + parA + " " + parB + "\n");
            }
        }
        else if (rcStrategy.equals("BONDCOMPLEMENTARITY"))
        {
            if (verbosity > 1)
                System.out.println("Using BONDCOMPLEMENTARITY RC-strategy.");

            rsSbKey.append("RC11BNDTERM\n");
            rsSbKey.append("RC12BNDTERM\n");
            for (ObjectPair op : rcaCombination)
            {
                int iRcaA = ((Integer) op.getFirst()).intValue();
                int iRcaB = ((Integer) op.getSecond()).intValue();
                RingClosingAttractor rca0 = rcMol3d.getAttractor(iRcaA);
                RingClosingAttractor rca1 = rcMol3d.getAttractor(iRcaB);
                int s0t = fmol.getAtomNumber(rca0.getSrcAtom()) + 1;
                int s1t = fmol.getAtomNumber(rca1.getSrcAtom()) + 1;
                int i0t = rcMol3d.getTnkAtmIdOfRCA(rca0);
                int i1t = rcMol3d.getTnkAtmIdOfRCA(rca1);

                double parA = 0.0;
                double parB = 0.0;
                parA = (rca0.getParamA11() + rca1.getParamA11()) / 2.0;
                parB = (rca0.getParamB11() + rca1.getParamB11()) / 2.0;

                rsSbKey.append("RC-11-PAIRS " + i0t + " " + i1t + " "
                               + parA + " " + parB + "\n");

                double[] intCoord0 = tmol.getAtom(i0t).getDistAngle();
                double[] intCoord1 = tmol.getAtom(i1t).getDistAngle();
                parA = intCoord0[0] + intCoord1[0];
                parB = (rca0.getParamB12() + rca1.getParamB12()) / 2.0;

                rsSbKey.append("RC-12-PAIRS " + s0t + " " + s1t + " "
                           + parA + " " + parB + "\n");
            }
        }
        else
        {
            System.err.println("Ring Closing Strategy '" + rcStrategy
                + "' not recognized. Plese choose between "
                + " 'BONDOVERLAP' and 'BONDCOMPLEMENTARITY'.");
            System.exit(-1);
        }
        //Finally write the keywords into the KEY file
        DenoptimIO.writeData(rsKeyFile, rsSbKey.toString(), false);

        // Prepare Tinker Submit file (PSSROT parameters)
        String rsSubFile = workDir + fsep + molName + "_rs" + itn + ".sub";
        StringBuilder rsSbSub = new StringBuilder(512);
        rsSbSub.append(rsIntFile).append("\n");
        // Molecule-independent section of SUB file
        for (String line : CGParameters.getRSInitPSSROTParams())
        {
            rsSbSub.append(line).append("\n");
        }
        // Rotatable bonds section of SUB file
        for (ObjectPair rotBndOp : rcMol3d.getRotatableBonds())
        {
            int t1 = ((Integer)rotBndOp.getFirst()).intValue() + 1;
            int t2 = ((Integer)rotBndOp.getSecond()).intValue() + 1;
            rsSbSub.append(t1).append(" ").append(t2).append("\n");
        }
        rsSbSub.append("\n");
        // Linear search section of SUB file
        int sz = rcMol3d.getNumberRotatableBonds();
        if (sz > 1)
        {
            ArrayList<String> txt = CGParameters.getRSRestPSSROTParams();
            for (int ir=0; ir<txt.size(); ir++)
            {
                // Control number of linear search directions
                if (ir == 2)
                {
                    int maxDirs = Integer.parseInt(txt.get(ir));
                    if (sz < maxDirs)
                    {
                        rsSbSub.append(sz).append("\n");
                    }
                    else
                    {
                        rsSbSub.append(maxDirs).append("\n");
                    }
                }
                else
                {
                    String row = txt.get(ir);
                    rsSbSub.append(row).append("\n");
                }
            }
        }
        else 
        {
            if (sz == 1)
            {
                // No linear search if there is only 1 rotation bond
                String firstLine = CGParameters.getRestPSSROTParams().get(0);
                String lastLine = CGParameters.getRestPSSROTParams().get(
                                 CGParameters.getRestPSSROTParams().size()-1);
                rsSbSub.append(firstLine).append("\n");
                rsSbSub.append("N").append("\n"); // no local search                
                rsSbSub.append(lastLine).append("\n");
            }
        }
        DenoptimIO.writeData(rsSubFile, rsSbSub.toString(), false);

        if (verbosity > 0)
            System.err.println("Submitting Ring-Closing PSSTOR");

        // Perform Ring Search with Tinker's PSSROT
        String rsOutFile = workDir + fsep + molName + "_rs" + itn + ".log";
        String rsCmdStr = CGParameters.getPSSROTTool() +
                          " < " + rsSubFile + " > " + rsOutFile;
        String rsID = "" + CGParameters.getTaskID();
        if (verbosity > 1)
        {
            System.err.println("CMD: " + rsCmdStr + " TskID: " + rsID);
        }
        ProcessHandler rsPh = new ProcessHandler(rsCmdStr, rsID);
        try
        {
            rsPh.runProcess();

            if (rsPh.getExitCode() != 0)
            {
                String msg = "PSSROT RingSearch failed for " + molName;
                throw new DENOPTIMException(msg + "\n"
                                            + rsPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }

        // Convert XYZ output of Tinker to INT with same z.matrix
        // Here we assume the file *.int with same basename exists
        // (was used it as input for PSSROT-RingSearch) and that file works as
        // template of the z-matrix
        String orsIntfile = getNameLastCycleFile(molName + "_rs" + itn, 
								     rsOutFile);
        String orsID = "" + CGParameters.getTaskID();
        String orsCmd = CGParameters.getXYZINTTool() + " "
                        + orsIntfile + " "
                        + " T"; //set use of template 
        if (verbosity > 1)
        {
            System.err.println("CMD: " + orsCmd + " TskID: " + orsID);
        }
        ProcessHandler orsPh = new ProcessHandler(orsCmd, orsID);
        try
        {
            orsPh.runProcess();
            if (orsPh.getExitCode() != 0)
            {
                String msg = "XYZINT (post RS) failed for " + molName;
                throw new DENOPTIMException(msg + "\n"
                                            + orsPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }

        // Update local molecular representation with output from RC-PSSROT
        String newTnkIC = workDir + fsep + molName + "_rs" + itn + ".int_2";
        TinkerMolecule tmpTmol = TinkerUtils.readTinkerIC(newTnkIC);
        ArrayList<TinkerAtom> lstAtoms = tmpTmol.getAtoms();
        for (int i=0; i<lstAtoms.size(); i++)
        {
            TinkerAtom ta = lstAtoms.get(i);
            tmol.getAtom(i+1).setDistAngle(ta.getDistAngle());
        }
        rcMol3d.updateXYZFromINT();

        if (verbosity > 0)
            System.err.println("RC-PSSROT done. Now, post-processing.");

        // Evaluate proximity of RingClosingAttractor and close rings
        closeRings(rcMol3d,rcaCombination);

        // Update list rotatabe bonds
        rcMol3d.purgeListRotatableBonds();

        // Finalize the molecule: saturate free RCA
        saturateRingClosingAttractor(rcMol3d);
        
        // Cleanup
        if (verbosity < 2)
        {
            DenoptimIO.deleteFilesContaining(workDir,molName + "_rs" + itn);
        }

        return rcMol3d;
    }

//------------------------------------------------------------------------------    

    /**
     * Makes new rings by connecting pairs of atoms that hold properly arranged
     * RingClosingAttractors. For the criteria deciding which bonds are to be 
     * closed, see method <code>boundableChainEnds</code>.
     *
     * @param mol the molecular system
     * @param rcaCombination the combination of RingClosingAttractors. This
     * object defines which pairs of RingClosingAttractors will we try to join
     * in this attempt.
     */

    public static void closeRings(Molecule3DBuilder mol, 
						 Set<ObjectPair> rcaCombination)
    {
        // Collect candidate RingClosures
        List<RingClosure> candidatesClosures = new ArrayList<RingClosure>();
        for (ObjectPair op : rcaCombination)
        {
    	    if (verbosity > 2)
    	        System.out.println("closeRings: evaluating closure of "+op);

            int iRcaA = ((Integer) op.getFirst()).intValue();
            int iRcaB = ((Integer) op.getSecond()).intValue();
            RingClosingAttractor rcaA = mol.getAttractor(iRcaA);
            RingClosingAttractor rcaB = mol.getAttractor(iRcaB);
            Point3d srcA = rcaA.getSrcAtom().getPoint3d();
            Point3d atmA = rcaA.getIAtom().getPoint3d();
            Point3d srcB = rcaB.getSrcAtom().getPoint3d();
            Point3d atmB = rcaB.getIAtom().getPoint3d();
            RingClosure rc = new RingClosure(srcA,atmA,srcB,atmB);
            candidatesClosures.add(rc);

            //Define closability conditions
            double lenH = srcA.distance(atmA);
            double lenT = srcB.distance(atmB);
            double distTolerance = (lenH + lenT) / 2.0;
//TODO may want to use different set criteria
            distTolerance = distTolerance 
				   * RingClosureParameters.getRCDistTolerance();
//
            double minDistH1T2 = -1.0;
            double minDistH2T1 = -1.0;
            double minDistH2T2 = -1.0;
            double maxDistH1T2 = 0.0;
            double maxDistH2T1 = 0.0;
            double maxDistH2T2 = 0.0;
//TODO may want to use different set criteria
            double maxDotProdHT = RingClosureParameters.getRCDotPrTolerance();

            if (RingClosureParameters.getRCStrategy().equals("BONDOVERLAP"))
            {
                maxDistH1T2 = distTolerance;
                maxDistH2T1 = distTolerance;
                maxDistH2T2 = lenH + lenT;
            }
            else if (RingClosureParameters.getRCStrategy().equals(
                                                         "BONDCOMPLEMENTARITY"))
            {
                distTolerance = distTolerance / 2.0;
                minDistH1T2 = lenH - distTolerance;
                minDistH2T1 = lenT - distTolerance;
                maxDistH1T2 = lenH + distTolerance;
                maxDistH2T1 = lenT + distTolerance;
//TODO may want to use different set criteria
                maxDistH2T2 = distTolerance 
				   * RingClosureParameters.getRCDistTolerance();
            }

    	    boolean closeThisBnd = false;
    	    if (verbosity > 2)
    	    {
    	        closeThisBnd = rc.isClosable(minDistH1T2,maxDistH1T2,
    					     minDistH2T1,maxDistH2T1,
    					     minDistH2T2,maxDistH2T2,
    					     maxDotProdHT,true);
    	    }
    	    else
    	    {
                closeThisBnd = rc.isClosable(minDistH1T2,maxDistH1T2,
                                                 minDistH2T1,maxDistH2T1,
                                                 minDistH2T2,maxDistH2T2,
                                                 maxDotProdHT,true);
    	    }
    	    if (closeThisBnd)
            {
    	        BondType bndTyp =  mol.getDRingFromRCAPair(op).getBondType();
    	        if (bndTyp.hasCDKAnalogue())
    	        {
    	            mol.addBond(rcaA.getSrcAtom(),rcaB.getSrcAtom(),rc, bndTyp);
    	        } else {
    	            System.out.println("WARNING! Attempt to add ring closing bond "
    	                    + "did not add any actual chemical bond because the "
    	                    + "bond type of the chord is '" + bndTyp +"'.");
    	        }
                rcaA.setUsed();
                rcaB.setUsed();
            }
        }
    }

//------------------------------------------------------------------------------    

    /**
     * Looks for unused RingClosingAttractors and attach proper capping group
     * saturating the free valency/bonding capability represented by free
     * RingClosingAttractors. 
     * TODO: make this method choose a proper capping group (if any) and use 
     * that to saturate the free AP
     * Note that to do that the capping og the RingClosure-related APclasses 
     * must be reported in the CompatibilityMatrix. Thus, it is also necessary 
     * to make DenoptimGA prefer RingClosure-related APclasses over other 
     * capping groups
     * 
     * @param mol the molecule
     */

    public static void saturateRingClosingAttractor(Molecule3DBuilder mol)
                                                       throws DENOPTIMException
    {
    	IAtomContainer fmol = mol.getIAtomContainer();
    	TinkerMolecule tmol = mol.getTinkerMolecule();
    	String duSymbol = DENOPTIMConstants.DUMMYATMSYMBOL;
        for (RingClosingAttractor rca : mol.getAttractorsList())
        {
            if (rca.isUsed())
            {
		        // Used RCA are changed to inert dummy atoms (to keep ZMatrix)
                IAtom fatm = rca.getIAtom();
                TinkerAtom tatm = tmol.getAtom(fmol.indexOf(fatm) + 1);
                tatm.setAtomString(duSymbol);
                
                IAtom newAtm = new PseudoAtom(duSymbol,new Point3d(fatm.getPoint3d()));
                newAtm.setProperties(fatm.getProperties());
                AtomContainerManipulator.replaceAtomByAtom(
                        mol.getIAtomContainer(),fatm,newAtm);
                rca.setIAtom(newAtm);
    	    }
    	    else
            {
        		// Unused RCA are replaced by capping group
        
        //TODO: select capping group (if any) and use that to saturate the free AP
        // Note that to do that the capping og the RingClosure-related APclasses must be
        // reported in the CompatibilityMatrix. Thus, it is also necessary to make DenoptimGA
        // prefer RingClosure-related APclasses over other capping groups
        /*
                        //Choose capping group
                        String freeAPClass = rca.getApClass();
                        ArrayList<String> capAPClasses = 
                                    CGParameters.getCompatibilityMap().get(freeAPClass);
        */
        		// We force the conversion to H with a fixed bond length
        		// This code is temporary as will be removed by the introduction
        		// proper capping group selection and use (TODO)
        		IAtom fatm = rca.getIAtom();
        		TinkerAtom tatm = tmol.getAtom(fmol.getAtomNumber(fatm) + 1);
        		tatm.setAtomString("H");
        		double[] ic = tatm.getDistAngle();
        		ic[0] = 1.10; //hard coded bond  
        		tatm.setDistAngle(ic);
        		
        		//UPGRADE: this cannot be done since CDK 2.*. So, we must
        		// create a new IAtom object to replace the old one.
        		/*
                ((PseudoAtom) fatm).setLabel("H"); 
        		fatm.setSymbol("H");
        		*/
        		IAtom newAtm = new PseudoAtom("H",new Point3d(fatm.getPoint3d()));
                newAtm.setProperties(fatm.getProperties());
                AtomContainerManipulator.replaceAtomByAtom(
                        mol.getIAtomContainer(),fatm,newAtm);
                rca.setIAtom(newAtm);
    	    }
    	}
    
    	// Update XYZ
    	mol.updateXYZFromINT();

        // Update Tinker atom types
        setTinkerTypes(tmol);
    }

//------------------------------------------------------------------------------
}
