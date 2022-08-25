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

package denoptim.integration.tinker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ChemicalObjectModel;
import denoptim.task.ProcessHandler;
import denoptim.utils.ObjectPair;

/**
 * Toolkit to perform conformational search via Tinker PSSROT program
 *
 * @author Marco Foscato 
 */

public class ConformationalSearchPSSROT
{
    /**
     * File separator
     */
    private final static String FSEP = System.getProperty("file.separator");
    
    /**
     * Newline character
     */
    private final static String NL = DENOPTIMConstants.EOL;

//------------------------------------------------------------------------------    
    /**
     * Performs PSSROT conformational search for all chemical objects in the 
     * list. The objects will be modifier: no cloning.
     * @param mols the list of object to modify.
     * @param runLabel a string to identify the type of run in log messages (
     * e.g., "cs" for conformational search, "rc" for ring closing 
     * conformational search).
     *  @param ffFilePathName the pathname of the force-field parameters file
     * to use in the PSSROT job.
     * @param keyFileLines the lines of Tinker's key file (if the 
     * <code>parameters</code> line is included it will be ignored as the 
     * name of the parameters file is given explicitly.
     * @param subParamsInit the initial part of Tinker's job submission 
     * parameters: the responses to the first two question.
     * @param subParamsRest the remaining part of Tinker's job submission 
     * parameters.
     * @param pssExePathName pathname to Tinker <code>pssrot</code> executable.
     * @param xyzintPathName pathname to Tinker <code>xyzint</code> executable.
     * @param workDir the pathname of the file system location where we are
     * going to create files and work with Tinker.
     * @param taskId a number identifying the overall task. Useful for logging.
     * @param logger the tool dealing with log messages.
     * @return the list of generated conformations.
     * @throws DENOPTIMException
     * @throws TinkerException if tinker fails.
     */

    public static void performPSSROT(ArrayList<ChemicalObjectModel> mols, 
            String runLabel, String ffFilePathName, List<String> keyFileLines, 
            List<String> subParamsInit, List<String> subParamsRest,
            String pssExePathName, String xyzintPathName, String workDir, 
            int taskId, Logger logger) throws DENOPTIMException, TinkerException
    {
        for (int i=0; i<mols.size(); i++)
        {
    	    Object molErroProp = mols.get(i).getIAtomContainer().getProperty(
    								   DENOPTIMConstants.MOLERRORTAG);
    	    if (molErroProp == null)
    	    {
    	        logger.log(Level.INFO, "Field MOL_ERROR is null: proceeding "
    	                + "with conformational search.");
        		performPSSROT(mols.get(i), i, 
        		        runLabel, ffFilePathName, keyFileLines,
        		        subParamsInit, subParamsRest,
        		        pssExePathName, xyzintPathName,
        		        workDir, taskId, logger);
    	    } else {
        		logger.log(Level.INFO, "Field MOL_ERROR is NOT null: skiping "
        		        + "conformational search. Reason: " + molErroProp);
    	    }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Performs PSSROT conformational search on the given chemical object.
     * The object is modified directly, no cloning.
     * @param chemObj the object to perform conformational search on.
     * @param idm index to distinguish molecules with same name (e.g., multiple
     * models starting from the same {@link ChemicalObjectModel}.
     * @param runLabel a string to identify the type of run in log messages (
     * e.g., "cs" for conformational search, "rc" for ring closing 
     * conformational search).
     * @param ffFilePathName the pathname of the force-field parameters file
     * to use in the PSSROT job.
     * @param keyFileLines the lines of Tinker's key file (if the 
     * <code>parameters</code> line is included it will be ignored as the 
     * name of the parameters file is given explicitly.
     * @param subParamsInit the initial part of Tinker's job submission 
     * parameters: the responses to the first two question.
     * @param subParamsRest the remaining part of Tinker's job submission 
     * parameters.
     * @param pssrotPathName pathname to Tinker <code>pssrot</code> executable.
     * @param xyzintPathName pathname to Tinker <code>xyzint</code> executable.
     * @param workDir the pathname of the file system location where we are
     * going to create files and work with Tinker.
     * @param taskId a number identifying the overall task. Useful for logging.
     * @param logger the tool dealing with log messages.
     * @return the new conformation of the molecular system 
     * @throws DENOPTIMException
     * @throws TinkerException 
     */

    public static void performPSSROT(ChemicalObjectModel chemObj, 
            int idm, String runLabel, 
            String ffFilePathName, List<String> keyFileLines, 
            List<String> subParamsInit, List<String> subParamsRest,
            String pssrotPathName, String xyzintPathName, String workDir, 
            int taskId, Logger logger)
						throws DENOPTIMException, TinkerException
    {
        logger.log(Level.INFO, "Start conformational search on mol: " + idm);

        // Is there any rotatable bond?
        int sz = chemObj.getNumberRotatableBonds();
    	if (sz == 0)
    	{
    	    logger.log(Level.FINE, "No rotatable bond: skiping "
    	            + " PSSROT conformational search.");
    	    return;
    	}

        TinkerMolecule tmol = chemObj.getTinkerMolecule();
        String molName = chemObj.getName();

        // Prepare Tinker INT file (Internal coordinates)
        String csIntFile = workDir + FSEP + molName + "_"+runLabel + idm 
                + ".int";
        TinkerUtils.writeIC(csIntFile, tmol);

        //Prepare Tinker KEY file (keywords, definition of potential)
        String csKeyFile = workDir + FSEP + molName + "_"+runLabel + idm 
                + ".key";
        StringBuilder csSbKey = new StringBuilder(512);
        // Molecule-independent keywords
        csSbKey.append("parameters    ").append(ffFilePathName).append(NL);
        for (String line : keyFileLines)
        {
            if (line.toUpperCase().startsWith("PARAMETERS"))
                continue;
            csSbKey.append(line).append(NL);
        }
        DenoptimIO.writeData(csKeyFile, csSbKey.toString(), false);

        // Prepare Tinker Submit file (PSSROT parameters)
        String csSubFile = workDir + FSEP + molName + "_"+runLabel + idm 
                + ".sub";
        StringBuilder csSbSub = new StringBuilder(512);
        csSbSub.append(csIntFile).append(NL);
        // Molecule-independent section of SUB file
        for (String line : subParamsInit)
        {
            csSbSub.append(line).append(NL);
        }
        // Rotatable bonds section of SUB file
        for (ObjectPair rotBndOp : chemObj.getRotatableBonds())
        {
            int t1 = ((Integer)rotBndOp.getFirst()).intValue() + 1;
            int t2 = ((Integer)rotBndOp.getSecond()).intValue() + 1;
            csSbSub.append(t1).append(" ").append(t2).append(NL);
        }
        csSbSub.append(NL);
        // Linear search section of SUB file
        if (sz > 1)
        {
            for (int ir=0; ir<subParamsRest.size(); ir++)
            {
        		// Control number of linear search directions
        		if (ir == 2)
        		{
        		    int maxDirs = Integer.parseInt(subParamsRest.get(ir));
        		    if (sz < maxDirs)
        		    {
        		        csSbSub.append(sz).append(NL);
        		    } else {
                        csSbSub.append(maxDirs).append(NL);
        		    }
        		} else {
        		    String row = subParamsRest.get(ir);
                    csSbSub.append(row).append(NL);
        		}
            }
        } else {
            if (sz == 1)
            {
                // No linear search if there is only 1 rotation bond
                String firstLine = subParamsRest.get(0);
                String lastLine = subParamsRest.get(subParamsRest.size()-1);
                csSbSub.append(firstLine).append(NL);
                csSbSub.append("N").append(NL); // no local search                
                csSbSub.append(lastLine).append(NL);
            }
        }
        DenoptimIO.writeData(csSubFile, csSbSub.toString(), false);

        logger.log(Level.INFO, "Submitting PSSTOR Conformational Search");

        // Perform Ring Search with Tinker's PSSROT
        String csLogFile = workDir + FSEP + molName + "_" + runLabel + idm 
                + ".log";
        String csCmdStr = pssrotPathName + " < " + csSubFile + " > " +csLogFile;
        String csID = "" + taskId;
        logger.log(Level.FINE, "CMD: " + csCmdStr + " TskID: " + csID);
        ProcessHandler csPh = new ProcessHandler(csCmdStr, csID);
        try
        {
            csPh.runProcessInBASH();
            if (csPh.getExitCode() != 0)
            {
                String msg = "PSSROT Conformational Search failed for ";
                msg = msg + molName;
                throw new DENOPTIMException(msg + NL + csPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        
        //We check for the first output file, i.e., .000 but there could be many
        TinkerUtils.ensureOutputExistsOrRelayError(workDir + FSEP + molName 
                + "_" + runLabel + idm + ".000", csLogFile, 
                runLabel + "PSSROT job");

        // Convert XYZ output of Tinker to INT with same z.matrix
        // Here we assume the file *.int with same basename exists
        // (was used it as input the PSSTOR step) and that file works as
        // template of the z-matrix
        String ocsIntfile = TinkerUtils.getNameLastCycleFile(workDir,
                molName + "_"+ runLabel + idm, csLogFile,
                " Final Function Value and Deformation");

    	// But before that, need to handle case where Tinker splits line in two
    	fixLongeLinesInXYZ(ocsIntfile);

    	String newTnkICLog = workDir + FSEP + molName + "_"+runLabel + idm 
    	        + ".log2";
        String ocsID = "" + taskId;
        String ocsCmd = xyzintPathName + " " + ocsIntfile + " "
                        + " T " //set use of template 
                        + " > " + newTnkICLog;
        logger.log(Level.INFO, "CMD: " + ocsCmd+" TskID: "+ocsID);
        ProcessHandler ocsPh = new ProcessHandler(ocsCmd, ocsID);
        try
        {
            ocsPh.runProcessInBASH();
            if (ocsPh.getExitCode() != 0)
            {
                String msg = "XYZINT (post conf.search) failed for " + molName;
                throw new DENOPTIMException(msg + NL + ocsPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        
        // Conversion can fail if system is beyond the capabilities of Tinker.
        // Such capabilities can be expanded by altering Tinker's parameters 
        // such as maxval and maxtors (and others?)
        String newTnkIC = workDir + FSEP + molName + "_" + runLabel + idm 
                + ".int_2";
        TinkerUtils.ensureOutputExistsOrRelayError(newTnkIC, newTnkICLog, 
                "convert xyz to int for " + runLabel + "job");
        
        // Update local molecular representation with output from PSSROT
        TinkerMolecule tmpTmol = TinkerUtils.readTinkerIC(newTnkIC);
        ArrayList<TinkerAtom> lstAtoms = tmpTmol.getAtoms();
        for (int i=0; i<lstAtoms.size(); i++)
        {
            TinkerAtom ta = lstAtoms.get(i);
            tmol.getAtom(i+1).setDistAngle(ta.getDistAngle());
        }
        chemObj.updateXYZFromINT();

        // Cleanup
        FileUtils.deleteFilesContaining(workDir,molName + "_" + runLabel + idm);
    }

//------------------------------------------------------------------------------    
    
    /**
     * The XYZ file generated by Tinker for molecules having atoms with many 
     * Neighbors is corrupted: the line of the atom with long list of connected
     * atoms is split in two lines. 
     * This method fixes the XYZ file displaying this problem for the given 
     * filename.
     * @param filename the pathname of the file to fix.
     */

    public static void fixLongeLinesInXYZ(String filename) 
            throws DENOPTIMException 
    {
    	ArrayList<String> txtLines = new ArrayList<String>();
    	try {
    	    txtLines = DenoptimIO.readList(filename);
    	}
    	catch (Throwable t)
    	{
    	    t.printStackTrace();
    	    throw new DENOPTIMException(" unable to read file " + filename
    								+ " " +t);
    	}
    	String[] wFrstLine = txtLines.get(0).trim().split("\\s+");
    	int numAtms = Integer.parseInt(wFrstLine[0]);
    
    	// Do we need to fix this file?
    	if (numAtms+1 == txtLines.size())
    	{
    	    return;
    	}
    
    	StringBuilder sb = new StringBuilder(512);
    	sb.append(txtLines.get(0));
    	int newI = 1;
    	for (int i=1; i<txtLines.size(); i++)
    	{
    	    String line = txtLines.get(i).trim();
    
    	    if (line == "" || line == null)
    	        continue;
    
    	    String[] wLine = line.split("\\s+");
    	    int atmNum = Integer.parseInt(wLine[0]);
    	    if (newI == atmNum)
    	    {
    		sb.append(NL).append(txtLines.get(i));
    	    }
    	    else
    	    {
    		sb.append(" ").append(txtLines.get(i));
    		newI--;
    	    }
    	    newI++;
    	}
    
    	sb.append(NL);
    	DenoptimIO.writeData(filename, sb.toString(), false);
    }

//------------------------------------------------------------------------------    
}
