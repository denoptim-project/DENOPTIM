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

package denoptim.task;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.combinatorial.GraphBuildingTask;
import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fitness.FitnessProvider;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.TaskUtils;

/**
 * Task that assesses the fitness of a given graph.
 */

public abstract class FitnessTask extends Task
{
	/**
	 * The graph representation of the entity to evaluate.
	 */
    protected DGraph dGraph;
    
	/**
	 * The chemical representation of the entity to evaluate. We do not check 
	 * for consistency between this member and the graph representation.
	 * This data structure holds also lots of attributes that are not used
	 * in all subclasses extending the FitnessTask. Moreover, it will be updated
	 * once a presumably refined molecular representation is produced by
	 * a fitness provider.
	 */
    protected IAtomContainer fitProvMol = null;
    
    /**
     * The data structure holding the results of this task
     */
    protected Candidate result;
    
    /**
     * The file where we store the input to the fitness provider.
     */
    protected String fitProvInputFile = "noName"
            + DENOPTIMConstants.FITFILENAMEEXTIN;
    
    /**
     * The file where we store the final output from the fitness provider.
     */
    protected String fitProvOutFile = "noName" 
            + DENOPTIMConstants.FITFILENAMEEXTOUT;
    
    /**
     * The file where we store the graphical representation of the candidate 
     * (i.e., a picture).
     */
    protected String fitProvPNGFile = "noName"
            + DENOPTIMConstants.CANDIDATE2DEXTENSION;
    
    /**
     * The file where we store the list of unique identifiers or previously 
     * evaluated candidates.
     */
    protected String fitProvUIDFile = null;
    
    /**
     * Flag specifying if a valid fitness value is required to consider the
     * task successfully completed.
     */
    protected boolean fitnessIsRequired = false;
    
    /**
     * Settings for the calculation of the fitness
     */
    protected FitnessParameters fitnessSettings;

//------------------------------------------------------------------------------
    
    public FitnessTask(FitnessParameters settings, Candidate c)
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	this.fitnessSettings = settings;
    	this.result = c;
        this.dGraph = c.getGraph();
    }

//------------------------------------------------------------------------------

    /**
     * This method runs the actual evaluation of the fitness, whether that is 
     * run internally (i.e., within this instance of the JAVA VM), or 
     * delegated to an external child process.
     * @return the object with data obtained from the fitness provider.
     * @throws DENOPTIMException
     */
    protected void runFitnessProvider() throws DENOPTIMException
    {
    	// Ensure these two variables have been set
        result.setSDFFile(fitProvOutFile);
        if (fitProvMol == null)
    	{
            ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(
                    fitnessSettings.getLogger(),
                    fitnessSettings.getRandomizer());
            fitProvMol = t3d.convertGraphTo3DAtomContainer(dGraph, true);
    	}
        
        if (fitProvMol.getProperty(DENOPTIMConstants.PROVENANCE) == null ||
        		fitProvMol.getProperty(
        		        DENOPTIMConstants.PROVENANCE).toString().equals(""))
        {
        	fitProvMol.removeProperty(DENOPTIMConstants.PROVENANCE);
        }
        
        // Run fitness provider
        boolean status = false;
        if (fitnessSettings.useExternalFitness()) {
            // Write file with input data to fitness provider
            DenoptimIO.writeSDFFile(fitProvInputFile, fitProvMol, false);

            // NB: inside this call we change fitProvMol for a reordered copy: 
            //     reference will not work!
            status = runExternalFitness();
        } else {
        	// NB: the internal fitness provider removes dummy atoms before 
            // calculating CDK descriptors, so the 'fitProvMol' changes
            status = runInternalFitness();
        }
        
        // Write the FIT file
        result.setChemicalRepresentation(fitProvMol);
        if (this instanceof GraphBuildingTask 
                || fitnessSettings.writeCandidatesOnDisk())
        {
            DenoptimIO.writeCandidateToFile(new File(fitProvOutFile), result, 
                    false);
        }
        
        // Optional image creation
        if (status && fitnessSettings.makePictures())
        {
            try
            {
                MoleculeUtils.moleculeToPNG(fitProvMol,fitProvPNGFile,
                        fitnessSettings.getLogger());
                result.setImageFile(fitProvPNGFile);
            }
            catch (Exception ex)
            {
                result.setImageFile(null);
                fitnessSettings.getLogger().log(Level.WARNING, 
                    "Unable to create image. {0}", ex.getMessage());
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if it is all good, <code>false</code> in case 
     * of any reason for premature returning of the results (error generated in
     * from the external tool, rejection on the candidate).
     * @throws DENOPTIMException 
     * @throws Exception 
     */

	private boolean runExternalFitness() throws DENOPTIMException
	{
		StringBuilder sb = new StringBuilder();
        sb.append(fitnessSettings.getExternalFitnessProviderInterpreter());
        sb.append(" ").append(fitnessSettings.getExternalFitnessProvider())
              .append(" ").append(fitProvInputFile)
              .append(" ").append(fitProvOutFile)
              .append(" ").append(workDir)
              .append(" ").append(id);
        if (fitProvUIDFile != null)
        {
            sb.append(" ").append(fitProvUIDFile);
        }
        
        String msg = "Calling external fitness provider: => " + sb + NL;
        fitnessSettings.getLogger().log(Level.INFO, msg);

        // run the process
        processHandler = new ProcessHandler(sb.toString(),Integer.toString(id));

        processHandler.runProcess();
        if (processHandler.getExitCode() != 0)
        {
            msg = "Failed to execute fitness provider " 
                + fitnessSettings.getExternalFitnessProviderInterpreter()
                    .toString()
		        + " command '" + fitnessSettings.getExternalFitnessProvider()
		        + "' on " + fitProvInputFile;
            fitnessSettings.getLogger().severe(msg);
            fitnessSettings.getLogger().severe(
            		processHandler.getErrorOutput());
            throw new DENOPTIMException(msg);
        }
        processHandler = null;
        
        // Read results from fitness provider
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer processedMol = builder.newAtomContainer();
        boolean unreadable = false;
        try
        {
            processedMol = DenoptimIO.readAllAtomContainers(new File(
                    fitProvOutFile)).get(0);
            if (processedMol.isEmpty())
            {
                unreadable=true;
            }
            Object o = processedMol.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
            if (o != null)
            {
                String[] parts = o.toString().trim().split("\\s+");
                if (processedMol.getAtomCount() != parts.length)
                {
                    throw new DENOPTIMException("Inconsistent number of vertex "
                            + "IDs (" + parts.length + ") and atoms (" 
                            + processedMol.getAtomCount() + ") in candidate "
                            + "processed by external fitness provider.");
                }
                for (int i=0; i<processedMol.getAtomCount(); i++)
                {
                    processedMol.getAtom(i).setProperty(
                            DENOPTIMConstants.ATMPROPVERTEXID, 
                            Integer.parseInt(parts[i]));
                }
            }
        }
        catch (Throwable t)
        {
            unreadable=true;
        }
        
        if (unreadable)
        {
        	// If file is not properly readable, we keep track of the 
        	// unreadable file, and we label the candidate to signal the 
        	// error, and we replace the unreadable one with a file that 
        	// is readable.
        	
            msg = "Unreadable file from fitness provider run (Task " + id 
            		+ "). Check " + result.getName() + ".";
            fitnessSettings.getLogger().log(Level.WARNING, msg);
            
            String fileBkp = fitProvOutFile 
                    + DENOPTIMConstants.UNREADABLEFILEPOSTFIX;
            try {
				FileUtils.copyFile(new File(fitProvOutFile), new File(fileBkp));
			} catch (IOException e) {
				// At this point the file must be there!
				throw new DENOPTIMException("File '"+ fitProvOutFile + "' has "
						+ "disappeared (it was there, but not anymore!)");
			}
            FileUtils.deleteQuietly(new File(fitProvOutFile));
            
            String err = "#FTask: Unable to retrive data. See " + fileBkp;
            processedMol = new AtomContainer();
            processedMol.addAtom(new Atom("H"));
            
            result.setChemicalRepresentation(processedMol);
            result.setError(err);
            return false;
        }
        
        // Unique identifier might be updated by the fitness provider, so
        // we need to update the returned value
        if (processedMol.getProperty(DENOPTIMConstants.UNIQUEIDTAG) != null)
        {
            result.setUID(processedMol.getProperty(
            		DENOPTIMConstants.UNIQUEIDTAG).toString());
        }
        
        if (processedMol.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
        {
        	String err = processedMol.getProperty(
        			DENOPTIMConstants.MOLERRORTAG).toString();
            msg = result.getName() + " has an error ("+err+")";
            fitnessSettings.getLogger().info(msg);

            result.setChemicalRepresentation(processedMol);
            result.setError(err);
            return false;
        }
        
        if (processedMol.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
        {
            String fitprp = processedMol.getProperty(
            		DENOPTIMConstants.FITNESSTAG).toString();
            double fitVal = 0.0;
            try
            {
                fitVal = Double.parseDouble(fitprp);
            }
            catch (Throwable t)
            {
                // TODO: why sync? Is it really needed?
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Fitness value '" + fitprp + "' of " 
                            + result.getName() + " could not be converted "
                            + "to double.";
                    errMsg = msg;
                    thrownExc = t;
                }
                fitnessSettings.getLogger().severe(msg);
                dGraph.cleanup();
                throw new DENOPTIMException(msg);
            }

            if (Double.isNaN(fitVal))
            {
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Fitness value is NaN for " + result.getName();
                    errMsg = msg;
                }
                fitnessSettings.getLogger().severe(msg);
                dGraph.cleanup();
                throw new DENOPTIMException(msg);
            }
            
            //TODO: consider this...
            // We want to retain as much as possible of the info we had on the
            // initial, pre-processing molecular representation. However, the
            // external task may have altered the molecular representation
            // to the point we cannot recover. Still, since the graph may be
            // conceptual, i.e., it intentionally does not translate into a valid
            // molecular representation within DENOPTIM, but it does so only
            // within the external fitness provider, we might still prefer
            // to collect the final molecular representation that generated by
            // the external tasks. This could be something to be made optional.
            
            // Replace initial molecular representation of this object with 
            // that coming from the external fitness provider.
            fitProvMol = processedMol;
            result.setChemicalRepresentation(processedMol);
            result.setFitness(fitVal);
        } else {
            if (fitnessIsRequired)
            {
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Could not find '" + DENOPTIMConstants.FITNESSTAG 
                    		+ "' tag in: " + fitProvOutFile;
                    errMsg = msg;
                }
                fitnessSettings.getLogger().severe(msg);
                throw new DENOPTIMException(msg);
        	}
        }
        return true;
	}
	
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if it is all good, <code>false</code> in case 
     * of any reason for premature returning of the results.
     * @throws DENOPTIMException 
     * @throws Exception 
     */
	
	private boolean runInternalFitness() throws DENOPTIMException 
	{
		String msg = "Calling internal fitness provider. "+ NL;
	    fitnessSettings.getLogger().log(Level.FINE, msg);

	    double fitVal = Double.NaN;
		try {
			FitnessProvider fp = new FitnessProvider(
					fitnessSettings.getDescriptors(),
					fitnessSettings.getFitnessExpression(),
					fitnessSettings.getLogger());
			// NB: here we remove dummy atoms!
			fitVal = fp.getFitness(fitProvMol);
		} catch (Exception e) {
			throw new DENOPTIMException("Failed to calculate fitness.", e);
		}

        if (Double.isNaN(fitVal))
        {
            
            if (fitProvMol.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
            {
                // The MOLERRORTAG has been passed by any embedded task, and we 
                // pass it to the general manipulation of the candidate. 
                msg = fitProvMol.getProperty(DENOPTIMConstants.MOLERRORTAG);
                
            } else {
                // The calculation of the fitness returns NaN 
                msg = "Fitness value is NaN for " + result.getName();
                fitProvMol.setProperty(DENOPTIMConstants.MOLERRORTAG, 
                        "#InternalFitness: NaN value");
            }
            fitProvMol.removeProperty(DENOPTIMConstants.FITNESSTAG);
            errMsg = msg;
            fitnessSettings.getLogger().severe(msg);
            result.setError(msg);
            
            //TODO-V3 make ignoring of NaN optional
            /*
            dGraph.cleanup();
            throw new DENOPTIMException(msg);
            */
            
        } else {
            result.setFitness(fitVal);
        }
        
		return true;
	}

//------------------------------------------------------------------------------

}
