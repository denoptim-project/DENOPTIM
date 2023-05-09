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

package denoptim.graph.rings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.Bond;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.graph.matrix.TopologicalMatrix;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RingClosingUtils;


/**
 * Tool box for determining whether a chain of atoms, i.e., a path, can be 
 * folded as to form a ring-closing bond that transforms the open chain in a 
 * ring.
 *
 * @author Marco Foscato 
 */

public class PathClosabilityTools
{
  
//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path in a graph.
     * The criteria defining the closability
     * condition are controlled by the given settings.
     * @param subGraph the subgraph representing the potentially closable path 
     * in the graph.
     * @param mol the molecule corresponding to the graph. This is only used to 
     * provide the molecular constitution and does not require 3D coordinates.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    public static boolean isCloseable(PathSubGraph subGraph,
            IAtomContainer mol, RingClosureParameters settings) 
    {
        boolean closable = false;
        switch (settings.getClosabilityEvalMode())
        {
            case -1:
            	closable = true; // ring size has been evaluated before
            	break;
            case 0:
                closable = evaluateConstitutionalClosability(subGraph, mol, 
                        settings);
                break;
            case 1:
                closable = evaluate3DPathClosability(subGraph, mol, settings);
                break;
            case 2:
                closable = evaluateConstitutionalClosability(subGraph, mol,
                        settings) && evaluate3DPathClosability(subGraph, mol, 
                                settings);
                break;
            default:
                String s = "Unrecognized closability evaluation mode";
                throw new IllegalArgumentException(s);
        }
        return closable;
    }

//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path considering only
     * its constitution.
     * @param subGraph the subgraph representing the path in the graph.
     * @param iac the molecular representation. 
     * This is only used to provide the molecular
     * constitution and does not require 3D coordinates. 
     * This will not be altered.
     * @param settings the parameters pertaining ring closures.
     * @return <code>true</code> is the path is constitutionally closable
     */

    private static boolean evaluateConstitutionalClosability(
            PathSubGraph subGraph, IAtomContainer iac, 
            RingClosureParameters settings)
    {
        settings.getLogger().log(Level.FINE, "Evaluating constitutional "
                + "closability of path: " + subGraph.getVertecesPath());

        // Get a working copy of the molecular container
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer mol = builder.newAtomContainer();
        try
        {
            mol = iac.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new IllegalArgumentException(e);
        }
        MoleculeUtils.removeRCA(mol);
        
        // Identify atoms of molecular representation that correspond to
        // this path of vertices
        Map<Vertex,ArrayList<Integer>> vIdToAtmId =
                MoleculeUtils.getVertexToAtomIdMap(
                        (ArrayList<Vertex>) subGraph.getVertecesPath(),
                        mol);
        List<Integer> atmIdsInVrtxPath = new ArrayList<Integer>();
        for (Vertex v : subGraph.getVertecesPath())
        {
            atmIdsInVrtxPath.addAll(vIdToAtmId.get(v));
        }

        // Find atoms to remove: keep only the atoms that belong to the 
        // vertex path and their neighbours
        ArrayList<IAtom> toRemove = new ArrayList<IAtom>();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            if (!atmIdsInVrtxPath.contains(i))
            {
                IAtom candAtm = mol.getAtom(i);
                boolean isNeighbour = false;
                List<IAtom> nbrs = mol.getConnectedAtomsList(candAtm);
                for (IAtom nbrAtm : nbrs)
                {
                    if (atmIdsInVrtxPath.contains(mol.indexOf(nbrAtm)))
                    {
                        isNeighbour = true;
                        break;
                    }
                }
                if (!isNeighbour)
                {
                    toRemove.add(candAtm);
                }
            }
        }
        // Deal with RCAs: head and tail vertices
        IAtom atmH = mol.getAtom(vIdToAtmId.get(subGraph.getHeadVertex()).get(0));
        IAtom atmT = mol.getAtom(vIdToAtmId.get(subGraph.getTailVertex()).get(0));
        IAtom srcH = mol.getConnectedAtomsList(atmH).get(0);
        IAtom srcT = mol.getConnectedAtomsList(atmT).get(0);
        int iSrcH = mol.indexOf(srcH);
        int iSrcT = mol.indexOf(srcT);
        toRemove.add(atmH);
        toRemove.add(atmT);
        
        BondType bndTyp = subGraph.getEdgesPath().get(0).getBondType();
        if (bndTyp.hasCDKAnalogue())
        {
            mol.addBond(iSrcH, iSrcT, bndTyp.getCDKOrder());
        } else {
            settings.getLogger().log(Level.WARNING, 
                    "WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        // Remove atoms
        for (IAtom a : toRemove)
        {
            mol.removeAtom(a);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Molecular representation of path includes:");
        for (IAtom a : mol.atoms())
        {
            sb.append("  " + a.getSymbol() + mol.indexOf(a) + " " 
                    + a.getProperties());
        }
        settings.getLogger().log(Level.FINEST, sb.toString());

        boolean closable = false;

        // Evaluate requirement based on elements contained in the ring
        boolean spanRequiredEls = false;
        Set<String> reqRingEl = settings.getRequiredRingElements();
        if (reqRingEl.size() != 0)
        {
            // Prepare shortest atom path 
            List<IAtom> atomsPath = new ArrayList<IAtom>();
            ShortestPaths sp = new ShortestPaths(mol, srcH);
            atomsPath = new ArrayList<IAtom>(Arrays.asList(sp.atomsTo(srcT)));

            // Look for the required elements
            for (String el : reqRingEl)
            {
                for (IAtom a : atomsPath)
                {
                    if (MoleculeUtils.getSymbolOrLabel(a).equals(el))
                    {
                        spanRequiredEls = true;
                        break;
                    }
                }
                if (spanRequiredEls)
                {
                    break;
                }
            }
            if (!spanRequiredEls)
            {
                settings.getLogger().log(Level.FINER, 
                        "Candidate ring doesn't involve any among the required "
                        + "elements.");
                return false;
            }
            closable = true;
        }

        // Try to find a match for any of the SMARTS queries
        Map<String,String> smarts = settings.getConstitutionalClosabilityConds();
        if (smarts.size() != 0)
        {
            closable = false;
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
            if (msq.hasProblems())
            {
                String msg = "Attempt to match SMARTS for "
                             + "constitution-based ring-closability conditions "
                             + "returned an error! Ignoring " + msq.getMessage();
                settings.getLogger().log(Level.WARNING,msg);
            }
            for (String name : smarts.keySet())
            {
                if (msq.getNumMatchesOfQuery(name) > 0)
                {
                    settings.getLogger().log(Level.FINER,
                            "Candidate closable path matches constitutional "
                            + "closability criterion: " + smarts.get(name));
                    closable = true;
                    break;
                }
            }
        }

        settings.getLogger().log(Level.FINE, 
                "Contitutional closability: " + closable);
        return closable;
    }

//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path in a graph 
     * representing a molecule. Since this is a computationally demanding
     * task, this method makes use of serialized data that is updated 
     * on the fly.
     * @param subGraph the subgraph representing the path in the graph.
     * @param mol the molecular representation of the system represented by
     * the whole graph, not by the subgraph. 
     * This is only used to provide the molecular
     * constitution and does not require 3D coordinates. 
     * This will not be altered.
     * @param settings the parameters pertaining ring closures.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    private static boolean evaluate3DPathClosability(PathSubGraph subGraph, 
            IAtomContainer mol, RingClosureParameters settings) 
    {
        String chainId = subGraph.getChainID();
        settings.getLogger().log(Level.FINE, 
                "Evaluating 3D closability of path: " 
                        + subGraph.getVertecesPath()+" ChainID: "+chainId);
        
        RingClosuresArchive rca = settings.getRingClosuresArchive();
        
        RingClosingConformations rcc;
        boolean closable = false;
        
        String foundID = rca.containsChain(subGraph);
        if (foundID != "")
        {
            // Get all info from archive
            closable = rca.getClosabilityOfChain(foundID);
            rcc = rca.getRCCsOfChain(foundID);
            if (settings.checkInterdependentChains() 
                    && settings.doExhaustiveConfSrch())
            {
                try
                {
                    subGraph.makeMolecularRepresentation(mol, false,
                            settings.getLogger(), settings.getRandomizer());
                } catch (DENOPTIMException e)
                {
                    settings.getLogger().warning("Could not create 3D model "
                            + "for potentially closeable path that will be "
                            + "considered not ring-closable.");
                    return false;
                }
                subGraph.setRCC(rcc);        
            }
        }
        else
        {
            // Need to generate 3D molecular representation
            try {
                subGraph.makeMolecularRepresentation(mol, true,
                    settings.getLogger(), settings.getRandomizer());
            } catch (DENOPTIMException e)
            {
                settings.getLogger().warning("Could not create 3D model "
                        + "for potentially closeable path that will be "
                        + "considered not ring-closable.");
                return false;
            }
            
            List<IAtom> atomsPath = subGraph.getAtomPath();
            List<IBond> bondsPath = subGraph.getBondPath();

            // Define rotatability
            ArrayList<Boolean> rotatability = new ArrayList<Boolean>();
            for (int i=0; i < bondsPath.size(); i++)
            {
                IBond bnd = bondsPath.get(i);
                Object rotFlag = bnd.getProperty(
                                         DENOPTIMConstants.BONDPROPROTATABLE);
                if (rotFlag == null || !Boolean.valueOf(rotFlag.toString()))
                {
                    rotatability.add(false);
                }
                else
                {
                    rotatability.add(true);
                }
            }
            settings.getLogger().log(Level.FINE, "Rotatability: "+rotatability); 
            
            // Define initial values of dihedrals
            // NOTE: these angles are not calculated on the atoms of the chain,
            //       but on the reference points that are unique for each bond
            //       This should allow to compare conformations of different
            //       chains that share one or more bonds
            ArrayList<ArrayList<Point3d>> dihRefs = 
                                               subGraph.getDihedralRefPoints();
            if (dihRefs.size() != rotatability.size()-2)
            {
                throw new IllegalStateException("Number of bonds and number of "
                        + "dihidrals angles are inconsistent in PathSubGraph."
                        + " Contact the author.");
            }

            // find ring closing conformations
            ArrayList<ArrayList<Double>> closableConfs = 
                                        new ArrayList<ArrayList<Double>>();
            closable = RingClosureFinder.evaluateClosability(atomsPath,
                                                         rotatability,
                                                         dihRefs,
                                                         closableConfs,
                                                         settings);

            // store in object graph
            rcc = new RingClosingConformations(chainId, closableConfs);
            subGraph.setRCC(rcc);

            // put ring-closure information in archive for further use
            rca.storeEntry(chainId,closable,rcc);
        }

        settings.getLogger().log(Level.FINE, "Path closablility: "+closable);
        
        return closable;
    }
    
//-----------------------------------------------------------------------------

}
