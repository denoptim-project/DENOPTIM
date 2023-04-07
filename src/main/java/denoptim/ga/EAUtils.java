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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragmenter.FragmenterTools;
import denoptim.fragmenter.ScaffoldingPolicy;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.GraphPattern;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricAPs;
import denoptim.graph.SymmetricVertexes;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.rings.CyclicGraphHandler;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.graph.rings.RingClosuresArchive;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.GeneralUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;
import denoptim.utils.RotationalSpaceUtils;
import denoptim.utils.SizeControlledSet;
import denoptim.utils.StatUtils;


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
     * Reads unique identifiers and initial population file according to the
     * {@link GAParameters}.
     * @throws IOException 
     */
    protected static Population importInitialPopulation(
            SizeControlledSet uniqueIDsSet, GAParameters settings) 
                    throws DENOPTIMException, IOException
    {
        Population population = new Population(settings);

        // Read existing or previously visited UIDs
        HashSet<String> lstUID = new HashSet<>(1024);
        if (!settings.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(settings.getUIDFileIn(),lstUID);
            for (String uid : lstUID)
            {
                uniqueIDsSet.addNewUniqueEntry(uid);
            }
            settings.getLogger().log(Level.INFO, "Read " + lstUID.size() 
                + " known UIDs from " + settings.getUIDFileIn());
        }
        
        // Read existing graphs
        int numFromInitGraphs = 0;
        String initPopFile = settings.getInitialPopulationFile();
        if (initPopFile.length() > 0)
        {
            EAUtils.getPopulationFromFile(initPopFile, population, uniqueIDsSet, 
                    EAUtils.getPathNameToGenerationFolder(0, settings),
                    settings);
            numFromInitGraphs = population.size();
            settings.getLogger().log(Level.INFO, "Imported " + numFromInitGraphs
                + " candidates (as graphs) from " + initPopFile);
        }
        
        return population;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Choose one of the methods to make new {@link Candidate}s. 
     * The choice is biased
     * by the weights of the methods as defined in the {@link GAParameters}.
     * @param the genetic algorithm settings
     * @return one among the possible {@link CandidateSource} excluding
     * {@value CandidateSource#MANUAL}.
     */
    protected static CandidateSource chooseGenerationMethod(GAParameters settings)
    {
        return pickNewCandidateGenerationMode(
                settings.getCrossoverWeight(), 
                settings.getMutationWeight(),
                settings.getConstructionWeight(),
                settings.getRandomizer());
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
     * @return one among the possible {@link CandidateSource} excluding
     * {@value CandidateSource#MANUAL}.
     */
    public static CandidateSource pickNewCandidateGenerationMode(
            double xoverWeight, double mutWeight, double newWeight,
            Randomizer randomizer)
    {
        double hit = randomizer.nextDouble() 
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
     * Generates a pair of new offspring by performing a crossover operation.
     * @param eligibleParents candidates that can be used as parents of the 
     * offspring.
     * @param population the collection of candidates where eligible candidates
     * are hosted.
     * @param mnt monitoring tool used to record events during the run of the
     * evolutionary algorithm.
     * @param settings the settings of the genetic algorithm.
     * @return a candidate chosen in a stochastic manner.
     */
    protected static List<Candidate> buildCandidatesByXOver(
            List<Candidate> eligibleParents, Population population, 
            Monitor mnt, GAParameters settings) throws DENOPTIMException
    {
        return buildCandidatesByXOver(eligibleParents, population, mnt, 
                null, -1, -1, settings, settings.maxOffsprintFromXover());
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
            List<Candidate> eligibleParents, Population population, 
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
     * offspring (at most two, for now) is returned as result. 
     * This avoids randomized 
     * decision making in case of test that need to be reproducible, 
     * but can be <code>null</code> which means "use random choice".
     * @return the candidate or null if none was produced.
     * @throws DENOPTIMException
     */
    protected static Candidate buildCandidateByXOver(
            List<Candidate> eligibleParents, Population population, 
            Monitor mnt, int[] choiceOfParents, int choiceOfXOverSites,
            int choiceOfOffstring, GAParameters settings) 
                    throws DENOPTIMException 
    {
        List<Candidate> cands = buildCandidatesByXOver(eligibleParents, 
                population, mnt, 
                choiceOfParents, choiceOfXOverSites, choiceOfOffstring, 
                settings, 1);
        if (cands.size()>0)
            return cands.get(0);
        else
            return null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Generates up to a pair of new offspring by performing a crossover 
     * operation.
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
     * offspring (at most two, for now) is returned as result. 
     * This avoids randomized 
     * decision making in case of test that need to be reproducible, 
     * but can be <code>null</code> which means "use random choice".
     * @param maxCandidatesToReturn the number of offspring to return in the 
     * list. Up to 2.
     * @return the list of candidates, or an empty list.
     * @throws DENOPTIMException
     */
    protected static List<Candidate> buildCandidatesByXOver(
            List<Candidate> eligibleParents, Population population, 
            Monitor mnt, int[] choiceOfParents, int choiceOfXOverSites,
            int choiceOfOffstring, GAParameters settings, 
            int maxCandidatesToReturn) throws DENOPTIMException 
    {   
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        mnt.increase(CounterID.XOVERATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);
        
        int numatt = 1;
        
        // Identify a pair of parents that can do crossover, and a pair of
        // vertexes from which we can define a subgraph (or a branch) to swap
        XoverSite xos = null;
        boolean foundPars = false;
        while (numatt < settings.getMaxGeneticOpAttempts())
        {   
            if (fragSpace.useAPclassBasedApproach())
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
                DGraph gpA = parents[0].getGraph();
                List<Vertex> subGraphA = new ArrayList<Vertex>();
                gpA.getChildrenTree(EAUtils.selectNonScaffoldNonCapVertex(
                        gpA, settings.getRandomizer()),subGraphA);

                DGraph gpB = parents[1].getGraph();
                List<Vertex> subGraphB = new ArrayList<Vertex>();
                gpB.getChildrenTree(EAUtils.selectNonScaffoldNonCapVertex(
                        gpB, settings.getRandomizer()),subGraphB);
            }
            foundPars = true;
            break;
        }
        mnt.increaseBy(CounterID.XOVERPARENTSEARCH, numatt);

        if (!foundPars)
        {
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS_FINDPARENTS);
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return new ArrayList<Candidate>();
        }
        
        Candidate cA = null, cB = null;
        Vertex vA = null, vB = null;
        vA = xos.getA().get(0);
        vB = xos.getB().get(0);
        DGraph gA = vA.getGraphOwner();
        cA = gA.getOutermostGraphOwner().getCandidateOwner();
        DGraph gB = vB.getGraphOwner();
        cB = gB.getOutermostGraphOwner().getCandidateOwner();
        
        String candIdA = cA.getName();
        String candIdB = cB.getName();
        int gid1 = gA.getGraphId();
        int gid2 = gB.getGraphId();
        
        // Start building the offspring
        XoverSite xosOnClones = xos.projectToClonedGraphs();
        DGraph gAClone = xosOnClones.getA().get(0).getGraphOwner();
        DGraph gBClone = xosOnClones.getB().get(0).getGraphOwner();
        
        try
        {
            if (!GraphOperations.performCrossover(xosOnClones,fragSpace))
            {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_PERFORM);
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
                return new ArrayList<Candidate>();
            }
        } catch (Throwable t) {
            if (!settings.xoverFailureTolerant)
            {
                t.printStackTrace();
                ArrayList<DGraph> parents = new ArrayList<DGraph>();
                parents.add(gA);
                parents.add(gB);
                DenoptimIO.writeGraphsToSDF(new File(settings.getDataDirectory()
                        + "_failed_xover.sdf"), parents, true,
                        settings.getLogger(), settings.getRandomizer());
                throw new DENOPTIMException("Error while performing crossover! "+NL
                        + "XOverSite:    " + xos.toString() + NL
                        + "XOverSite(C): " + xosOnClones.toString() + NL
                        + " Please, report this to the authors ",t);
            }
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS_PERFORM);
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return new ArrayList<Candidate>();
        }
        gAClone.setGraphId(GraphUtils.getUniqueGraphIndex());
        gBClone.setGraphId(GraphUtils.getUniqueGraphIndex());
        String lstIdVA = "";
        for (Vertex v : xos.getA())
            lstIdVA = lstIdVA + "_" + v.getVertexId();
        String lstIdVB = "";
        for (Vertex v : xos.getB())
            lstIdVB = lstIdVB + "_" + v.getVertexId();
        String[] msgs = new String[2];
        msgs[0] = "Xover: "
                + "Gen:" + cA.getGeneration() + " Cand:" + candIdA 
                + "|" + gid1 + "|" + lstIdVA 
                + " X "
                + "Gen:" + cB.getGeneration() + " Cand:" + candIdB
                + "|" + gid2 + "|" + lstIdVB;
        msgs[1] = "Xover: "
                + "Gen:" + cB.getGeneration() + " Cand:" + candIdB
                + "|" + gid2 + "|" + lstIdVB 
                + " X "
                + "Gen:" + cA.getGeneration() + " Cand:" + candIdA 
                + "|" + gid1 + "|" + lstIdVA;
        
        DGraph[] graphsAffectedByXover = new DGraph[2];
        graphsAffectedByXover[0] = gAClone;
        graphsAffectedByXover[1] = gBClone;
        
        List<Candidate> validOffspring = new Population(settings);
        for (int ig=0; ig<graphsAffectedByXover.length; ig++)
        {
            DGraph g = graphsAffectedByXover[ig];

            // It makes sense to do this on the possibly embedded graph and not
            // on their embedding owners because there cannot be any new cycle
            // affecting the latter, but there can be ones affecting the first.
            if (!EAUtils.setupRings(null, g, settings))
            {
                mnt.increase(CounterID.FAILEDXOVERATTEMPTS_SETUPRINGS);
                continue;
            }

            // Finalize the graph that is at the outermost level
            DGraph gOutermost = g.getOutermostGraphOwner();
            gOutermost.addCappingGroups(fragSpace);
            gOutermost.renumberGraphVertices();
            gOutermost.setLocalMsg(msgs[ig]);
            
            // Consider if the result can be used to define a new candidate
            Object[] res = null;
            try
            {
                res = gOutermost.checkConsistency(settings);
            } catch (NullPointerException|IllegalArgumentException e)
            {
                if (!settings.xoverGraphFailedEvalTolerant)
                {
                    ArrayList<DGraph> parents = new ArrayList<DGraph>();
                    parents.add(gA);
                    parents.add(gB);
                    parents.add(gAClone);
                    parents.add(gBClone);
                    DenoptimIO.writeGraphsToSDF(new File(settings.getDataDirectory()
                            + "_failed_xover-ed_check.sdf"), parents, true,
                            settings.getLogger(), settings.getRandomizer());
                    throw e;
                } else {
                    res = null;
                }
            }
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
            for (Vertex rcv : gOutermost.getFreeRCVertices())
            {
                APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                if (fragSpace.getCappingMap().get(apc)==null 
                        && fragSpace.getForbiddenEndList().contains(apc))
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
            return new ArrayList<Candidate>();
        }
        
        if (maxCandidatesToReturn==1)
        {
            Candidate chosenOffspring = null;
            if (choiceOfOffstring<0)
            {
                chosenOffspring = settings.getRandomizer().randomlyChooseOne(
                        validOffspring);
                chosenOffspring.setName("M" + GeneralUtils.getPaddedString(
                        DENOPTIMConstants.MOLDIGITS,
                        GraphUtils.getUniqueMoleculeIndex()));
            } else {
                chosenOffspring = validOffspring.get(choiceOfOffstring);
            }
            validOffspring.retainAll(Arrays.asList(chosenOffspring));
        } else {
            for (Candidate cand : validOffspring)
            {
                cand.setName("M" + GeneralUtils.getPaddedString(
                        DENOPTIMConstants.MOLDIGITS,
                        GraphUtils.getUniqueMoleculeIndex()));
            }
        }
        return validOffspring;
    }
    
//------------------------------------------------------------------------------

    protected static Candidate buildCandidateByMutation(
            List<Candidate> eligibleParents, Monitor mnt, 
            GAParameters settings) throws DENOPTIMException
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
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
        
        DGraph graph = parent.getGraph().clone();
        graph.renumberGraphVertices();
        
        String parentMolName = FilenameUtils.getBaseName(parent.getSDFFile());
        int parentGraphId = parent.getGraph().getGraphId();
        graph.setLocalMsg("Mutation:"
                + " Gen:" + parent.getGeneration() + " Cand:" + parentMolName 
                + "|" + parentGraphId);
        
        if (!GraphOperations.performMutation(graph,mnt,settings))
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM);
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        graph.addCappingGroups(fragSpace);
        
        Object[] res = null;
        try
        {
            res = graph.checkConsistency(settings);
        } catch (NullPointerException|IllegalArgumentException e)
        {
            if (!settings.mutatedGraphFailedEvalTolerant)
            {
                settings.getLogger().log(Level.INFO, "WRITING DEBUG FILE for " 
                        + graph.getLocalMsg());
                DenoptimIO.writeGraphToSDF(new File("debug_evalGrp_parent.sdf"),
                        parent.getGraph(),false, settings.getLogger(),
                        settings.getRandomizer());
                DenoptimIO.writeGraphToSDF(new File("debug_evalGrp_curr.sdf"), 
                        graph,false, settings.getLogger(),
                        settings.getRandomizer());
                throw e;
            } else {
                res = null;
                mnt.increase(CounterID.FAILEDMUTATTEMTS_EVAL);
            }
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
        for (Vertex rcv : graph.getFreeRCVertices())
        {
            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
            if (fragSpace.getCappingMap().get(apc)==null 
                    && fragSpace.getForbiddenEndList().contains(apc))
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
        offspring.setName("M" + GeneralUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        return offspring;
    }
    
//------------------------------------------------------------------------------
    
    //TODO: move to IO class
    protected static Candidate readCandidateFromFile(File srcFile, Monitor mnt,
            GAParameters settings) throws DENOPTIMException
    {
        mnt.increase(CounterID.MANUALADDATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        ArrayList<DGraph> graphs;
        try
        {
            graphs = DenoptimIO.readDENOPTIMGraphsFromFile(srcFile);
        } catch (Exception e)
        {
            e.printStackTrace();
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Could not read graphs from file " + srcFile
                    + ". No candidate generated!";
            settings.getLogger().log(Level.SEVERE, msg);
            return null;
        }
        if (graphs.size() == 0 || graphs.size() > 1)
        {
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Found " + graphs.size() + " graphs in file " + srcFile
                    + ". I expect one and only one graph. "
                    + "No candidate generated!";
            settings.getLogger().log(Level.SEVERE, msg);
            return null;
        }

        DGraph graph = graphs.get(0);
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDMANUALADDATTEMPTS);
            String msg = "Null graph from file " + srcFile
                    + ". Expected one and only one graph. "
                    + "No candidate generated!";
            settings.getLogger().log(Level.SEVERE, msg);
            return null;
        }
        graph.setLocalMsg("MANUAL_ADD");
        
        // We expect users to know what they ask for. Therefore, we do
        // evaluate the graph, but in a permissive manner, meaning that 
        // several filters are disabled to permit the introduction of graphs 
        // that cannot be generated automatically.
        Object[] res = graph.checkConsistency(settings, true);
        
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
        
        candidate.setName("M" + GeneralUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        String msg = "Candidate " + candidate.getName() + " is imported from " 
                + srcFile;
        settings.getLogger().log(Level.INFO, msg);
        
        return candidate;
    }
    
//------------------------------------------------------------------------------
    
    protected static Candidate buildCandidateFromScratch(Monitor mnt, 
            GAParameters settings) 
            throws DENOPTIMException
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        mnt.increase(CounterID.BUILDANEWATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        DGraph graph = EAUtils.buildGraph(settings);
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS_GRAPHBUILD);
            mnt.increase(CounterID.FAILEDBUILDATTEMPTS);
            return null;
        }
        graph.setLocalMsg("NEW");
        
        Object[] res = graph.checkConsistency(settings);
        
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
        for (Vertex rcv : graph.getFreeRCVertices())
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
            if (fragSpace.getCappingMap().get(apc)==null 
                    && fragSpace.getForbiddenEndList().contains(apc))
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
        
        candidate.setName("M" + GeneralUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
            
        return candidate;
    }
    
//------------------------------------------------------------------------------

    /**
     * Generates a candidate by fragmenting a molecule and generating the graph
     * that reconnects all fragments to reform the original molecule. 
     * Essentially, it converts an {@link IAtomContainer} into a
     * {@link DGraph} and makes a {@link Candidate} out of it.
     * @param mol the molecule to convert.
     * @param cutRules the cutting rules to use in the fragmentation.
     * @param mnt the tool monitoring events for logging purposes.
     * @param settings GA settings.
     * @throws DENOPTIMException 
     */
    public static Candidate buildCandidateByFragmentingMolecule(IAtomContainer mol,
            Monitor mnt, GAParameters settings) throws DENOPTIMException
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters) settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        FragmenterParameters frgParams = new FragmenterParameters();
        if (settings.containsParameters(ParametersType.FRG_PARAMS))
        {
            frgParams = (FragmenterParameters) settings.getParameters(
                    ParametersType.FRG_PARAMS);
        }
        
        if (frgParams.getCuttingRules()==null 
                || frgParams.getCuttingRules().isEmpty())
        {
            throw new DENOPTIMException("Request to generate candidates by "
                    + "fragmentation but no cutting rules provided. Please,"
                    + "add FRG-CUTTINGRULESFILE=path/to/your/file to the "
                    + "input.");
        }
        mnt.increase(CounterID.CONVERTBYFRAGATTEMPTS);
        mnt.increase(CounterID.NEWCANDIDATEATTEMPTS);

        DGraph graph = null;
        try {
            graph = makeGraphFromFragmentationOfMol(mol, 
                    frgParams.getCuttingRules(), settings.getLogger(),
                    frgParams.getScaffoldingPolicy(),
                    frgParams.getLinearAngleLimit());
        } catch (DENOPTIMException de)
        {
            String msg = "Unable to convert molecule (" + mol.getAtomCount() 
                + " atoms) to DENPTIM graph. " + de.getMessage();
            settings.getLogger().log(Level.WARNING, msg);
        }
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDCONVERTBYFRAGATTEMPTS_FRAGMENTATION);
            mnt.increase(CounterID.FAILEDCONVERTBYFRAGATTEMPTS);
            return null;
        }

        if (frgParams.embedRingsInTemplate())
        {
            try {
                graph = graph.embedPatternsInTemplates(GraphPattern.RING, 
                        fragSpace, frgParams.getEmbeddedRingsContract());
            } catch (DENOPTIMException e) {
                graph.cleanup();
                mnt.increase(CounterID.FAILEDCONVERTBYFRAGATTEMPTS_TMPLEMBEDDING);
                return null;
            }
        }
        
        graph.setLocalMsg("INITIAL_MOL_FRAGMENTED");
        
        Object[] res = graph.checkConsistency(settings);
        if (res == null)
        {
            graph.cleanup();
            mnt.increase(CounterID.FAILEDCONVERTBYFRAGATTEMPTS_EVAL);
            return null;
        }
        
        Candidate candidate = new Candidate(graph);
        candidate.setUID(res[0].toString().trim());
        candidate.setSmiles(res[1].toString().trim());
        candidate.setChemicalRepresentation((IAtomContainer) res[2]);
        
        candidate.setName("M" + GeneralUtils.getPaddedString(
                DENOPTIMConstants.MOLDIGITS,
                GraphUtils.getUniqueMoleculeIndex()));
        
        return candidate;
    }

//------------------------------------------------------------------------------  
    
    /**
     * Converts a molecule into a {@link DGraph} by fragmentation and 
     * re-assembling of the fragments.
     * @param mol the molecule to convert
     * @param cuttingRules the cutting rules defining how to do fragmentation.
     * @param logger tool managing log.
     * @param scaffoldingPolicy the policy for deciding which vertex should be 
     * given the role of scaffold.
     * @return the graph.
     * @throws DENOPTIMException 
     */
    public static DGraph makeGraphFromFragmentationOfMol(IAtomContainer mol,
            List<CuttingRule> cuttingRules, Logger logger, 
            ScaffoldingPolicy scaffoldingPolicy) 
                    throws DENOPTIMException
    {
        return makeGraphFromFragmentationOfMol(mol, cuttingRules, logger, 
                scaffoldingPolicy, 190); 
        // NB: and angle of 190 means we are not adding Du on linearities
    }
    
//------------------------------------------------------------------------------  
    
    /**
     * Converts a molecule into a {@link DGraph} by fragmentation and 
     * re-assembling of the fragments.
     * @param mol the molecule to convert
     * @param cuttingRules the cutting rules defining how to do fragmentation.
     * @param logger tool managing log.
     * @param scaffoldingPolicy the policy for deciding which vertex should be 
     * given the role of scaffold.
     * @param linearAngleLimit the max bond angle before we start considering 
     * the angle linear and add a linearity-breaking dummy atom.
     * @return the graph.
     * @throws DENOPTIMException 
     */
    public static DGraph makeGraphFromFragmentationOfMol(IAtomContainer mol,
            List<CuttingRule> cuttingRules, Logger logger, 
            ScaffoldingPolicy scaffoldingPolicy, double linearAngleLimit) 
                    throws DENOPTIMException
    {
        // We expect only Fragments here.
        List<Vertex> fragments = FragmenterTools.fragmentation(mol, 
                cuttingRules, logger);
        for (Vertex v : fragments)
        {
            Fragment frag = (Fragment) v;
            
            // This is done to set the symmetry relations in each vertex
            frag.updateAPs();
            
            // Add linearity-breaking dummy atoms
            DummyAtomHandler.addDummiesOnLinearities(frag, linearAngleLimit);
        }
        if (fragments.size()==0)
        {
            throw new DENOPTIMException("Fragmentation of molecule with "
                    + mol.getAtomCount() + " atoms produced 0 fragments.");
        }
        
        // Define which fragment is the scaffold
        Vertex scaffold = null;
        switch (scaffoldingPolicy)
        {
            case ELEMENT:
            {
                for (Vertex v : fragments)
                {
                    if (v instanceof Fragment)
                    {
                        boolean setAsScaffold = false;
                        IAtomContainer iac = v.getIAtomContainer();
                        for (IAtom atm : iac.atoms())
                        {
                            if (scaffoldingPolicy.label.equals(
                                    MoleculeUtils.getSymbolOrLabel(atm)))
                            {
                                setAsScaffold = true;
                                break;
                            }
                        }
                        if (setAsScaffold)
                        {
                            scaffold = v;
                            break;
                        }
                    }
                }
                break;
            }

            default:
            case LARGEST_FRAGMENT:
            {
                try {
                    scaffold = fragments.stream()
                            .max(Comparator.comparing(
                                    Vertex::getHeavyAtomsCount))
                            .get();
                } catch (Exception e)
                {
                    throw new DENOPTIMException("Cannot get largest fragment "
                            + "among " + fragments.size() + " fragments.", e);
                }
                break;
            }
        }
        if (scaffold==null)
        {
            throw new DENOPTIMException("No fragment matches criteria to be "
                    + "identified as the " 
                    + BBType.SCAFFOLD.toString().toLowerCase() + ".");
        }
        scaffold.setVertexId(0);
        scaffold.setBuildingBlockType(BBType.SCAFFOLD);
        
        // Build the graph
        DGraph graph = new DGraph();
        graph.addVertex(scaffold);
        AtomicInteger vId = new AtomicInteger(1);
        for (int i=1; i<fragments.size(); i++)
        {
            appendVertexesToGraphFollowingEdges(graph, vId, fragments);
        }
        
        // Set symmetry relations: these depend on which scaffold we have chosen
        graph.detectSymVertexSets();
        
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    private static void appendVertexesToGraphFollowingEdges(DGraph graph,
            AtomicInteger vId, List<Vertex> vertexes) throws DENOPTIMException
    {
        // We seek for  the last and non-RCV vertex added to the graph
        Vertex lastlyAdded = null;
        for (int i=-1; i>-4; i--)
        {
            lastlyAdded = graph.getVertexList().get(
                    graph.getVertexList().size()+i);
            if (!lastlyAdded.isRCV())
                break;
        }
        for (AttachmentPoint apI : lastlyAdded.getAttachmentPoints())
        {
            if (!apI.isAvailable())
                continue;
            
            for (int j=0; j<vertexes.size(); j++)
            {
                Vertex fragJ = vertexes.get(j);
                
                boolean ringClosure = false;
                if (graph.containsVertex(fragJ))
                {   
                    ringClosure = true;
                }
                for (AttachmentPoint apJ : fragJ.getAttachmentPoints())
                {
                    if (apI==apJ)
                        continue;
                    
                    if (apI.getCutId()==apJ.getCutId())
                    {
                        if (ringClosure)
                        {
                            Vertex rcvI = FragmenterTools.getRCPForAP(apI,
                                    APClass.make(APClass.ATPLUS, 0, 
                                            BondType.ANY));
                            rcvI.setBuildingBlockType(BBType.FRAGMENT);
                            rcvI.setVertexId(vId.getAndIncrement());
                            graph.appendVertexOnAP(apI, rcvI.getAP(0));
                            
                            Vertex rcvJ = FragmenterTools.getRCPForAP(apJ,
                                    APClass.make(APClass.ATMINUS, 0, 
                                            BondType.ANY));
                            rcvJ.setBuildingBlockType(BBType.FRAGMENT);
                            rcvJ.setVertexId(vId.getAndIncrement());
                            graph.appendVertexOnAP(apJ, rcvJ.getAP(0));
                            graph.addRing(rcvI, rcvJ);
                        } else {
                            fragJ.setBuildingBlockType(BBType.FRAGMENT);
                            fragJ.setVertexId(vId.getAndIncrement());
                            graph.appendVertexOnAP(apI, apJ);
                            
                            // Recursion into the branch of the graph that is
                            // rooted onto the lastly added vertex
                            appendVertexesToGraphFollowingEdges(graph, vId, 
                                    vertexes);
                        }
                    }
                }
            }
        }
    }

//------------------------------------------------------------------------------  
    
    /**
     * Write out summary for the current GA population.
     * @param population
     * @param filename
     * @throws DENOPTIMException
     */

    public static void outputPopulationDetails(Population population, 
            String filename, GAParameters settings, boolean printpathNames) 
                    throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(DENOPTIMConstants.GAGENSUMMARYHEADER);
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
                    
                    if (printpathNames)
                    {
                        sb.append("    ").append(mol.getSDFFile());
                    }
                    
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
        double sdev = StatUtils.stddev(fitness, true);
        String res = "";
        df.setMaximumFractionDigits(settings.getPrecisionLevel());

        StringBuilder sb = new StringBuilder(128);
        sb.append(NL+NL+"#####POPULATION SUMMARY#####"+NL);
        int n = popln.size();
        sb.append(String.format("%-30s", "SIZE:"));
        sb.append(String.format("%12s", n));
        sb.append(NL);
        double f;
        f = StatUtils.max(fitness);
        sb.append(String.format("%-30s", "MAX:")).append(df.format(f));
        sb.append(NL);
        f = StatUtils.min(fitness);
        sb.append(String.format("%-30s", "MIN:")).append(df.format(f));
        sb.append(NL);
        f = StatUtils.mean(fitness);
        sb.append(String.format("%-30s", "MEAN:")).append(df.format(f));
        sb.append(NL);
        f = StatUtils.median(fitness);
        sb.append(String.format("%-30s", "MEDIAN:")).append(df.format(f));
        sb.append(NL);
        f = StatUtils.stddev(fitness, true);
        sb.append(String.format("%-30s", "STDDEV:")).append(df.format(f));
        sb.append(NL);
        if (sdev > 0.0001)
        {
            f = StatUtils.skewness(fitness, true);
            sb.append(String.format("%-30s", "SKEW:")).append(df.format(f));
            sb.append(NL);
        } else {
            sb.append(String.format("%-30s", "SKEW:")).append(" NaN (sdev too small)");
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
     * @param eligibleParents the list of candidates to chose from.
     * @param number how many candidate to pick.
     * @return indexes of the selected members of the given population.
     */

    protected static Candidate[] selectBasedOnFitness(
            List<Candidate> eligibleParents, int number, GAParameters settings)
    {
        Candidate[] mates = new Candidate[number];
        switch (settings.getSelectionStrategyType())
        {
        case 1:
            mates = SelectionHelper.performTournamentSelection(eligibleParents, 
                    number, settings);
            break;
        case 2:
            mates = SelectionHelper.performRWS(eligibleParents, number, settings);
            break;
        case 3:
            mates = SelectionHelper.performSUS(eligibleParents, number, settings);
            break;
        case 4:
            mates = SelectionHelper.performRandomSelection(eligibleParents, number, 
                    settings);
            break;
        }
        return mates;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Chose randomly a vertex that is neither scaffold or capping group.
     */
    protected static Vertex selectNonScaffoldNonCapVertex(DGraph g, 
            Randomizer randomizer)
    {
        List<Vertex> candidates = new ArrayList<Vertex>(
                g.getVertexList());
        candidates.removeIf(v ->
                v.getBuildingBlockType() == BBType.SCAFFOLD
                || v.getBuildingBlockType() == BBType.CAP);
        return randomizer.randomlyChooseOne(candidates);
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
            List<Candidate> eligibleParents, Population population, 
            int[] choiceOfParents, int choiceOfXOverSites, GAParameters settings)
    {
        Candidate parentA = null;
        if (choiceOfParents==null)
            parentA = selectBasedOnFitness(eligibleParents, 1, settings)[0];
        else
            parentA = eligibleParents.get(choiceOfParents[0]);
        
        if (parentA == null)
            return null;
        
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        List<Candidate> matesCompatibleWithFirst = population.getXoverPartners(
                parentA, eligibleParents, fragSpace);
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
            result = settings.getRandomizer().randomlyChooseOne(
                    population.getXoverSites(parentA, parentB));
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
        
        sb.append(settings.getDataDirectory()).append(FSEP).append(
                DENOPTIMConstants.GAGENDIRNAMEROOT)
            .append(GeneralUtils.getPaddedString(ndigits, genID));
        
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getPathNameToGenerationDetailsFile(int genID, 
            GAParameters settings)
    {
        StringBuilder sb = new StringBuilder(32);
        
        int ndigits = String.valueOf(settings.getNumberOfGenerations()).length();
        
        sb.append(settings.getDataDirectory()).append(FSEP)
            .append(DENOPTIMConstants.GAGENDIRNAMEROOT)
            .append(GeneralUtils.getPaddedString(ndigits, genID))
            .append(FSEP)
            .append(DENOPTIMConstants.GAGENDIRNAMEROOT)
            .append(GeneralUtils.getPaddedString(ndigits, genID))
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
     * Saves the final results to disk. If the files for each candidate have 
     * been saved on disk along the way, then it copies them from their location
     * into the folder for final results, which is defined based on the 
     * GA settings.
     * @param popln the final list of best molecules
     * @param settings the GS settings containing defaults and parameters given 
     * by the user.
     */

    protected static void outputFinalResults(Population popln, 
            GAParameters settings) throws DENOPTIMException
    {
        String dirName = EAUtils.getPathNameToFinalPopulationFolder(settings);
        denoptim.files.FileUtils.createDirectory(dirName);
        File fileDir = new File(dirName);
        
        boolean intermediateCandidatesAreOnDisk = 
                ((FitnessParameters) settings.getParameters(
                ParametersType.FIT_PARAMS)).writeCandidatesOnDisk();

        for (int i=0; i<popln.size(); i++)
        {
            Candidate c = popln.get(i);
            String sdfile = c.getSDFFile();
            String imgfile = c.getImageFile();

            try {
                if (intermediateCandidatesAreOnDisk && sdfile!=null)
                {
                    FileUtils.copyFileToDirectory(new File(sdfile), fileDir);
                } else {
                    File candFile = new File(fileDir, c.getName() 
                            + DENOPTIMConstants.FITFILENAMEEXTOUT);
                    c.setSDFFile(candFile.getAbsolutePath());
                    DenoptimIO.writeCandidateToFile(candFile, c, false);
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException("Failed to copy file '" 
                        + sdfile + "' to '" + fileDir + "' for candidate "
                        + c.getName(), ioe);
            }
            if (imgfile != null && intermediateCandidatesAreOnDisk)
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
                settings, true);        
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
            String genDir, GAParameters settings) 
                    throws DENOPTIMException, IOException
    {
        ArrayList<Candidate> candidates = DenoptimIO.readCandidates(
                new File(filename), true);
        if (candidates.size() == 0)
        {
        	String msg = "Found 0 candidates in file " + filename;
            settings.getLogger().log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        for (Candidate candidate : candidates)
        {
            if (uniqueIDsSet.addNewUniqueEntry(candidate.getUID()))
            {
                int ctr = GraphUtils.getUniqueMoleculeIndex();
                int gctr = GraphUtils.getUniqueGraphIndex();
                
                String molName = "M" + GeneralUtils.getPaddedString(8, ctr);
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
            settings.getLogger().log(Level.SEVERE, msg);
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
        long val = Long.MIN_VALUE;
        for (Candidate popln1 : population)
        {
            DGraph g = popln1.getGraph();
            val = Math.max(val, g.getMaxVertexId());
        }
        GraphUtils.ensureVertexIDConsistency(val);
    }

//------------------------------------------------------------------------------

    /**
     * Graph construction starts with selecting a random core/scaffold.
     *
     * @return the molecular graph representation
     * @throws DENOPTIMException
     */

    protected static DGraph buildGraph(GAParameters settings) 
            throws DENOPTIMException
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        DGraph graph = new DGraph();
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        // building a molecule starts by selecting a random scaffold
        Vertex scafVertex = fragSpace.makeRandomScaffold();
        
        // add the scaffold as a vertex
        graph.addVertex(scafVertex);
        graph.setLocalMsg("NEW");
        
        if (scafVertex instanceof Template
                && !((Template) scafVertex).getContractLevel().equals(
                        ContractLevel.FIXED))
        {
            Monitor mnt = new Monitor();
            mnt.name = "IntraTemplateBuild";
            List<Vertex> initialMutableSites = graph.getMutableSites(
                    settings.getExcludedMutationTypes());
            for (Vertex mutableSite : initialMutableSites)
            {
                // This accounts for the possibility that a mutation changes a 
                // branch of the initial graph or deletes vertexes.
                if (!graph.containsOrEmbedsVertex(mutableSite))
                    continue;
                
                // TODO: need to discriminate between EmptyVertexes that 
                // represent placeholders and those that represent property carriers
                // The first should always be mutated (as it happens now), but
                // the latter should be kept intact.
                // Possibly this is a case for subclassing the EmptyVertex.
                
                if (!GraphOperations.performMutation(mutableSite, mnt,
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
        graph.setCandidateClosableChains(rca.getCCFromTurningPointId(
                scafVertex.getBuildingBlockId()));

        if (scafVertex.hasFreeAP())
        {
            GraphOperations.extendGraph(scafVertex, true, false, settings);
        }
        
        if (!(scafVertex instanceof Template) 
                && graph.getVertexCount() == 0)
        {
            return null;
        }
        
        graph.addCappingGroups(fragSpace);
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

    protected static boolean setupRings(Object[] res, DGraph molGraph, 
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
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        if (!fragSpace.useAPclassBasedApproach())
            return true;

        if (!rcParams.allowRingClosures())
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(settings.getLogger(),
                settings.getRandomizer());
        t3d.setAlignBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(molGraph,true);
        
        // Set rotatability property as property of IBond
        String rotoSpaceFile = "";
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            rotoSpaceFile = ((FragmentSpaceParameters) settings.getParameters(
                    ParametersType.FS_PARAMS)).getRotSpaceDefFile();
        }
        RotationalSpaceUtils.defineRotatableBonds(mol, rotoSpaceFile, true,
                true, settings.getLogger());
        
        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(rcParams,fragSpace);

        //TODO: remove hard-coded variable that exclude considering all 
        // combination of rings
        boolean onlyRandomCombOfRings = true;
        
        if (onlyRandomCombOfRings)
        {
            List<Ring> combsOfRings = cgh.getRandomCombinationOfRings(
                    mol, molGraph, rcParams.getMaxRingClosures());
            if (combsOfRings.size() > 0)
            {
                for (Ring ring : combsOfRings)
                {
                    // Consider the crowding probability
                    double shot = settings.getRandomizer().nextDouble();
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
            ArrayList<List<Ring>> allCombsOfRings = 
                            cgh.getPossibleCombinationOfRings(mol, molGraph);
        
            // Keep closable chains that are relevant for chelate formation
            if (rcParams.buildChelatesMode())
            {
                ArrayList<List<Ring>> toRemove = new ArrayList<>();
                for (List<Ring> setRings : allCombsOfRings)
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
                    settings.getLogger().log(Level.INFO, msg);
                    return false;
                }
            }

            // Select a combination, if any still available
            int sz = allCombsOfRings.size();
            if (sz > 0)
            {
                List<Ring> selected = new ArrayList<>();
                if (sz == 1)
                {
                    selected = allCombsOfRings.get(0);
                }
                else
                {
                    int selId = settings.getRandomizer().nextInt(sz);
                    selected = allCombsOfRings.get(selId);
                }

                // append new rings to existing list of rings in graph
                for (Ring ring : selected)
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
            String molsmiles = MoleculeUtils.getSMILESForMolecule(mol,
                    settings.getLogger());
            if (molsmiles == null)
            {
                String msg = "Evaluation of graph: SMILES is null! "
                                                            + molGraph.toString();
                settings.getLogger().log(Level.INFO, msg);
                molsmiles = "FAIL: NO SMILES GENERATED";
            }
            res[1] = molsmiles;
        }

        // Update the INCHI key representation
        if (res!=null)
        {
            String inchikey = MoleculeUtils.getInChIKeyForMolecule(mol, 
                    settings.getLogger());
            if (inchikey == null)
            {
                String msg = "Evaluation of graph: INCHI is null!";
                settings.getLogger().log(Level.INFO, msg);
                inchikey = "UNDEFINED";
            }
            res[0] = inchikey;
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
        return StatUtils.stddev(fitvals, true);
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
    public static double getMolSizeProbability(DGraph graph, 
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
    public static double getMolSizeProbability(DGraph graph,
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
    public static double getCrowdingProbability(AttachmentPoint ap,
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
     * Use {@link EAUtils#getCrowdingProbability(AttachmentPoint)} to
     * get the crowding probability for an actual attachment point.
     * Use {@link EAUtils#getCrowdedness(AttachmentPoint)} to calculate
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
    public static int getCrowdedness(AttachmentPoint ap)
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
    public static int getCrowdedness(AttachmentPoint ap, 
            boolean ignoreFreeRCVs)
    {
        if (ap.getOwner() instanceof EmptyVertex)
        {
            return 0;
        }
        int crowdness = 0;
        DGraph g = ap.getOwner().getGraphOwner();
        for (AttachmentPoint oap : ap.getOwner().getAttachmentPoints())
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
    public static double getCrowdingProbability(AttachmentPoint ap, 
            int scheme,
            double lambda, double sigmaOne, double sigmaTwo)
    {   
        //Applies only to molecular fragments
        if (ap.getOwner() instanceof Fragment == false)
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

    protected static boolean foundForbiddenEnd(DGraph molGraph,
            FragmentSpaceParameters fsParams)
    {
        List<Vertex> vertices = molGraph.getVertexList();
        Set<APClass> classOfForbEnds = fsParams.getFragmentSpace()
                .getForbiddenEndList();
        for (Vertex vtx : vertices)
        {
            List<AttachmentPoint> daps = vtx.getAttachmentPoints();
            for (AttachmentPoint dp : daps)
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
                        fsParams.getLogger().log(Level.WARNING, msg);
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
