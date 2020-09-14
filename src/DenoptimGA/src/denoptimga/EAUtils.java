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

package denoptimga;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.mcss.RMap;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.IGraphBuildingBlock;
import denoptim.molecule.SymmetricSet;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;
import denoptim.rings.RingClosuresArchive;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.DENOPTIMStatUtils;
import denoptim.utils.FragmentUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RandomUtils;
import denoptim.utils.RotationalSpaceUtils;


/**
 * Helper methods for the genetic algorithm
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class EAUtils
{
    // cluster the fragments based on their #APs
    protected static HashMap<Integer, ArrayList<Integer>> fragmentPool;
    
    protected static DecimalFormat df = new DecimalFormat();

    // for each fragment store the reactions associated with it
    protected static HashMap<Integer, ArrayList<String>> lstFragmentClass;

    // flag for debugging
    private static final boolean DEBUG = false;

//------------------------------------------------------------------------------

    /**
     * Write out summary for the current GA population
     * @param popln
     * @param filename
     * @throws DENOPTIMException
     */

    protected static void outputPopulationDetails
                            (ArrayList<DENOPTIMMolecule> popln, String filename)
                                                        throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);

	//Headers
        sb.append(String.format("%-20s", "#Name "));
        sb.append(String.format("%-20s", "GraphId "));
        sb.append(String.format("%-30s", "UID "));
        sb.append(String.format("%-15s","Fitness "));
	sb.append("Source ");
        sb.append(System.getProperty("line.separator"));

        df.setMaximumFractionDigits(GAParameters.getPrecisionLevel());
        df.setMinimumFractionDigits(GAParameters.getPrecisionLevel());

        for (int i=0; i<GAParameters.getPopulationSize(); i++)
        {
            DENOPTIMMolecule mol = popln.get(i);
            if (mol != null)
            {
                String mname = new File(mol.getMoleculeFile()).getName();
                if (mname != null)
                    sb.append(String.format("%-20s", mname));
                sb.append(String.format("%-20s", 
					  mol.getMoleculeGraph().getGraphId()));
                sb.append(String.format("%-30s", mol.getMoleculeUID()));
                sb.append(df.format(mol.getMoleculeFitness()));
		sb.append("    ").append(mol.getMoleculeFile());
                sb.append(System.getProperty("line.separator"));
            }
        }

        // calculate descriptive statistics for the population
        String stats = getSummaryStatistics(popln);
        if (stats.trim().length() > 0)
            sb.append(stats);
        DenoptimIO.writeData(filename, sb.toString(), false);

        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    private static String getSummaryStatistics(ArrayList<DENOPTIMMolecule> popln)
    {
        double[] fitness = getFitnesses(popln);
        double sdev = DENOPTIMMathUtils.stddevp(fitness);
        String res = "";
        df.setMaximumFractionDigits(GAParameters.getPrecisionLevel());
        
        String floatForm = "%12." + GAParameters.getPrecisionLevel() + "f";

        if (sdev > 0.0001)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("\n\n#####POPULATION SUMMARY#####\n");
            int n = popln.size();
            sb.append(String.format("%-30s", "SIZE:")).append(String.format("%12s", n));
            sb.append(System.getProperty("line.separator"));
            double f;
            f = DENOPTIMStatUtils.max(fitness);
            sb.append(String.format("%-30s", "MAX:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.min(fitness);
            sb.append(String.format("%-30s", "MIN:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.mean(fitness);
            sb.append(String.format("%-30s", "MEAN:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.median(fitness);
            sb.append(String.format("%-30s", "MEDIAN:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.stddev(fitness, true);
            sb.append(String.format("%-30s", "STDDEV:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.skewness(fitness, true);
            sb.append(String.format("%-30s", "SKEW:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));

            int sz = FragmentSpace.getScaffoldLibrary().size();
            HashMap<Integer, Integer> scf_cntr = new HashMap<>();
            for (int i=1; i<=sz; i++)
            {
                scf_cntr.put(i, 0);
            }

            for (int i=0; i<GAParameters.getPopulationSize(); i++)
            {
                DENOPTIMMolecule mol = popln.get(i);
                DENOPTIMGraph g = mol.getMoleculeGraph();
                int scafIdx = g.getVertexAtPosition(0).getMolId() + 1;
                scf_cntr.put(scafIdx, scf_cntr.get(scafIdx)+1);
            }

            sb.append("\n\n#####SCAFFOLD ANALYSIS#####\n");
            for (Map.Entry pairs : scf_cntr.entrySet())
            {
                sb.append(pairs.getKey()).append(" ").append(pairs.getValue()).append(System.getProperty("line.separator"));
            }
            res = sb.toString();
            sb.setLength(0);
        }
        return res;
    }
    
//------------------------------------------------------------------------------

    /**
     * Selects a single parent  using the scheme specified.
     * @param molPopulation
     * @return the index of the parent
     */

    protected static int selectSingleParent(ArrayList<DENOPTIMMolecule> popln)
    {
        int selmate = -1;
        int stype = GAParameters.getSelectionStrategyType();

        //MersenneTwister rng = GAParameters.getRNG();
        MersenneTwister rng = RandomUtils.getRNG();

        int[] mates = null;

        switch (stype)
        {
        case 1:
            mates = SelectionHelper.performTournamentSelection(rng, popln, 2);
            break;
        case 2:
            mates = SelectionHelper.performRWS(rng, popln, 2);
            break;
        case 3:
            mates = SelectionHelper.performSUS(rng, popln, 2);
            break;
        case 4:
            mates = SelectionHelper.performRandomSelection(rng, popln, 2);
            break;
        }


        selmate = mates[rng.nextInt(2)];

        return selmate;
    }
    
//------------------------------------------------------------------------------

    /**
     * Selects two parents for crossover using the scheme specified.
     * @param molPopulation
     * @return array of parents for crossover
     */

    protected static int[] selectParents(ArrayList<DENOPTIMMolecule> molPopulation)
    {
        int[] mates = null;
        int stype = GAParameters.getSelectionStrategyType();

        //MersenneTwister rng = GAParameters.getRNG();
        MersenneTwister rng = RandomUtils.getRNG();

        if (!FragmentSpace.useAPclassBasedApproach())
        {
            switch (stype)
            {
            case 1:
                mates = SelectionHelper.performTournamentSelection
                                                (rng, molPopulation, 2);
                break;
            case 2:
                mates = SelectionHelper.performRWS
                                                (rng, molPopulation, 2);
                break;
            case 3:
                mates = SelectionHelper.performSUS
                                                (rng, molPopulation, 2);
                break;
            case 4:
                mates = SelectionHelper.performRandomSelection
                                                (rng, molPopulation, 2);
                break;
            }
        }
        else
        {
            // select compatible parents
            mates = EAUtils.performFBCC(molPopulation);
        }

        return mates;
    }    
              
//------------------------------------------------------------------------------

    /**
     * perform fitness based class compatible selection of parents
     * @param molPopulation list of molecules
     * @param sz size of the pool
     * @param stype
     * @return indices of the parents that have at least 1 compatible Xover point
     */

    protected static int[] performFBCC(ArrayList<DENOPTIMMolecule> molPopulation)
    {
        int[] selection = new int[2];
        selection[0] = -1;
        selection[1] = -1;

        // first select 1st parent through whatever scheme is applied
        int p1 = selectSingleParent(molPopulation);
        
        if (p1 != -1)
        {
            selection[0] = p1;

            // loop through the rest of the population, break when a member shares
            // at least 1 compatible point
            // since the population is sorted by fitness, this routine will pick 
            // the fitter parent, although the first parent may be less fit

            DENOPTIMGraph g1 = molPopulation.get(p1).getMoleculeGraph();

            ArrayList<Integer> indices = new ArrayList<>();

            for (int i=0; i<molPopulation.size(); i++)
            {
                if (i == p1)
                    continue;
                DENOPTIMGraph g2 = molPopulation.get(i).getMoleculeGraph();
                RMap rp = DENOPTIMGraphOperations.locateCompatibleXOverPoints(g1, g2);
                if (rp != null)
                {
                    indices.add(i);
                }
            }

            if (indices.isEmpty())
            {
                selection[0] = -1;
                selection[1] = -1;
                return selection;
            }
        
            switch (indices.size()) 
            {
                case 1:
                    selection[1] = indices.get(0);
                    break;
                case 2:
                    // select the fitter of the 2
                    selection[1] = indices.get(0);
                    break;
                default:
                    //MersenneTwister rng = GAParameters.getRNG();
                    MersenneTwister rng = RandomUtils.getRNG();
                    selection[1] = indices.get(rng.nextInt(indices.size()));
                    break;
            }
            indices.clear();
        }
        
        return selection;
    }
                            
                            
//------------------------------------------------------------------------------

    /**
     * Perform a mutation such as deletion, append or substitution
     * @param molGraph
     * @return <code>true</code> if the mutation is successful
     * @throws DENOPTIMException
     */

    protected static boolean performMutation(DENOPTIMGraph molGraph)
                                                    throws DENOPTIMException
    {
        String msg;

        //System.err.println("performMutation " + molGraph.getGraphId());

        // This is done to maintain a unique vertex-id mapping
        GraphUtils.renumberGraphVertices(molGraph);

        if (FragmentSpace.useAPclassBasedApproach())
        {
            // remove the capping groups
            GraphUtils.removeCappingGroups(molGraph);
        }

        boolean status;

        MersenneTwister rng = RandomUtils.getRNG();
        
        int rnd = rng.nextInt(2);

        int k = rng.nextInt(molGraph.getVertexCount());
        if (k == 0)
            k = k + 1; // not selecting the first vertex
        DENOPTIMVertex molVertex = molGraph.getVertexAtPosition(k);


        switch (rnd) 
        {
            case 0:
                // substitute vertex
                // select vertex to substitute
                //System.err.println("FRAGMENT SUBSTITUTION");
                
                msg = "Performing fragment substitution mutation\n";
                DENOPTIMLogger.appLogger.info(msg);
                status = DENOPTIMGraphOperations.
                        substituteFragment(molGraph, molVertex);
                if (status)
                {
                    msg = "Fragment substitution successful.\n";
                    //molGraph.setMsg(molGraph.getMsg() + " <> Substitution");
                }
                else
                    msg = "Fragment substitution unsuccessful.\n";
                DENOPTIMLogger.appLogger.info(msg);
                break;
            case 1:
                //System.err.println("FRAGMENT APPEND");
                
                msg = "Performing fragment append.\n";
                DENOPTIMLogger.appLogger.info(msg);
		// nerer done directly on the scaffold -> symmetry falg = false
                status = DENOPTIMGraphOperations.
                        extendGraph(molGraph, molVertex, false, false);
                if (status)
                {
                    msg = "Fragment append successful.\n";
                    //molGraph.setMsg(molGraph.getMsg() + " <> Append");
                }
                else
                    msg = "Fragment append unsuccessful.\n";
                DENOPTIMLogger.appLogger.info(msg);
                break;
            default:
                // select delete vertex
                
                //System.err.println("FRAGMENT DELETION");
                
                msg = "Performing vertex deletion\n";
                DENOPTIMLogger.appLogger.info(msg);
                // Deletion
                status = DENOPTIMGraphOperations.deleteFragment(molGraph,molVertex);
                if (status)
                {
                    msg = "Vertex deletion successful.\n";
                    //molGraph.setMsg(molGraph.getMsg() + " <> Deletion");
                }
                else
                    msg = "Vertex deletion unsuccessful.\n";
                DENOPTIMLogger.appLogger.info(msg);
                break;
        }
        return status;
    }


//------------------------------------------------------------------------------

    /**
     * Simply copies the files from the previous directories into the specified
     * folder.
     * @param popln the final list of best molecules
     * @param destDir the name of the output directory
     */

    protected static void outputFinalResults(ArrayList<DENOPTIMMolecule> popln,
                            String destDir) throws DENOPTIMException
    {
        String genOutfile = destDir + System.getProperty("file.separator") +
                                "Final.txt";

        File fileDir = new File(destDir);

        try
        {
            for (int i=0; i<GAParameters.getPopulationSize(); i++)
            {
                String sdfile = popln.get(i).getMoleculeFile();
                String imgfile = popln.get(i).getImageFile();

                if (sdfile != null)
                {
                    FileUtils.copyFileToDirectory(new File(sdfile), fileDir);
		}
                if (imgfile != null)
                {
                    FileUtils.copyFileToDirectory(new File(imgfile), fileDir);
                }
            }
            outputPopulationDetails(popln, genOutfile);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reconstruct the molecular population from the file.
     * @param filename
     * @param molPopulation
     * @param lstInchi
     * @throws DENOPTIMException
     */
    protected static void getPopulationFromFile(String filename,
            ArrayList<DENOPTIMMolecule> molPopulation, HashSet<String> lstInchi,
            String genDir) throws DENOPTIMException
    {
        ArrayList<IAtomContainer> mols;
        if (GenUtils.getFileExtension(filename).compareToIgnoreCase(".sdf") == 0)
        {
            mols = DenoptimIO.readSDFFile(filename);
        }
        // process everything else as a text file with links to individual molecules
        else
        {
            mols = DenoptimIO.readLinksToMols(filename);
        }

        String fsep = System.getProperty("file.separator");

	HashSet<String> uidsFromInitPop = new HashSet();
        for (int i=0; i<mols.size(); i++)
        {
            DENOPTIMGraph graph = null;
            double fitness = 0, ad = 0;
            String molsmiles = null, molinchi = null, molfile = null;

            IAtomContainer mol = mols.get(i);
            Object apProperty = mol.getProperty("GraphENC");
            if (apProperty != null)
            {
                graph = GraphConversionTool.getGraphFromString(apProperty.toString().trim());
            }
            else
            {
                DENOPTIMLogger.appLogger.log(Level.SEVERE,
                        "Molecule does not have the DENOPTIMGraph encoding.");
                throw new DENOPTIMException(
                        "Molecule does not have the DENOPTIMGraph encoding.");
            }

            apProperty = mol.getProperty("FITNESS");
            if (apProperty != null)
            {
                fitness = Double.parseDouble(apProperty.toString());
            }
            else
            {
                DENOPTIMLogger.appLogger.log(Level.SEVERE,
                            "Molecule does not have the associated fitness.");
                throw new DENOPTIMException(
                            "Molecule does not have the associated fitness.");
            }

            apProperty = mol.getProperty("SMILES");
            if (apProperty != null)
            {
                molsmiles = apProperty.toString().trim();
            }
            else
            {
                molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            }

            apProperty = mol.getProperty("InChi");
            if (apProperty != null)
            {
                molinchi = apProperty.toString();
            }
            if (mol.getProperty("UID") != null)
            {
                molinchi = mol.getProperty("UID").toString();
            }
            else
            {
                ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
                if (pr.getFirst() != null)
                {
                    molinchi = pr.getFirst().toString();
                }
            }

	    // Add molecule to population, unless it has previously known UID
            if (lstInchi.add(molinchi))
            {
                int ctr = GraphUtils.getUniqueMoleculeIndex();
                String molName = "M" + GenUtils.getPaddedString(8, ctr);
                molfile = genDir + fsep + molName + "_FIT.sdf";

                int gctr = GraphUtils.getUniqueGraphIndex();
                graph.setGraphId(gctr);
                graph.setMsg("NEW");
                mol.setProperty("GCODE", gctr);
                mol.setProperty(CDKConstants.TITLE, molName);
                mol.setProperty("GraphENC", graph.toString());
                mol.setProperty("GraphMsg", "From Initial Population File");

                DENOPTIMMolecule pmol =
                    new DENOPTIMMolecule(graph, molinchi, molsmiles, fitness);
                DenoptimIO.writeMolecule(molfile, mol, false);
                pmol.setMoleculeFile(molfile);
                pmol.setImageFile(null);
                molPopulation.add(pmol);
		uidsFromInitPop.add(molinchi);
            }
        }
	writeUID(GAParameters.getUIDFileOut(),uidsFromInitPop,true);

        if (molPopulation.isEmpty())
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                                        "No data found in file {0}", filename);
            throw new DENOPTIMException("No data found in file " + filename);
        }

        setVertexCounterValue(molPopulation);
    }

//------------------------------------------------------------------------------

    protected static void writeUID(String outfile, HashSet<String> lstInchi, 
                                            boolean append) throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(256);
        Iterator<String> iter = lstInchi.iterator();

	boolean  first = true;
        while(iter.hasNext())
        {
	    if (first)
	    {
                sb.append(iter.next());
		first = false;
	    }
	    else
	    {
                sb.append(System.getProperty("line.separator")).append(iter.next());
	    }
        }

        DenoptimIO.writeData(outfile, sb.toString(), append);
        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    /**
     * Set the Vertex counter value
     * @param popln
     */

    protected static void setVertexCounterValue(ArrayList<DENOPTIMMolecule> popln)
    {
        int val = Integer.MIN_VALUE;
        for (DENOPTIMMolecule popln1 : popln)
        {
            DENOPTIMGraph g = popln1.getMoleculeGraph();
            val = Math.max(val, g.getMaxVertexId());
        }
        GraphUtils.updateVertexCounter(val);
    }

//------------------------------------------------------------------------------

    protected static void updatePool(HashMap<Integer, ArrayList<Integer>> mp,
                                Integer key, Integer b)
    {
        if (mp.containsKey(key))
        {
            ArrayList<Integer> lst = mp.get(key);
            lst.add(b);
            mp.put(key, lst);
        }
        else
        {
            ArrayList<Integer> lst = new ArrayList<>();
            lst.add(b);
            mp.put(key, lst);
        }
    }

//------------------------------------------------------------------------------

    /**
     * pools fragments based on the number of attachment points.
     * This is done in order to make selection of fragments based on #APs easier
     * @param bbs the list of building blocks to analyze
     */
    protected static void poolFragments(ArrayList<IGraphBuildingBlock> bbs)
    {
        fragmentPool = new HashMap<>();

        for (int i=0; i<bbs.size(); i++)
        {
            IGraphBuildingBlock bb = bbs.get(i);
            int len = bb.getAPCount();
            if (len != 0)
                updatePool(fragmentPool, len, i);
        }
    }

//------------------------------------------------------------------------------

    /**
     * For each building block collect the reactions it is involved in
     * @param mols
     */

    protected static void poolAPClasses(ArrayList<IGraphBuildingBlock> bbs)
    {
        lstFragmentClass = new HashMap<>();
        for (int i=0; i<bbs.size(); i++)
        {
        	IGraphBuildingBlock bb = bbs.get(i);
            ArrayList<String> lstRcn = bb.getAllAPClassess();
            lstFragmentClass.put(i, lstRcn);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Select randomly a base fragment
     * @return index of a seed fragment
     */

    protected static int selectRandomScaffold()
    {
        if (FragmentSpace.getScaffoldLibrary().size() == 1)
            return 0;
        else
        {
            MersenneTwister rng = RandomUtils.getRNG();
            //return GAParameters.getRNG().nextInt(
            //            FragmentSpace.getScaffoldLibrary().size());
            return rng.nextInt(FragmentSpace.getScaffoldLibrary().size());
        }
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param query the reaction whose equivalents are to be identified
     * @return list of reaction matching the query
     */

    protected static ArrayList<String> findMatchingClass(String query)
    {
        HashMap<String, ArrayList<String>> mp = 
					 FragmentSpace.getCompatibilityMatrix();

        return mp.get(query);
    }

//------------------------------------------------------------------------------

    /**
     * select a fragment with a compatible reaction of lstReac
     * @param lstReac list of reactions that are compatible with the molecule
     * @return matching fragment index
     */
    @SuppressWarnings("empty-statement")
    protected static int selectRandomReactionFragment(ArrayList<String> lstReac)
    {
        // find fragments with compatible reactions
        ArrayList<IGraphBuildingBlock> mols = FragmentSpace.getFragmentLibrary();
        ArrayList<Integer> lstMols = new ArrayList<>();

        for (int i=0; i<mols.size(); i++)
        {
        	IGraphBuildingBlock mol = mols.get(i);
            ArrayList<String> mReac = mol.getAllAPClassess();
            // check reaction compatibility
            for (int j=0; j<mReac.size(); j++)
            {
                if (lstReac.contains(mReac.get(j)));
                {
                    if (!lstMols.contains(i))
                    {
                        lstMols.add(i);
                    }
                }
            }
        }

        if (lstMols.isEmpty())
        {
            return -1;
        }
        else if (lstMols.size() == 1)
        {
            return 0;
        }
        else
        {
            MersenneTwister rng = RandomUtils.getRNG();
            //int idx = GAParameters.getRNG().nextInt(lstMols.size());
            int idx = rng.nextInt(lstMols.size());
            return lstMols.get(idx);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Select randomly a fragment from the available list
     * @return the fragment index
     */

    protected static int selectRandomFragment()
    {
        if (FragmentSpace.getFragmentLibrary().size() == 1)
            return 0;
        else
        {
            MersenneTwister rng = RandomUtils.getRNG();
            //return GAParameters.getRNG().nextInt(
            //                    FragmentSpace.getFragmentLibrary().size());
            return rng.nextInt(FragmentSpace.getFragmentLibrary().size());

        }
    }

//------------------------------------------------------------------------------

    /**
     * @param numAP select a fragment with #APs less than numAP
     * @param equals select a fragment with #APs = numAP
     * @return the index of the molecule in the library. -1 if no molecule available
     */
    protected static int selectRandomFragment(int numAP, boolean equals)
    {
        int r = -1;
        
        MersenneTwister rng = RandomUtils.getRNG();

        // select fragment with APs = maxAP
        if (equals)
        {
            ArrayList<Integer> lst = fragmentPool.get(numAP);
            if (lst != null && lst.size() > 0)
            {
                //int j = GAParameters.getRNG().nextInt(lst.size());
                int j = rng.nextInt(lst.size());
                return lst.get(j);
            }
        }
        else
        {
            // select a fragment that has #APs < numAP
            // select from the fragment pool

            // need to do this in case, the pool does not have members
            // with the desired AP

            //System.err.println("Looking for frags with #AP: " + numAP);

            ArrayList<Integer> vlst = new ArrayList<>();

            if (numAP == 1)
            {
                ArrayList<Integer> lst = fragmentPool.get(numAP);
                if (lst != null && lst.size() > 0)
                {
                    //int j = GAParameters.getRNG().nextInt(lst.size());
                    int j = rng.nextInt(lst.size());
                    return lst.get(j);
                }
            }

            for (int k=numAP-1; k>0; k--)
            {
                ArrayList<Integer> lst = fragmentPool.get(k);
                if (lst != null)
                    vlst.add(k);
            }


            //System.err.println("Found vlst: " + vlst.size());

            if (!vlst.isEmpty())
            {
                //int l = GAParameters.getRNG().nextInt(vlst.size());
                int l = rng.nextInt(vlst.size());
                int k = vlst.get(l);
                ArrayList<Integer> lst = fragmentPool.get(k);

                //int j = GAParameters.getRNG().nextInt(lst.size());
                int j = rng.nextInt(lst.size());
                return lst.get(j);
            }
        }

        //System.err.println("NO fragment selected");

        return -1;
    }


//------------------------------------------------------------------------------

    /**
     * Graph construction starts with selecting a random core/scaffold.
     *
     * @return the molecular graph representation
     * @throws DENOPTIMException
     */

    protected static DENOPTIMGraph buildGraph() throws DENOPTIMException
    {
        DENOPTIMGraph molGraph = new DENOPTIMGraph();
        molGraph.setGraphId(GraphUtils.getUniqueGraphIndex());

        // building a molecule starts by selecting a random scaffold
        int scafIdx = selectRandomScaffold();

        ArrayList<DENOPTIMAttachmentPoint> scafAP = 
				      FragmentUtils.getAPForFragment(scafIdx,0);

        DENOPTIMVertex scafVertex = 
        new DENOPTIMVertex(GraphUtils.getUniqueVertexIndex(),scafIdx,scafAP, 0);
        // we set the level to -1, as the base
        scafVertex.setLevel(-1);
        
        //TODO: here again the issue of not having the symmetry set in the graph building blocks
        // it would be best to just ass the symmetric set to the building blocks, maybe. Consider it
        
        IGraphBuildingBlock mol = FragmentSpace.getScaffoldLibrary().get(scafIdx);
        // identify the symmetric APs if any for this fragment vertex
        ArrayList<SymmetricSet> simAP = FragmentUtils.getMatchingAP(mol,scafAP);
        scafVertex.setSymmetricAP(simAP);
        
        
        //TODO: did we pick a template? Then, we'll have to deal with it.
        // as we can pick a template in other graph operations, the dealing of the template
        // should be a public method or something that can be called from elsewhere
        
        
        // add the scaffold as a vertex
        molGraph.addVertex(scafVertex);
        molGraph.setMsg("NEW");

//TODO this works only for scaffolds at the moment. make the preference for 
// fragments that lead to known closable chains operate also when fragments are
// the "turning point".
        molGraph.setCandidateClosableChains(
                        RingClosuresArchive.getCCFromTurningPointId(scafIdx));

        if (DEBUG)
        {
            System.err.println(" ");
            System.err.println("START GRAPH: " + molGraph.getGraphId() 
                                + " scaffold: " + molGraph.toString());
        }

        DENOPTIMGraphOperations.extendGraph(molGraph, scafVertex, true, false);

        if (DEBUG)
        {
            System.err.println("AFTER EXTENSION: " + molGraph.toString());
        }

        if (molGraph.getVertexCount() > 1)
        {

            // add Capping if necessary
            addCappingGroup(molGraph);

            if (DEBUG)
            {
                System.err.println("AFTER CAPPING: " + molGraph.toString());
                //GenUtils.pause();
            }

            return molGraph;
        }

        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Select a compatible capping group
     * @param rcnCap
     * @return the index of capping group
     */

    protected static int getCappingFragment(String rcnCap)
    {
        if (rcnCap == null)
            return -1;

        ArrayList<Integer> reacFrags = getCompatibleCappingFragments(rcnCap);

        int fapidx = -1;
//TODO make random selection!?
        if (reacFrags.size() > 0)
        {
            fapidx = reacFrags.get(0);
        }

        return fapidx;
    }

//------------------------------------------------------------------------------

    /**
     * Retrieve a list of compatible capping groups
     * @param cmpReac
     * @return a list of compatible capping groups
     */

    protected static ArrayList<Integer> getCompatibleCappingFragments(
                                                                 String cmpReac)
    {
        ArrayList<Integer> lstFragIdx = new ArrayList<>();
        for (int i=0; i<FragmentSpace.getCappingLibrary().size(); i++)
        {
        	IGraphBuildingBlock mol = FragmentSpace.getCappingLibrary().get(i);
            ArrayList<String> lstRcn = mol.getAllAPClassess();
            if (lstRcn.contains(cmpReac))
                lstFragIdx.add(i);
        }

        return lstFragIdx;
    }

//------------------------------------------------------------------------------

    /**
     * Add a capping group fragment to the vertex at the specified
     * attachment point index
     * @param molGraph
     * @param curVertex
     * @param dapIdx
     * @return id of the vertex added; -1 if capping is not required
     * found
     */

    protected static int attachCappingFragmentAtPosition
                            (DENOPTIMGraph molGraph, DENOPTIMVertex curVertex,
                                int dapIdx) throws DENOPTIMException
    {
        int lvl = curVertex.getLevel();

        String rcn =  curVertex.getAttachmentPoints().get(dapIdx).getAPClass();
        // locate the capping group for this rcn
        String rcnCap = getCappingGroup(rcn);

        if (rcnCap != null)
        {

            int fid = getCappingFragment(rcnCap);

            if (fid != -1)
            {
                // for the current capping fragment get the list of APs
                ArrayList<DENOPTIMAttachmentPoint> fragAP = 
					 FragmentUtils.getAPForFragment(fid, 2);

                DENOPTIMVertex fragVertex =
                           new DENOPTIMVertex(GraphUtils.getUniqueVertexIndex(),
                                                                fid, fragAP, 2);
                // level 0 attachment
                fragVertex.setLevel(lvl+1);

                //Get the index of the AP of the capping group to use
                //(always the first and only AP)
                ArrayList<Integer> apIdx =
                        getCompatibleClassAPIndex(rcnCap, fragVertex);
                int dap = apIdx.get(0);

                DENOPTIMEdge edge = GraphUtils.connectVertices(
			       curVertex, fragVertex, dapIdx, dap, rcn, rcnCap);
                if (edge != null)
                {
                    // add the fragment as a vertex
                    molGraph.addVertex(fragVertex);

                    molGraph.addEdge(edge);

                    return fragVertex.getVertexId();
                }
                else
                {
                    String msg = "Unable to connect capping group "
                                     + fragVertex + " to graph" + molGraph;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                    throw new DENOPTIMException(msg);
                }
            }
            else
            {
                String msg = "Capping is required but no proper capping "
                                + "fragment found with APCalss " + rcnCap;
                DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                throw new DENOPTIMException(msg);
            }
        }

        return -1;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param rcn
     * @throws DENOPTIMException
     * @return For the given reaction return the corresponding capping group
     */

    protected static String getCappingGroup(String rcn) throws DENOPTIMException
    {
        String capRcn = null;

        if (FragmentSpace.getCappingMap().containsKey(rcn))
        {
            String rcns = FragmentSpace.getCappingMap().get(rcn);
            if (rcns.contains(","))
            {
                String[] st = rcns.split(",");
                MersenneTwister rng = RandomUtils.getRNG();
                //int k = GAParameters.getRNG().nextInt(st.length);
                int k = rng.nextInt(st.length);
                capRcn = st[k];
            }
            else
            {
                capRcn = rcns;
            }

            if (capRcn == null)
            {
                String msg = "Failure in reading APClass of capping group for "
                                 + rcn;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
        }
        else
        {
            String msg = "CPMap does not require capping for " + rcn;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
        }

        return capRcn;
    }

//------------------------------------------------------------------------------

    /**
     * Find a list of fragments which match the reaction scheme
     * @param cmpReac
     * @return a list of fragment indices that have reaction names equal to
     * cmpReac.
     */

    protected static ArrayList<Integer> getFragmentList(String cmpReac)
    {
        ArrayList<Integer> lstFragIdx = new ArrayList<>();

        Iterator<Map.Entry<Integer, ArrayList<String>>> entries =
                                    lstFragmentClass.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<Integer, ArrayList<String>> entry = entries.next();
            Integer key = entry.getKey();
            ArrayList<String> clst = entry.getValue();
            if (clst.contains(cmpReac))
            {
                if (!lstFragIdx.contains(key))
                    lstFragIdx.add(key);
            }
        }

        return lstFragIdx;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in a given graph and if
     * any ring can be closed choses one of the combinations of ring closures
     * that involves the highest number of new rings.
     * @param res an object array containing the inchi code, the smiles string
     * and the 2D representation of the molecule. This object can be
     * <code>null</code> if inchi/smiles/2D conversion fails.
     * @param molGraph the <code>DENOPTIMGraph</code> on which rings are to
     * be identified
     * @return <code>true</code> unless no ring can be set up even if required
     */

    protected static boolean setupRings(Object[] res, DENOPTIMGraph molGraph)
                                                    throws DENOPTIMException
    {
        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        if (!rcnEnabled)
            return true;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        IAtomContainer mol = 
            GraphConversionTool.convertGraphToMolecule(molGraph, false);

        // Set rotatability property as property of IBond
        ArrayList<ObjectPair> rotBonds = 
                                 RotationalSpaceUtils.defineRotatableBonds(mol,
                                  FragmentSpaceParameters.getRotSpaceDefFile(),
                                                                   true, true);
        
	// get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(
                                      FragmentSpace.getScaffoldLibrary(),
                                      FragmentSpace.getFragmentLibrary(),
                                      FragmentSpace.getCappingLibrary(),
                                      FragmentSpace.getRCCompatibilityMatrix());

//TODO decide to make optional
        boolean onlyRandomCombOfRings = true;
        if (onlyRandomCombOfRings)
        {
            Set<DENOPTIMRing> combsOfRings = cgh.getRandomCombinationOfRings(
                                                                          mol,
                                                                    molGraph);
            if (combsOfRings.size() > 0)
            {
                for (DENOPTIMRing ring : combsOfRings)
                {
                    molGraph.addRing(ring);
                }
            }
        }
        else
        {
            ArrayList<Set<DENOPTIMRing>> allCombsOfRings = 
                            cgh.getPossibleCombinationOfRings(mol, molGraph);
        
            if (DEBUG)
            {
                System.out.println("Got combination of rings: " 
                                                    + allCombsOfRings.size());
            }

            // Keep closable chains that are relevant for chelate formation
            if (RingClosureParameters.buildChelatesMode())
            {
                ArrayList<Set<DENOPTIMRing>> toRemove = new ArrayList<>();
                for (Set<DENOPTIMRing> setRings : allCombsOfRings)
                {
                    if (!cgh.checkChelatesGraph(molGraph,setRings))
                    {
                        toRemove.add(setRings);
                    }
                }
    
                allCombsOfRings.removeAll(toRemove);
                if (allCombsOfRings.isEmpty())
                {
                    String msg = "Setup Rings: no combination of rings.";
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return false;
                }
            }

            // Select a combination, if any still available
            int sz = allCombsOfRings.size();
            if (sz > 0)
            {
                Set<DENOPTIMRing> selected = new HashSet<>();
                if (sz == 1)
                {
                    selected = allCombsOfRings.get(0);
                }
                else
                {
                    //MersenneTwister rng = GAParameters.getRNG();
                    MersenneTwister rng = RandomUtils.getRNG();
                    int selId = rng.nextInt(sz);
                    selected = allCombsOfRings.get(selId);
                }

                // append new rings to existing list of rings in graph
                for (DENOPTIMRing ring : selected)
                {
                    molGraph.addRing(ring);
                }
            }
        }

	// Update the IAtomContainer representation
	DENOPTIMMoleculeUtils.removeRCA(mol,molGraph);
        res[2] = mol;

	// Update the SMILES representation
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (molsmiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molsmiles = "FAIL: NO SMILES GENERATED";
        }
	res[1] = molsmiles;

	// Update the INCHI key representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED");
        }
	res[0] = pr.getFirst();

        if (DEBUG)
        {
            System.out.println("After setupRings: Graph "+molGraph);
            System.out.println("Number of rings: "+molGraph.getRingCount());
            //GenUtils.pause();
        }

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Add a capping group if free connection is available
     * Addition of Capping groups does not update the symmetry table
     * for a symmetric graph. Before crossover/mutation we must delete the
     * capping groups and then perform the prescribed operation
     * @param molGraph
     */

    protected static void addCappingGroup(DENOPTIMGraph molGraph)
                                                    throws DENOPTIMException
    {
        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        if (!rcnEnabled)
            return;

        ArrayList<DENOPTIMVertex> lstVert = molGraph.getVertexList();

        for (int i=0; i<lstVert.size(); i++)
        {
            // check if the vertex has a free valence
            DENOPTIMVertex curVertex = lstVert.get(i);

            // no capping of a capping group

            if (curVertex.getFragmentType() == 2)
            {
                //String msg = "Attempting to cap a capping group. Check your data.";
                //DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                continue;
            }

            ArrayList<DENOPTIMAttachmentPoint> lstDaps =
                                            curVertex.getAttachmentPoints();

            for (int j=0; j<lstDaps.size(); j++)
            {
                DENOPTIMAttachmentPoint curDap = lstDaps.get(j);

                if (curDap.isAvailable())
                {
                    //Add capping group if required by capping map
                    int nvid = attachCappingFragmentAtPosition(molGraph,
                                                                curVertex, j);
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param cmpReac list of reactions of the source vertex attachment point
     * @param molVertex target vertex containing compatible reaction APs
     * @return list of indices of the attachment points in molVertex that has
     * the corresponding reaction
     */

    protected static ArrayList<Integer> getCompatibleClassAPIndex
                                    (String cmpReac, DENOPTIMVertex molVertex)
    {
        ArrayList<DENOPTIMAttachmentPoint> apLst = molVertex.getAttachmentPoints();

        ArrayList<Integer> apIdx = new ArrayList<>();

        for (int i=0; i<apLst.size(); i++)
        {
            DENOPTIMAttachmentPoint dap = apLst.get(i);
            if (dap.isAvailable())
            {
                // check if this AP has the compatible reactions
                String dapReac = dap.getAPClass();
                if (dapReac.compareToIgnoreCase(cmpReac) == 0)
                {
                    apIdx.add(i);
                }
            }
        }

        return apIdx;
    }

//------------------------------------------------------------------------------

    /**
     * Check if the population contains the specified InChi code
     * @param mols
     * @param molcode
     * @return <code>true</code> if found
     */

    protected static boolean containsMolecule(ArrayList<DENOPTIMMolecule> mols,
                                                                String molcode)
    {
        if(mols.isEmpty())
            return false;

        for (DENOPTIMMolecule mol : mols)
        {
            if (mol.getMoleculeUID().compareToIgnoreCase(molcode) == 0)
            {
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * calculate the number of atoms from the graph representation
     * @return number of heavy atoms in the molecule
     */
    protected static int getNumberOfAtoms(DENOPTIMGraph molGraph)
    {
        int n = 0;
        ArrayList<DENOPTIMVertex> vlst = molGraph.getVertexList();

        for (int i=0; i<vlst.size(); i++)
        {
            int id = vlst.get(i).getMolId();
            int ftype = vlst.get(i).getFragmentType();
            IGraphBuildingBlock bb = null;
			try {
				bb = FragmentSpace.getFragment(ftype, id);
				if (bb instanceof DENOPTIMFragment)
	            {
	            	n += DENOPTIMMoleculeUtils.getHeavyAtomCount(
	            			((DENOPTIMFragment) bb).getAtomContainer());
	            }
			} catch (DENOPTIMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param molGraph
     * @param fragIdx
     * @param nfrags
     * @return <code>true</code> if addition of a fragment does not violate
     * any rules (max number of atoms)
     */

    protected static boolean isFragmentAdditionPossible(DENOPTIMGraph molGraph,
            int fragIdx, int nfrags)
    {
    	int n = 0;
    	IGraphBuildingBlock bb = null;
		try {
			//WARNING: assumption that the building block is a proper fragment
			bb = FragmentSpace.getFragment(1, fragIdx);
			if (bb instanceof DENOPTIMFragment)
            {
            	n = nfrags * DENOPTIMMoleculeUtils.getHeavyAtomCount(
            			((DENOPTIMFragment) bb).getAtomContainer());
            }
		} catch (DENOPTIMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        int natom = getNumberOfAtoms(molGraph);

        if (n + natom > FragmentSpaceParameters.getMaxHeavyAtom())
            return false;
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Compare attachment points based on the reaction types
     * @param A attachment point information
     * @param B attachment point information
     * @return <code>true</code> if the points share a common reaction or more
     * else <code>false</code>
     */

    protected static boolean isFragmentClassCompatible(DENOPTIMAttachmentPoint A,
                                                    DENOPTIMAttachmentPoint B)
    {
        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        // if no reaction information is available return true
        if (!rcnEnabled)
        {
            //System.err.println("No reactions defined. Always TRUE");
            return true;
        }

        // if both have reaction info
        String strA = A.getAPClass();
        String strB = B.getAPClass();
        if (strA != null && strB != null)
        {
            if (strA.compareToIgnoreCase(strB) == 0)
                    return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the atoms at the given positions have similar environments
     * i.e. are similar in atom types etc.
     * @param mol
     * @param a1 atom position
     * @param a2 atom position
     * @return <code>true</code> if atoms have similar environments
     */

    protected static boolean isCompatible(IAtomContainer mol, int a1, int a2)
    {
        // check atom types
        IAtom atm1 = mol.getAtom(a1);
        IAtom atm2 = mol.getAtom(a2);

        if (atm1.getSymbol().compareTo(atm2.getSymbol()) != 0)
            return false;

        // check connected bonds
        if (mol.getConnectedBondsCount(atm1)!=mol.getConnectedBondsCount(atm2))
            return false;


        // check connected atoms
        if (mol.getConnectedAtomsCount(atm1)!=mol.getConnectedAtomsCount(atm2))
            return false;

        List<IAtom> la1 = mol.getConnectedAtomsList(atm2);
        List<IAtom> la2 = mol.getConnectedAtomsList(atm2);

        int k = 0;
        for (int i=0; i<la1.size(); i++)
        {
            IAtom b1 = la1.get(i);
            for (int j=0; j<la2.size(); j++)
            {
                IAtom b2 = la2.get(j);
                if (b1.getSymbol().compareTo(b2.getSymbol()) == 0)
                {
                    k++;
                    break;
                }
            }
        }

        return k == la1.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get the fitness values for the list of molecules
     * @param mols
     * @return array of fitness values
     */

    protected static double[] getFitnesses(ArrayList<DENOPTIMMolecule> mols)
    {
        int k = mols.size();
        double[] arr = new double[k];

        for (int i=0; i<k; i++)
        {
            arr[i] = mols.get(i).getMoleculeFitness();
        }
        return arr;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check if fitness values have significant standard deviation
     * @param molPopulation
     * @return <code>true</code> if population standard deviation of fitness
     * values exceeds 0.0001
     */

    protected static double getPopulationSD(ArrayList<DENOPTIMMolecule> molPopulation)
    {
        double[] fitvals = getFitnesses(molPopulation);
        double sdev = DENOPTIMMathUtils.stddevp(fitvals);
        fitvals = null;
        return sdev;
    }
    

//------------------------------------------------------------------------------

    /**
     *
     * @param molPopulation
     * @return list of INCHI codes for the molecules in the population
     */

    protected static ArrayList<String> getInchiCodes
                                    (ArrayList<DENOPTIMMolecule> molPopulation)
    {
        int k = molPopulation.size();
        ArrayList<String> arr = new ArrayList<>();

        for (int i=0; i<k; i++)
        {
            arr.add(molPopulation.get(i).getMoleculeUID());
        }
        return arr;
    }

//------------------------------------------------------------------------------

    /**
     * check if the graph to molecule translation
     * @param molGraph the molecular graph representation
     * @return an object array containing the inchi code, the smiles string
     *         and the 2D representation of the molecule
     *         <code>null</code> is returned if inchi/smiles/2D conversion fails
     *         An additional check is the number of atoms in the graph
     */

    protected static Object[] evaluateGraph(DENOPTIMGraph molGraph)
                                                      throws DENOPTIMException
    {
        if (molGraph == null)
        {
            String msg = "Evaluation of graph: graph is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // calculate the molecule representation
        GraphConversionTool gct = new GraphConversionTool();
        IAtomContainer mol = gct.convertGraphToMolecule(molGraph,true);
        if (mol == null)
        {
            String msg ="Evaluation of graph: graph-to-mol returned null!" 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molGraph.cleanup();
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: Not all connected" 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molGraph.cleanup();
            mol.removeAllElements();
            return null;
        }

//TODO del or make optional
//        IAtomContainer mol2D = DENOPTIMMoleculeUtils.generate2DCoordinates(mol);
//        //IAtomContainer mol2D = DENOPTIMMoleculeUtils.get2DStructureUsingBabel(mol);
//        if (mol2D == null)
//        {
//            String msg = "Evaluation of graph: mol2D is null!" + molGraph.toString();
//            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
//            return null;
//        }

        // hopefully the null shouldn't happen if all goes well
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
//TODO del or make optional
        //String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMoleculeUsingBabel(mol);
        if (molsmiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! " 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molsmiles = "FAIL: NO SMILES GENERATED";
        }

        // if by chance the molecule is disconnected
        if (molsmiles.contains("."))
        {
            String msg = "Evaluation of graph: SMILES contains \".\"" 
                                                                  + molsmiles;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molGraph.cleanup();
            mol.removeAllElements();
            return null;
        }

        if (FragmentSpaceParameters.getMaxHeavyAtom() > 0)
        {
            if (DENOPTIMMoleculeUtils.getHeavyAtomCount(mol) >
                                        FragmentSpaceParameters.getMaxHeavyAtom())
            {
                //System.err.println("Max atoms constraint violated");
                String msg = "Evaluation of graph: Max atoms constraint "
                                                  + " violated: " + molsmiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                molGraph.cleanup();
                mol.removeAllElements();
                return null;
            }
        }

        double mw = DENOPTIMMoleculeUtils.getMolecularWeight(mol);

        if (FragmentSpaceParameters.getMaxMW() > 0)
        {
            if (mw > FragmentSpaceParameters.getMaxMW())
            {
                //System.err.println("Max weight constraint violated");
                String msg = "Evaluation of graph: Molecular weight "
                       + "constraint violated: " + molsmiles + " | MW: " + mw;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                molGraph.cleanup();
                mol.removeAllElements();
                return null;
            }
        }
        mol.setProperty("MOL_WT", mw);

        int nrot = DENOPTIMMoleculeUtils.getNumberOfRotatableBonds(mol);
        if (FragmentSpaceParameters.getMaxRotatableBond() > 0)
        {
            if (nrot > FragmentSpaceParameters.getMaxRotatableBond())
            {
                String msg = "Evaluation of graph: Max rotatable bonds "
                                         + "constraint violated: "+ molsmiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                molGraph.cleanup();
                mol.removeAllElements();
                return null;
            }
        }
        mol.setProperty("ROT_BND", nrot);


        //Detect free AP that are not permitted
        if (FragmentSpace.useAPclassBasedApproach())
        {
            if (foundForbiddenEnd(molGraph))
            {
                String msg = "Evaluation of graph: forbidden end in graph!";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                molGraph.cleanup();
                mol.removeAllElements();
                return null;
            }
        }

//TODO make room for more optional filtering criteria?
        
        if (RingClosureParameters.allowRingClosures())
        {
            // Count rings and RCAs
            int nPossRings = 0;
            Set<String> doneType = new HashSet<String>();
            Map<String,String> rcaTypes = DENOPTIMConstants.RCATYPEMAP;
            for (String rcaTyp : rcaTypes.keySet())
            {
                if (doneType.contains(rcaTyp))
                {
                    continue;
                }

                int nThisType = 0;
                int nCompType = 0;
                for (IAtom atm : mol.atoms())
                {
                    if (atm.getSymbol().equals(rcaTyp))
                    {
                        nThisType++;
                    }
                    else if (atm.getSymbol().equals(rcaTypes.get(rcaTyp)))
                    {
                        nCompType++;
                    }
                }

                // check number of rca per type
                if (nThisType > RingClosureParameters.getMaxRcaPerType() || 
                         nCompType > RingClosureParameters.getMaxRcaPerType())
                {
                    String msg = "Evaluation of graph: too many RCAs! "
                                  + rcaTyp + ":" + nThisType + " "
                                  + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }
                if (nThisType < RingClosureParameters.getMinRcaPerType() ||
                         nCompType < RingClosureParameters.getMinRcaPerType())
                {
                    String msg = "Evaluation of graph: too few RCAs! "
                                  + rcaTyp + ":" + nThisType + " "
                                  + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }

                nPossRings = nPossRings + Math.min(nThisType, nCompType);
                doneType.add(rcaTyp);
                doneType.add(rcaTypes.get(rcaTyp));
            }

            if (nPossRings < RingClosureParameters.getMinRingClosures())
            {
                String msg = "Evaluation of graph: too few ring candidates";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // get the smiles/Inchi representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED");
        }

        Object[] res = new Object[3];
        res[0] = pr.getFirst(); // inchi
        res[1] = molsmiles; // smiles
        //res[2] = mol2D; // 2d coordinates
        res[2] = mol;

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Write the graph or the molecular representation of a DENOPTIMGraph that
     * did not fulfill one of the criteria evaluated
     * @param graph graph representation
     * @param mol molecular representation
     * @param cause message explaining the causes for rejecting the molecule
     */
/*
MF: TO BE TESTED
    private static void writeRejectedGraph(DENOPTIMGraph graph,
                        IAtomContainer mol, String cause)
                        throws DENOPTIMException
    {
        IAtomContainer molecule = new AtomContainer();
        if ( mol != null )
        {
            molecule = mol;
        }

        molecule.setProperty("DENOPTIMGraph", graph.toString());
        molecule.setProperty("Cause", cause);

        String rejectedFile = "checkRejectedMols.sdf";
        DenoptimIO.writeMolecule(rejectedFile, molecule, true);
    }
*/
  //------------------------------------------------------------------------------

    /**
     * Calculates the probability of adding a fragment to the given level.
     * This will require a coin toss with the calculated probability. If a newly
     * drawn random number is less than this value, a new fragment may be added.
     * @param level level of the graph at which fragment is to be added
     * @param scheme the chosen scheme
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return probability of adding a new fragment at this level.
     */
    
    public static double getGrowthProbabilityAtLevel(int level, int scheme, 
    		double lambda, double sigmaOne, double sigmaTwo)
    {
        double prob = 0.0;
        
        if (scheme == 0)
        {
            double f = Math.exp(-1.0 * (double)level * lambda);
            prob = 1 - ((1-f)/(1+f));
        }
        else if (scheme == 1)
        {
            prob = 1.0 - Math.tanh(lambda * (double)level);
        }
        else if (scheme == 2)
        {
            prob = 1.0-1.0/(1.0 + Math.exp(-sigmaOne * ((double) level - sigmaTwo)));
        }
        else if (scheme == 3)
        {
            prob = 1.0;
        }
        
        return prob;
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculates the probability of adding a fragment to the given level.
     * Used the settings defined by the GAParameters class.
     * @param level level of the graph at which fragment is to be added
     * @return probability of adding a new fragment at this level.
     */
    public static double getGrowthProbabilityAtLevel(int level)
    {
        int scheme = GAParameters.getGrowthProbabilityScheme();
        double lambda =GAParameters.getGrowthMultiplier();
        double sigmaOne = GAParameters.getGrowthFactorSteepSigma();
        double sigmaTwo = GAParameters.getGrowthFactorMiddleSigma();
        return getGrowthProbabilityAtLevel(level, scheme, lambda, sigmaOne, sigmaTwo);
    }


//------------------------------------------------------------------------------

    /**
     * For the class associated with the AP identify a compatible fragment and
     * a proper attachment point in it. This method searches only among 
     * fragments (i.e., library of type 1).
     * @param curDap the attachment point for which we ask for a partner.
     * @return the vector of indeces defining the chosen fragment and the
     * chosen attachment point. Note, the fist index is always -1, since 
     * no verted ID is assigned to the chosen fragment by this method.
     */

    protected static IdFragmentAndAP
            selectClassBasedFragment(DENOPTIMAttachmentPoint curDap)
                                                        throws DENOPTIMException
    {
        String rcn = curDap.getAPClass();
        int fid = -1, apid = -1, rnd = -1;
        String cmpReac = null;

        MersenneTwister rng = RandomUtils.getRNG();

        // loop thru the reactions for the current AP
        // check if this reaction bond is already satisfied
        if (curDap.isAvailable())
        {
            ArrayList<String> lstRcn = findMatchingClass(rcn);
            if (lstRcn == null)
            {
                String msg = "No matching class found for " + rcn;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }

            if(lstRcn.size() == 1)
            {
                cmpReac = lstRcn.get(0);
            }
            else
            {
                // if there are multiple compatible reactions, random selection
                rnd = rng.nextInt(lstRcn.size());
                cmpReac = lstRcn.get(rnd);
            }

            // get the list of fragment indices with matching reaction cmpReac
            ArrayList<Integer> reacFrags = getFragmentList(cmpReac);
            if (!reacFrags.isEmpty())
            {
                // choose a random fragment if there are more than 1
                if (reacFrags.size() == 1)
		{
                    fid = reacFrags.get(0);
		}
                else
                {
                    rnd = rng.nextInt(reacFrags.size());
                    fid = reacFrags.get(rnd);
                }

		// choose one of the compatible APs on the chosen fragment
        	ArrayList<DENOPTIMAttachmentPoint> fragAPs =
                                         FragmentUtils.getAPForFragment(fid, 1);
		ArrayList<Integer> compatApIds = new ArrayList<Integer>();
		for (int i=0; i<fragAPs.size(); i++)
		{
		    if (fragAPs.get(i).getAPClass().equals(cmpReac))
		    {
			compatApIds.add(i);
		    }
		}
                if (compatApIds.size() == 1)
                {
                    apid = compatApIds.get(0);
                }
                else
                {
                    rnd = rng.nextInt(compatApIds.size());
                    apid = compatApIds.get(rnd);
                }
            }
        }

	IdFragmentAndAP res = new IdFragmentAndAP(-1,fid,1,apid,-1,-1);

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * For the given vertex identify the index of the attachment point
     * that has the matching class
     * @param fragVertex
     * @param cmpReac reaction associated with the vertex
     * @return index of the attachment point
     */

    protected static int selectClassBasedAP(DENOPTIMVertex fragVertex,
                                                String cmpReac)
    {
        // get the list of indices of the compatible reaction AP
        ArrayList<Integer> apIdx =
                            getCompatibleClassAPIndex(cmpReac, fragVertex);

        MersenneTwister rng = RandomUtils.getRNG();
        int fapidx = -1;
        if (apIdx.size() == 1)
        {
            fapidx = apIdx.get(0);
        }
        // if there are more than 1 compatible AP choose randomly
        else
        {
            //int rnd = GAParameters.getRNG().nextInt(apIdx.size());
            int rnd = rng.nextInt(apIdx.size());
            fapidx = apIdx.get(rnd);
        }

        return fapidx;
    }

//------------------------------------------------------------------------------

    /**
     * Check if there are forbidden ends: free attachment points that are not
     * suitable for capping and not allowed to stay unused.
     *
     * @param molGraph the Graph representation of the molecule
     * @return <code>true</code> if a forbidden end is found
     */

    protected static boolean foundForbiddenEnd(DENOPTIMGraph molGraph)
    {
        ArrayList<DENOPTIMVertex> vertices = molGraph.getVertexList();
        Set<String> classOfForbEnds = FragmentSpace.getForbiddenEndList();
        boolean found = false;
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            // loop thru the APs of the current vertex
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    String apClass = dp.getAPClass();
                    if (classOfForbEnds.contains(apClass))
                    {
                        found = true;
                        String msg = "Forbidden free AP for Vertex: "
                            + vtx.getVertexId()
                            + " MolId: " + (vtx.getMolId() + 1)
                            + " Ftype: " + vtx.getFragmentType()
                            + "\n"+ molGraph+" \n "
                            + " AP class: " + apClass;
                        DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                        break;
                    }
                }
            }
            if (found)
            {
                break;
            }
        }

        return found;
    }

//------------------------------------------------------------------------------

    protected static void readUID(String infile, HashSet<String> lstInchi)
                                                    throws DENOPTIMException
    {
        ArrayList<String> lst = DenoptimIO.readList(infile);
        for (String str:lst)
            lstInchi.add(str);
        lst.clear();
    }

//------------------------------------------------------------------------------
   
    protected static void cleanup()
    {
        fragmentPool.clear();
        lstFragmentClass.clear();        
    }
  
//------------------------------------------------------------------------------    
}
