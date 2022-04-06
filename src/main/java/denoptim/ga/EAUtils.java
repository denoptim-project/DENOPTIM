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

package denoptim.ga;

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
import denoptim.graph.rings.CyclicGraphHandler;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.graph.rings.RingClosuresArchive;
import denoptim.graph.EmptyVertex;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.DENOPTIMLogger;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
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
    protected static void createFolderForGeneration(int genId, 
            GAParameters settings)
    {
        denoptim.files.FileUtils.createDirectory(
                EAUtils.getPathNameToGenerationFolder(genId, settings));
    }

//------------------------------------------------------------------------------

    /**
     * Reads unique identifiers and initial population files according to the
     * {@link GAParameters}.
     * @throws IOException 
     */
    protected static Population importInitialPopulation(
            SizeControlledSet uniqueIDsSet, GAParameters settings) 
                    throws DENOPTIMException, IOException
    {
        Population population = new Population(settings);

        HashSet<String> lstUID = new HashSet<>(1024);
        if (!settings.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(settings.getUIDFileIn(),lstUID);
            for (String uid : lstUID)
            {
                uniqueIDsSet.addNewUniqueEntry(uid);
            }
            DENOPTIMLogger.appLogger.log(Level.INFO, "Read " + lstUID.size() 
                + " known UIDs from " + settings.getUIDFileIn());
        }
        String inifile = settings.getInitialPopulationFile();
        if (inifile.length() > 0)
        {
            EAUtils.getPopulationFromFile(inifile, population, uniqueIDsSet, 
                    EAUtils.getPathNameToGenerationFolder(0, settings));
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
    protected static CandidateSource chooseGenerationMethod(GAParameters settings)
    {
        return pickNewCandidateGenerationMode(
                settings.getConstructionWeight(), 
                settings.getMutationWeight(),
                settings.getConstructionWeight());
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

    /**
     * Generates a new offspring by performing a crossover operation.
     * @param eligibleParents candidates that can be used as parents of the 
     * offspring.
     * @param population the collection of candidates where eligible candidates
     * are hosted.
     * @param mnt monitoring tool used to record events during the run of the
     * evolutionary algorithm.
     * @return a candidate chosen in a stochastic manner.
     */
    protected static Candidate buildCandidateByXOver(
            ArrayList<Candidate> eligibleParents, Population population, 
            Monitor mnt, GAParameters settings) throws DENOPTIMException
    {
        return buildCandidateByXOver(eligibleParents, population, mnt, 
                null, -1, -1, settings);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Generates a new offspring by performing a crossover operation.
     * @param eligibleParents candidates that can be used as parents of the 
     * offspring.
     * @param population the collection of candidates where eligible candidates
     * are hosted.
     * @param mnt monitoring tool used to record events during the run of the
     * evolutionary algorithm.
     * @param choiceOfParents a pair of indexes dictating which ones among the 
     * eligible parents we should use as parents. This avoids randomized 
     * decision making in case of test that need to be reproducible, 
     * but can be <code>null</code> which means "use random choice".
     * @param choiceOfXOverSites index indicating which crossover site to use.
     * This avoids randomized 
     * decision making in case of test that need to be reproducible, 
     * but can be <code>null</code> which means "use random choice".
     * @param choiceOfOffstring index dictating which among the available two
     * offspring (at most two, for now) if returned as result. 
     * This avoids randomized 
     * decision making in case of test that need to be reproducible, 
     * but can be <code>null</code> which means "use random choice".
     * @return
     * @throws DENOPTIMException
     */
    protected static Candidate buildCandidateByXOver(
            ArrayList<Candidate> eligibleParents, Population population, 
            Monitor mnt, int[] choiceOfParents, int choiceOfXOverSites,
            int choiceOfOffstring, GAParameters settings) throws DENOPTIMException
    {
        mnt.increase(CounterID.XOVERATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);
        
        int numatt = 0;
        
        // Identify a pair of parents that can do crossover, and a pair of
        // vertexes from which we can define a subgraph (or a branch) to swap
        XoverSite xos = null;
        boolean foundPars = false;
        while (numatt < settings.getMaxGeneticOpAttempts())
        {   
            if (FragmentSpace.useAPclassBasedApproach())
            {
                xos = EAUtils.performFBCC(eligibleParents, 
                        population, choiceOfParents, choiceOfXOverSites,
                        settings);
                if (xos == null)
                {
                    numatt++;
                    continue;
                }
            } else {
                //TODO: make it reproducible using choiceOfParents and choiceOfXOverSites
                Candidate[] parents = EAUtils.selectBasedOnFitness(
                        eligibleParents, 2, settings);
                if (parents[0] == null || parents[1] == null)
                {
                    numatt++;
                    continue;
                }
                //NB: this does not go into templates!
                DENOPTIMGraph gpA = parents[0].getGraph();
                List<DENOPTIMVertex> subGraphA = new ArrayList<DENOPTIMVertex>();
                gpA.getChildrenTree(EAUtils.selectNonScaffoldNonCapVertex(
                        gpA),subGraphA);

                DENOPTIMGraph gpB = parents[1].getGraph();
                List<DENOPTIMVertex> subGraphB = new ArrayList<DENOPTIMVertex>();
                gpB.getChildrenTree(EAUtils.selectNonScaffoldNonCapVertex(
                        gpB),subGraphB);
            }
            foundPars = true;
            break;
        }
        mnt.increaseBy(CounterID.XOVERPARENTSEARCH, numatt);

        if (!foundPars)
        {
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS_FINDPARENTS);
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return null;
        }
        
        Candidate cA = null, cB = null;
        DENOPTIMVertex vA = null, vB = null;
        vA = xos.getA().get(0);
        vB = xos.getB().get(0);
        DENOPTIMGraph gA = vA.getGraphOwner();
        cA = gA.getOutermostGraphOwner().getCandidateOwner();
        DENOPTIMGraph gB = vB.getGraphOwner();
        cB = gB.getOutermostGraphOwner().getCandidateOwner();
        
        String candIdA = cA.getName();
        String candIdB = cB.getName();
        int gid1 = gA.getGraphId();
        int gid2 = gB.getGraphId();
        
        // Start building the offspring
        XoverSite xosOnClones = xos.projectToClonedGraphs();
        DENOPTIMGraph gAClone = xosOnClones.getA().get(0).getGraphOwner();
        DENOPTIMGraph gBClone = xosOnClones.getB().get(0).getGraphOwner();
        try
        {
            if (!DENOPTIMGraphOperations.performCrossover(xosOnClones))
            {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_PERFORM);
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
                return null;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            ArrayList<DENOPTIMGraph> parents = new ArrayList<DENOPTIMGraph>();
            parents.add(xos.getA().get(0).getGraphOwner());
            parents.add(xos.getB().get(0).getGraphOwner());
            DenoptimIO.writeGraphsToSDF(new File(settings.getDataDirectory()
                    + "failed_xover.sdf"), parents, true);
            throw new DENOPTIMException("Error while performing crossover. "
                    + "Please, report this to the authors ",t);
        }
        gAClone.setGraphId(GraphUtils.getUniqueGraphIndex());
        gBClone.setGraphId(GraphUtils.getUniqueGraphIndex());
        String lstIdVA = "";
        for (DENOPTIMVertex v : xos.getA())
            lstIdVA = lstIdVA + "_" + v.getVertexId();
        String lstIdVB = "";
        for (DENOPTIMVertex v : xos.getB())
            lstIdVB = lstIdVB + "_" + v.getVertexId();
        String[] msgs = new String[2];
        msgs[0] = "Xover: " + candIdA + "|" + gid1 + "|" + lstIdVA + "="
                + candIdB + "|" + gid2 + "|" + lstIdVB;
        msgs[1] = "Xover: " + candIdB + "|" + gid2 + "|" + lstIdVB + "="
                + candIdA + "|" + gid1 + "|" + lstIdVA;
        
        DENOPTIMGraph[] graphsAffectedByXover = new DENOPTIMGraph[2];
        graphsAffectedByXover[0] = gAClone;
        graphsAffectedByXover[1] = gBClone;
        
        List<Candidate> validOffspring = new Population(settings);
        for (int ig=0; ig<graphsAffectedByXover.length; ig++)
        {
            DENOPTIMGraph g = graphsAffectedByXover[ig];

            // It makes sense to do this on the possibly embedded graph and not
            // on their embedding owners because there cannot be any new cycle
            // affecting the latter, but there can be ones affecting the first.
            if (!EAUtils.setupRings(null, g, settings))
            {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_SETUPRINGS);
                continue;
            }

            // Finalize the graph that is at the outermost level
            DENOPTIMGraph gOutermost = g.getOutermostGraphOwner();
            gOutermost.addCappingGroups();
            gOutermost.renumberGraphVertices();
            gOutermost.setLocalMsg(msgs[ig]);
            
            // Consider if the result can be used to define a new candidate
            Object[] res = EAUtils.evaluateGraph(gOutermost, settings);
            if (res != null)
            {
                if (!EAUtils.setupRings(res, gOutermost, settings))
                {
                    mnt.increase(CounterID.FAILEDXOVERATTEMPTS_SETUPRINGS);
                    res = null;
                }
            } else {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_EVAL);
            }
            
            // Check if the chosen combination gives rise to forbidden ends
            for (DENOPTIMVertex rcv : gOutermost.getFreeRCVertices())
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
                gOutermost.cleanup();
                gOutermost = null;
                continue;
            }
            
            // OK: we can now use it to make a new candidate
            Candidate offspring = new Candidate(gOutermost);
            offspring.setUID(res[0].toString().trim());
            offspring.setSmiles(res[1].toString().trim());
            offspring.setChemicalRepresentation((IAtomContainer) res[2]);
            
            validOffspring.add(offspring);
        }
        
        if (validOffspring.size() == 0)
        {
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return null;
        }
        
        Candidate chosenOffspring = null;
        if (choiceOfOffstring<0)
        {
            chosenOffspring = RandomUtils.randomlyChooseOne(validOffspring);
            chosenOffspring.setName("M" + GenUtils.getPaddedString(
                    DENOPTIMConstants.MOLDIGITS,
                    GraphUtils.getUniqueMoleculeIndex()));
        } else {
            chosenOffspring = validOffspring.get(choiceOfOffstring);
        }
        return chosenOffspring;
    }
    
//------------------------------------------------------------------------------

    protected static Candidate buildCandidateByMutation(
            ArrayList<Candidate> eligibleParents, Monitor mnt, 
            GAParameters settings) throws DENOPTIMException
    {
        mnt.increase(CounterID.MUTATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);
        
        int numatt = 0;
        Candidate parent = null;
        while (numatt < settings.getMaxGeneticOpAttempts())
        {
            parent = EAUtils.selectBasedOnFitness(eligibleParents,1, settings)[0];
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
        
        if (!DENOPTIMGraphOperations.performMutation(graph,mnt,settings))
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
            res = EAUtils.evaluateGraph(graph,settings);
        } catch (NullPointerException|IllegalArgumentException e)
        {
            System.out.println("WRITING DEBUG FILE for "+graph.getLocalMsg());
            DenoptimIO.writeGraphToSDF(new File("/tmp/debug_evalGrp_parent.sdf"), parent.getGraph(),false);
            DenoptimIO.writeGraphToSDF(new File("/tmp/debug_evalGrp_curr.sdf"), graph,false);
            throw e;
        }
        
        if (res != null)
        {
            if (!EAUtils.setupRings(res,graph,settings))
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
    
    protected static Candidate readCandidateFromFile(File srcFile, Monitor mnt,
            GAParameters settings) throws DENOPTIMException
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
        Object[] res = EAUtils.evaluateGraph(graph, true, settings);
        
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
    
    protected static Candidate buildCandidateFromScratch(Monitor mnt, 
            GAParameters settings) 
            throws DENOPTIMException
    {
        mnt.increase(CounterID.BUILDANEWATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        DENOPTIMGraph graph = EAUtils.buildGraph(settings);
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS_GRAPHBUILD);
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
            return null;
        }
        graph.setLocalMsg("NEW");
        
        Object[] res = EAUtils.evaluateGraph(graph, settings);
        
        if (res != null)
        {
            if (!EAUtils.setupRings(res,graph, settings))
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
            String filename, GAParameters settings) throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format("%-20s", "#Name "));
        sb.append(String.format("%-20s", "GraphId "));
        sb.append(String.format("%-30s", "UID "));
        sb.append(String.format("%-15s","Fitness "));

        sb.append("Source ");
        sb.append(NL);

        df.setMaximumFractionDigits(settings.getPrecisionLevel());
        df.setMinimumFractionDigits(settings.getPrecisionLevel());

        // NB: we consider the configured size of the population, not the actual 
        // size of list representing the population.
        String stats = "";
        synchronized (population)
        {
            for (int i=0; i<settings.getPopulationSize(); i++)
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
            stats = getSummaryStatistics(population, settings);
        }
        if (stats.trim().length() > 0)
            sb.append(stats);
        DenoptimIO.writeData(filename, sb.toString(), false);

        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    private static String getSummaryStatistics(Population popln, 
            GAParameters settings)
    {
        double[] fitness = getFitnesses(popln);
        double sdev = DENOPTIMStatUtils.stddev(fitness, true);
        String res = "";
        df.setMaximumFractionDigits(settings.getPrecisionLevel());

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
            ArrayList<Candidate> candidates, int number, GAParameters settings)
    {
        Candidate[] mates = new Candidate[number];
        switch (settings.getSelectionStrategyType())
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
     * Perform fitness-based, class-compatible selection of parents that can do
     * crossover operations.
     * @param eligibleParents list of candidates among which to choose.
     * @param population the dynamic population containing the eligible parents.
     * @param choiceOfParents the integers dictating the selection of parents. 
     * Use this only to ensure reproducibility in tests, 
     * otherwise use <code>null</code>
     * @param choiceOfXOverSites the integers dictating the selection of 
     * crossover sites. Use this only to ensure reproducibility in tests, 
     * otherwise use <code>negative</code>
     * @return returns the definition of the crossover in terms of subgraphs 
     * to swap.
     */

    protected static XoverSite performFBCC(
            ArrayList<Candidate> eligibleParents, Population population, 
            int[] choiceOfParents, int choiceOfXOverSites, GAParameters settings)
    {
        Candidate parentA = null;
        if (choiceOfParents==null)
            parentA = selectBasedOnFitness(eligibleParents, 1, settings)[0];
        else
            parentA = eligibleParents.get(choiceOfParents[0]);
        
        if (parentA == null)
            return null;
        
        ArrayList<Candidate> matesCompatibleWithFirst = 
                population.getXoverPartners(parentA,eligibleParents);
        if (matesCompatibleWithFirst.size() == 0)
            return null;
        
        Candidate parentB = null;
        if (choiceOfParents==null)
        {   
            parentB = selectBasedOnFitness(matesCompatibleWithFirst, 1, 
                    settings)[0];
        } else {
            parentB = eligibleParents.get(choiceOfParents[1]);
        }
        if (parentB == null)
            return null;
        
        XoverSite result = null;
        if (choiceOfXOverSites<0)
        {
            result = RandomUtils.randomlyChooseOne(population.getXoverSites(
                    parentA, parentB));
        } else {
            result = population.getXoverSites(parentA, parentB).get(
                    choiceOfXOverSites);
        }
        return result;
    }

//------------------------------------------------------------------------------
    
    public static String getPathNameToGenerationFolder(int genID, 
            GAParameters settings)
    {
        StringBuilder sb = new StringBuilder(32);
        
        int ndigits = String.valueOf(settings.getNumberOfGenerations()).length();
        
        sb.append(settings.getDataDirectory()).append(FSEP).append("Gen")
            .append(GenUtils.getPaddedString(ndigits, genID));
        
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToGenerationDetailsFile(int genID, 
            GAParameters settings)
    {
        StringBuilder sb = new StringBuilder(32);
        
        int ndigits = String.valueOf(settings.getNumberOfGenerations()).length();
        
        sb.append(settings.getDataDirectory()).append(FSEP)
            .append("Gen").append(GenUtils.getPaddedString(ndigits, genID))
            .append(FSEP)
            .append("Gen").append(GenUtils.getPaddedString(ndigits, genID))
            .append(".txt");
        
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToFinalPopulationFolder(GAParameters settings)
    {
        StringBuilder sb = new StringBuilder(32);
        sb.append(settings.getDataDirectory()).append(FSEP).append("Final");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToFinalPopulationDetailsFile(
            GAParameters settings)
    {
        StringBuilder sb = new StringBuilder(32);
        sb.append(settings.getDataDirectory()).append(FSEP).append("Final")
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
            String destDir, GAParameters settings) throws DENOPTIMException
    {
        String genOutfile = destDir + System.getProperty("file.separator") +
                                "Final.txt";

        File fileDir = new File(destDir);

        try
        {
            for (int i=0; i<settings.getPopulationSize(); i++)
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
            outputPopulationDetails(popln, genOutfile, settings);
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

    protected static void outputFinalResults(Population popln, 
            GAParameters settings) throws DENOPTIMException
    {
        String dirName = EAUtils.getPathNameToFinalPopulationFolder(settings);
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
                EAUtils.getPathNameToFinalPopulationDetailsFile(settings),
                settings);        
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

    protected static DENOPTIMGraph buildGraph(GAParameters settings) 
            throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        // building a molecule starts by selecting a random scaffold
        int scafIdx = selectRandomScaffold();

        DENOPTIMVertex scafVertex = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), scafIdx, 
                DENOPTIMVertex.BBType.SCAFFOLD);
        
        // add the scaffold as a vertex
        graph.addVertex(scafVertex);
        graph.setLocalMsg("NEW");
        
        if (graph.getAvailableAPs().size()==0
                && scafVertex instanceof DENOPTIMTemplate)
        {
            Monitor mnt = new Monitor();
            mnt.name = "IntraTemplateBuild";
            List<DENOPTIMVertex> initialMutableSites = graph.getMutableSites(
                    settings.getExcludedMutationTypes());
            for (DENOPTIMVertex mutableSite : initialMutableSites)
            {
                // This accounts for the possibility that a mutation changes a 
                // branch of the initial graph or deletes vertexes.
                if (!graph.containsOrEmbedsVertex(mutableSite))
                    continue;
                
                if (!DENOPTIMGraphOperations.performMutation(mutableSite, mnt,
                        settings))
                {
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM);
                    mnt.increase(CounterID.FAILEDMUTATTEMTS);
                    return null;
                }
            }
        }
        
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
//TODO this works only for scaffolds at the moment. make the preference for 
// fragments that lead to known closable chains operate also when fragments are
// the "turning point".
        RingClosuresArchive rca = rcParams.getRingClosuresArchive();
        graph.setCandidateClosableChains(rca.getCCFromTurningPointId(scafIdx));

        if (scafVertex.hasFreeAP())
        {
            DENOPTIMGraphOperations.extendGraph(scafVertex, true, false, settings);
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

    protected static boolean setupRings(Object[] res, DENOPTIMGraph molGraph, 
            GAParameters settings) throws DENOPTIMException
    {
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        if (!FragmentSpace.useAPclassBasedApproach())
            return true;

        if (!rcParams.allowRingClosures())
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(molGraph,true);
        
        // Set rotatability property as property of IBond
        String rotoSpaceFile = "";
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            rotoSpaceFile = ((FragmentSpaceParameters) settings.getParameters(
                    ParametersType.FS_PARAMS)).getRotSpaceDefFile();
        }
        RotationalSpaceUtils.defineRotatableBonds(mol, rotoSpaceFile, true, true);
        
        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(rcParams);

        //TODO: remove hard-coded variable that exclude considering all 
        // combination of rings
        boolean onlyRandomCombOfRings = true;
        
        if (onlyRandomCombOfRings)
        {
            List<DENOPTIMRing> combsOfRings = cgh.getRandomCombinationOfRings(
                    mol, molGraph, rcParams.getMaxRingClosures());
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
                    double crowdProbH = EAUtils.getCrowdingProbability(crowdOnH,
                            settings);
                    double crowdProbT = EAUtils.getCrowdingProbability(crowdOnT,
                            settings);
                    
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
            if (rcParams.buildChelatesMode())
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
        if (res!=null)
        {
            res[2] = mol;
        }
        // Update the SMILES representation
        if (res!=null)
        {
            String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            if (molsmiles == null)
            {
                String msg = "Evaluation of graph: SMILES is null! "
                                                            + molGraph.toString();
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                molsmiles = "FAIL: NO SMILES GENERATED";
            }
            res[1] = molsmiles;
        }

        // Update the INCHI key representation
        if (res!=null)
        {
            ObjectPair pr = DENOPTIMMoleculeUtils.getInChIForMolecule(mol);
            if (pr.getFirst() == null)
            {
                String msg = "Evaluation of graph: INCHI is null!";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                pr.setFirst("UNDEFINED");
            }
            res[0] = pr.getFirst();
        }

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Check if the population contains the specified InChi code
     * @param mols
     * @param molcode
     * @return <code>true</code> if found
     */

    protected static boolean containsMolecule(Population mols, String molcode)
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

    protected static Object[] evaluateGraph(DENOPTIMGraph molGraph, 
            GAParameters settings) 
            throws DENOPTIMException
    {
        return evaluateGraph(molGraph, false, settings);
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
            boolean permissive, GAParameters settings) throws DENOPTIMException
    {
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        
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

        if (fsParams.getMaxHeavyAtom() > 0 && !permissive)
        {
            if (DENOPTIMMoleculeUtils.getHeavyAtomCount(mol) >
                fsParams.getMaxHeavyAtom())
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

        if (fsParams.getMaxMW() > 0 && !permissive)
        {
            if (mw > fsParams.getMaxMW())
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
        if (fsParams.getMaxRotatableBond() > 0 && !permissive)
        {
            if (nrot > fsParams.getMaxRotatableBond())
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
        
        if (rcParams.allowRingClosures() && !permissive)
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
                if (nThisType > rcParams.getMaxRcaPerType() || 
                         nCompType > rcParams.getMaxRcaPerType())
                {
                    String msg = "Evaluation of graph: too many RCAs! "
                                  + rcaTyp + ":" + nThisType + " "
                                  + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }
                if (nThisType < rcParams.getMinRcaPerType() ||
                         nCompType < rcParams.getMinRcaPerType())
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

            if (nPossRings < rcParams.getMinRingClosures())
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
    public static double getMolSizeProbability(DENOPTIMGraph graph, 
            GAParameters settings)
    {
        if (!settings.useMolSizeBasedProb())
            return 1.0;
        int scheme = settings.getMolGrowthProbabilityScheme();
        double lambda =settings.getMolGrowthMultiplier();
        double sigmaOne = settings.getMolGrowthFactorSteepSigma();
        double sigmaTwo = settings.getMolGrowthFactorMiddleSigma();
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
    public static double getGrowthByLevelProbability(int level, 
            GAParameters settings)
    {
        if (!settings.useLevelBasedProb())
            return 1.0;
        int scheme = settings.getGrowthProbabilityScheme();
        double lambda =settings.getGrowthMultiplier();
        double sigmaOne = settings.getGrowthFactorSteepSigma();
        double sigmaTwo = settings.getGrowthFactorMiddleSigma();
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
    public static double getCrowdingProbability(DENOPTIMAttachmentPoint ap,
            GAParameters settings)
    {
        int scheme = settings.getCrowdingProbabilityScheme();
        double lambda =settings.getCrowdingMultiplier();
        double sigmaOne = settings.getCrowdingFactorSteepSigma();
        double sigmaTwo = settings.getCrowdingFactorMiddleSigma();
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
    public static double getCrowdingProbability(int crowdedness, 
            GAParameters settings)
    {
        int scheme = settings.getCrowdingProbabilityScheme();
        double lambda =settings.getCrowdingMultiplier();
        double sigmaOne = settings.getCrowdingFactorSteepSigma();
        double sigmaTwo = settings.getCrowdingFactorMiddleSigma();
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
     * blocks. Returns zero for APs belonging to {@link EmptyVertex}s.
     */
    public static int getCrowdedness(DENOPTIMAttachmentPoint ap, 
            boolean ignoreFreeRCVs)
    {
        if (ap.getOwner() instanceof EmptyVertex)
        {
            return 0;
        }
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
