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

package denoptim.programs.fragmenter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.combinatorial.CEBLUtils;
import denoptim.combinatorial.CheckPoint;
import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.fragmenter.FragmentClusterer;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.programs.RunTimeParameters;
import denoptim.utils.FormulaUtils;


/**
 * Parameters controlling execution of the frqgmenter.
 * 
 * @author Marco Foscato
 */

public class FragmenterParameters extends RunTimeParameters
{
    /**
     * Pathname to the file containing the structures of the molecules 
     * to fragment.
     */
    private String structuresFile;
    
    /**
     * Pathname to the file containing the formulae of the molecules 
     * to fragment
     */
    private String formulaeFile;
    
    /**
     * Molecular formula read-in from CSD file. Data
     * collected by CSD refcode.
     */
    private LinkedHashMap<String, String> formulae;
    
    /**
     * Pathname to the file containing the cutting rules.
     */
    private String cutRulesFile;
    
    /**
     * List of cutting rules sorted by priority.
     */
    List<CuttingRule> cuttingRules;
    
    /**
     * List of known 'any-atom' SMARTS queries. We keep this information to
     * by-pass any attempt to search for any atom.
     */
    List<String> anyAtomQuesties;
    
    /**
     * Number of parallel tasks to run.
     */
    private int numParallelTasks = 1;
    
    /**
     * Flag requesting the execution of elemental analysis and comparison 
     * of the content of the structure file against a given molecular formula.
     * This task is meant to identify structures with missing atoms.
     */
    private boolean checkFormula = false;
    
    /**
     * Flag requesting to force-accepting the approximation that converts all
     * unset bond orders to single bond orders. This to signify 'a bond exist'
     * between atoms for which there is no proper bond order. Still, 
     * considerations based on evaluating the bond order will be misguided.
     */
    private boolean acceptUnsetToSingeBOApprox = false;
    
    /**
     * Fag requesting the pre-fragmentation filtering of the structures.
     */
    private boolean preFilter = false;
    
    /**
     * SMARTS identifying substructures that lead to rejection of a structure
     * before fragmentation. I.e., structures matching any of these queries will
     *  not be fragmented.
     */
    private Set<String> preFilterSMARTS = new HashSet<String>();
    
    /**
     * Fag requesting the fragmentation of the structures.
     */
    private boolean doFragmentation = false;
    
    /**
     * Flag requesting to do post-fragmentation processing of fragments, i.e.,
     * application of all filtration and rejection rules that can be applied 
     * after fragmentation, though starting from an input that is already a
     * collection of fragments. Essentially, skip fragmentation and filter the 
     * given fragments
     */
    private boolean doFiltering = false;
    
    /**
     * Flag requesting to reject fragments with minor isotopes.
     */
    private boolean doRejectWeirdIsotopes = true;
    
    /**
     * Symbols of elements that lead to rejection of a fragment.
     */
    private Set<String> rejectedElements = new HashSet<String>();
    
    /**
     * Lower limits of formula-based criteria for fragment rejection. I.e., if 
     * a fragment has a formula that counts less then what defined here, it is
     * rejected.
     */
    private Map<String,Double> formulaCriteriaLessThan = 
            new HashMap<String,Double>();
    
    /**
     * Upper limits of formula-based criteria for fragment rejection. I.e., if 
     * a fragment has a formula that counts more then what defined here, it is
     * rejected.
     */
    private Set<Map<String,Double>> formulaCriteriaMoreThan = 
            new HashSet<Map<String,Double>>();
    
    /**
     * The initial part of APClasses that lead to rejection of a fragment.
     */
    private Set<String> rejectedAPClasses = new HashSet<String>();
    
    /**
     * Combination of strings matching the beginning of APClass names that 
     * lead to rejection of a fragment.
     */
    private Set<String[]> rejectedAPClassCombinations = new HashSet<String[]>();
    
    /**
     * Upper limit for number of non-H atoms in fragments. Negative number is
     * used to disable checking of the number of atoms.
     */
    private int maxFragHeavyAtomCount = -1;
    
    /**
     * Lower limit for number of non-H atoms in fragments. Negative number is
     * used to disable checking of the number of atoms.
     */
    private int minFragHeavyAtomCount = -1;
    
    /**
     * SMARTS leading to rejection of a fragment.
     */
    private Map<String, String> fragRejectionSMARTS = new HashMap<String, String>();
    
    /**
     * SMARTS leading to retention of a fragment. All fragments not matching one
     * of these are rejected.
     */
    private Map<String, String> fragRetentionSMARTS = new HashMap<String, String>();

    /**
     * Flag requesting to add dummy atoms on linearities. This to enable 
     * 3D-modeling of the system with internal coordinates.
     */
    private boolean doAddDuOnLinearity = true;

    /**
     * Upper limit for an angle before it is treated as "flat" ("linear")
     * angle, i.e., close enough to 180 DEG.
     */
    private double linearAngleLimit = 170.0;

    /**
     * Pathname to file with fragments that can be ignored.
     */
    private String ignorableFragmentsFile = "";
    
    /**
     * List of fragment that can be rejected.
     */
    private ArrayList<Vertex> ignorableFragments = new ArrayList<Vertex>();
    
    /**
     * Pathname to file with fragments that will be retained, i.e., any 
     * isomorphic fragment of any of these will be kept, all the rest rejected.
     */
    private String targetFragmentsFile = "";
    
    /**
     * List of fragment that will be retained, i.e., any 
     * isomorphic fragment of any of these will be kept, all the rest rejected.
     */
    private ArrayList<Vertex> targetFragments = new ArrayList<Vertex>();
    
    /**
     * <p>Flag signaling the need to manage isomorphic families, i.e., manage
     * duplicate fragments. This is needed if 
     * we want to identify isomorphic fragments and keep only one 
     * isomorphic fragment (i.e.,
     * remove all duplicate fragments), or more then more isomorphic fragment. 
     * In the latter case, we essentially want to sample the isomorphic family.
     * The extent of this, i.e., the size of the sample is controlled by
     * {@link #isomorphicSampleSize}).</p>
     * <p>Also, if we run multiple threads and want to remove duplicate fragments
     * , each of thread may generate a new fragment 
     * that the others have not yet found. Thus, the existence of the new 
     * fragment must be communicated to the other threads avoiding concurrent
     * generation of the same fragment from different threads.</p>
     * The management of isomorphic families involves:<ol>
     * <li>Splitting fragments according to molecular weight (MW) to limit the
     * operations on the list of fragments to a small portion of the entire 
     * list of fragments.</li>
     * <li>Separate collection of unique versions of a fragment (the first found)
     * and of the sample of the family of fragments isomorphic to the first.</li>
     * <li>Thread-safe manipulation of the two MW-split collections: 
     * the unique, and the family sample.</li>
     * <li>Unification of the MW-split collections to obtain the overall result.
     * </li>
     * </ol>
     */
    // NB: javadoc reproduced also in the getter method. See below
    private boolean doManageIsomorphicFamilies = false;
    
    /**
     * Size of the sample of isomorphic fragments to collect. When this number
     * N is larger then one, we will collect the first N isomorphic forms of
     * each fragment. A value of 1 corresponds to saying "remove all isomorphic
     * duplicates".
     */
    private int isomorphicSampleSize = 1;
    
    /**
     * Maximum isomorphic sample size
     */
    public static final int MAXISOMORPHICSAMPLESIZE = 50;
    
    /**
     * Molecular weight slot width for collecting fragments.
     */
    private int mwSlotSize = 10;
    
    /**
     * Maximum number of clusters of isomorphic fragments to seek for.
     */
    private int maxK = 20;
    
    /**
     * Fraction of the list of clusterizable fragment (as M of 1/M) used to 
     * determine the max number of clusters to search for as data_size*1/M.
     */
    private int dataFraction = 3;
    
    /**
     * Maximum number of cluster refinement iteration for a given k in k-means
     * clustering of fragments.
     */
    private int maxIter = 50;
    
    /**
     * Maximum number of independent fragment clustering trials for a given k.
     *  We'll take the best result from any of these trials.
     */
    private int maxTrials = 10;
    
    /**
     * Threshold value for maximum silhouette coefficient below which we do no
     * trust silhouette analysis anymore.
     */
    private double throwsholdSilhouetteSignificance = 0.65;
    
    /**
     * The threshold value for sanity check of elbow plot from k-means
     * clustering. If the range the sumOfVariances for k does show a step
     * of size larger than this threshold, then we do have an elbow on which
     * we apply the elbow method.
     * clustering.
     */
    private double thresholdsRangeSoV = 0.01;
    
    /**
     * Mapping of the molecular weight slot identifier to the file collecting
     * all collected fragments belonging to that MW slot.
     */
    private Map<String,File> mwSlotToAllFragsFile = new HashMap<String,File>();
    
    /**
     * Mapping of the molecular weight slot identifier to the file collecting
     * unique fragments belonging to that MW slot.
     */
    private Map<String,File> mwSlotToUnqFragsFile = new HashMap<String,File>();
    
    /**
     * Counts of isomorphic versions of each known fragment generated in
     * a fragmentation process. The key is a string that identifies the vertex 
     * without having to hold the entire data structure of it.
     */
    private Map<String,Integer> isomorphsCount = new HashMap<String,Integer>();
    
    //TODO: We could use something like the SizeControlledSet used in the EA to 
    // collect unique identifiers.
    
    /**
     * Unique identifier of a family of isomorphic versions of a fragment,.
     */
    private AtomicInteger unqIsomorphicFamilyId = new AtomicInteger(0);
    
    /**
     * Synchronization lock for manipulating a) the collections (i.e., MW slots)
     * of fragments produced by multiple threads and b) the relative information 
     * (i.e., isomorphic family size).
     */
    public final Object MANAGEMWSLOTSSLOCK = new Object();
    
    /**
     * Flag signaling the request to analyze each isomorphic family to extract
     * the most representative fragment and make it be the champion of that
     * family.
     */
    private boolean extactRepresentativeConformer = false;
    
    /**
     * Size of on-the-fly generated, normally distributed noise-distorted 
     * population of geometries used to determine properties of unimodal 
     * population of distorted of points around an N-dimensional point. Used
     * by {@link FragmentClusterer}.
     */
    private int sizeUnimodalPop = 20;
    
    /**
     * Maximum amount of absolute noise used to generate normally distributed 
     * noise-distorted population of points around an N-dimensional point.
     * Used by {@link FragmentClusterer}.
     */
    private double maxNoiseUnimodalPop = 0.2;
    
    /**
     * Factor used to multiply the standard deviation when adding it to the mean
     * of the RMSD for a unimodal
     * population of distorted of points around an N-dimensional point, thus 
     * defining a threshold for deciding whether a query point belong to that
     * population or not.
     * Used by {@link FragmentClusterer}.
     */
    private double factorForSDOnStatsOfUnimodalPop = 1.0;
    
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public FragmenterParameters()
    {
        super(ParametersType.FRG_PARAMS);
    }

//------------------------------------------------------------------------------

    /**
     * @return the number of parallel tasks to run. Default is 1.
     */
    public int getNumTasks()
    {
        return numParallelTasks;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the number of parallel tasks to run.
     * @param numParallelTasks
     */
    public void setNumTasks(int numParallelTasks)
    {
        this.numParallelTasks = numParallelTasks;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the pathname to the file containing the structures to work with.
     */
    public String getStructuresFile()
    {
        return structuresFile;
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the pathname of the file containing input structures.
     * @param structuresFile the pathname.
     */
    public void setStructuresFile(String structuresFile)
    {
        this.structuresFile = structuresFile;
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the pathname of the file containing molecular formula with a format
     * respecting Cambridge Structural Database format).
     * @param formulaeFile
     */
    public void setFormulaeFile(String formulaeFile)
    {
        this.formulaeFile = formulaeFile;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the pathname to the file containing the molecular formulae to 
     * work with.
     */
    public String getFormulaeFile()
    {
        return formulaeFile;
    }

//------------------------------------------------------------------------------

    /**
     * @return the cutting rules loaded from the input.
     */
    public List<CuttingRule> getCuttingRules()
    {
        return cuttingRules;
    }

//------------------------------------------------------------------------------

    /**
     * @return the SMRTS strings used in the cutting rules to identify any atom.
     */
    public List<String> getAnyAtomQuesties()
    {
        return anyAtomQuesties;
    }

//------------------------------------------------------------------------------

    /**
     * @return the list of molecular formulae read-in from text file.
     */
    public LinkedHashMap<String, String> getFormulae()
    {
        return formulae;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if we are asked to perform the comparison of
     * about of atoms for each element (i.e., elemental analysis) present in the
     * structure file ({@link #structuresFile}) against that of a given 
     * molecular formula, which comes
     * from the {@link #formulaeFile}.
     */
    public boolean doCheckFormula()
    {
        return checkFormula;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the value of the flag controlling the execution of elemental analysis
     * on the structures.
     * @param checkFormula use <code>true</code> to request the elemental analysis.
     */
    public void setCheckFormula(boolean checkFormula)
    {
        this.checkFormula = checkFormula;
    }
    
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if we are asked to filter initial structures.
     */
    public boolean doPreFilter()
    {
        return preFilter;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the SMARTS queries identifying substructures that lead to 
     * rejection of a structure before fragmentation.
     */
    public Set<String> getPreFiltrationSMARTS()
    {
        return preFilterSMARTS;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if we are asked to fragment structures.
     */
    public boolean doFragmentation()
    {
        return doFragmentation;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if we want to remove fragments that contain
     * isotopes that are not the major isotope for that element.
     */
    public boolean doRejectWeirdIsotopes()
    {
        return doRejectWeirdIsotopes;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if we want to add dummy atoms to resolve
     * linearities in internal coordinates.
     */
    public boolean doAddDuOnLinearity()
    {
        return doAddDuOnLinearity;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the elemental symbols leading to rejection of a fragment.
     */
    public Set<String> getRejectedElements()
    {
        return rejectedElements;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the formula-based criteria to reject a fragment if there are too
     * few atoms of a certain element.
     */
    public Map<String, Double> getRejectedFormulaLessThan()
    {
        return formulaCriteriaLessThan;
    }
  
//------------------------------------------------------------------------------
    
    /**
     * @return the formula-based criteria to reject a fragment if there are too
     * many atoms of a certain element.
     */
    public Set<Map<String, Double>> getRejectedFormulaMoreThan()
    {
        return formulaCriteriaMoreThan;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the classes leading to rejection of a fragment.
     */
    public Set<String> getRejectedAPClasses()
    {
        return rejectedAPClasses;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the combinations of classes leading to rejection of a fragment.
     */
    public Set<String[]> getRejectedAPClassCombinations()
    {
        return rejectedAPClassCombinations;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the max number of heavy atoms to retain a fragment.
     */
    public int getMaxFragHeavyAtomCount()
    {
        return maxFragHeavyAtomCount;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the min number of heavy atoms to retain a fragment.
     */
    public int getMinFragHeavyAtomCount()
    {
        return minFragHeavyAtomCount;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the SMARTS that lead to rejection of a fragment.
     */
    public Map<String, String> getFragRejectionSMARTS()
    {
        return fragRejectionSMARTS;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the SMARTS that lead to retention of a fragment.
     */
    public Map<String, String> getFragRetentionSMARTS()
    {
        return fragRetentionSMARTS;
    }

//------------------------------------------------------------------------------

    public void setRejectWeirdIsotopes(boolean doRejectWeirdIsotopes)
    {
        this.doRejectWeirdIsotopes = doRejectWeirdIsotopes;
    }

//------------------------------------------------------------------------------
    
    public void setRejectedElements(Set<String> rejectedElements)
    {
        this.rejectedElements = rejectedElements;
    }

//------------------------------------------------------------------------------
    
    public void setRejectedFormulaLessThan(
            Map<String, Double> formulaMax)
    {
        this.formulaCriteriaLessThan = formulaMax;
    }

//------------------------------------------------------------------------------
    
    public void setRejectedFormulaMoreThan(
            Set<Map<String, Double>> formulaCriteriaMoreThan)
    {
        this.formulaCriteriaMoreThan = formulaCriteriaMoreThan;
    }

//------------------------------------------------------------------------------
    
    public void setRejectedAPClasses(Set<String> rejectedAPClasses)
    {
        this.rejectedAPClasses = rejectedAPClasses;
    }

//------------------------------------------------------------------------------
    
    public void setRejectedAPClassCombinations(
            Set<String[]> rejectedAPClassCombinations)
    {
        this.rejectedAPClassCombinations = rejectedAPClassCombinations;
    }

//------------------------------------------------------------------------------
    
    public void setMaxFragHeavyAtomCount(int maxFragHeavyAtomCount)
    {
        this.maxFragHeavyAtomCount = maxFragHeavyAtomCount;
    }

//------------------------------------------------------------------------------
    
    public void setMinFragHeavyAtomCount(int minFragHeavyAtomCount)
    {
        this.minFragHeavyAtomCount = minFragHeavyAtomCount;
    }

//------------------------------------------------------------------------------
    
    public void setFragRejectionSMARTS(Map<String, String> fragRejectionSMARTS)
    {
        this.fragRejectionSMARTS = fragRejectionSMARTS;
    }

//------------------------------------------------------------------------------
    
    public void setFragRetentionSMARTS(Map<String, String> fragRetentionSMARTS)
    {
        this.fragRetentionSMARTS = fragRetentionSMARTS;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return list of fragment that can be rejected.
     */
    public ArrayList<Vertex> getIgnorableFragments()
    {
        return ignorableFragments;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the list of fragment that will be retained, i.e., any 
     * isomorphic fragment of any of these will be kept, all the rest rejected.
     */
    public ArrayList<Vertex> getTargetFragments()
    {
        return targetFragments;
    }
    
//------------------------------------------------------------------------------

    /**
     * <p>One needs to manage isomorphic families, i.e., manage
     * duplicate fragments if we want to identify isomorphic fragments and 
     * keep only one 
     * isomorphic fragment (i.e.,
     * remove all duplicate fragments), or more then more isomorphic fragment. 
     * In the latter case, we essentially want to sample the isomorphic family.
     * The extent of this, i.e., the size of the sample is controlled by
     * {@link #isomorphicSampleSize}).</p>
     * <p>Also, if we run multiple threads and want to remove duplicate fragments
     * , each of thread may generate a new fragment 
     * that the others have not yet found. Thus, the existence of the new 
     * fragment must be communicated to the other threads avoiding concurrent
     * generation of the same fragment from different threads.</p>
     * 
     * The management of isomorphic families involves:<ol>
     * <li>Splitting fragments according to molecular weight (MW) to limit the
     * operations on the list of fragments to a small portion of the entire 
     * list of fragments.</li>
     * <li>Separate collection of unique versions of a fragment (the first found)
     * and of the sample of the family of fragments isomorphic to the first.</li>
     * <li>Thread-safe manipulation of the two MW-split collections: 
     * the unique, and the family sample.</li>
     * <li>Unification of the MW-split collections to obtain the overall result.
     * </li>
     * </ol>
     * @return <code>true</code> if we need to manage isomorphic families.
     */
    public boolean doManageIsomorphicFamilies()
    {
        return doManageIsomorphicFamilies;
    }
    
//------------------------------------------------------------------------------

    public int getIsomorphicSampleSize()
    {
        return isomorphicSampleSize;
    }

//------------------------------------------------------------------------------

    public void setIsomorphicSampleSize(int isomorphicSampleSize)
    {
        this.isomorphicSampleSize = isomorphicSampleSize;
    }

//------------------------------------------------------------------------------

    public int getMWSlotSize()
    {
        return mwSlotSize;
    }

//------------------------------------------------------------------------------

    public void setMWSlotSize(int mwSlotSize)
    {
        this.mwSlotSize = mwSlotSize;
    }
    
    
//------------------------------------------------------------------------------
    
    /**
     * @return the threshold value for sanity check of elbow plot from k-means
     * clustering.
     */
    public double getThresholdRangeSoV()
    {
        return thresholdsRangeSoV;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the threshold value for sanity check of elbow plot from k-means
     * clustering.
     * @param thresholdsRangeSoV the new value.
     */
    public void setThresholdRangeSoV(double thresholdsRangeSoV)
    {
        this.thresholdsRangeSoV = thresholdsRangeSoV;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the denominator (M) to use for determining the maximum number of 
     * clusters to search for (as data.size * 1/M) when
     * We try k-means clustering with k = 1, ... , N. Where N is the minimum
     * between 1/M of the data size and maxK = {@value #maxK}.
     */
    public int getDataFraction()
    {
        return dataFraction;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the maximum number of clusters to search for. 
     * We try k-means clustering with k = 1, ... , N. Where N is the minimum
     * between 1/M of the data size and maxK = {@value #maxK}.
     *  This method sets M
     * @param m the new value.
     */
    public void setMaxClusterCountAsDataFraction(int m)
    {
        this.dataFraction = m;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * The maximum number of clusters to search for in  k-means clustering 
     * is k = 1, ... , N. Where N is the minimum
     * between 1/M of the data size and maxK = {@value #maxK}.
     * This method sets maxK.
     * @return the value of M
     */
    public int getMaxK()
    {
        return maxK;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the maximum number of clusters to search for. 
     * We try k-means clustering with k = 1, ... , N. Where N is the minimum
     * between 1/M of the data size and maxK = {@value #maxK}.
     * This method sets maxK.
     * @param maxK the new value.
     */
    public void setMaxK(int maxK)
    {
        this.maxK = maxK;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return  the number of cluster refinement (i.e., movement of centroid)
     * in k-means clustering.
     */
    public int getMaxIter()
    {
        return maxIter;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the number of cluster refinement (i.e., movement of centroid)
     * in k-means clustering
     * @param maxIter the new value.
     */
    public void setMaxIter(int maxIter)
    {
        this.maxIter = maxIter;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the number of attempts to perform k-means clustering starting from
     * an independent set of initial centroids.
     */
    public int getMaxTrials()
    {
        return maxTrials;
    }
    
//------------------------------------------------------------------------------    

    /**
     * Sets the number of attempts to perform k-means clustering starting from
     * an independent set of initial centroids.
     * @param maxTrials
     */
    public void setMaxTrials(int maxTrials)
    {
        this.maxTrials = maxTrials;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the value of the threshold for the maximum silhouette coefficient.
     * Below such threshold we do not trust the silhouette analysis anymore.
     */
    public double getThrowsholdSilhouetteSignificance()
    {
        return throwsholdSilhouetteSignificance;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the value of the threshold for the maximum silhouette coefficient.
     * Below such threshold we do not trust the silhouette analysis anymore.
     * @param throwsholdSilhouetteSignificance the new value.
     */
    public void setThrowsholdSilhouetteSignificance(
            double throwsholdSilhouetteSignificance)
    {
        this.throwsholdSilhouetteSignificance = throwsholdSilhouetteSignificance;
    }

//------------------------------------------------------------------------------

    public Map<String, File> getMWSlotToAllFragsFile()
    {
        return mwSlotToAllFragsFile;
    }

//------------------------------------------------------------------------------

    public void setMWSlotToAllFragsFile(Map<String, File> mwSlotToAllFragsFile)
    {
        this.mwSlotToAllFragsFile = mwSlotToAllFragsFile;
    }

//------------------------------------------------------------------------------

    public Map<String, File> getMWSlotToUnqFragsFile()
    {
        return mwSlotToUnqFragsFile;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the file meant to hold unique fragments 
     * from within a given MW slot, i.e., holding the unique version of 
     * isomorphic fragment families.
     * @param mwSlotId the identifier of the MW slot.
     * @return the file collecting the unique version of isomorphic fragment
     * families in the MW range of interest.
     */
    public File getMWSlotFileNameUnqFrags(String mwSlotId)
    {
        return new File(getWorkDirectory() + DenoptimIO.FS 
                + DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT + mwSlotId 
                + DENOPTIMConstants.MWSLOTFRAGSUNQFILENANEEND + "."
                + DENOPTIMConstants.TMPFRAGFILEFORMAT.getExtension());
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the file meant to hold all isomorphic fragments 
     * from a given MW slot.
     * @param mwSlotId the identifier of the MW slot.
     * @return the file collecting all isomorphic fragment
     * families in the MW range of interest.
     */
    public File getMWSlotFileNameAllFrags(String mwSlotId)
    {
        return new File(getWorkDirectory() + DenoptimIO.FS 
                + DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT + mwSlotId 
                + DENOPTIMConstants.MWSLOTFRAGSALLFILENANEEND + "."
                + DENOPTIMConstants.TMPFRAGFILEFORMAT.getExtension());
    }

//------------------------------------------------------------------------------

    /**
     * @return the counts of isomorphic versions of each fragment.
     */
    public Map<String,Integer> getIsomorphsCount()
    {
        return isomorphsCount;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a new unique identifier for a family of isomorphic fragments.
     * @return the unique ID.
     */
    public String newIsomorphicFamilyID()
    {
        return "IsomorphicFamily_" + unqIsomorphicFamilyId.getAndIncrement();
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if we want to do post-processing (i.e., filter 
     * and reject or collect fragments) on a given list of fragments (i.e.,
     * the input), thus skipping any fragmentation.
     */
    public boolean doFiltering()
    {
        return doFiltering;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if we are want to ignore the fact we have 
     * translated unset bond orders to single-order bonds.
     */
    public boolean acceptUnsetToSingeBO()
    {
        return acceptUnsetToSingeBOApprox;
    }

//------------------------------------------------------------------------------
    
    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */
    public void interpretKeyword(String key, String value)
            throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
            case "WORKDIR=":
                workDir = value;
                break;
                
            case "STRUCTURESFILE=":
                structuresFile = value;
                break;

            case "FORMULATXTFILE=":
                checkFormula = true;
                formulaeFile = value;
                break;
                
            case "PREFILTERSMARTS=":
                preFilter = true;
                preFilterSMARTS.add(value);
                break;

            case "CUTTINGRULESFILE=":
                doFragmentation = true;
                cutRulesFile = value;
                break;
                
            case "UNSETTOSINGLEBO":
                acceptUnsetToSingeBOApprox = true;
                break;

            case "IGNORABLEFRAGMENTS=":
                ignorableFragmentsFile = value;
                doFiltering = true;
                break;
                
            case "TARGETFRAGMENTS=":
                targetFragmentsFile = value;
                doFiltering = true;
                break;
                
            case "ISOMORPHICSAMPLESIZE=":
                try {
                    isomorphicSampleSize = Integer.parseInt(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                if (isomorphicSampleSize>0)
                    doManageIsomorphicFamilies = true;
                break;
                
            case "REMOVEDUPLICATES":
                doManageIsomorphicFamilies = true;
                break;
                
            case "MWSLOTSIZE=":
                try {
                    mwSlotSize = Integer.parseInt(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case "REJECTMINORISOTOPES":
                doRejectWeirdIsotopes = true;
                doFiltering = true;
                break;
                
            case "REJECTELEMENT=":
                rejectedElements.add(value);
                doFiltering = true;
                break;
                
            case "REJFORMULALESSTHAN=":
                if (formulaCriteriaLessThan.size()>0)
                {
                    msg = "Attempt to specify more than one criterion for "
                            + "rejecting fragments based on a lower-limit "
                            + "molecular formula. ";
                    throw new DENOPTIMException(msg);
                }
                Map<String,Double> elSymbolsCount = null;
                try {
                    elSymbolsCount = FormulaUtils.parseFormula(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                formulaCriteriaLessThan = elSymbolsCount;
                doFiltering = true;
                break;
                
            case "REJFORMULAMORETHAN=":
                Map<String,Double> elSymbolsCount2= null;
                try {
                    elSymbolsCount2 = FormulaUtils.parseFormula(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                formulaCriteriaMoreThan.add(elSymbolsCount2);
                doFiltering = true;
                break;
                
            case "REJECTAPCLASS=":
                rejectedAPClasses.add(value);
                doFiltering = true;
                break;
                
            case "REJECTAPCLASSCOMBINATION=":
                String[] lst = value.split("\\s+");
                rejectedAPClassCombinations.add(lst);
                doFiltering = true;
                break;
                
            case "MAXFRAGSIZE=":
                try {
                    maxFragHeavyAtomCount = Integer.parseInt(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                doFiltering = true;
                break;
                
            case "MINFRAGSIZE=":
                try {
                    minFragHeavyAtomCount = Integer.parseInt(value);
                } catch (Throwable t)
                {
                    msg = "Unable to parse value of " + key + ": '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                doFiltering = true;
                break;
                
            case "REJECTSMARTS=":
                fragRejectionSMARTS.put(value, value);
                doFiltering = true;
                break;
                
            case "RETAINSMARTS=":
                fragRetentionSMARTS.put(value, value);
                doFiltering = true;
                break;
                
/*
            case "=":
                = value;
                doFiltering = true;
                break;
  */              
                
            case "PARALLELTASKS=":
                try
                {
                    numParallelTasks = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value " + key + "'" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case "VERBOSITY=":
                try
                {
                    verbosity = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value " + key + "'" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
                
            default:
                 msg = "Keyword " + key + " is not a known Fragmenter-" 
                		 + "related keyword. Check input files.";
                throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */
    
    public void checkParameters() throws DENOPTIMException
    {
    	if (!workDir.equals(System.getProperty("user.dir")))
    	{
    	    ensureFileExists(workDir);
    	}
    	ensureIsPositive("numParallelTasks", numParallelTasks, "PARALLELTASKS");
    	ensureIsPositive("isomorphicSampleSize", isomorphicSampleSize, 
    	        "ISOMORPHICSAMPLESIZE");
    	ensureIsPositive("mwSlotSize", mwSlotSize, "MWSLOTSIZE");
    	ensureFileExistsIfSet(structuresFile);
    	ensureFileExistsIfSet(cutRulesFile);
    	ensureFileExistsIfSet(formulaeFile);
    	ensureFileExistsIfSet(ignorableFragmentsFile);
    	ensureFileExistsIfSet(targetFragmentsFile);
    	
    	checkOtherParameters();
    }

//------------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException
    {
        if (isMaster)
            createWorkingDirectory();
        
        anyAtomQuesties = new ArrayList<String>();
        cuttingRules = new ArrayList<CuttingRule>();
        if (cutRulesFile!=null && !cutRulesFile.isBlank())
        {
            DenoptimIO.readCuttingRules(new File(cutRulesFile), anyAtomQuesties, 
                    cuttingRules);
        }
        if (formulaeFile!=null && !formulaeFile.isBlank())
        {
            formulae = DenoptimIO.readCSDFormulae(new File(formulaeFile));
        }
        processOtherParameters();
        
        if (ignorableFragmentsFile!=null && !ignorableFragmentsFile.isBlank())
        {
            try
            {
                ignorableFragments = DenoptimIO.readVertexes(
                        new File(ignorableFragmentsFile), BBType.UNDEFINED);
            } catch (Throwable e)
            {
                throw new DENOPTIMException("Problems reading file '" 
                        + ignorableFragmentsFile + "'", e);
            }
        }
        
        if (targetFragmentsFile!=null && !targetFragmentsFile.isBlank())
        {
            try
            {
                targetFragments = DenoptimIO.readVertexes(
                        new File(targetFragmentsFile), BBType.UNDEFINED);
            } catch (Throwable e)
            {
                throw new DENOPTIMException("Problems reading file '" 
                        + targetFragmentsFile + "'", e);
            }
        }
       
		if (isMaster)
		{
            StaticLogger.appLogger.log(Level.INFO, "Program log file: " 
                    + logFile + DENOPTIMConstants.EOL 
                    + "Output files associated with the current run are "
                    + "located in " + workDir);
		}
    }
    
//------------------------------------------------------------------------------
    
    private void createWorkingDirectory()
    {
        String curDir = workDir;
        String fileSep = System.getProperty("file.separator");
        boolean success = false;
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");
            String str = "FRG" + sdf.format(new Date());
            workDir = curDir + fileSep + str;
            success = FileUtils.createDirectory(workDir);
        }
        FileUtils.addToRecentFiles(workDir, FileFormat.FRG_RUN);
        logFile = workDir + ".log";
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of parameters in a string with newline characters as
     * delimiters.
     * @return the list of parameters in a string with newline characters as
     * delimiters.
     */
    public String getPrintedList()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" " + paramTypeName() + " ").append(NL);
        for (Field f : this.getClass().getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(this)).append(NL);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print " + paramTypeName() 
                        + " parameters. Cause: " + t);
                break;
            }
        }
        for (RunTimeParameters otherCollector : otherParameters.values())
        {
            sb.append(otherCollector.getPrintedList());
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * @return the upper limit for an angle before it is treated as "flat"
     * angle, i.e., close enough to 180 DEG.
     */
    public double getLinearAngleLimit()
    {
        return linearAngleLimit;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the upper limit for an angle before it is treated as "flat" 
     * angle, i.e., close enough to 180 DEG.
     * @param linearAngleLimit the new value.
     */
    public void setLinearAngleLimit(double linearAngleLimit)
    {
        this.linearAngleLimit = linearAngleLimit;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if we want to extract the most representative 
     * conformer from each isomorphic family.
     */
    public boolean extactRepresentativeConformer()
    {
        return extactRepresentativeConformer;
    }

//------------------------------------------------------------------------------

    /**
     * @return the size of the population of normally distributed noise-distorted
     * population used to define the threshold RMSD of a unimodal distribution
     * of geometric distortions.
     */
    public int getSizeUnimodalPop()
    {
        return sizeUnimodalPop;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the size of the population of normally distributed noise-distorted
     * population used to define the threshold RMSD of a unimodal distribution
     * of geometric distortions.
     */
    public void setSizeUnimodalPop(int sizeUnimodalPop)
    {
        this.sizeUnimodalPop = sizeUnimodalPop;
    }

//------------------------------------------------------------------------------

    /**
     * @return the maximum noise of the population of normally distributed 
     * noise-distorted
     * population used to define the threshold RMSD of a unimodal distribution
     * of geometric distortions.
     */
    public double getMaxNoiseUnimodalPop()
    {
        return maxNoiseUnimodalPop;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the maximum noise of the population of normally distributed 
     * noise-distorted
     * population used to define the threshold RMSD of a unimodal distribution
     * of geometric distortions.
     */
    public void setMaxNoiseUnimodalPop(double maxNoiseUnimodalPop)
    {
        this.maxNoiseUnimodalPop = maxNoiseUnimodalPop;
    }

//------------------------------------------------------------------------------

    /**
     * @return the weight of the standard deviation when calculating the 
     * RMSD threshold from the statistics of the RMSD over the population of 
     * normally distributed noise-distorted points with unimodal distribution
     * of geometric distortions.
     */
    public double getFactorForSDOnStatsOfUnimodalPop()
    {
        return factorForSDOnStatsOfUnimodalPop;
    }

    
//------------------------------------------------------------------------------

    /**
     * Sets the weight of the standard deviation when calculating the 
     * RMSD threshold from the statistics of the RMSD over the population of 
     * normally distributed noise-distorted points with unimodal distribution
     * of geometric distortions.
     */
    public void setFactorForSDOnStatsOfUnimodalPop(
            double factorForSDOnStatsOfUnimodalPop)
    {
        this.factorForSDOnStatsOfUnimodalPop = factorForSDOnStatsOfUnimodalPop;
    }
    
//------------------------------------------------------------------------------

}
