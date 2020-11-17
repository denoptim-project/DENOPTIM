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
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;
import denoptim.rings.RingClosuresArchive;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.DENOPTIMStatUtils;
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
    protected static DecimalFormat df = new DecimalFormat();

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
        double sdev = DENOPTIMStatUtils.stddev(fitness);
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
            f = DENOPTIMStatUtils.stddev(fitness);
            sb.append(String.format("%-30s", "STDDEV:")).append(String.format(floatForm, f));
            sb.append(System.getProperty("line.separator"));
            f = DENOPTIMStatUtils.skewness(fitness);
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
     * Selects a single parent using the scheme specified.
     * @param popln
     * @return the index of the parent
     */
    
    //TODO-V3 merge this with the selectParents to get a selectParents(population,how_many_do_you_want)

    protected static int selectSingleParent(ArrayList<DENOPTIMMolecule> popln)
    {
        int selmate = -1;
        int stype = GAParameters.getSelectionStrategyType();

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
     * Selects two parents for crossover.
     * @param molPopulation the population of candidates.
     * @return array of parents for crossover.
     */

    protected static int[] selectParents(ArrayList<DENOPTIMMolecule> molPopulation)
    {
        int[] mates = null;

        MersenneTwister rng = RandomUtils.getRNG();
        
        //TODO-V3 make this work on a subset of population members chosen
        // according to APClass compatibility, thus getting rid of the 
        // if statement.

        if (!FragmentSpace.useAPclassBasedApproach())
        {
            switch (GAParameters.getSelectionStrategyType())
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

        HashSet<String> uidsFromInitPop = new HashSet<String>();
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

        //TODO-V3: use a type-agnostic w.r.t vertex constructor
        DENOPTIMVertex scafVertex = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), scafIdx, BBType.SCAFFOLD);
        
        // we set the level to -1, as the base
        scafVertex.setLevel(-1);
        
        //TODO-V3: check that symmetry is inherited from the original vertex stored in the library of building blocks.
        
        /*
        DENOPTIMVertex mol = FragmentSpace.getScaffoldLibrary().get(scafIdx);
        ArrayList<SymmetricSet> simAP = mol.getSymmetricAPsSets();
        scafVertex.setSymmetricAP(simAP);
        */
        
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

    protected static int getCappingFragment(APClass rcnCap)
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
            APClass cmpReac)
    {
        ArrayList<Integer> lstFragIdx = new ArrayList<>();
        for (int i=0; i<FragmentSpace.getCappingLibrary().size(); i++)
        {
                DENOPTIMVertex mol = FragmentSpace.getCappingLibrary().get(i);
            ArrayList<APClass> lstRcn = mol.getAllAPClasses();
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
     * @return id of the vertex added; -1 if capping is not required.
     */

    protected static int attachCappingFragmentAtPosition
                            (DENOPTIMGraph molGraph, DENOPTIMVertex curVertex,
                                int dapIdx) throws DENOPTIMException
    {
        int lvl = curVertex.getLevel();

        APClass apcSrc =  curVertex.getAttachmentPoints().get(dapIdx).getAPClass();
        // locate the capping group for this rcn
        APClass apcCap = getCappingGroup(apcSrc);

        if (apcCap != null)
        {

            int bbIdCap = getCappingFragment(apcCap);

            if (bbIdCap != -1)
            {
                DENOPTIMVertex capVrtx = DENOPTIMVertex.newVertexFromLibrary(
                        GraphUtils.getUniqueVertexIndex(), bbIdCap, BBType.CAP);
                
                capVrtx.setLevel(lvl+1);

                //Get the index of the AP of the capping group to use
                //(always the first and only AP)
                DENOPTIMEdge edge = curVertex.connectVertices(
                        capVrtx, dapIdx, 0, apcSrc, apcCap
                );
                if (edge != null)
                {
                    // add the fragment as a vertex
                    molGraph.addVertex(capVrtx);

                    molGraph.addEdge(edge);

                    return capVrtx.getVertexId();
                }
                else
                {
                    String msg = "Unable to connect capping group "
                                     + capVrtx + " to graph" + molGraph;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                    throw new DENOPTIMException(msg);
                }
            }
            else
            {
                String msg = "Capping is required but no proper capping "
                                + "fragment found with APCalss " + apcCap;
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

    protected static APClass getCappingGroup(APClass rcn) 
            throws DENOPTIMException
    {
        APClass capRcn = null;

        if (FragmentSpace.getCappingMap().containsKey(rcn))
        {
            capRcn = FragmentSpace.getCappingMap().get(rcn);
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
     * for a symmetric graph.
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

            if (curVertex.getFragmentType() == BBType.CAP)
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
                    attachCappingFragmentAtPosition(molGraph, curVertex, j);
                }
            }
        }
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
        return DENOPTIMStatUtils.stddev(fitvals);
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
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(
                molGraph, true);
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
        return getGrowthProbabilityAtLevel(level, scheme, lambda, sigmaOne, 
                sigmaTwo);
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
        Set<APClass> classOfForbEnds = FragmentSpace.getForbiddenEndList();
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    APClass apClass = dp.getAPClass();
                    //TODO-V3 remove loop once we can ensure all APClass intances
                    // refer to the unique ones
                    for (APClass fe : classOfForbEnds)
                    {
                        if (fe.equals(apClass))
                        {
                            String msg = "Forbidden free AP for Vertex: "
                                + vtx.getVertexId()
                                + " MolId: " + (vtx.getMolId() + 1)
                                + " Ftype: " + vtx.getFragmentType()
                                + "\n"+ molGraph+" \n "
                                + " AP class: " + apClass;
                            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                            return true;
                        }
                    }
                    /*
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
                    */
                }
            }
        }

        return false;
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
}
