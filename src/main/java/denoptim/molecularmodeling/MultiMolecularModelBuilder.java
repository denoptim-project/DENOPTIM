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

package denoptim.molecularmodeling;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.integration.tinker.TinkerException;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.moldecularmodelbuilder.MMBuilderParameters;
import denoptim.utils.AtomOrganizer;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.GenUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RotationalSpaceUtils;

/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class MultiMolecularModelBuilder
{
    private String molName;
    private DGraph molGraph;
    
    private MMBuilderParameters settings;
    
    /**
     * Program.specific logger
     */
    private Logger logger;

//------------------------------------------------------------------------------

    public MultiMolecularModelBuilder(String molName, DGraph molGraph,
            MMBuilderParameters settings)
    {
        this.settings = settings;
        this.logger = settings.getLogger();
        this.molName = molName;
        this.molGraph = molGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Given the graph representation of the molecular constitution,
     * along with the 3D
     * coordinates of the fragments and direction vectors, create 3D
     * structures by merging internal coordinates. More than one 
     * structure can be obtained if the provided graph allows for isomerism
     * for example by multiple possibility of ring formation or conformational
     * isomerism (not implemented yet).
     * @throws TinkerException if thinker fails
     */

    public ArrayList<IAtomContainer> buildMulti3DStructure() 
            throws DENOPTIMException, TinkerException
    {
        logger.log(Level.INFO, "Building Multiple 3D representations for "
                 + "graph = " + molGraph.toString());

        // Generate XYZ and INT representations
        long startTime = System.nanoTime();
        ChemicalObjectModel mol = build3DTree();
        long endTime = System.nanoTime();
        long time = (endTime - startTime);
        logger.log(Level.FINE, "TIME (build 3D model): " + time/1000000 + " ms"
                  + " #frags: " + mol.getGraph().getVertexList().size() 
                  + " #atoms: " + mol.getIAtomContainer().getAtomCount());
        
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }

        // Evaluate source of isomerism
        // 1: Attempt Ring Closures 
        RingClosureTool rct = new RingClosureTool(settings);
        ArrayList<ChemicalObjectModel> rcMols =new ArrayList<ChemicalObjectModel>();
        boolean skipConfSearch = false;
        if (rcParams.allowRingClosures() && 
            mol.getGraph().getRings().size() != 0)
        {
            startTime = System.nanoTime();
            rcMols = rct.attemptAllRingClosures(mol);
            endTime = System.nanoTime();
            time = (endTime - startTime);
            int numAllClosedCombs = 0;
            for (ChemicalObjectModel rcMol : rcMols)
            {
                Object o = rcMol.getIAtomContainer().getProperty(
                        DENOPTIMConstants.MOLERRORTAG);
                if (o == null)
                    numAllClosedCombs++;
            }
            logger.log(Level.FINE, "TIME (close ring): "+time/1000000+" ms"
                      + " #frags: " + mol.getGraph().getVertexList().size()
                      + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                      + " #rings: " + mol.getGraph().getRings().size()
                      + " #rcaCombs: " + mol.getRCACombinations().size()
                      + " #allClosedRCSCombs: " + numAllClosedCombs);
            if (rcParams.requireCompleteRingclosure && numAllClosedCombs<1)
            {
                logger.log(Level.INFO, "No fully closed RCS combinaton. "
                        + "Nothing to send to conformational search.");
                skipConfSearch = true;
            }
        } else {
            ChemicalObjectModel nMol = mol.deepcopy();
            rct.saturateRingClosingAttractor(nMol);
            rcMols = new ArrayList<ChemicalObjectModel>();
            rcMols.add(nMol);
        }


        // 2: Conformational search (if possible)
        ArrayList<ChemicalObjectModel> csMols = new ArrayList<ChemicalObjectModel>();
        if (skipConfSearch)
        {
            csMols.addAll(rcMols);
        } else {
            ConformationalSearchPSSROT csPssRot = new ConformationalSearchPSSROT(
                    settings);
            startTime = System.nanoTime();
            csMols = csPssRot.performPSSROT(rcMols);
            endTime = System.nanoTime();
            time = (endTime - startTime);
            logger.log(Level.FINE, "TIME (conf. search): "+time/1000000+" ms"
                      + " #frags: " + mol.getGraph().getVertexList().size()
                      + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                      + " #rotBnds: " + mol.getRotatableBonds().size());
        }

        // Convert and return results
        ArrayList<IAtomContainer> results = new ArrayList<IAtomContainer>();
        DummyAtomHandler dah = new DummyAtomHandler(
                DENOPTIMConstants.DUMMYATMSYMBOL, logger);
        for (ChemicalObjectModel mol3db : csMols)
        {
            IAtomContainer iac = mol3db.getIAtomContainer();
            
            //TODO-V3 make reordering optional
            
            IAtomContainer originalOrderMol = AtomOrganizer.makeReorderedCopy(
                    iac, mol3db.getOldToNewOrder(), mol3db.getNewToOldOrder());
            iac = originalOrderMol;
            
            if (!settings.getKeepDummyFlag())
            {
                // To keep track of which vertexID should be removed from mol 
                // properties, we remove that property from the mol. It remains
                // defined in each atom.
                iac.removeProperty(DENOPTIMConstants.ATMPROPVERTEXID);
                
                iac = dah.removeDummyInHapto(iac);
                iac = dah.removeDummy(iac);
                
                // Now we put the property back among the molecular ones
                StringBuilder sbMolProp = new StringBuilder();
                for (IAtom atm : iac.atoms())
                {
                    sbMolProp.append(" ").append(atm.getProperty(
                            DENOPTIMConstants.ATMPROPVERTEXID).toString());
                }
                iac.setProperty(DENOPTIMConstants.ATMPROPVERTEXID, 
                        sbMolProp.toString().trim());
                results.add(iac);
            } else {
                results.add(iac);
            }
        }

        return results;
    }

//------------------------------------------------------------------------------

    /**
     * Generate 3D structure by assembling 3D fragments according to 
     * attachment point vector and following the tree-like structure of the 
     * graph.
     *
     * @throws DENOPTIMException
     */

    public ChemicalObjectModel build3DTree() throws DENOPTIMException
    {
        // Create 3D tree-like structure
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, 
                settings.getRandomizer());
        IAtomContainer initMol = tb.convertGraphTo3DAtomContainer(molGraph);
       
        // Reorder atoms and clone molecule.
        AtomOrganizer oa = new AtomOrganizer();
        oa.setScheme(settings.getAtomOrderingScheme());
        int seedAtm = 0;
        IAtomContainer reorderedMol = oa.reorderStartingFrom(seedAtm, initMol);
        ArrayList<Integer> newToOldMap = oa.getNewToOldOrder(seedAtm);
        ArrayList<Integer> oldToNewMap = oa.getOldToNewOrder(seedAtm);
        logger.log(Level.FINEST, "oldToNewMap: "+oldToNewMap);
        
        // Collect rotatable bonds defined by fragment-fragment connections
        ArrayList<ObjectPair> rotBonds = RotationalSpaceUtils
                .defineRotatableBonds(reorderedMol,
                        ((FragmentSpaceParameters) settings.getParameters(
                                ParametersType.FS_PARAMS)).getRotSpaceDefFile(),
                        true, true, settings.getLogger());
        
        logger.log(Level.FINE, "Rotatable bonds: "+rotBonds);
        if (logger.isLoggable(Level.FINEST))
        {
            logger.log(Level.FINEST, "Reordered IAtomContainer: 'iacToIC.sdf'");
            DenoptimIO.writeSDFFile("iacToIC.sdf",reorderedMol,false);
        }

        // Generate Internal Coordinates
        TinkerMolecule tmol = TinkerUtils.getICFromIAC(reorderedMol, 
                settings.getTinkerMap());
        TinkerUtils.setTinkerTypes(tmol, settings.getTinkerMap());

        // Generate combined molecular representations (both XYZ and INT)
        return new ChemicalObjectModel(
                molGraph,
                reorderedMol,
                tmol,
                molName,
                rotBonds,
                oldToNewMap,
                newToOldMap
        );
    }

//------------------------------------------------------------------------------

}
