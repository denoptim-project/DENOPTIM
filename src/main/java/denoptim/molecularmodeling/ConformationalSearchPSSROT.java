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

package denoptim.molecularmodeling;

import java.util.ArrayList;
import java.util.logging.Level;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.integration.tinker.TinkerAtom;
import denoptim.integration.tinker.TinkerException;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.programs.moldecularmodelbuilder.MMBuilderParameters;
import denoptim.task.ProcessHandler;
import denoptim.utils.GenUtils;
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
    final String fsep = System.getProperty("file.separator");
    
    /**
     * Settings controlling the execution of this task
     */
    private MMBuilderParameters settings;
    
    private final static String NL = DENOPTIMConstants.EOL;

//------------------------------------------------------------------------------

    /**
     * Construct an empty ConformationalSearchPSSROT
     */
    public ConformationalSearchPSSROT(MMBuilderParameters settings)
    {
        this.settings = settings;
    }

//------------------------------------------------------------------------------

    /**
     *
     * TODO: this method (from DENOPTIM3DMoleculeBuilder.java) should be made 
     * public and moved to a more sensible location (i.e., a class of utilities
     * for tinker methods)
     *
     */

    private String getNameLastCycleFile(String fname, String tinkerresfile)
                                                        throws DENOPTIMException
    {
        int lastIdx = MMBuilderUtils.countLinesWKeywordInFile(tinkerresfile,
                                " Final Function Value and Deformation");
        String workDir = settings.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." +
                                 GenUtils.getPaddedString(3, lastIdx - 1);
        return xyzfile;
    }

//------------------------------------------------------------------------------    
    /**
     * Performs PSSROT conformational search for all molecules in the list
     * @param fitProvMol the list of input molecules 
     * @return the list of generated conformations.
     * @throws DENOPTIMException
     * @throws TinkerException if tinker fails.
     */

    public ArrayList<ChemicalObjectModel> performPSSROT(
		     ArrayList<ChemicalObjectModel> mols) 
		             throws DENOPTIMException, TinkerException
    {
        ArrayList<ChemicalObjectModel> nMols = new ArrayList<ChemicalObjectModel>();
        for (int i=0; i<mols.size(); i++)
        {
    	    Object molErroProp = mols.get(i).getIAtomContainer().getProperty(
    								   DENOPTIMConstants.MOLERRORTAG);
    	    if (molErroProp == null)
    	    {
    	        settings.getLogger().log(Level.INFO, "Field MOL_ERROR is "
        		            + "null: proceeding with conformational search.");
        		ChemicalObjectModel csMol = performPSSROT(mols.get(i), i);
                nMols.add(csMol);
    	    } else {
        		settings.getLogger().log(Level.INFO, "Field MOL_ERROR is "
        		            + "NOT null: skiping conformational search. "
        		            + "Reason: " + molErroProp);
        		nMols.add(mols.get(i).deepcopy());
    	    }
        }

        return nMols;
    }

//------------------------------------------------------------------------------

    /**
     * Performs PSSROT conformational search on the given molecule
     * @param mol the input molecular system 
     * @param idm index to distinguish molecules with same name
     * @return the new conformation of the molecular system 
     * @throws DENOPTIMException
     * @throws TinkerException 
     */

    public ChemicalObjectModel performPSSROT(ChemicalObjectModel mol, int idm)
						throws DENOPTIMException, TinkerException
    {
        settings.getLogger().log(Level.INFO, 
                "Start conformational search on mol: " + idm);

        ChemicalObjectModel csMol3d = mol.deepcopy();

        // Is there any rotatable bond?
        int sz = csMol3d.getNumberRotatableBonds();
    	if (sz == 0)
    	{
    	    settings.getLogger().log(Level.FINE, "No rotatable bond: skiping "
    	            + " PSSROT conformational search.");
    	    return csMol3d;
    	}

        TinkerMolecule tmol = csMol3d.getTinkerMolecule();
        String workDir = settings.getWorkingDirectory();
        String molName = csMol3d.getName();

        // Prepare Tinker INT file (Internal coordinates)
        String csIntFile = workDir + fsep + molName + "_cs" + idm + ".int";
        TinkerUtils.writeIC(csIntFile, tmol);

        //Prepare Tinker KEY file (keywords, definition of potential)
        String csKeyFile = workDir + fsep + molName + "_cs" + idm + ".key";
        StringBuilder csSbKey = new StringBuilder(512);
        // Molecule-independent keywords
        csSbKey.append("parameters    ").append(settings.getParamFile()).append(NL);
        boolean isFirstLine = true;
        for (String line : settings.getKeyFileParams())
        {
    	    if (isFirstLine)
    	    {
                isFirstLine = false;
                continue;
    	    }
            csSbKey.append(line).append(NL);
        }
        DenoptimIO.writeData(csKeyFile, csSbKey.toString(), false);

        // Prepare Tinker Submit file (PSSROT parameters)
        String csSubFile = workDir + fsep + molName + "_cs" + idm + ".sub";
        StringBuilder csSbSub = new StringBuilder(512);
        csSbSub.append(csIntFile).append(NL);
        // Molecule-independent section of SUB file
        for (String line : settings.getInitPSSROTParams())
        {
            csSbSub.append(line).append(NL);
        }
        // Rotatable bonds section of SUB file
        for (ObjectPair rotBndOp : csMol3d.getRotatableBonds())
        {
            int t1 = ((Integer)rotBndOp.getFirst()).intValue() + 1;
            int t2 = ((Integer)rotBndOp.getSecond()).intValue() + 1;
            csSbSub.append(t1).append(" ").append(t2).append(NL);
        }
        csSbSub.append(NL);
        // Linear search section of SUB file
        if (sz > 1)
        {
            ArrayList<String> txt = settings.getRestPSSROTParams();
            for (int ir=0; ir<txt.size(); ir++)
            {
        		// Control number of linear search directions
        		if (ir == 2)
        		{
        		    int maxDirs = Integer.parseInt(txt.get(ir));
        		    if (sz < maxDirs)
        		    {
        		        csSbSub.append(sz).append(NL);
        		    }
        		    else
        		    {
                        csSbSub.append(maxDirs).append(NL);
        		    }
        		} else {
        		    String row = txt.get(ir);
                    csSbSub.append(row).append(NL);
        		}
            }
        } else {
            if (sz == 1)
            {
                // No linear search if there is only 1 rotation bond
                String firstLine = settings.getRestPSSROTParams().get(0);
                String lastLine = settings.getRestPSSROTParams().get(
                                 settings.getRestPSSROTParams().size()-1);
                csSbSub.append(firstLine).append(NL);
                csSbSub.append("N").append(NL); // no local search                
                csSbSub.append(lastLine).append(NL);
            }
        }
        DenoptimIO.writeData(csSubFile, csSbSub.toString(), false);

        settings.getLogger().log(Level.INFO, 
                "Submitting PSSTOR Conformational Search");

        // Perform Ring Search with Tinker's PSSROT
        String csLogFile = workDir + fsep + molName + "_cs" + idm + ".log";
        String csCmdStr = settings.getPSSROTTool() +
                          " < " + csSubFile + " > " + csLogFile;
        String csID = "" + settings.getTaskID();
        settings.getLogger().log(Level.FINE, "CMD: "+csCmdStr+" TskID: "+csID);
        ProcessHandler csPh = new ProcessHandler(csCmdStr, csID);
        try
        {
            csPh.runProcess();
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
        TinkerUtils.ensureOutputExistsOrRelayError(workDir + fsep + molName 
                + "_cs" + idm + ".000", csLogFile, "PSSROT job");

        // Convert XYZ output of Tinker to INT with same z.matrix
        // Here we assume the file *.int with same basename exists
        // (was used it as input the PSSTOR step) and that file works as
        // template of the z-matrix
        String ocsIntfile = getNameLastCycleFile(molName + "_cs" 
							+ idm, csLogFile);

    	// But before that, need to handle case where Tinker splits line in two
    	fixLongeLinesInXYZ(ocsIntfile);

    	String newTnkICLog = workDir + fsep + molName + "_cs" + idm + ".log2";
        String ocsID = "" + settings.getTaskID();
        String ocsCmd = settings.getXYZINTTool() + " "
                        + ocsIntfile + " "
                        + " T " //set use of template 
                        + " > " + newTnkICLog;
        settings.getLogger().log(Level.INFO, "CMD: " + ocsCmd+" TskID: "+ocsID);
        ProcessHandler ocsPh = new ProcessHandler(ocsCmd, ocsID);
        try
        {
            ocsPh.runProcess();
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
        String newTnkIC = workDir + fsep + molName + "_cs" + idm + ".int_2";
        TinkerUtils.ensureOutputExistsOrRelayError(newTnkIC, newTnkICLog, 
                "convert xyz to int");
        
        // Update local molecular representation with output from PSSROT
        TinkerMolecule tmpTmol = TinkerUtils.readTinkerIC(newTnkIC);
        ArrayList<TinkerAtom> lstAtoms = tmpTmol.getAtoms();
        for (int i=0; i<lstAtoms.size(); i++)
        {
            TinkerAtom ta = lstAtoms.get(i);
            tmol.getAtom(i+1).setDistAngle(ta.getDistAngle());
        }
        csMol3d.updateXYZFromINT();

        // Cleanup
        FileUtils.deleteFilesContaining(workDir,molName + "_cs" + idm);
        
        return csMol3d;
    }

//------------------------------------------------------------------------------    
    
    /**
     * The XYZ file generated by tinker for molecules having atoms with many 
     * neighbours is corrupted: the line ofthe atom with long list of connected
     * atoms is splitted in two. This method fixes the XYZ file displaying this
     * problem
     */

    public void fixLongeLinesInXYZ(String filename) throws DENOPTIMException 
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
	String eol = System.getProperty("line.separator");
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
		sb.append(eol).append(txtLines.get(i));
	    }
	    else
	    {
		sb.append(" ").append(txtLines.get(i));
		newI--;
	    }
	    newI++;
	}

	sb.append(eol);
	DenoptimIO.writeData(filename, sb.toString(), false);
    }

//------------------------------------------------------------------------------    
}
