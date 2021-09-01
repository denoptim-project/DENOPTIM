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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;
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
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.Candidate;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;
import denoptim.rings.RingClosuresArchive;
import denoptim.task.TasksBatchManager;
import denoptim.threedim.ThreeDimTreeBuilder;
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

    // cluster the fragments based on their #APs
    protected static HashMap<Integer, ArrayList<Integer>> fragmentPool;
    
    /**
     * Locale used to write reports
     */
    private static Locale enUsLocale = new Locale("en", "US");
    
    /**
     * Format for decimal fitness numbers that overwrites Locale to en_US
     */
    protected static DecimalFormat df = (DecimalFormat)
    		NumberFormat.getNumberInstance(enUsLocale);
    
    // for each fragment store the reactions associated with it
    protected static HashMap<Integer, ArrayList<String>> lstFragmentClass;

    /**
     * A chosen method for generation of new {@link Candidate}s.
     */
    public enum CandidateSource {
        CROSSOVER, MUTATION, CONSTRUCTION;
    }
    
    private static final String NL =System.getProperty("line.separator");
    private static final String FSEP = System.getProperty("file.separator");
   
    // flag for debugging
    private static final boolean DEBUG = false;

//------------------------------------------------------------------------------

    /**
     * Creates a folder meant to hold all the data generated during a generation.
     * The folder is created under the work space.
     * @param genId the generation's identity number
     * @throws DENOPTIMException
     */
    protected static void createFolderForGeneration(int genId)
    {
        DenoptimIO.createDirectory(EAUtils.getPathNameToGenerationFolder(genId));
    }

//------------------------------------------------------------------------------

    /**
     * Reads unique identifiers and initial population files according to the
     * {@link GAParameters}.
     */
    protected static Population importInitialPopulation() throws DENOPTIMException
    {
        Population population = new Population();

        HashSet<String> lstUID = new HashSet<>(1024);
        if (!GAParameters.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(GAParameters.getUIDFileIn(),lstUID);
            EAUtils.writeUID(GAParameters.getUIDFileOut(),lstUID,false); //overwrite
            DENOPTIMLogger.appLogger.log(Level.INFO, "Read " + lstUID.size() 
                + " known UIDs from " + GAParameters.getUIDFileIn());
        }
        String inifile = GAParameters.getInitialPopulationFile();
        if (inifile.length() > 0)
        {
            EAUtils.getPopulationFromFile(inifile, population, lstUID, 
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
        int mvid = -1, fvid = -1;
        boolean foundPars = false;
        DENOPTIMVertex[] pair = null;
        while (numatt < GAParameters.getMaxGeneticOpAttempts())
        {   
            if (FragmentSpace.useAPclassBasedApproach())
            {
                pair = EAUtils.performFBCC(eligibleParents, population);
                if (pair == null)
                {
                    numatt++;
                    continue;
                }
                maleCandidate = pair[0].getGraphOwner().getCandidateOwner();
                femaleCandidate = pair[1].getGraphOwner().getCandidateOwner();
                mvid = pair[0].getGraphOwner().indexOf(pair[0]);
                fvid = pair[1].getGraphOwner().indexOf(pair[1]);
            } else {
                int parents[] = EAUtils.selectBasedOnFitness(eligibleParents, 2);
                if (parents[0] == -1 || parents[1] == -1)
                {
                    numatt++;
                    continue;
                }
                maleCandidate = eligibleParents.get(parents[0]);
                femaleCandidate = eligibleParents.get(parents[1]);
                mvid = EAUtils.selectNonScaffoldNonCapVertex(
                        maleCandidate.getGraph());
                fvid = EAUtils.selectNonScaffoldNonCapVertex(
                        femaleCandidate.getGraph());
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
        
        String molid1 = maleCandidate.getName();
        String molid2 = femaleCandidate.getName();

        int gid1 = maleCandidate.getGraph().getGraphId();
        int gid2 = femaleCandidate.getGraph().getGraphId();
        
        DENOPTIMGraph graph1 = maleCandidate.getGraph().clone();
        DENOPTIMGraph graph2 = femaleCandidate.getGraph().clone();
        
        graph1.renumberGraphVertices();
        graph2.renumberGraphVertices();

        //TODO: evaluate if all this can be simplified by using refs to vertexes
        if (!DENOPTIMGraphOperations.performCrossover(graph1, 
                graph1.getVertexAtPosition(mvid).getVertexId(),
                graph2,
                graph2.getVertexAtPosition(fvid).getVertexId()))
        {

            mnt.increase(CounterID.FAILEDXOVERATTEMPTS_PERFORM);
            mnt.increase(CounterID.FAILEDXOVERATTEMPTS);
            return null;
        }
        graph1.setGraphId(GraphUtils.getUniqueGraphIndex());
        graph2.setGraphId(GraphUtils.getUniqueGraphIndex());
        EAUtils.addCappingGroup(graph1);
        EAUtils.addCappingGroup(graph2);
        String msg = "Xover: "+molid1+"|"+gid1+"="+molid2+"|"+gid2;
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
            //TODO-V3 this should be considered already when making the list of
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
        int parentIdx = -1;
        while (numatt < GAParameters.getMaxGeneticOpAttempts())
        {
            parentIdx = EAUtils.selectBasedOnFitness(eligibleParents,1)[0];
            if (parentIdx == -1)
            {
                numatt++;
                continue;
            }
            break;
        }
        mnt.increaseBy(CounterID.MUTPARENTSEARCH,numatt);
        if (parentIdx == -1)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        
        Candidate parent = eligibleParents.get(parentIdx);
        DENOPTIMGraph graph = parent.getGraph().clone();
        graph.renumberGraphVertices();
        
        String parentMolName = FilenameUtils.getBaseName(parent.getSDFFile());
        int parentGraphId = parent.getGraph().getGraphId();

        if (!DENOPTIMGraphOperations.performMutation(graph,mnt))
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM);
            mnt.increase(CounterID.FAILEDMUTATTEMTS);
            return null;
        }
        graph.setGraphId(GraphUtils.getUniqueGraphIndex());
        graph.setLocalMsg("Mutation: " + parentMolName + "|" + parentGraphId);
    
        EAUtils.addCappingGroup(graph);
        
        Object[] res = EAUtils.evaluateGraph(graph);

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
        //TODO-V3 this should be considered already when making the list of
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
        //TODO-V3 this should be considered already when making the list of
        // possible combination of rings
        for (DENOPTIMVertex rcv : graph.getFreeRCVertices())
        {
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
        String stats = getSummaryStatistics(population);
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

        int sz = FragmentSpace.getScaffoldLibrary().size();
        HashMap<Integer, Integer> scf_cntr = new HashMap<>();
        for (int i=1; i<=sz; i++)
        {
            scf_cntr.put(i, 0);
        }

        for (int i=0; i<GAParameters.getPopulationSize(); i++)
        {
            Candidate mol = popln.get(i);
            DENOPTIMGraph g = mol.getGraph();
            int scafIdx = g.getVertexAtPosition(0).getBuildingBlockId() + 1;
            scf_cntr.put(scafIdx, scf_cntr.get(scafIdx)+1);
        }

        sb.append(NL+NL+"#####SCAFFOLD ANALYSIS#####"+NL);
        for (Map.Entry pairs : scf_cntr.entrySet())
        {
            sb.append(pairs.getKey()).append(" ").append(pairs.getValue());
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
     * @param keys the list of candidates to chose from.
     * @param number how many candidate to pick.
     * @return indexes of the selected members of the given population.
     */

    protected static int[] selectBasedOnFitness(ArrayList<Candidate> keys, 
            int number)
    {
        int[] mates = new int[number];
        for (int i=0; i<number; i++)
        {
            mates[i] = -1;
        }
        switch (GAParameters.getSelectionStrategyType())
        {
        case 1:
            mates = SelectionHelper.performTournamentSelection(keys, 
                    number);
            break;
        case 2:
            mates = SelectionHelper.performRWS(keys, number);
            break;
        case 3:
            mates = SelectionHelper.performSUS(keys, number);
            break;
        case 4:
            mates = SelectionHelper.performRandomSelection(keys, number);
            break;
        }
        return mates;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Chose randomly a vertex that is neither scaffold or capping group.
     */
    protected static int selectNonScaffoldNonCapVertex(DENOPTIMGraph g)
    {
        List<DENOPTIMVertex> candidates = new ArrayList<DENOPTIMVertex>(g.getVertexList());
        candidates.removeIf(v ->
                v.getBuildingBlockType() == BBType.SCAFFOLD
                || v.getBuildingBlockType() == BBType.CAP);
        return g.indexOf(RandomUtils.randomlyChooseOne(candidates));
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
        int p1 = selectBasedOnFitness(eligibleParents, 1)[0];
        if (p1 == -1)
            return null;
        
        Candidate parentA = eligibleParents.get(p1);
        DENOPTIMGraph g1 = parentA.getGraph();
        
        ArrayList<Candidate> matesCompatibleWithFirst = 
                population.getXoverPartners(parentA,eligibleParents);
        if (matesCompatibleWithFirst.size() == 0)
            return null;
        
        int chosen = selectBasedOnFitness(matesCompatibleWithFirst,1)[0];
        if (chosen < 0)
            return null;
        Candidate parentB = matesCompatibleWithFirst.get(chosen);
        
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
        DenoptimIO.createDirectory(dirName);
        File fileDir = new File(dirName);

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
            outputPopulationDetails(popln,
                    EAUtils.getPathNameToFinalPopulationDetailsFile());
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
     * @param population
     * @param lstInchi
     * @throws DENOPTIMException
     */
    protected static void getPopulationFromFile(String filename,
            Population population, HashSet<String> lstInchi,
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
            Object apProperty = mol.getProperty(DENOPTIMConstants.GRAPHTAG);
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

            apProperty = mol.getProperty(DENOPTIMConstants.FITNESSTAG);
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

            apProperty = mol.getProperty(DENOPTIMConstants.SMILESTAG);
            if (apProperty != null)
            {
                molsmiles = apProperty.toString().trim();
            }
            else
            {
                molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            }

            apProperty = mol.getProperty(DENOPTIMConstants.INCHIKEYTAG);
            if (apProperty != null)
            {
                molinchi = apProperty.toString();
            }
            if (mol.getProperty(DENOPTIMConstants.UNIQUEIDTAG) != null)
            {
                molinchi = mol.getProperty(DENOPTIMConstants.UNIQUEIDTAG).toString();
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
                molfile = genDir + fsep + molName + 
                        DENOPTIMConstants.FITFILENAMEEXTOUT;

                int gctr = GraphUtils.getUniqueGraphIndex();
                graph.setGraphId(gctr);
                graph.setLocalMsg("NEW");
                mol.setProperty(DENOPTIMConstants.GCODETAG, gctr);
                mol.setProperty(CDKConstants.TITLE, molName);
                mol.setProperty(DENOPTIMConstants.GRAPHTAG, graph.toString());
                mol.setProperty(DENOPTIMConstants.GMSGTAG, 
                        "From Initial Population File");

                Candidate pmol = new Candidate(molName, graph, fitness, 
                        molinchi, molsmiles);
                
                DenoptimIO.writeMolecule(molfile, mol, false);
                pmol.setSDFFile(molfile);
                pmol.setImageFile(null);
                pmol.setName(molName);
                population.add(pmol);
                uidsFromInitPop.add(molinchi);
            }
        }
        writeUID(GAParameters.getUIDFileOut(),uidsFromInitPop,true);

        if (population.isEmpty())
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                                        "No data found in file {0}", filename);
            throw new DENOPTIMException("No data found in file " + filename);
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
     * Set the Vertex counter value
     * @param population
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
        DENOPTIMGraph molGraph = new DENOPTIMGraph();
        molGraph.setGraphId(GraphUtils.getUniqueGraphIndex());
        
        // building a molecule starts by selecting a random scaffold
        int scafIdx = selectRandomScaffold();

        DENOPTIMVertex scafVertex = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), scafIdx, DENOPTIMVertex.BBType.SCAFFOLD);

        // we set the level to -1, as the base
        scafVertex.setLevel(-1);
        
        //TODO-V3: did we pick a template? Then, we'll have to deal with it:
        // meaning, if the template has a fully finished content, it just behaves as
        // a normal vertex, otherwise it has to be filled with content.
        
        // add the scaffold as a vertex
        molGraph.addVertex(scafVertex);
        molGraph.setLocalMsg("NEW");

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
            String filename = "/tmp/" + molGraph.getGraphId() + "_growth.sdf";
            DenoptimIO.deleteFile(filename);
        }

        DENOPTIMGraphOperations.extendGraph(scafVertex, true, false);

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
        if (reacFrags.size() > 0)
        {
            fapidx = RandomUtils.randomlyChooseOne(reacFrags);
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

        //TODO: remove hard-coded variable that exclude considering combination of rings
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
        if (!FragmentSpace.useAPclassBasedApproach())
            return;

        ArrayList<DENOPTIMVertex> lstVert = molGraph.getVertexList();

        for (int i=0; i<lstVert.size(); i++)
        {
            // check if the vertex has a free valence
            DENOPTIMVertex curVertex = lstVert.get(i);

            // no capping of a capping group. Since capping groups are expected
            // to have only one AP, there should never be a capping group with 
            // a free AP.
            if (curVertex.getBuildingBlockType() == DENOPTIMVertex.BBType.CAP)
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
                    APClass apcCap = FragmentSpace.getAPClassOfCappingVertex(
                            curDap.getAPClass());
                    if (apcCap != null)
                    {
                        int bbIdCap = getCappingFragment(apcCap);

                        if (bbIdCap != -1)
                        {
                            DENOPTIMVertex capVrtx = DENOPTIMVertex.newVertexFromLibrary(
                                    GraphUtils.getUniqueVertexIndex(), bbIdCap, 
                                    DENOPTIMVertex.BBType.CAP);
                            molGraph.appendVertexOnAP(curDap, capVrtx.getAP(0));
                        }
                        else
                        {
                            String msg = "Capping is required but no proper capping "
                                            + "fragment found with APCalss " + apcCap;
                            DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                            throw new DENOPTIMException(msg);
                        }
                    }
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
     * Extracts the list of unique identifiers from a list of candidates.
     * @param candidateParents
     * @return list of INCHI codes for the molecules in the population
     */

    protected static ArrayList<String> getUniqueIdentifiers(
            ArrayList<Candidate> candidateParents)
    {
        ArrayList<String> arr = new ArrayList<>();
        for (Candidate c : candidateParents)
        {
            arr.add(c.getUID());
        }
        return arr;
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
        if (molGraph == null)
        {
            String msg = "Evaluation of graph: graph is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // calculate the molecule representation
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(molGraph,true);

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
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
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
        
        int crowdness = 0;
        for (DENOPTIMAttachmentPoint oap : ap.getOwner().getAttachmentPoints())
        {
            if (oap.getAtomPositionNumber() == ap.getAtomPositionNumber()
                    && !oap.isAvailable())
            {
                crowdness = crowdness + 1;
            }
        }
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
        double prob = 1.0;
        if (scheme == 0)
        {
            double f = Math.exp(-1.0 * crowdedness * lambda);
            prob = 1 - ((1-f)/(1+f));
        }
        else if (scheme == 1)
        {
            prob = 1.0 - Math.tanh(lambda * crowdedness);
        }
        else if (scheme == 2)
        {
            prob = 1.0-1.0/(1.0 + Math.exp(-sigmaOne * (crowdedness - sigmaTwo)));
        }
        else if (scheme == 3)
        {
            prob = 1.0;
        }
        
        return prob;
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
