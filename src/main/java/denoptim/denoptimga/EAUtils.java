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

package denoptim.denoptimga;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.DENOPTIMLogger;
import denoptim.logging.Monitor;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;
import denoptim.rings.RingClosuresArchive;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.DENOPTIMStatUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RandomUtils;
import denoptim.utils.RotationalSpaceUtils;
import denoptim.utils.SizeControlledSet;


/**
 * Helper methods for the genetic algorithm
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class EAUtils
{

    // cluster the fragments based on their #APs
    protected static HashMap<Integer, ArrayList<Integer>> fragmentPool;
    
    /**
     * Locale used to write reports
     */
    private static Locale enUsLocale = new Locale("en", "US");
    
    /**
     * Format for decimal fitness numbers that overwrites Locale to en_US
     */
    private static DecimalFormat df = initialiseFormatter();
    private static DecimalFormat initialiseFormatter() {
    	DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(
    			enUsLocale);
    	df.setGroupingUsed(false);
    	return df;
    }
    
    // for each fragment store the reactions associated with it
    protected static HashMap<Integer, ArrayList<String>> lstFragmentClass;

    /**
     * A chosen method for generation of new {@link Candidate}s.
     */
    public enum CandidateSource {
        CROSSOVER, MUTATION, CONSTRUCTION, MANUAL;
    }
    
    private static final String NL =System.getProperty("line.separator");
    private static final String FSEP = System.getProperty("file.separator");
    
//------------------------------------------------------------------------------

    /**
     * Creates a folder meant to hold all the data generated during a generation.
     * The folder is created under the work space.
     * @param genId the generation's identity number
     * @throws DENOPTIMException
     */
    protected static void createFolderForGeneration(int genId)
    {
        denoptim.files.FileUtils.createDirectory(EAUtils.getPathNameToGenerationFolder(genId));
    }

//------------------------------------------------------------------------------

    /**
     * Reads unique identifiers and initial population files according to the
     * {@link GAParameters}.
     * @throws IOException 
     */
    protected static Population importInitialPopulation(
            SizeControlledSet uniqueIDsSet) throws DENOPTIMException, IOException
    {
        Population population = new Population();

        HashSet<String> lstUID = new HashSet<>(1024);
        if (!GAParameters.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(GAParameters.getUIDFileIn(),lstUID);
            for (String uid : lstUID)
            {
                uniqueIDsSet.addNewUniqueEntry(uid);
            }
            DENOPTIMLogger.appLogger.log(Level.INFO, "Read " + lstUID.size() 
                + " known UIDs from " + GAParameters.getUIDFileIn());
        }
        String inifile = GAParameters.getInitialPopulationFile();
        if (inifile.length() > 0)
        {
            EAUtils.getPopulationFromFile(inifile, population, uniqueIDsSet, 
                    EAUtils.getPathNameToGenerationFolder(0));
            DENOPTIMLogger.appLogger.log(Level.INFO, "Read " + population.size() 
                + " molecules from " + inifile);
        }
        return population;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Choose one of the methods to make new {@link Candidate}s. 
     * The choice is biased
     * by the weights of the methods as defined in the {@link GAParameters}.
     */
    protected static CandidateSource chooseGenerationMethod()
    {
        return pickNewCandidateGenerationMode(
                GAParameters.getConstructionWeight(), 
                GAParameters.getMutationWeight(),
                GAParameters.getConstructionWeight());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Takes a decision on how many sites to mutate on a candidate.
     * @param multiSiteMutationProb the probability (0-1) of multi-site mutation.
     * @param hit a random real number between 0 and 1.
     * @return an integer indicating how many mutation to perform.
     */
    public static int chooseNumberOfSitesToMutate(double[] multiSiteMutationProb,
            double hit)
    {
        double tot = 0;
        for (int i=0; i<multiSiteMutationProb.length; i++)
            tot = tot + multiSiteMutationProb[i];
        
        double scaledHit = hit * tot;
        
        double min = 0;
        double max = 0;
        int choice = 0;
        for (int i=0; i<multiSiteMutationProb.length; i++)
        {
            max = max + multiSiteMutationProb[i];
            if (min < scaledHit && scaledHit <= max)
            {
                choice = i;
                break;
            }
            min = Math.max(min,min+multiSiteMutationProb[i]);
        }
        return choice;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Takes a decision on which {@link CandidateSource} method to use for 
     * generating a new {@link Candidate}. The choice is made according to the
     * weights given as arguments, and shooting a random number over a weighted
     * score range.
     * @param xoverWeight weight of crossover between existing population 
     * members.
     * @param mutWeight weight of mutation of existing population members.
     * @param newWeight weight of construction from scratch.
     * @return
     */
    public static CandidateSource pickNewCandidateGenerationMode(
            double xoverWeight, double mutWeight, double newWeight)
    {
        double hit = RandomUtils.nextDouble() 
                * (xoverWeight + mutWeight + newWeight);
        if (hit <= xoverWeight)
        {
            return CandidateSource.CROSSOVER;
        } else if (xoverWeight < hit && hit <= (mutWeight+xoverWeight))
        {
            return CandidateSource.MUTATION;
        } else {
            return CandidateSource.CONSTRUCTION;
        }
    }
    
//------------------------------------------------------------------------------
    
    protected static Candidate buildCandidateByXOver(
            ArrayList<Candidate> eligibleParents, Population population, 
            Monitor mnt) throws DENOPTIMException
    {
        mnt.increase(CounterID.XOVERATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);
        
        int numatt = 0;
        
        // Identify a pair of parents that can do crossover
        Candidate maleCandidate = null, femaleCandidate = null;
        DENOPTIMGraph maleGraph = null, femaleGraph = null;
        DENOPTIMVertex vertxOnMale = null, vertxOnFemale = null;
        boolean foundPars = false;
        while (numatt < GAParameters.getMaxGeneticOpAttempts())
        {   
            if (FragmentSpace.useAPclassBasedApproach())
            {
                DENOPTIMVertex[] pair = EAUtils.performFBCC(eligibleParents, 
                        population);
                if (pair == null)
                {
                    numatt++;
                    continue;
                }
                vertxOnMale = pair[0];
                vertxOnFemale = pair[1];
                maleGraph = vertxOnMale.getGraphOwner();
                maleCandidate = maleGraph.getCandidateOwner();
                femaleGraph = vertxOnFemale.getGraphOwner();
                femaleCandidate = femaleGraph.getCandidateOwner();
            } else {
                Candidate[] parents = EAUtils.selectBasedOnFitness(eligibleParents, 2);
                if (parents[0] == null || parents[1] == null)
                {
                    numatt++;
                    continue;
                }
                maleCandidate = parents[0];
                maleGraph = maleCandidate.getGraph();
                femaleCandidate = parents[1];
                femaleGraph = femaleCandidate.getGraph();
                vertxOnMale = EAUtils.selectNonScaffoldNonCapVertex(maleGraph);
                vertxOnFemale = EAUtils.selectNonScaffoldNonCapVertex(femaleGraph);
            }
            
            // Avoid redundant xover, i.e., xover that swaps the same subgraph
            try {
                DENOPTIMGraph test1 = maleGraph.clone();
                DENOPTIMGraph test2 = femaleGraph.clone();
                DENOPTIMGraph subGraph1 = test1.extractSubgraph(
                        maleGraph.indexOf(vertxOnMale));
                DENOPTIMGraph subGraph2 = test2.extractSubgraph(
                        femaleGraph.indexOf(vertxOnFemale));
                if (!subGraph1.isIsomorphicTo(subGraph2))
                {
                    foundPars = true;
                }
            } catch (Throwable t) {
                t.printStackTrace();
                continue;
            }
            break;
        }
        mnt.increaseBy(CounterID.XOVERPARENTSEARCH, numatt);

        if (!foundPars)
        {
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS_FINDPARENTS);
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return null;
        }
        
        String molid1 = maleCandidate.getName();
        String molid2 = femaleCandidate.getName();
        int gid1 = maleGraph.getGraphId();
        int gid2 = femaleGraph.getGraphId();
        int vid1 = maleGraph.indexOf(vertxOnMale);
        int vid2 = femaleGraph.indexOf(vertxOnFemale);
        
        DENOPTIMGraph graph1 = maleCandidate.getGraph().clone();
        DENOPTIMGraph graph2 = femaleCandidate.getGraph().clone();
        
        graph1.renumberGraphVertices();
        graph2.renumberGraphVertices();
        
        try
        {
            if (!DENOPTIMGraphOperations.performCrossover(
                    graph1.getVertexAtPosition(vid1),
                    graph2.getVertexAtPosition(vid2)))
            {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_PERFORM);
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
                return null;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            ArrayList<DENOPTIMGraph> parents = new ArrayList<DENOPTIMGraph>();
            parents.add(maleGraph);
            parents.add(graph1);
            parents.add(femaleGraph);
            parents.add(graph2);
            DenoptimIO.writeGraphsToSDF(new File(GAParameters.getDataDirectory()
                    + "failed_xover.sdf"), parents, true);
            throw new DENOPTIMException("Error while performing crossover. "
                    + "Please, report this to the authors ",t);
        }
        
        graph1.setGraphId(GraphUtils.getUniqueGraphIndex());
        graph2.setGraphId(GraphUtils.getUniqueGraphIndex());
        graph1.addCappingGroups();
        graph2.addCappingGroups();
        String msg = "Xover: " + molid1 + "|" + gid1 + "|" + vid1 + "="
                    + molid2 + "|" + gid2 + "|" + vid2;
        graph1.setLocalMsg(msg);
        graph2.setLocalMsg(msg);
        
        DENOPTIMGraph[] graphs = new DENOPTIMGraph[2];
        graphs[0] = graph1;
        graphs[1] = graph2;
        
        List<Candidate> validOnes = new Population();
        for (DENOPTIMGraph g : graphs)
        {
            Object[] res = EAUtils.evaluateGraph(g);

            if (res != null)
            {
                if (!EAUtils.setupRings(res,g))
                {
                    mnt.increase(CounterID.FAILEDXOVERATTEMPTS_SETUPRINGS);
                    res = null;
                }
            } else {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_EVAL);
            }
            
            // Check if the chosen combination gives rise to forbidden ends
            //TODO this should be considered already when making the list of
            // possible combination of rings
            for (DENOPTIMVertex rcv : g.getFreeRCVertices())
            {
                APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                if (FragmentSpace.getCappingMap().get(apc)==null 
                        && FragmentSpace.getForbiddenEndList().contains(apc))
                {
                    mnt.increase(CounterID.FAILEDXOVERATTEMPTS_FORBENDS);
                    res = null;
                }
            }
            
            if (res == null)
            {
                g.cleanup();
                g = null;
                continue;
            }
            
            g.renumberGraphVertices();
            
            Candidate offspring = new Candidate(g);
            offspring.setUID(res[0].toString().trim());
            offspring.setSmiles(res[1].toString().trim());
            offspring.setChemicalRepresentation((IAtomContainer) res[2]);
            
            validOnes.add(offspring);
        }
        
        if (validOnes.size() == 0)
        {
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return null;
        }
        
        Candidate chosenOffspring = RandomUtils.randomlyChooseOne(validOnes);
        chosenOffspring.setName("M" + GenUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        return chosenOffspring;
    }
    
//------------------------------------------------------------------------------
    
    protected static Candidate buildCandidateByMutation(
            ArrayList<Candidate> eligibleParents, Monitor mnt)
                    throws DENOPTIMException
    {
        mnt.increase(CounterID.MUTATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);
        
        int numatt = 0;
        Candidate parent = null;
        while (numatt < GAParameters.getMaxGeneticOpAttempts())
        {
            parent = EAUtils.selectBasedOnFitness(eligibleParents,1)[0];
            if (parent == null)
            {
                numatt++;
                continue;
            }
            break;
        }
        mnt.increaseBy(CounterID.MUTPARENTSEARCH,numatt);
        if (parent == null)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        
        DENOPTIMGraph graph = parent.getGraph().clone();
        graph.renumberGraphVertices();
        
        String parentMolName = FilenameUtils.getBaseName(parent.getSDFFile());
        int parentGraphId = parent.getGraph().getGraphId();
        graph.setLocalMsg("Mutation: " + parentMolName + "|" + parentGraphId);
        
        if (!DENOPTIMGraphOperations.performMutation(graph,mnt))
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM);
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        graph.addCappingGroups();
        
        Object[] res = null;
        try
        {
            res = EAUtils.evaluateGraph(graph);
        } catch (NullPointerException|IllegalArgumentException e)
        {
            System.out.println("WRITING DEBUG FILE for "+graph.getLocalMsg());
            DenoptimIO.writeGraphToSDF(new File("/tmp/debug_evalGrp_parent.sdf"), parent.getGraph(),false);
            DenoptimIO.writeGraphToSDF(new File("/tmp/debug_evalGrp_curr.sdf"), graph,false);
            throw e;
        }
        
        if (res != null)
        {
            if (!EAUtils.setupRings(res,graph))
            {
                res = null;
                mnt.increase(CounterID.FAILEDMUTATTEMTS_SETUPRINGS);
            }
        } else {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_EVAL);
        }
        
        // Check if the chosen combination gives rise to forbidden ends
        //TODO this should be considered already when making the list of
        // possible combination of rings
        for (DENOPTIMVertex rcv : graph.getFreeRCVertices())
        {
            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
            if (FragmentSpace.getCappingMap().get(apc)==null 
                    && FragmentSpace.getForbiddenEndList().contains(apc))
            {
                res = null;
                mnt.increase(CounterID.FAILEDMUTATTEMTS_FORBENDS);
            }
        }
        
        if (res == null)
        {
            graph.cleanup();
            graph = null;
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        
        Candidate offspring = new Candidate(graph);
        offspring.setUID(res[0].toString().trim());
        offspring.setSmiles(res[1].toString().trim());
        offspring.setChemicalRepresentation((IAtomContainer) res[2]);
        offspring.setName("M" + GenUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        return offspring;
    }
    
//------------------------------------------------------------------------------
    
    protected static Candidate readCandidateFromFile(File srcFile, Monitor mnt) 
            throws DENOPTIMException
    {
        mnt.increase(CounterID.MANUALADDATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        ArrayList<DENOPTIMGraph> graphs;
        try
        {
            graphs = DenoptimIO.readDENOPTIMGraphsFromFile(srcFile);
        } catch (Exception e)
        {
            e.printStackTrace();
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Could not read graphs from file " + srcFile
                    + ". No candidate generated!";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            return null;
        }
        if (graphs.size() == 0 || graphs.size() > 1)
        {
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Found " + graphs.size() + " graphs in file " + srcFile
                    + ". I expect one and only one graph. "
                    + "No candidate generated!";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            return null;
        }

        DENOPTIMGraph graph = graphs.get(0);
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Null graph from file " + srcFile
                    + ". Expected one and only one graph. "
                    + "No candidate generated!";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            return null;
        }
        graph.setLocalMsg("MANUAL_ADD");
        
        // We expect users to know what they ask for. Therefore, we do
        // evaluate the graph, but in a permissive manner, meaning that 
        // several filters are disabled to permit the introduction of graphs 
        // that cannot be generated automatically.
        Object[] res = EAUtils.evaluateGraph(graph, true);
        
        if (res == null)
        {
            graph.cleanup();
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS_EVAL);
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            return null;
        }

        Candidate candidate = new Candidate(graph);
        candidate.setUID(res[0].toString().trim());
        candidate.setSmiles(res[1].toString().trim());
        candidate.setChemicalRepresentation((IAtomContainer) res[2]);
        
        candidate.setName("M" + GenUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        String msg = "Candidate " + candidate.getName() + " is imported from " 
                + srcFile;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);
        
        return candidate;
    }
    
//------------------------------------------------------------------------------
    
    protected static Candidate buildCandidateFromScratch(Monitor mnt) 
            throws DENOPTIMException
    {
        mnt.increase(CounterID.BUILDANEWATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        DENOPTIMGraph graph = EAUtils.buildGraph();
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS_GRAPHBUILD);
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
            return null;
        }
        graph.setLocalMsg("NEW");
        
        Object[] res = EAUtils.evaluateGraph(graph);
        
        if (res != null)
        {
            if (!EAUtils.setupRings(res,graph))
            {
                graph.cleanup();
                mnt.increase(CounterID.FAILEDBUILDATTEMPTS_SETUPRINGS);
                mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
                return null;
            }
        } else {
            graph.cleanup();
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS_EVAL);
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
            return null;
        }
        
        // Check if the chosen combination gives rise to forbidden ends
        //TODO: this should be considered already when making the list of
        // possible combination of rings
        for (DENOPTIMVertex rcv : graph.getFreeRCVertices())
        {
            // Also exclude any RCV that is not bound to anything?
            if (rcv.getEdgeToParent() == null)
            {
                res = null;
                mnt.increase(CounterID.FAILEDBUILDATTEMPTS_FORBIDENDS);
            }
            if (rcv.getEdgeToParent() == null)
            {
                // RCV as scaffold! Ignore special case
                continue;
            }
            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
            if (FragmentSpace.getCappingMap().get(apc)==null 
                    && FragmentSpace.getForbiddenEndList().contains(apc))
            {
                res = null;
                mnt.increase(CounterID.FAILEDBUILDATTEMPTS_FORBIDENDS);
            }
        }
        
        if (res == null)
        {
            graph.cleanup();
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
            return null;
        }

        Candidate candidate = new Candidate(graph);
        candidate.setUID(res[0].toString().trim());
        candidate.setSmiles(res[1].toString().trim());
        candidate.setChemicalRepresentation((IAtomContainer) res[2]);
        
        candidate.setName("M" + GenUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        return candidate;
    }
    
//------------------------------------------------------------------------------

    /**
     * Write out summary for the current GA population
     * @param population
     * @param filename
     * @throws DENOPTIMException
     */

    protected static void outputPopulationDetails(Population population, 
            String filename) throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format("%-20s", "#Name "));
        sb.append(String.format("%-20s", "GraphId "));
        sb.append(String.format("%-30s", "UID "));
        sb.append(String.format("%-15s","Fitness "));

        sb.append("Source ");
        sb.append(NL);

        df.setMaximumFractionDigits(GAParameters.getPrecisionLevel());
        df.setMinimumFractionDigits(GAParameters.getPrecisionLevel());

        // NB: we consider the configured size of the population, not the actual 
        // size of list representing the population.
        String stats = "";
        synchronized (population)
        {
            for (int i=0; i<GAParameters.getPopulationSize(); i++)
            {
                Candidate mol = population.get(i);
                if (mol != null)
                {
                    String mname = new File(mol.getSDFFile()).getName();
                    if (mname != null)
                        sb.append(String.format("%-20s", mname));
    
                    sb.append(String.format("%-20s", 
                            mol.getGraph().getGraphId()));
                    sb.append(String.format("%-30s", mol.getUID()));
                    sb.append(df.format(mol.getFitness()));
                    sb.append("    ").append(mol.getSDFFile());
                    sb.append(System.getProperty("line.separator"));
                }
            }
    
            // calculate descriptive statistics for the population
            stats = getSummaryStatistics(population);
        }
        if (stats.trim().length() > 0)
            sb.append(stats);
        DenoptimIO.writeData(filename, sb.toString(), false);

        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    private static String getSummaryStatistics(Population popln)
    {
        double[] fitness = getFitnesses(popln);
        double sdev = DENOPTIMStatUtils.stddev(fitness, true);
        String res = "";
        df.setMaximumFractionDigits(GAParameters.getPrecisionLevel());

        StringBuilder sb = new StringBuilder(128);
        sb.append(NL+NL+"#####POPULATION SUMMARY#####"+NL);
        int n = popln.size();
        sb.append(String.format("%-30s", "SIZE:"));
        sb.append(String.format("%12s", n));
        sb.append(NL);
        double f;
        f = DENOPTIMStatUtils.max(fitness);
        sb.append(String.format("%-30s", "MAX:")).append(df.format(f));
        sb.append(NL);
        f = DENOPTIMStatUtils.min(fitness);
        sb.append(String.format("%-30s", "MIN:")).append(df.format(f));
        sb.append(NL);
        f = DENOPTIMStatUtils.mean(fitness);
        sb.append(String.format("%-30s", "MEAN:")).append(df.format(f));
        sb.append(NL);
        f = DENOPTIMStatUtils.median(fitness);
        sb.append(String.format("%-30s", "MEDIAN:")).append(df.format(f));
        sb.append(NL);
        f = DENOPTIMStatUtils.stddev(fitness, true);
        sb.append(String.format("%-30s", "STDDEV:")).append(df.format(f));
        sb.append(NL);
        if (sdev > 0.0001)
        {
            f = DENOPTIMStatUtils.skewness(fitness, true);
            sb.append(String.format("%-30s", "SKEW:")).append(df.format(f));
            sb.append(NL);
        } else {
            sb.append(String.format("%-30s", "SKEW:")).append(" NaN (sdev too small)");
            sb.append(NL);
        }

        HashMap<Integer, Integer> scf_cntr = new HashMap<>();
        for (int i=0; i<popln.size(); i++)
        {
            Candidate mol = popln.get(i);
            DENOPTIMGraph g = mol.getGraph();
            int scafIdx = g.getSourceVertex().getBuildingBlockId() + 1;
            if (scf_cntr.containsKey(scafIdx)) {
                scf_cntr.put(scafIdx, scf_cntr.get(scafIdx)+1);
            } else {
                scf_cntr.put(scafIdx, 1);
            }
        }

        sb.append(NL+NL+"#####SCAFFOLD ANALYSIS##### (Scaffolds list current "
                + "size: " + FragmentSpace.getScaffoldLibrary().size() + ")" 
                + NL);
        List<Integer> sortedKeys = new ArrayList<Integer>(scf_cntr.keySet());
        Collections.sort(sortedKeys);
        for (Integer k : sortedKeys)
        {
            
            sb.append(k).append(" ").append(scf_cntr.get(k));
            sb.append(NL);
        }
        
        res = sb.toString();
        sb.setLength(0);
        
        return res;
    }
    
//------------------------------------------------------------------------------

    /**
     * Selects a number of members from the given population. 
     * The selection method is what specified by the
     * configuration of the genetic algorithm ({@link GAParameters}).
     * @param candidates the list of candidates to chose from.
     * @param number how many candidate to pick.
     * @return indexes of the selected members of the given population.
     */

    protected static Candidate[] selectBasedOnFitness(
            ArrayList<Candidate> candidates, int number)
    {
        Candidate[] mates = new Candidate[number];
        switch (GAParameters.getSelectionStrategyType())
        {
        case 1:
            mates = SelectionHelper.performTournamentSelection(candidates, 
                    number);
            break;
        case 2:
            mates = SelectionHelper.performRWS(candidates, number);
            break;
        case 3:
            mates = SelectionHelper.performSUS(candidates, number);
            break;
        case 4:
            mates = SelectionHelper.performRandomSelection(candidates, number);
            break;
        }
        return mates;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Chose randomly a vertex that is neither scaffold or capping group.
     */
    protected static DENOPTIMVertex selectNonScaffoldNonCapVertex(DENOPTIMGraph g)
    {
        List<DENOPTIMVertex> candidates = new ArrayList<DENOPTIMVertex>(
                g.getVertexList());
        candidates.removeIf(v ->
                v.getBuildingBlockType() == BBType.SCAFFOLD
                || v.getBuildingBlockType() == BBType.CAP);
        return RandomUtils.randomlyChooseOne(candidates);
    }
              
//------------------------------------------------------------------------------

    /**
     * Perform fitness-based, class-compatible selection of parents for 
     * crossover.
     * @param eligibleParents list of candidates among which to choose.
     * @param population the dynamic population containing the eligible parents.
     * @return returns the pair of vertexes where crossover can be performed,
     * or null if no possibility was found.
     */

    protected static DENOPTIMVertex[] performFBCC(
            ArrayList<Candidate> eligibleParents, Population population)
    {
        Candidate parentA = selectBasedOnFitness(eligibleParents, 1)[0];
        if (parentA == null)
            return null;
        
        ArrayList<Candidate> matesCompatibleWithFirst = 
                population.getXoverPartners(parentA,eligibleParents);
        if (matesCompatibleWithFirst.size() == 0)
            return null;
        
        Candidate parentB = selectBasedOnFitness(matesCompatibleWithFirst,1)[0];
        if (parentB == null)
            return null;
        
        return RandomUtils.randomlyChooseOne(population.getXoverSites(parentA,
                parentB));
    }

//------------------------------------------------------------------------------
    
    public static String getPathNameToGenerationFolder(int genID)
    {
        StringBuilder sb = new StringBuilder(32);
        
        int ndigits = String.valueOf(GAParameters.getNumberOfGenerations()).length();
        
        sb.append(GAParameters.getDataDirectory()).append(FSEP).append("Gen")
            .append(GenUtils.getPaddedString(ndigits, genID));
        
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToGenerationDetailsFile(int genID)
    {
        StringBuilder sb = new StringBuilder(32);
        
        int ndigits = String.valueOf(GAParameters.getNumberOfGenerations()).length();
        
        sb.append(GAParameters.getDataDirectory()).append(FSEP)
            .append("Gen").append(GenUtils.getPaddedString(ndigits, genID))
            .append(FSEP)
            .append("Gen").append(GenUtils.getPaddedString(ndigits, genID))
            .append(".txt");
        
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToFinalPopulationFolder()
    {
        StringBuilder sb = new StringBuilder(32);
        sb.append(GAParameters.getDataDirectory()).append(FSEP).append("Final");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToFinalPopulationDetailsFile()
    {
        StringBuilder sb = new StringBuilder(32);
        sb.append(GAParameters.getDataDirectory()).append(FSEP).append("Final")
            .append(FSEP).append("Final.txt");
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Simply copies the files from the previous directories into the specified
     * folder.
     * @param popln the final list of best molecules
     * @param destDir the name of the output directory
     */

    protected static void outputFinalResults(Population popln,
                            String destDir) throws DENOPTIMException
    {
        String genOutfile = destDir + System.getProperty("file.separator") +
                                "Final.txt";

        File fileDir = new File(destDir);

        try
        {
            for (int i=0; i<GAParameters.getPopulationSize(); i++)
            {
                String sdfile = popln.get(i).getSDFFile();
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
     * Simply copies the files from the previous directories into the specified
     * folder.
     * @param popln the final list of best molecules
     * @param destDir the name of the output directory
     */

    protected static void outputFinalResults(Population popln) throws DENOPTIMException
    {
        String dirName = EAUtils.getPathNameToFinalPopulationFolder();
        denoptim.files.FileUtils.createDirectory(dirName);
        File fileDir = new File(dirName);

        for (int i=0; i<popln.size(); i++)
        {
            Candidate c = popln.get(i);
            String sdfile = c.getSDFFile();
            String imgfile = c.getImageFile();

            if (sdfile != null)
            {
                try {
                    FileUtils.copyFileToDirectory(new File(sdfile), fileDir);
                } catch (IOException ioe) {
                    throw new DENOPTIMException("Failed to copy file '" 
                            + sdfile + "' to '" + fileDir + "' for candidate "
                            + c.getName(), ioe);
                }
            }
            if (imgfile != null)
            {
                try {
                    FileUtils.copyFileToDirectory(new File(imgfile), fileDir);
                } catch (IOException ioe) {
                    throw new DENOPTIMException("Failed to copy file '" 
                            + imgfile + "' to '" + fileDir + "' for candidate "
                            + c.getName(), ioe);
                }
            }
        }
        outputPopulationDetails(popln,
                EAUtils.getPathNameToFinalPopulationDetailsFile());        
    }

//------------------------------------------------------------------------------

    /**
     * Reconstruct the molecular population from the file.
     * @param filename the pathname to read in.
     * @param population the collection of population members.
     * @param uniqueIDsSet collection of unique identifiers.
     * @throws DENOPTIMException
     * @throws IOException 
     */
    protected static void getPopulationFromFile(String filename,
            Population population, SizeControlledSet uniqueIDsSet,
            String genDir) throws DENOPTIMException, IOException
    {
        ArrayList<Candidate> candidates = DenoptimIO.readCandidates(
                new File(filename), true);
        if (candidates.size() == 0)
        {
        	String msg = "Found 0 candidates in file " + filename;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        for (Candidate candidate : candidates)
        {
            if (!uniqueIDsSet.contains(candidate.getUID()))
            {
                int ctr = GraphUtils.getUniqueMoleculeIndex();
                int gctr = GraphUtils.getUniqueGraphIndex();
                
                String molName = "M" + GenUtils.getPaddedString(8, ctr);
                candidate.setName(molName);
                candidate.getGraph().setGraphId(gctr);
                candidate.getGraph().setLocalMsg("INITIAL_POPULATION");
                String sdfPathName = genDir + System.getProperty("file.separator") 
                            + molName + DENOPTIMConstants.FITFILENAMEEXTOUT;
                candidate.setSDFFile(sdfPathName);
                candidate.setImageFile(null);
                
                // Write the candidate to file as if it had been processed by fitness provider
                DenoptimIO.writeCandidateToFile(new File(sdfPathName), 
                        candidate, false);
                
                population.add(candidate);
            }
        }

        if (population.isEmpty())
        {
        	String msg = "Population is still empty after having processes "
        			+ candidates.size() + " candidates from file " + filename;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        setVertexCounterValue(population);
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
                sb.append(NL).append(iter.next());
            }
        }

        DenoptimIO.writeData(outfile, sb.toString(), append);
        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    /**
     * Set the Vertex counter value according to the largest value found in
     * the given population.
     * @param population the collection of candidates to analyse.
     * @throws DENOPTIMException 
     */

    protected static void setVertexCounterValue(Population population) 
            throws DENOPTIMException
    {
        int val = Integer.MIN_VALUE;
        for (Candidate popln1 : population)
        {
            DENOPTIMGraph g = popln1.getGraph();
            val = Math.max(val, g.getMaxVertexId());
        }
        GraphUtils.ensureVertexIDConsistency(val);
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
            return RandomUtils.nextInt(FragmentSpace.getScaffoldLibrary().size());
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
            return RandomUtils.nextInt(
                    FragmentSpace.getFragmentLibrary().size());
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
        DENOPTIMGraph graph = new DENOPTIMGraph();
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        // building a molecule starts by selecting a random scaffold
        int scafIdx = selectRandomScaffold();

        DENOPTIMVertex scafVertex = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), scafIdx, DENOPTIMVertex.BBType.SCAFFOLD);
        
        // add the scaffold as a vertex
        graph.addVertex(scafVertex);
        graph.setLocalMsg("NEW");
        
        if (graph.getAvailableAPs().size()==0
                && scafVertex instanceof DENOPTIMTemplate)
        {
            Monitor mnt = new Monitor();
            mnt.name = "IntraTemplateBuild";
            List<DENOPTIMVertex> initialMutableSites = graph.getMutableSites(
                    GAParameters.getExcludedMutationTypes());
            for (DENOPTIMVertex mutableSite : initialMutableSites)
            {
                // This accounts for the possibility that a mutation changes a 
                // branch of the initial graph or deletes vertexes.
                if (!graph.containsOrEmbedsVertex(mutableSite))
                    continue;
                
                if (!DENOPTIMGraphOperations.performMutation(mutableSite,mnt))
                {
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM);
                    mnt.increase(CounterID.FAILEDMUTATTEMTS);
                    return null;
                }
            }
        }
        
//TODO this works only for scaffolds at the moment. make the preference for 
// fragments that lead to known closable chains operate also when fragments are
// the "turning point".
        graph.setCandidateClosableChains(
                        RingClosuresArchive.getCCFromTurningPointId(scafIdx));

        if (scafVertex.hasFreeAP())
        {
            DENOPTIMGraphOperations.extendGraph(scafVertex, true, false);
        }
        
        if (!(scafVertex instanceof DENOPTIMTemplate) 
                && graph.getVertexCount() == 0)
        {
            return null;
        }
        
        graph.addCappingGroups();
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in a given graph and if
     * any ring can be closed, it chooses one of the combinations of ring 
     * closures that involves the highest number of new rings.
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
        if (!FragmentSpace.useAPclassBasedApproach())
            return true;

        if (!RingClosureParameters.allowRingClosures())
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(molGraph,true);
        
        // Set rotatability property as property of IBond
        ArrayList<ObjectPair> rotBonds = 
                                 RotationalSpaceUtils.defineRotatableBonds(mol,
                                  FragmentSpaceParameters.getRotSpaceDefFile(),
                                                                   true, true);
        
        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler();

        //TODO: remove hard-coded variable that exclude considering all 
        // combination of rings
        boolean onlyRandomCombOfRings = true;
        
        if (onlyRandomCombOfRings)
        {
            List<DENOPTIMRing> combsOfRings = cgh.getRandomCombinationOfRings(
                    mol, molGraph, RingClosureParameters.getMaxRingClosures());
            if (combsOfRings.size() > 0)
            {
                for (DENOPTIMRing ring : combsOfRings)
                {
                    // Consider the crowding probability
                    double shot = RandomUtils.nextDouble();
                    int crowdOnH = EAUtils.getCrowdedness(
                            ring.getHeadVertex().getEdgeToParent().getSrcAP(),
                            true);
                    int crowdOnT = EAUtils.getCrowdedness(
                            ring.getTailVertex().getEdgeToParent().getSrcAP(),
                            true);
                    double crowdProbH = EAUtils.getCrowdingProbability(crowdOnH);
                    double crowdProbT = EAUtils.getCrowdingProbability(crowdOnT);
                    
                    if (shot < crowdProbH && shot < crowdProbT)
                    {
                        molGraph.addRing(ring);
                    }
                }
            }
        }
        else
        {
            ArrayList<List<DENOPTIMRing>> allCombsOfRings = 
                            cgh.getPossibleCombinationOfRings(mol, molGraph);
        
            // Keep closable chains that are relevant for chelate formation
            if (RingClosureParameters.buildChelatesMode())
            {
                ArrayList<List<DENOPTIMRing>> toRemove = new ArrayList<>();
                for (List<DENOPTIMRing> setRings : allCombsOfRings)
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
                List<DENOPTIMRing> selected = new ArrayList<>();
                if (sz == 1)
                {
                    selected = allCombsOfRings.get(0);
                }
                else
                {
                    int selId = RandomUtils.nextInt(sz);
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
        //DENOPTIMMoleculeUtils.removeUsedRCA(mol,molGraph);
        // Done already at t3d.convertGraphTo3DAtomContainer
        
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
        ObjectPair pr = DENOPTIMMoleculeUtils.getInChIForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED");
        }
        res[0] = pr.getFirst();

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Check if the population contains the specified InChi code
     * @param mols
     * @param molcode
     * @return <code>true</code> if found
     */

    protected static boolean containsMolecule(Population mols,
                                                                String molcode)
    {
        if(mols.isEmpty())
            return false;

        for (Candidate mol : mols)
        {
            if (mol.getUID().compareToIgnoreCase(molcode) == 0)
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

    protected static double[] getFitnesses(Population mols)
    {
        int k = mols.size();
        double[] arr = new double[k];

        for (int i=0; i<k; i++)
        {
            arr[i] = mols.get(i).getFitness();
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

    protected static double getPopulationSD(Population molPopulation)
    {
        double[] fitvals = getFitnesses(molPopulation);
        return DENOPTIMStatUtils.stddev(fitvals, true);
    }
    
//------------------------------------------------------------------------------

    /**
     * check conversion of the graph to molecule translation
     * @param molGraph the molecular graph representation
     * @return an object array containing the inchi code, the molecular
     * representation of the candidate, and additional attributes. Or 
     * <code>null</code> is returned if inchi/smiles/2D conversion fails
     * An additional check is the number of atoms in the graph
     */

    protected static Object[] evaluateGraph(DENOPTIMGraph molGraph) 
            throws DENOPTIMException
    {
        return evaluateGraph(molGraph, false);
    }

//------------------------------------------------------------------------------

    /**
     * check conversion of the graph to molecule translation
     * @param molGraph the molecular graph representation
     * @param permissive use <code>true</code> to by-pass some of the filters 
     * that prevent graphs from violating constrains on 
     * fully connected molecules (<code>true</code> enables disconnected systems),
     * molecular weight,
     * max number of rotatable bonds, 
     * and number of ring closures.
     * @return an object array containing the inchi code, the molecular
     * representation of the candidate, and additional attributes. Or 
     * <code>null</code> is returned if inchi/smiles/2D conversion fails
     * An additional check is the number of atoms in the graph
     */

    protected static Object[] evaluateGraph(DENOPTIMGraph molGraph, 
            boolean permissive) throws DENOPTIMException
    {
        if (molGraph == null)
        {
            String msg = "Evaluation of graph: graph is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // calculate the molecule representation
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = null;
        mol = t3d.convertGraphTo3DAtomContainer(molGraph,true);

        if (mol == null)
        { 
            String msg ="Evaluation of graph: graph-to-mol returned null! " 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molGraph.cleanup();
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: molecular representation has "
                    + "multiple components. See graph " 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
        }

        // hopefully the null shouldn't happen if all goes well
        boolean smilesIsAvailable = true;
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (molsmiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! " 
                                                        + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molsmiles = "FAIL: NO SMILES GENERATED";
            smilesIsAvailable = false;
        }

        // if by chance the molecule is disconnected
        if (molsmiles.contains(".") && !permissive)
        {
            String msg = "Evaluation of graph: SMILES contains \".\"" 
                                                                  + molsmiles;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molGraph.cleanup();
            mol.removeAllElements();
            return null;
        }

        if (FragmentSpaceParameters.getMaxHeavyAtom() > 0 && !permissive)
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

        if (FragmentSpaceParameters.getMaxMW() > 0 && !permissive)
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
        if (FragmentSpaceParameters.getMaxRotatableBond() > 0 && !permissive)
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
        
        if (RingClosureParameters.allowRingClosures() && !permissive)
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
        ObjectPair pr = DENOPTIMMoleculeUtils.getInChIForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            if (smilesIsAvailable)
                pr.setFirst("UNDEFINED-INCHI_"+molsmiles);
            else
                pr.setFirst("UNDEFINED-INCHI_"+RandomUtils.nextInt(100000));
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
     * @param scheme the chosen scheme.
     * @param lambda parameter used by scheme 0 and 1.
     * @param sigmaOne parameter used by scheme 2 (steepness).
     * @param sigmaTwo parameter used by scheme 2 (middle point).
     * @return probability of adding a new fragment at this level.
     */
    
    public static double getGrowthProbabilityAtLevel(int level, int scheme,
                    double lambda, double sigmaOne, double sigmaTwo)
    {
        return getProbability(level, scheme, lambda, sigmaOne, sigmaTwo);
    }
    
  //------------------------------------------------------------------------------

    /**
     * Calculated the probability of extending a graph based on the current
     * size of the molecular representation contained in the graph
     * and the given parameters.
     * @param graph the current graph for which to calculate the probability
     * of extension.
     * @param scheme the chosen shape of the probability function.
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return the crowding probability.
     */
    public static double getMolSizeProbability(DENOPTIMGraph graph)
    {
        if (!GAParameters.useMolSizeBasedProb)
            return 1.0;
        int scheme = GAParameters.getMolGrowthProbabilityScheme();
        double lambda =GAParameters.getMolGrowthMultiplier();
        double sigmaOne = GAParameters.getMolGrowthFactorSteepSigma();
        double sigmaTwo = GAParameters.getMolGrowthFactorMiddleSigma();
        return getMolSizeProbability(graph, scheme, lambda, sigmaOne, sigmaTwo);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the probability of extending a graph based on the current
     * size of the molecular representation contained in the graph
     * and the given parameters.
     * @param graph the current graph for which to calculate the probability
     * of extension.
     * @param scheme the chosen shape of the probability function.
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return the crowding probability.
     */
    public static double getMolSizeProbability(DENOPTIMGraph graph,
            int scheme, double lambda, double sigmaOne, double sigmaTwo)
    {
        return getProbability(graph.getHeavyAtomsCount(), scheme, lambda, 
                sigmaOne, sigmaTwo);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated a probability given parameters defining the shape of the 
     * probability function and a single input value.
     * @param value the value x for which we calculate f(x).
     * @param scheme the chosen shape of the probability function.
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return the probability.
     */
    public static double getProbability(double value, 
            int scheme, double lambda, double sigmaOne, double sigmaTwo)
    {
        double prob = 1.0;
        if (scheme == 0)
        {
            double f = Math.exp(-1.0 * value * lambda);
            prob = 1 - ((1-f)/(1+f));
        }
        else if (scheme == 1)
        {
            prob = 1.0 - Math.tanh(lambda * value);
        }
        else if (scheme == 2)
        {
            prob = 1.0-1.0/(1.0 + Math.exp(-sigmaOne * (value - sigmaTwo)));
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
    public static double getGrowthByLevelProbability(int level)
    {
        if (!GAParameters.useLevelBasedProb)
            return 1.0;
        int scheme = GAParameters.getGrowthProbabilityScheme();
        double lambda =GAParameters.getGrowthMultiplier();
        double sigmaOne = GAParameters.getGrowthFactorSteepSigma();
        double sigmaTwo = GAParameters.getGrowthFactorMiddleSigma();
        return getGrowthProbabilityAtLevel(level, scheme, lambda, sigmaOne, 
                sigmaTwo);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the probability of using and attachment point rooted on
     * an atom that is holding other attachment points which have already been
     * used.
     * @param ap the attachment point candidate to be used.
     * @return probability of adding a new building block on the given 
     * attachment point.
     */
    public static double getCrowdingProbability(DENOPTIMAttachmentPoint ap)
    {
        int scheme = GAParameters.getCrowdingProbabilityScheme();
        double lambda =GAParameters.getCrowdingMultiplier();
        double sigmaOne = GAParameters.getCrowdingFactorSteepSigma();
        double sigmaTwo = GAParameters.getCrowdingFactorMiddleSigma();
        return getCrowdingProbability(ap, scheme, lambda, sigmaOne, sigmaTwo);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the probability of using and attachment point rooted on
     * an atom that is holding other attachment points which have already been
     * used. This method does not use the actual information on attachment point
     * usage, but relies on a externally calculated values of the crowdedness.
     * Use {@link EAUtils#getCrowdingProbability(DENOPTIMAttachmentPoint)} to
     * get the crowding probability for an actual attachment point.
     * Use {@link EAUtils#getCrowdedness(DENOPTIMAttachmentPoint)} to calculate
     * the crowdedness for an attachment point.
     * @param crowdedness the level of crowdedness
     * @return probability of adding a new building block on an attachment point
     * with the given crowdedness.
     */
    public static double getCrowdingProbability(int crowdedness)
    {
        int scheme = GAParameters.getCrowdingProbabilityScheme();
        double lambda =GAParameters.getCrowdingMultiplier();
        double sigmaOne = GAParameters.getCrowdingFactorSteepSigma();
        double sigmaTwo = GAParameters.getCrowdingFactorMiddleSigma();
        return getCrowdingProbabilityForCrowdedness(crowdedness, scheme, lambda,
                sigmaOne, sigmaTwo);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculate the current crowdedness of the given attachment point.
     * @param ap the attachment point to consider
     * @return the integer representing how many AP rooted on the same atom that 
     * holds the given attachment point are used by non-capping group building 
     * blocks.
     */
    public static int getCrowdedness(DENOPTIMAttachmentPoint ap)
    {
        return getCrowdedness(ap,false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculate the current crowdedness of the given attachment point.
     * @param ap the attachment point to consider
     * @param ignoreFreeRCVs use <code>true</code> to avoid counting unused 
     * ring-closing vertexes and actual connections.
     * @return the integer representing how many AP rooted on the same atom that 
     * holds the given attachment point are used by non-capping group building 
     * blocks.
     */
    public static int getCrowdedness(DENOPTIMAttachmentPoint ap, 
            boolean ignoreFreeRCVs)
    {
        int crowdness = 0;
        DENOPTIMGraph g = ap.getOwner().getGraphOwner();
        for (DENOPTIMAttachmentPoint oap : ap.getOwner().getAttachmentPoints())
        {
            if (oap.getAtomPositionNumber() == ap.getAtomPositionNumber()
                    && !oap.isAvailableThroughout() 
                    && oap.getLinkedAP().getOwner()
                    .getBuildingBlockType() != BBType.CAP)
            {
                if (ignoreFreeRCVs && oap.getLinkedAP().getOwner().isRCV())
                {
                    if (g.getUsedRCVertices().contains(oap.getLinkedAP().getOwner()))
                        crowdness = crowdness + 1;
                } else {
                    crowdness = crowdness + 1;
                }
            }
        }
        return crowdness;
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the probability of using and attachment point rooted on
     * an atom that is holding other attachment points that have already been
     * used.
     * @param ap the attachment point candidate to be used.
     * @param scheme the chosen shape of the probability function.
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return probability of adding a new building block on the given 
     * attachment point.
     */
    public static double getCrowdingProbability(DENOPTIMAttachmentPoint ap, 
            int scheme,
            double lambda, double sigmaOne, double sigmaTwo)
    {   
        //Applies only to molecular fragments
        if (ap.getOwner() instanceof DENOPTIMFragment == false)
        {
            return 1.0;
        }
        int crowdness = getCrowdedness(ap);
        return getCrowdingProbabilityForCrowdedness(crowdness, scheme, lambda,
                sigmaOne, sigmaTwo);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the crowding probability for a given level of crowdedness.
     * @param crowdedness the level of crowdedness
     * @param scheme the chosen shape of the probability function.
     * @param lambda parameter used by scheme 0 and 1
     * @param sigmaOne parameter used by scheme 2 (steepness)
     * @param sigmaTwo parameter used by scheme 2 (middle point)
     * @return the crowding probability.
     */
    public static double getCrowdingProbabilityForCrowdedness(int crowdedness, 
            int scheme,
            double lambda, double sigmaOne, double sigmaTwo)
    {
        return getProbability(crowdedness, scheme, lambda, sigmaOne, sigmaTwo);
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
                    if (classOfForbEnds.contains(apClass))
                    {
                        String msg = "Forbidden free AP for Vertex: "
                            + vtx.getVertexId()
                            + " MolId: " + (vtx.getBuildingBlockId() + 1)
                            + " Ftype: " + vtx.getBuildingBlockType()
                            + "\n"+ molGraph+" \n "
                            + " AP class: " + apClass;
                        DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                        return true;
                    }
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
