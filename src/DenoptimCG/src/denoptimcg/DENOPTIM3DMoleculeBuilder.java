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

package denoptimcg;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.integration.tinker.TinkerException;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.rings.RingClosureParameters;
import denoptim.threedim.ThreeDimTreeBuilder;
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
public class DENOPTIM3DMoleculeBuilder
{
    private String molName;
    private DENOPTIMGraph molGraph;
    
    private boolean debug = CGParameters.debug();
    private static int verbosity = CGParameters.getVerbosity();

//------------------------------------------------------------------------------

    public DENOPTIM3DMoleculeBuilder(String molName, DENOPTIMGraph molGraph)
    {
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
        String msg = "Building Multiple 3D representations for "
                 + "graph = " + molGraph.toString();
        System.out.println(msg);

        // Generate XYZ and INT representations
        long startTime = System.nanoTime();
        Molecule3DBuilder mol = build3DTree();
        long endTime = System.nanoTime();
        long time = (endTime - startTime);
        if (verbosity > 0)
        {
            msg = "TIME (build 3D model): " + time/1000000 + " ms"
                  + " #frags: " + mol.getGraph().getVertexList().size() 
                  + " #atoms: " + mol.getIAtomContainer().getAtomCount();
            System.out.println(msg);
        }

        // Evaluate source of isomerism
        // 1: Attempt Ring Closures 
        RingClosureTool rct = new RingClosureTool();
        ArrayList<Molecule3DBuilder> rcMols =new ArrayList<Molecule3DBuilder>();
        boolean skipConfSearch = false;
        if (RingClosureParameters.allowRingClosures() && 
            mol.getGraph().getRings().size() != 0)
        {
            startTime = System.nanoTime();
            rcMols = rct.attemptAllRingClosures(mol);
            endTime = System.nanoTime();
            time = (endTime - startTime);
            int numAllClosedCombs = 0;
            for (Molecule3DBuilder rcMol : rcMols)
            {
                Object o = rcMol.getIAtomContainer().getProperty(
                        DENOPTIMConstants.MOLERRORTAG);
                if (o == null)
                    numAllClosedCombs++;
            }
            if (verbosity > 0)
            {
                msg = "TIME (close ring): "+time/1000000+" ms"
                      + " #frags: " + mol.getGraph().getVertexList().size()
                      + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                      + " #rings: " + mol.getGraph().getRings().size()
                      + " #rcaCombs: " + mol.getRCACombinations().size()
                      + " #allClosedRCSCombs: " + numAllClosedCombs;
                System.out.println(msg);
            }
            if (RingClosureParameters.requireCompleteRingclosure 
                    && numAllClosedCombs<1)
            {
                msg = "No fully closed RCS combinaton. Nothing to send to "
                        + "conformational search.";
                System.out.println(msg);
                skipConfSearch = true;
            }
        }
        else
        {
            Molecule3DBuilder nMol = mol.deepcopy();
            rct.saturateRingClosingAttractor(nMol);
            rcMols = new ArrayList<Molecule3DBuilder>();
            rcMols.add(nMol);
        }


        // 2: Conformational search (if possible)
        ArrayList<Molecule3DBuilder> csMols = new ArrayList<Molecule3DBuilder>();
        if (skipConfSearch)
        {
            csMols.addAll(rcMols);
        } else {
            ConformationalSearchPSSROT csPssRot = new ConformationalSearchPSSROT();
            startTime = System.nanoTime();
            csMols = csPssRot.performPSSROT(rcMols);
            endTime = System.nanoTime();
            time = (endTime - startTime);
            if (verbosity > 0)
            {
                msg = "TIME (conf. search): "+time/1000000+" ms"
                      + " #frags: " + mol.getGraph().getVertexList().size()
                      + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                      + " #rotBnds: " + mol.getRotatableBonds().size();
                System.out.println(msg);
            }
        }

        // Convert and return results
        ArrayList<IAtomContainer> results = new ArrayList<IAtomContainer>();
        DummyAtomHandler dah = new DummyAtomHandler(
                DENOPTIMConstants.DUMMYATMSYMBOL);
        for (Molecule3DBuilder mol3db : csMols)
        {
            IAtomContainer iac = mol3db.getIAtomContainer();
            
            //TODO-V3 make reordering optional
            
            IAtomContainer originalOrderMol = AtomOrganizer.makeReorderedCopy(
                    iac, mol3db.getOldToNewOrder(), mol3db.getNewToOldOrder());
            iac = originalOrderMol;
            
            
            if (!CGParameters.getKeepDummyFlag())
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

    public Molecule3DBuilder build3DTree() throws DENOPTIMException
    {
        // Create 3D tree-like structure
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder();
        IAtomContainer initMol = tb.convertGraphTo3DAtomContainer(molGraph);
       
        // Reorder atoms and clone molecule.
        AtomOrganizer oa = new AtomOrganizer();
        oa.setScheme(CGParameters.getAtomOrderingScheme());
        int seedAtm = 0;
        IAtomContainer reorderedMol = oa.reorderStartingFrom(seedAtm, initMol);
        ArrayList<Integer> newToOldMap = oa.getNewToOldOrder(seedAtm);
        ArrayList<Integer> oldToNewMap = oa.getOldToNewOrder(seedAtm);
        if (debug)
        {
            System.out.println("oldToNewMap: "+oldToNewMap);
        }
        
        // Collect rotatable bonds defined by fragment-fragment connections
        ArrayList<ObjectPair> rotBonds = 
                         RotationalSpaceUtils.defineRotatableBonds(reorderedMol,
                        FragmentSpaceParameters.getRotSpaceDefFile(),true,true);
        if  (verbosity > 0)
        {
            System.out.println("Rotatable bonds: "+rotBonds);
            if (debug)
            {
                System.out.println("Reordered IAtomContainer: 'iacToIC.sdf'");
                DenoptimIO.writeMolecule("iacToIC.sdf",reorderedMol,false);
                GenUtils.pause();
            }
        }

        // Generate Internal Coordinates
        TinkerMolecule tmol = TinkerUtils.getICFromIAC(reorderedMol,
                                                   CGParameters.getTinkerMap());
        TinkerUtils.setTinkerTypes(tmol,CGParameters.getTinkerMap());

        // Generate combined molecular representations (both XYZ and INT)
        return new Molecule3DBuilder(
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
