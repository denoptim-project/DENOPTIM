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

package denoptim.programs.denovo;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.logging.Monitor;
import denoptim.logging.StaticLogger;
import denoptim.programs.RunTimeParameters;
import denoptim.utils.MutationType;


/**
 *Parameters for genetic algorithm.
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class GAParameters extends RunTimeParameters
{
    /**
     * Time stamp identifying this run
     */
    public String timeStamp = "NOTIMESTAMP";
    
    /**
     * Pathname to the working directory for the current run
     */
    private String dataDir = System.getProperty("user.dir");
    
    /**
     * Pathname to the interface directory for the current run. This is the
     * pathname that is watched for external instructions
     */
    private String interfaceDir = dataDir 
    		+ System.getProperty("file.separator") + "interface";

    /** 
     * Pathname of the initial population file. The file itself can be an SDF or
     * a list of pathnames
     */
    protected String initPoplnFile = "";

    /**
     * Pathname of the file with the list of individuals unique identifiers that
     * are initially known.
     */
    protected String uidFileIn = "";

    /**
     * Pathname of the file where the individuals unique identifiers will be 
     * recorded.
     */
    protected String uidFileOut = "";
    
    /**
     * Pathname of file where EA monitors dumps are printed
     */
    private String monitorFile = "";

    /**
     * Default name of the UIDFileOut
     */
    private final String DEFUIDFILEOUTNAME = "MOLUID.txt";

    /**
     * Pathname to the file containing the list of previously visited graph
     */
    protected String visitedGraphsFile = "GRAPHS.txt";

    /**
     * Size of the population 
     */
    protected int populationSize = 50;

    /**
     * Number of children (i.e., new offspring) to be produced in each 
     * generation
     */
    protected int numOfChildren = 5;

    /**
     * Number of identical generations before convergence is reached
     */
    protected int numConvGen = 5;

    /**
     * Maximum number of generations to run for
     */
    protected int numGenerations = 100;

    /**
     * Factor controlling the maximum number of attempts to build a graph
     * so that the maximum number of attempts = factor * population size
     */
    protected int maxTriesPerPop = 25;
    
    /**
     * Maximum number of attempts to perform any genetic operation (i.e., either
     * crossover or mutation) on any parents before giving up.
     */
    protected int maxGeneticOpAttempts = 100;

    /**
     * Replacement strategy: 1) replace worst individuals with new ones that are
     * better than the worst, 2) no replacement (the population keeps growing)
     */
    protected int replacementStrategy = 1;

    /**
     * Definition of the growth probability function:
     */
    protected int lvlGrowthProbabilityScheme = 0;
 
    /**
     * Parameter controlling the growth probability function of types
     * 'EXP_DIFF' and 'TANH'
     */
    protected double lvlGrowthMultiplier = 0.5;
 
    /** 
     * Parameters controlling the growth probability function
     * of type 'SIGMA': steepness of the function where p=50%
     */
    protected double lvlGrowthSigmaSteepness = 1.0;

    /** 
     * Parameters controlling the growth probability function
     * of type 'SIGMA': level at which p=50% (can be  a float)
     */
    protected double lvlGrowthSigmaMiddle = 2.5;
    
    /**
     * Flag recording the intention to use level-controlled graph extension 
     * probability.
     */
    protected boolean useLevelBasedProb = false;
    
    /**
     * Flag recording the intention to use molecular size-controlled graph
     * extension probability.
     */
    protected boolean useMolSizeBasedProb = false;
    
    /**
     * Definition of the molGrowth probability function:
     */
    protected int molGrowthProbabilityScheme = 2;

    /**
     * Parameter controlling the molGrowth probability function of types
     * 'EXP_DIFF' and 'TANH'
     */
    protected double molGrowthMultiplier = 0.5;

    /**
     * Parameters controlling the molGrowth probability function
     * of type 'SIGMA': steepness of the function where p=50%
     */
    protected double molGrowthSigmaSteepness = 0.2;

    /**
     * Parameters controlling the molGrowth probability function
     * of type 'SIGMA': level at which p=50% (can be  a float)
     */
    protected double molGrowthSigmaMiddle = 25;
    
    /**
     * Definition of the crowding probability function. By default, the 
     * probability of using an AP that is hosted on an atom that already has an 
     * used AP is 100%.
     */
    protected int crowdingProbabilityScheme = 3;
 
    /**
     * Parameter controlling the crowding probability function of types
     * 'EXP_DIFF' and 'TANH'
     */
    protected double crowdingMultiplier = 0.5;
 
    /** 
     * Parameters controlling the crowding probability function
     * of type 'SIGMA': steepness of the function where p=50%
     */
    protected double crowdingSigmaSteepness = 1.0;

    /** 
     * Parameters controlling the crowding probability function
     * of type 'SIGMA': level at which p=50% (can be  a float)
     */
    protected double crowdingSigmaMiddle = 2.5;

    /**
     * The probability at which symmetric substitution occurs
     */
    protected double symmetricSubProbability = 0.8;
    
    /**
     * The relative weight at which mutation is performed
     */
    protected double mutationWeight = 1.0;
    
    /**
     * The relative weight at which construction from scratch is performed
     */
    protected double builtAnewWeight = 1.0;
    
    /**
     * The relative weight at which crossover is performed
     */
    protected double crossoverWeight = 1.0;

    /**
     * Crossover parents selection strategy: integer code
     */
    protected int xoverSelectionMode = 3;

    /**
     * Crossover parents selection strategy: string
     */
    protected String strXoverSelectionMode =
            "STOCHASTIC UNIVERSAL SAMPLING";

    /**
     * Mutation types that are excluded everywhere.
     */
    List<MutationType> excludedMutationTypes = new ArrayList<MutationType>();
    
    /**
     * The seed value for random number generation
     */
    protected long seed = 0L;
   
    /**
     * Parallelization scheme: synchronous or asynchronous 
     */
    protected int parallelizationScheme = 1;

    /**
     * Maximum number of parallel tasks
     */
    protected int numParallelTasks = 0;

    /**
     * Flag controlling how to sort the population based on the fitness
     */
    protected boolean sortOrderDecreasing = true;

    /**
     * Precision for reporting the value of the fitness
     */
    protected int precisionLevel = 3;

    /**
     * Monitor dumps step. The EA {@link Monitor} will dump data to file every 
     * this number of new attempts to generate candidate. 
     */
    protected int monitorDumpStep = 50;
    
    /**
     * Flag controlling if we dump monitored data or not
     */
    protected boolean dumpMonitor = false;
    
    /**
     * Minimal standard deviation accepted in the fitness values of the initial population
     */
    protected double minFitnessSD = 0.000001;
    
    /**
     * Flag controlling the possibility of collecting cyclic graph systems that 
     * include a scaffold and save them as new template scaffolds.
     */
    protected boolean saveRingSystemsAsTemplatesScaffolds = false;
    
    /**
     * Flag controlling the possibility of collecting cyclic graph systems that 
     * do NOT include a scaffold and save them as new template non-scaffold
     * building blocks.
     */
    protected boolean saveRingSystemsAsTemplatesNonScaff = false;

    /**
     * Fitness threshold for adding template to building block libraries. 
     * This is expressed as percentage, i.e., if the fitness is in the best X%
     * of the population, then the template is added to the scaffold/vertex
     * library.
     */
    protected double saveRingSystemsFitnessThreshold = 0.10;

    /**
     * The weights of multisite mutations
     */
    private double[] mutliSiteMutationWeights = new double[]{0,10,1};

    /**
     * Maximum number of unique identifiers kept in memory. Beyond this value
     * the identifiers are dealt with using a file on disk.
     */
    public int maxUIDMemory = 1000000;

    /**
     * Text file used to store unique identifiers beyond the limits of the
     * memory (see {@link GAParameters#maxUIDMemory}).
     */
    public String uidMemoryOnDisk = "memory_UIDs.txt";
    
    /**
     * Flag that enables the ignoring of mutated graphs that lead to a failure 
     * in the evaluation of graphs that generates SMILES, InChI and molecular
     * representation.
     */
    public boolean mutatedGraphFailedEvalTolerant = true;
    
    /**
     * Flag that enables the ignoring of crossover-ed graphs that lead to a f
     * ailure 
     * in the evaluation of graphs that generates SMILES, InChI and molecular
     * representation.
     */
    public boolean xoverGraphFailedEvalTolerant = true;
    
    /**
     * Flag that enables the ignoring of crossover events triggering exceptions.
     * Such events will still be recorded in the GA run {@link Monitor}.
     */
    public boolean xoverFailureTolerant = true;
    
    /**
     * Flag that enables the ignoring of mutation events triggering exceptions.
     * Such events will still be recorded in the GA run {@link Monitor}.
     */
    public boolean mutationFailureTolerant = true;
    
    /**
     * Flag that enables the ignoring of construction from scratch events 
     * triggering exceptions.
     * Such events will still be recorded in the GA run {@link Monitor}.
     */
    public boolean buildAnewFailureTolerant = true;
    

//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param paramType
     */
    public GAParameters()
    {
        super(ParametersType.GA_PARAMS);
    }

//------------------------------------------------------------------------------

    public String getUIDFileIn()
    {
        return uidFileIn;
    }

//------------------------------------------------------------------------------

    public String getUIDFileOut()
    {
        return uidFileOut;
    }

//------------------------------------------------------------------------------

    public String getVisitedGraphsFile()
    {
        return visitedGraphsFile;
    }
    
//------------------------------------------------------------------------------
    
    public String getInterfaceDir()
    {
        return interfaceDir;
    }
    
//------------------------------------------------------------------------------

    public String getMonitorFile()
    {
        return monitorFile;
    }
    
//------------------------------------------------------------------------------
    
    public int getMonitorDumpStep()
    {
        return monitorDumpStep;
    }

//------------------------------------------------------------------------------

    public int getPrecisionLevel()
    {
        return precisionLevel;
    }
    
//------------------------------------------------------------------------------

    public int getNumberOfCPU()
    {
        return numParallelTasks;
    }    

//------------------------------------------------------------------------------

    public boolean isSortOrderDecreasing()
    {
        return sortOrderDecreasing;
    }

//------------------------------------------------------------------------------

    public int getMaxTriesFactor()
    {
        return maxTriesPerPop;
    }
    
//------------------------------------------------------------------------------

    public int getMaxGeneticOpAttempts()
    {
        return maxGeneticOpAttempts;
    }

//------------------------------------------------------------------------------

    public String getDataDirectory()
    {
        return dataDir;
    }
    
//------------------------------------------------------------------------------
    
    public void setWorkingDirectory(String pathName)
    {
        dataDir = pathName;
        monitorFile = dataDir + ".eaMonitor";
        interfaceDir = pathName + DENOPTIMConstants.FSEP + "interface";
        
        logFile = dataDir + ".log";

        if (uidFileOut.equals(""))
        {
            uidFileOut = dataDir + DENOPTIMConstants.FSEP + DEFUIDFILEOUTNAME;
        }
    }

//------------------------------------------------------------------------------

    public int getReplacementStrategy()
    {
        return replacementStrategy;
    }

//------------------------------------------------------------------------------

    public double getCrowdingFactorSteepSigma()
    {
        return crowdingSigmaSteepness;
    }

//------------------------------------------------------------------------------

    public double getCrowdingFactorMiddleSigma()
    {
        return crowdingSigmaMiddle;
    }

//------------------------------------------------------------------------------

    public double getCrowdingMultiplier()
    {
        return crowdingMultiplier;
    }

//------------------------------------------------------------------------------

    public int getCrowdingProbabilityScheme()
    {
        return crowdingProbabilityScheme;
    }
    
//------------------------------------------------------------------------------

    public double getGrowthFactorSteepSigma()
    {
        return lvlGrowthSigmaSteepness;
    }

//------------------------------------------------------------------------------

    public double getGrowthFactorMiddleSigma()
    {
        return lvlGrowthSigmaMiddle;
    }

//------------------------------------------------------------------------------

    public double getGrowthMultiplier()
    {
        return lvlGrowthMultiplier;
    }

//------------------------------------------------------------------------------

    public int getGrowthProbabilityScheme()
    {
        return lvlGrowthProbabilityScheme;
    }
    
//------------------------------------------------------------------------------

    public double getMolGrowthFactorSteepSigma()
    {
        return molGrowthSigmaSteepness;
    }

//------------------------------------------------------------------------------

    public double getMolGrowthFactorMiddleSigma()
    {
        return molGrowthSigmaMiddle;
    }

//------------------------------------------------------------------------------

    public double getMolGrowthMultiplier()
    {
        return molGrowthMultiplier;
    }

//------------------------------------------------------------------------------

    public int getMolGrowthProbabilityScheme()
    {
        return molGrowthProbabilityScheme;
    }

//------------------------------------------------------------------------------

    public void setPopulationSize(int size)
    {
        populationSize = size;
    }
    
//------------------------------------------------------------------------------

    public int getPopulationSize()
    {
        return populationSize;
    }

//------------------------------------------------------------------------------

    public int getNumberOfGenerations()
    {
        return numGenerations;
    }

//------------------------------------------------------------------------------

    public String getSelectionStrategy()
    {
        return strXoverSelectionMode;
    }
    
//------------------------------------------------------------------------------
    
    public List<MutationType> getExcludedMutationTypes()
    {
        return excludedMutationTypes;
    }

//------------------------------------------------------------------------------

    public int getSelectionStrategyType()
    {
        return xoverSelectionMode;
    }

//------------------------------------------------------------------------------

    public int getNumberOfConvergenceGenerations()
    {
        return numConvGen;
    }

//------------------------------------------------------------------------------

    public int getNumberOfChildren()
    {
        return numOfChildren;
    }
    
//------------------------------------------------------------------------------
    
    public double getCrossoverWeight()
    {
        return crossoverWeight;
    }

//------------------------------------------------------------------------------

    public double getMutationWeight()
    {
        return mutationWeight;
    }
    
//------------------------------------------------------------------------------

    public double getConstructionWeight()
    {
        return builtAnewWeight;
    }

//------------------------------------------------------------------------------

    public double getSymmetryProbability()
    {
        return symmetricSubProbability;
    }
    
//------------------------------------------------------------------------------

    public String getInitialPopulationFile()
    {
        return initPoplnFile;
    }
    

//------------------------------------------------------------------------------
      
    public double[] getMultiSiteMutationWeights()
    {
        return mutliSiteMutationWeights;
    }

//------------------------------------------------------------------------------
   
    public boolean useMolSizeBasedProb()
    {
        return useMolSizeBasedProb;
    }
    
//------------------------------------------------------------------------------
   
    public boolean useLevelBasedProb()
    {
        return useLevelBasedProb;
    }
    
//-----------------------------------------------------------------------------
    
    public int getParallelizationScheme()
    {
        return parallelizationScheme;
    }
    
//-----------------------------------------------------------------------------
    
    public double getMinFitnessSD()
    {
        return minFitnessSD;
    }
    
//-----------------------------------------------------------------------------
    
    public boolean dumpMonitor()
    {
        return dumpMonitor;
    }
    
//-----------------------------------------------------------------------------
    
    public double getSaveRingSystemsFitnessThreshold()
    {
        return saveRingSystemsFitnessThreshold;
    }
    
//-----------------------------------------------------------------------------
    
    public boolean getSaveRingSystemsAsTemplatesNonScaff()
    {
        return saveRingSystemsAsTemplatesNonScaff;
    }
    
//-----------------------------------------------------------------------------
    
    public boolean getSaveRingSystemsAsTemplatesScaff()
    {
        return saveRingSystemsAsTemplatesScaffolds;
    }
    
//-----------------------------------------------------------------------------

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
            case "VERBOSITY=":
            {
                verbosity = Integer.parseInt(value);
                break;
            }
            
            case "NUMPARALLELTASKS=":
            {
                if (value.length() > 0)
                {
                    numParallelTasks = Integer.parseInt(value);
                }
                break;
            }
            
            case "PARALLELIZATION=":
            {
                switch (value.toUpperCase())
                {
                    case "SYNCHRONOUS":
                        parallelizationScheme = 1;
                        break;
                    case "ASYNCHRONOUS":
                        parallelizationScheme = 2;
                        break;
                    default:
                        throw new DENOPTIMException("Unknown parallelization scheme.");
                }
                break;
            }
        
            case "PRECISIONLEVEL=":
            {
                if (value.length() > 0)
                {
                    precisionLevel = Integer.parseInt(value);
                }
                break;
            }
        
            case "UIDFILEIN=":
            {
                if (value.length() > 0)
                {
                    uidFileIn = value;
                }
                break;
            }
        
            case "UIDFILEOUT=":
            {
                if (value.length() > 0)
                {
                    uidFileOut = value;
                }
                break;
            }
            
            case "MONITORFILE=":
            {
                if (value.length() > 0)
                {
                    monitorFile = value;
                }
                break;
            }
            
            case "MONITORDUMPSTEP=":
            {
                if (value.length() > 0)
                {
                    monitorDumpStep = Integer.parseInt(value);
                    dumpMonitor = true;
                }
                break;
            }
            
            case "RANDOMSEED=":
            {
                if (value.length() > 0)
                {
                    seed = Long.parseLong(value);
                }
                break;
            }
            
            case "MAXTRIESPERPOPULATION=":
            {
                if (value.length() > 0)
                    maxTriesPerPop  = Integer.parseInt(value);
                break;
            }
            
            case "MAXGENETICOPSATTEMPTS=":
            {
                if (value.length() > 0)
                    maxGeneticOpAttempts  = Integer.parseInt(value);
                break;
            }
        
            case "INITPOPLNFILE=":
            {
                if (value.length() > 0)
                {
                    initPoplnFile = value;
                }
                break;
            }
        
            case "SORTBYINCREASINGFITNESS":
            {
                if (value.length() > 0)
                {
                    sortOrderDecreasing = false;
                }
                break;
            }
        
            case "LEVELGROWTHMULTIPLIER=":
            {
                if (value.length() > 0)
                {
                    lvlGrowthMultiplier = Double.parseDouble(value);
                    useLevelBasedProb = true;
                }
                break;
            }
        
            case "LEVELGROWTHSIGMASTEEPNESS=":
            {
                if (value.length() > 0)
                {
                    lvlGrowthSigmaSteepness = Double.parseDouble(value);
                    useLevelBasedProb = true;
                }
                break;
            }
        
            case "LEVELGROWTHSIGMAMIDDLE=":
            {
                if (value.length() > 0)
                {
                    lvlGrowthSigmaMiddle = Double.parseDouble(value);
                    useLevelBasedProb = true;
                }
                break;
            }
        
            case "LEVELGROWTHPROBSCHEME=":
            {
                lvlGrowthProbabilityScheme = convertProbabilityScheme(value);
                useLevelBasedProb = true;
                break;
            }
            
            case "MOLGROWTHMULTIPLIER=":
            {
                if (value.length() > 0)
                {
                    molGrowthMultiplier = Double.parseDouble(value);
                    useMolSizeBasedProb = true;
                }
                break;
            }
        
            case "MOLGROWTHSIGMASTEEPNESS=":
            {
                if (value.length() > 0)
                {
                    molGrowthSigmaSteepness = Double.parseDouble(value);
                    useMolSizeBasedProb = true;
                }
                break;
            }
        
            case "MOLGROWTHSIGMAMIDDLE=":
            {
                if (value.length() > 0)
                {
                    molGrowthSigmaMiddle = Double.parseDouble(value);
                    useMolSizeBasedProb = true;
                }
                break;
            }
        
            case "MOLGROWTHPROBSCHEME=":
            {
                molGrowthProbabilityScheme = convertProbabilityScheme(value);
                useMolSizeBasedProb = true;
                break;
            }
            
            case "CROWDMULTIPLIER=":
            {
                if (value.length() > 0)
                {
                    crowdingMultiplier = Double.parseDouble(value);
                }
                break;
            }
        
            case "CROWDSIGMASTEEPNESS=":
            {
                if (value.length() > 0)
                {
                    crowdingSigmaSteepness = Double.parseDouble(value);
                }
                break;
            }
        
            case "CROWDSIGMAMIDDLE=":
            {
                if (value.length() > 0)
                {
                    crowdingSigmaMiddle = Double.parseDouble(value);
                }
                break;
            }
            
            case "SYMMETRYPROBABILITY=":
            {
                if (value.length() > 0)
                {
                    symmetricSubProbability = Double.parseDouble(value);
                }
                break;
            }
        
            case "CROWDPROBSCHEME=":
            {
                crowdingProbabilityScheme = convertProbabilityScheme(value);
                break;
            }
        
            case "NUMGENERATIONS=":
            {
                if (value.length() > 0)
                {
                    numGenerations = Integer.parseInt(value);
                }
                break;
            }
        
            case "NUMCHILDREN=":
            {
                if (value.length() > 0)
                {
                    numOfChildren = Integer.parseInt(value);
                }
                break;
            }
        
            case "CROSSOVERWEIGHT=":
            {
                if (value.length() > 0)
                {
                    crossoverWeight = Double.parseDouble(value);
                }
                break;
            }
        
            case "MUTATIONWEIGHT=":
            {
                if (value.length() > 0)
                {
                    mutationWeight = Double.parseDouble(value);
                }
                break;
            }
            
            case "EXCLUDEMUTATIONTYPE=":
            {
                if (value.length() > 0)
                {
                    excludedMutationTypes.add(MutationType.valueOf(value));
                }
                break;
            }
            
            case "CONSTRUCTIONWEIGHT=":
            {
                if (value.length() > 0)
                {
                    builtAnewWeight = Double.parseDouble(value);
                }
                break;
            }
            
            case "REPLACEMENTSTRATEGY=":
            {
                switch (value.toUpperCase())
                {
                    case "NONE":
                    	replacementStrategy = 2;
                        break;
                    case "ELITIST":
                        replacementStrategy = 1;
                        break;
                    default:
                        throw new DENOPTIMException("Unknown replacement strategy.");
                }
                break;
            }
        
            case "POPULATIONSIZE=":
            {
                if (value.length() > 0)
                {
                    populationSize = Integer.parseInt(value);
                }
                break;
            }
        
            case "NUMCONVGEN=":
            {
                if (value.length() > 0)
                {
                    numConvGen = Integer.parseInt(value);
                }
                break;
            }
            
            case "KEEPNEWRINGSYSTEMVERTEXES":
            {
                saveRingSystemsAsTemplatesNonScaff = true;
                break;
            }
            
            case "KEEPNEWRINGSYSTEMSCAFFOLDS":
            {
                saveRingSystemsAsTemplatesScaffolds = true;
                break;
            }
            
            
            case "KEEPNEWRINGSYSTEMFITNESSTRSH=":
            {
                if (value.length() > 0)
                {
                    saveRingSystemsFitnessThreshold = Double.parseDouble(value);
                }
                break;
            }
            
            case "MULTISITEMUTATIONWEIGHTS=":
            {
                String[] ws = value.split(",|\\s+");
                List<Double> lst = new ArrayList<Double>();
                for (String w : ws)
                {
                    if (!w.trim().equals(""))
                        lst.add(Double.parseDouble(w));
                }
                mutliSiteMutationWeights = new double[lst.size()];
                for (int i=0; i<lst.size(); i++)
                {
                    mutliSiteMutationWeights[i] = lst.get(i);
                }
                break;
            }
        
            case "XOVERSELECTIONMODE=":
            {
                if (value.length() > 0)
                {
                    xoverSelectionMode = -1;
                    if (value.compareToIgnoreCase("TS") == 0)
                    {
                        xoverSelectionMode = 1;
                        strXoverSelectionMode = "TOURNAMENT";
                    }
                    if (value.compareToIgnoreCase("RWS") == 0)
                    {
                        xoverSelectionMode = 2;
                        strXoverSelectionMode = "ROULETTE WHEEL";
                    }
                    if (value.compareToIgnoreCase("SUS") == 0)
                    {
                        xoverSelectionMode = 3;
                        strXoverSelectionMode = "STOCHASTIC UNIVERSAL SAMPLING";
                    }
                    if (value.compareToIgnoreCase("RANDOM") == 0)
                    {
                        xoverSelectionMode = 4;
                        strXoverSelectionMode = "RANDOM";
                    }
                }
                break;
            }
            
            case "MUTATEDGRAPHCHECKFAILTOLERANT=":
            {
                mutatedGraphFailedEvalTolerant = readYesNoTrueFalse(value);
                break;
            }
            
            case "XOVERGRAPHCHECKFAILTOLERANT=":
            {
                xoverGraphFailedEvalTolerant = readYesNoTrueFalse(value);
                break;
            }
            
            case "MUTATIONFAILURETOLERANT=":
            {
                mutationFailureTolerant = readYesNoTrueFalse(value);
                break;
            }
            
            case "XOVERFAILURETOLERANT=":
            {
                xoverFailureTolerant = readYesNoTrueFalse(value);
                break;
            }
            
            case "BUILDFAILURETOLERANT=":
            {
                buildAnewFailureTolerant = readYesNoTrueFalse(value);
                break;
            }
            
            
            default:
                msg = "Keyword " + key + " is not a known GeneticAlgorithm-" 
                        + "related keyword. Check input files.";
               throw new DENOPTIMException(msg);
        }
    }
    
//------------------------------------------------------------------------------
    
    public static int convertProbabilityScheme(String option) 
    		throws DENOPTIMException 
    {
        int res = 0;
    	switch (option.toUpperCase())
        {
            case "EXP_DIFF":
                res = 0;
                break;
            case "TANH":
                res = 1;
                break;
            case "SIGMA":
                res = 2;
                break;
            case "UNRESTRICTED":
                res = 3;
                break;
            default:
                throw new DENOPTIMException(
                		"Unknown growth probability scheme.");
        }
		return res;
	}
    
//-----------------------------------------------------------------------------

	/**
     * Create the directory that will store the output of the GA run
     * @throws DENOPTIMException 
     */

    private void createWorkingDirectory() throws DENOPTIMException
    {
        String cdataDir = dataDir;
        boolean success = false;
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");
            timeStamp = sdf.format(new Date());
            String str = "RUN" + timeStamp;
            dataDir = cdataDir + DENOPTIMConstants.FSEP + str;
            success = denoptim.files.FileUtils.createDirectory(dataDir);
        }
        setWorkingDirectory(dataDir);
        if (!denoptim.files.FileUtils.createDirectory(interfaceDir))
        {
        	throw new DENOPTIMException("ERROR! Unable to make interface "
        			+ "folder '" + interfaceDir + "'");
        }
        denoptim.files.FileUtils.addToRecentFiles(dataDir, FileFormat.GA_RUN);
    }

//------------------------------------------------------------------------------

    /** 
     * Processes currently loaded fields.  
     * @throws DENOPTIMException typically due to I/O exception.
     */

    public void processParameters() throws DENOPTIMException
    {
        if (isMaster)
            createWorkingDirectory();

        if (seed == 0)
        {
            startRandomizer();
            seed = getRandomSeed();
        } else {
            startRandomizer(seed);
        }

        int nproc = Runtime.getRuntime().availableProcessors();
        if (numParallelTasks == 0)
        {
            numParallelTasks = nproc;
        }
        
        processOtherParameters();
        
        if (isMaster)
        {    
            StaticLogger.appLogger.log(Level.INFO, "Program log file: " 
                    + logFile + DENOPTIMConstants.EOL 
                    + "Output files associated with the current run are "
                    + "located in " + dataDir);
        }
    }

//------------------------------------------------------------------------------

    public void checkParameters() throws DENOPTIMException
    {
        String error = "";
        //TODO: use something like the following for checking the parameters:
        //ensureIsPositive("GA-NUMOFFSPRING", numOfChildren, "blabla");
        if (numOfChildren <= 0)
        {
            error = "Number of children must be a positive number.";
            throw new DENOPTIMException(error);
        }
        if (numGenerations <= 0)
        {
            error = "Number of generations must be a positive number.";
            throw new DENOPTIMException(error);
        }
        
        if (numConvGen <= 0)
        {
            error = "Number of convergence iterations must be a positive "
                    + "number.";
            throw new DENOPTIMException(error);
        }
        
        if (symmetricSubProbability < 0. ||
                            symmetricSubProbability > 1.)
        {
            error = "Symmetric molecule probability must be between 0 and 1.";
            throw new DENOPTIMException(error);
        }
        
        if (mutationWeight < 0.)
        {
            error = "Weight of mutation must be a positive number";
            throw new DENOPTIMException(error);
        }
        
        if (crossoverWeight < 0.)
        {
            error = "Weight of crossover must be a positive number";
            throw new DENOPTIMException(error);
        }
        
        if (builtAnewWeight < 0.)
        {
            error = "Weight of construction must be a positive number";
            throw new DENOPTIMException(error);
        }

        if (initPoplnFile.length() > 0)
        {
            if (!denoptim.files.FileUtils.checkExists(initPoplnFile))
            {
                error = "Cannot find initial population data: " + initPoplnFile;
                throw new DENOPTIMException(error);
            }
        }

        if (replacementStrategy < 0 || replacementStrategy > 2)
        {
            error = "Allowed values for replacementStrategy (1-2)";
            throw new DENOPTIMException(error);
        }
        
        if (useMolSizeBasedProb && useLevelBasedProb)
        {
            error = "Cannot use both graph level or molecular size as criterion "
                    + "for controlling the growth of graphs. "
                    + "Please, use either of them.";
            throw new DENOPTIMException(error);
        } else if (!useMolSizeBasedProb && !useLevelBasedProb) {
            useMolSizeBasedProb = true;
        }
        checkOtherParameters();
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

}
