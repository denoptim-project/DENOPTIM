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

package denoptimcg;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import exception.DENOPTIMException;
import io.DenoptimIO;
import logging.DENOPTIMLogger;
import java.util.logging.Level;
import task.ProcessHandler;
import utils.DummyAtomHandler;
import utils.GenUtils;
import utils.ObjectPair;
import tinker.TinkerMolecule;
import tinker.TinkerAtom;
import tinker.TinkerUtils;
import molecule.DENOPTIMEdge;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMVertex;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.silent.RingSet;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.PseudoAtom;

/**
 * Toolkit to perform conformational search via Tinker PSSROT program
 *
 * @author Marco Foscato 
 */

public class ConformationalSearchPSSROT
{

    /**
     * Verbosity level
     */
    private static int verbosity = CGParameters.getVerbosity();

    /**
     * File separator
     */
    final String fsep = System.getProperty("file.separator");

//------------------------------------------------------------------------------

    /**
     * Construct an empty ConformationalSearchPSSROT
     */
    public ConformationalSearchPSSROT()
    {
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
        int lastIdx = CGUtils.countLinesWKeywordInFile(tinkerresfile,
                                " Final Function Value and Deformation");
        String workDir = CGParameters.getWorkingDirectory();
        String xyzfile = workDir + fsep + fname + "." +
                                 GenUtils.getPaddedString(3, lastIdx - 1);
        return xyzfile;
    }

//------------------------------------------------------------------------------    
    /**
     * Performs PSSROT conformational search for all molecules in the list
     * @param mol the list of input molecules 
     * @return the list of generated conformations.
     * @throws DENOPTIMException
     */

    public ArrayList<Molecule3DBuilder> performPSSROT(
		     ArrayList<Molecule3DBuilder> mols) throws DENOPTIMException
    {
        ArrayList<Molecule3DBuilder> nMols = new ArrayList<Molecule3DBuilder>();
        for (int i=0; i<mols.size(); i++)
        {
	    Object molErroProp = mols.get(i).getIAtomContainer().getProperty(
								   "MOL_ERROR");
	    if (molErroProp == null)
	    {
		if (verbosity > 1)
		{
		    System.out.println("Field MOL_ERROR is null: proceeding "
				   + "with conformational search.");
		}
                Molecule3DBuilder csMol = performPSSROT(mols.get(i), i);
                nMols.add(csMol);
	    }
	    else
	    {
		if (verbosity > 1)
		{
		    System.out.println("Field MOL_ERROR is NOT null: skiping "
				   + "conformational search. Reason: "
				   + molErroProp);
		}
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
     */

    public Molecule3DBuilder performPSSROT(Molecule3DBuilder mol, int idm)
						throws DENOPTIMException
    {
        if (verbosity > 1)
        {
            System.out.println("Start conformational search on mol: " + idm);
        }

        Molecule3DBuilder csMol3d = mol.deepcopy();

	// Is there any rotatable bond?
        int sz = csMol3d.getNumberRotatableBonds();
	if (sz == 0)
	{
	    if (verbosity > 1)
	    {
	        System.out.println("No rotatable bond: skip PSSROT "
				+ "conformational search.");
	    }
	    return csMol3d;
	}

        TinkerMolecule tmol = csMol3d.getTinkerMolecule();
        String workDir = CGParameters.getWorkingDirectory();
        String molName = csMol3d.getName();

        // Prepare Tinker INT file (Internal coordinates)
        String csIntFile = workDir + fsep + molName + "_cs" + idm + ".int";
        TinkerUtils.writeIC(csIntFile, tmol);

        //Prepare Tinker KEY file (keywords, definition of potential)
        String csKeyFile = workDir + fsep + molName + "_cs" + idm + ".key";
        StringBuilder csSbKey = new StringBuilder(512);
        // Molecule-independent keywords
	csSbKey.append("parameters    ").append(
				    CGParameters.getParamFile()).append("\n");
	boolean isFirstLine = true;
        for (String line : CGParameters.getKeyFileParams())
        {
	    if (isFirstLine)
	    {
                isFirstLine = false;
		continue;
	    }
            csSbKey.append(line).append("\n");
        }
        DenoptimIO.writeData(csKeyFile, csSbKey.toString(), false);

        // Prepare Tinker Submit file (PSSROT parameters)
        String csSubFile = workDir + fsep + molName + "_cs" + idm + ".sub";
        StringBuilder csSbSub = new StringBuilder(512);
        csSbSub.append(csIntFile).append("\n");
        // Molecule-independent section of SUB file
        for (String line : CGParameters.getInitPSSROTParams())
        {
            csSbSub.append(line).append("\n");
        }
        // Rotatable bonds section of SUB file
        for (ObjectPair rotBndOp : csMol3d.getRotatableBonds())
        {
            int t1 = ((Integer)rotBndOp.getFirst()).intValue() + 1;
            int t2 = ((Integer)rotBndOp.getSecond()).intValue() + 1;
            csSbSub.append(t1).append(" ").append(t2).append("\n");
        }
        csSbSub.append("\n");
        // Linear search section of SUB file
        if (sz > 1)
        {
	    ArrayList<String> txt = CGParameters.getRestPSSROTParams();
            for (int ir=0; ir<txt.size(); ir++)
            {
		// Control number of linear search directions
		if (ir == 2)
		{
		    int maxDirs = Integer.parseInt(txt.get(ir));
		    if (sz < maxDirs)
		    {
			csSbSub.append(sz).append("\n");
		    }
		    else
		    {
                        csSbSub.append(maxDirs).append("\n");
		    }
		}
		else
		{
		    String row = txt.get(ir);
                    csSbSub.append(row).append("\n");
		}
            }
        }
        else 
        {
            if (sz == 1)
            {
                // No linear search if there is only 1 rotation bond
                String firstLine = CGParameters.getRestPSSROTParams().get(0);
                String lastLine = CGParameters.getRestPSSROTParams().get(
                                 CGParameters.getRestPSSROTParams().size()-1);
                csSbSub.append(firstLine).append("\n");
                csSbSub.append("N").append("\n"); // no local search                
                csSbSub.append(lastLine).append("\n");
            }
        }
        DenoptimIO.writeData(csSubFile, csSbSub.toString(), false);

        if (verbosity > 0)
            System.err.println("Submitting PSSTOR Conformational Search");

        // Perform Ring Search with Tinker's PSSROT
        String csOutFile = workDir + fsep + molName + "_cs" + idm + ".log";
        String csCmdStr = CGParameters.getPSSROTTool() +
                          " < " + csSubFile + " > " + csOutFile;
        String csID = "" + CGParameters.getTaskID();
        if (verbosity > 1)
        {
            System.err.println("CMD: " + csCmdStr + " TskID: " + csID);
        }
        ProcessHandler csPh = new ProcessHandler(csCmdStr, csID);
        try
        {
            csPh.runProcess();

            if (csPh.getExitCode() != 0)
            {
                String msg = "PSSROT Conformational Search failed for ";
		msg = msg + molName;
                throw new DENOPTIMException(msg + "\n"
                                            + csPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }

        // Convert XYZ output of Tinker to INT with same z.matrix
        // Here we assume the file *.int with same basename exists
        // (was used it as input the PSSTOR step) and that file works as
        // template of the z-matrix
        String ocsIntfile = getNameLastCycleFile(molName + "_cs" 
							+ idm, csOutFile);

	// But beforethan, need to handle case where Tinker splits line in two
	fixLongeLinesInXYZ(ocsIntfile);

        String ocsID = "" + CGParameters.getTaskID();
        String ocsCmd = CGParameters.getXYZINTTool() + " "
                        + ocsIntfile + " "
                        + " T"; //set use of template 
        if (verbosity > 1)
        {
            System.err.println("CMD: " + ocsCmd + " TskID: " + ocsID);
        }
        ProcessHandler ocsPh = new ProcessHandler(ocsCmd, ocsID);
        try
        {
            ocsPh.runProcess();
            if (ocsPh.getExitCode() != 0)
            {
                String msg = "XYZINT (post conf.search) failed for " + molName;
                throw new DENOPTIMException(msg + "\n"
                                            + ocsPh.getErrorOutput());
            }
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }

        // Update local molecular representation with output from PSSROT
        String newTnkIC = workDir + fsep + molName + "_cs" + idm + ".int_2";
        TinkerMolecule tmpTmol = TinkerUtils.readTinkerIC(newTnkIC);
        ArrayList<TinkerAtom> lstAtoms = tmpTmol.getAtoms();
        for (int i=0; i<lstAtoms.size(); i++)
        {
            TinkerAtom ta = lstAtoms.get(i);
            tmol.getAtom(i+1).setDistAngle(ta.getDistAngle());
        }
        csMol3d.updateXYZFromINT();

        // Cleanup
        if (verbosity < 2)
        {
            DenoptimIO.deleteFilesContaining(workDir,molName + "_cs" + idm);
        }

//Useful code only for debug
/*
TinkerUtils.writeIC("tmol_afterCS.int", csMol3d.getTinkerMolecule());
TinkerUtils.writeIC("tmol_afterCS_fM.int", mol.getTinkerMolecule());
DenoptimIO.writeMolecule("fmol_afterCS.sdf", csMol3d.getIAtomContainer(),true);
DenoptimIO.writeMolecule("fmol_afterCS_fM.sdf", mol.getIAtomContainer(),true);
System.out.println("STOP at end of conf search");
GenUtils.pause();
*/
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
