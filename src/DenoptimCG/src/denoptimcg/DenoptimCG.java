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

package denoptimcg;

import java.io.File;
import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMGraph;
import denoptim.integration.tinker.TinkerException;
import denoptim.io.DenoptimIO;
import denoptim.utils.GraphConversionTool;

/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DenoptimCG 
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java DenoptimCG paramFile");
            System.exit(-1);
        }
        
        String paramFile = args[0];
        
        try
        {
            CGParameters.readParameterFile(paramFile);
	        CGParameters.checkParameters();
            CGParameters.processParameters();
            CGParameters.printParameters();
            
            // read the input molecule
            DENOPTIMGraph grph = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(CGParameters.getInputSDFFile()), true).get(0);
            
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(
                    CGParameters.getInputSDFFile());
            
            String mname = "noname";
            Object propName = mol.getProperty("cdk:Title");
            if (propName != null)
            {
                mname = propName.toString();
            } else {
                Object propTitle = mol.getTitle();
                if (propTitle != null)
                {
                    mname = propTitle.toString();
                }
            }
            
            DENOPTIM3DMoleculeBuilder mbuild = 
                    new DENOPTIM3DMoleculeBuilder(mname, grph);

            ArrayList<IAtomContainer> nmols = mbuild.buildMulti3DStructure();
            for (int i = 0; i<nmols.size(); i++)
            {
                //NB: here we reset the IAC properties, so, any property that
                // should be passed on to the future should be copied.
                String propVIDs = nmols.get(i).getProperty(
                        DENOPTIMConstants.ATMPROPVERTEXID).toString();
                Object propMolErr = nmols.get(i).getProperty(
                        DENOPTIMConstants.MOLERRORTAG);
                nmols.get(i).setProperties(mol.getProperties());
                nmols.get(i).setProperty(
                        DENOPTIMConstants.ATMPROPVERTEXID, propVIDs);
                if (propMolErr != null)
                {
                    nmols.get(i).setProperty(DENOPTIMConstants.MOLERRORTAG, 
                            propMolErr.toString());
                }
            }
            // write file
            DenoptimIO.writeSDFFile(CGParameters.getOutputSDFFile(), nmols);
            
        } catch (TinkerException te)
        {
            String msg = "ERROR! Tinker failed on task '" + te.taskName 
                    + "'!";
            if (te.solution != "")
            {
                msg = msg + System.getProperty("line.separator") + te.solution;
            }
            System.out.println(msg);
            System.exit(-1);
        } 
        catch (Exception de)
        {
            de.printStackTrace(System.err);
            System.exit(-1);
        } 
        
        System.out.println("DenoptimCG terminated normally!");
        System.exit(0);
    }
}
