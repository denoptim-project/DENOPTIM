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

package denoptim.molecularmodeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.Edge.BondType;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.graph.rings.RingClosure;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.integration.tinker.ConformationalSearchPSSROT;
import denoptim.integration.tinker.TinkerAtom;
import denoptim.integration.tinker.TinkerException;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.moldecularmodelbuilder.MMBuilderParameters;
import denoptim.task.ProcessHandler;
import denoptim.utils.GenUtils;
import denoptim.utils.ObjectPair;

/**
 * Toolkit to perform ring closing conformational search. 
 * This tool makes use of Tinker's PSSROT engine.
 *
 * @author Marco Foscato 
 */

public class RingClosureTool
{
    /**
     * Iteration counter for making unique filenames.
     */
    private int itn = 0;

    /**
     * File separator
     */
    final String fsep = System.getProperty("file.separator");
    
    /**
     * New line character
     */
    private static final String NL = DENOPTIMConstants.EOL;
    
    /**
     * Settings controlling the calculation
     */
    private MMBuilderParameters settings;
    
    /**
     * Program.specific logger
     */
    private Logger logger;

//------------------------------------------------------------------------------

    /**
     * Construct an empty RingClosureTool
     */
    public RingClosureTool(MMBuilderParameters settings)
    {
        this.settings = settings;
        this.logger = settings.getLogger();
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
    private void setTinkerTypes(TinkerMolecule tmol) throws DENOPTIMException
    {

        ArrayList<TinkerAtom> lstAtoms = tmol.getAtoms();
        int numberOfAtoms = lstAtoms.size();
        for (int i = 0; i < numberOfAtoms; i++)
        {
            TinkerAtom tatom = lstAtoms.get(i);

            String st = tatom.getAtomString().trim();
            if (!settings.getTinkerMap().containsKey(st))
            {
                String msg = "Unable to assign atom type to atom '" + st +"'.";
                throw new DENOPTIMException(msg);
            }
            Integer val = settings.getTinkerMap().get(st);
            if (val != null)
            {
                tatom.setAtomType(val.intValue());
                logger.log(Level.FINEST, "Set parameter for " + st);
            } 
            else
            {
                String msg = "No valid Tinker atom type for atom " + st;
                throw new DENOPTIMException(msg);
            }
        }
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

        String workDir = settings.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." + GenUtils.getPaddedString(3, idx);
        ArrayList<double[]> coords = TinkerUtils.readTinkerXYZ(xyzfile);
        return coords;
    }

//------------------------------------------------------------------------------
    
    /**
     * Performs one or more attempts to close rings by conformational adaptation.
     * The number of attempts (i.e, different set of rings) and the list of
     * definition of each attempt are defined into the object provided as input.
     * If no ring closure is possible, returns an empty array.
     * @param mol the input molecular system 
     * @return the list of generated molecules, if any.
     * @throws DENOPTIMException
     * @throws TinkerException 
     */

    public ArrayList<ChemicalObjectModel> attemptAllRingClosures(
            ChemicalObjectModel mol) throws DENOPTIMException, TinkerException
    {
        ArrayList<ChemicalObjectModel> rcMols = new ArrayList<ChemicalObjectModel>();
        for (int i=0; i<mol.getRCACombinations().size(); i++)
        {
            Set<ObjectPair> rcaComb = mol.getRCACombinations().get(i);
            if (logger.isLoggable(Level.FINE))
            {
                String s = "";
                for (ObjectPair p : rcaComb)
                {
                    s = s + p.getFirst() + ":" + p.getSecond() + " ";
                }
                logger.log(Level.FINE,"Attempting Ring Closure with RCA "
                        + "Combination (" + i + "): " + s);
            }

            // Try to create new molecule
            ChemicalObjectModel molTo3d = mol.deepcopy();
            ChemicalObjectModel rcMol = attemptRingClosure(molTo3d, 
                    molTo3d.getRCACombinations().get(i));

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

    private class RingClosedMolComparator implements Comparator<ChemicalObjectModel>
    {
    	public int compare(ChemicalObjectModel mA, ChemicalObjectModel mB)
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
     * @param chemObj the definition of the system to work with. This system
     * will be modified.
     * @param rcaCombination the combination of RingClosingAttractors. This
     * object defines which pairs of RingClosingAttractors will we try to join
     * in this attempt.
     * @return a new molecular system with the freshly closed rings, if any,
     * otherwise an empty molecule.
     * @throws DENOPTIMException
     * @throws TinkerException 
     */

    public ChemicalObjectModel attemptRingClosure(ChemicalObjectModel chemObj,
            Set<ObjectPair> rcaCombination) throws DENOPTIMException, TinkerException
    {
        IAtomContainer fmol = chemObj.getIAtomContainer();
        String workDir = settings.getWorkingDirectory();
        String molName = chemObj.getName();

        // Increment iteration number (to make unique file names)
        itn++;

        logger.log(Level.INFO, "Attempting Ring Closure via conformational"
                                + " adaptation for " + molName
                                + " (PSSROT - Iteration: " + itn + ")");
        
        List<String> molSpecificKeyFileLines = new ArrayList<String>();
        molSpecificKeyFileLines.addAll(settings.getRSKeyFileParams());
        for (ObjectPair op : rcaCombination)
        {
            int iTnkAtmRcaA = chemObj.getTnkAtmIdOfRCA(
                    (RingClosingAttractor) op.getFirst());
            int iTnkAtmRcaB = chemObj.getTnkAtmIdOfRCA(
                    (RingClosingAttractor) op.getSecond());
            molSpecificKeyFileLines.add("RC-PAIR " + iTnkAtmRcaA + " " 
                    + iTnkAtmRcaB);
        }
        
        // Definition of RingClosingPotential
        molSpecificKeyFileLines.add("RC11BNDTERM");
        molSpecificKeyFileLines.add("RC12BNDTERM NONE");
        for (ObjectPair op : rcaCombination)
        {
            RingClosingAttractor rca0 = (RingClosingAttractor) op.getFirst();
            RingClosingAttractor rca1 = (RingClosingAttractor) op.getSecond();
            int s0t = fmol.indexOf(rca0.getSrcAtom()) + 1;
            int s1t = fmol.indexOf(rca1.getSrcAtom()) + 1;
            int i0t = chemObj.getTnkAtmIdOfRCA(rca0);
            int i1t = chemObj.getTnkAtmIdOfRCA(rca1);

            double parA = 0.0;
            double parB = 0.0;
            parA = (rca0.getParamA11() + rca1.getParamA11()) / 2.0;
            parB = (rca0.getParamB11() + rca1.getParamB11()) / 2.0;

            molSpecificKeyFileLines.add("RC-11-PAIRS " + i0t + " " +s1t + " "
                           + parA + " " + parB);
            molSpecificKeyFileLines.add("RC-11-PAIRS " + s0t + " " + i1t + " "
                           + parA + " " + parB);
        }
        
        long startTime = System.nanoTime();
        ConformationalSearchPSSROT.performPSSROT(chemObj, itn, "rs",
                settings.getParamFile(), 
                molSpecificKeyFileLines,
                settings.getInitPSSROTParams(),
                settings.getRestPSSROTParams(),
                settings.getPSSROTTool(),
                settings.getXYZINTTool(),
                workDir,
                settings.getTaskID(), logger);
        long endTime = System.nanoTime();
        long time = (endTime - startTime);
        logger.log(Level.FINE, "TIME (RC conf. search): "+time/1000000+" ms"
                  + " #frags: " + chemObj.getGraph().getVertexList().size()
                  + " #atoms: " + chemObj.getIAtomContainer().getAtomCount()
                  + " #rotBnds: " + chemObj.getRotatableBonds().size());

        logger.log(Level.INFO, "RC-PSSROT done. Now, post-processing.");

        // Evaluate proximity of RingClosingAttractor and close rings
        closeRings(chemObj, rcaCombination);
        
        // Update list rotatable bonds
        chemObj.purgeListRotatableBonds();

        // Finalize the molecule: saturate free RCA
        saturateRingClosingAttractor(chemObj);
        
        // Cleanup
        FileUtils.deleteFilesContaining(workDir,molName + "_rs" + itn);
        
        return chemObj;
    }

//------------------------------------------------------------------------------    

    /**
     * Makes new rings by connecting pairs of atoms that hold properly arranged
     * RingClosingAttractors.
     *
     * @param mol the molecular system
     * @param rcaCombination the combination of RingClosingAttractors. This
     * object defines which pairs of RingClosingAttractors will we try to join
     * in this attempt.
     */

    public void closeRings(ChemicalObjectModel mol, Set<ObjectPair> rcaCombination)
    {
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        
        // Collect candidate RingClosures
        List<RingClosure> candidatesClosures = new ArrayList<RingClosure>();
        for (ObjectPair op : rcaCombination)
        {
            logger.log(Level.FINEST, "closeRings: evaluating closure of " + op);
            RingClosingAttractor rcaA = (RingClosingAttractor) op.getFirst();
            RingClosingAttractor rcaB = (RingClosingAttractor) op.getSecond();
            Point3d srcA = rcaA.getSrcAtom().getPoint3d();
            Point3d atmA = rcaA.getIAtom().getPoint3d();
            Point3d srcB = rcaB.getSrcAtom().getPoint3d();
            Point3d atmB = rcaB.getIAtom().getPoint3d();
            RingClosure rc = new RingClosure(srcA, atmA, srcB, atmB);
            candidatesClosures.add(rc);

            //Define closability conditions
            double lenH = srcA.distance(atmA);
            double lenT = srcB.distance(atmB);
            double distTolerance = (lenH + lenT) / 2.0;
            distTolerance = distTolerance * rcParams.getRCDistTolerance();
            double minDistH1T2 = -1.0;
            double minDistH2T1 = -1.0;
            double minDistH2T2 = -1.0;
            double maxDistH1T2 = 0.0;
            double maxDistH2T1 = 0.0;
            double maxDistH2T2 = 0.0;
            double maxDotProdHT = rcParams.getRCDotPrTolerance();

            maxDistH1T2 = distTolerance;
            maxDistH2T1 = distTolerance;
            maxDistH2T2 = lenH + lenT;

    	    boolean closeThisBnd = rc.isClosable(minDistH1T2, maxDistH1T2,
    	            minDistH2T1, maxDistH2T1, 
    	            minDistH2T2, maxDistH2T2,
    	            maxDotProdHT, 
    	            logger);
                
    	    if (closeThisBnd)
            {
    	        BondType bndTyp = rcaA.getRCBondType();
    	        if (bndTyp.hasCDKAnalogue())
    	        {
    	            mol.addBond(rcaA.getSrcAtom(),rcaB.getSrcAtom(),rc, bndTyp);
    	        } else {
    	            logger.log(Level.WARNING, "WARNING! "
    	                    + "Attempt to add ring closing bond "
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

    public void saturateRingClosingAttractor(ChemicalObjectModel mol)
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
