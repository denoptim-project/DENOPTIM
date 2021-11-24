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

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.utils.GenUtils;
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
        // TODO code application logic here
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
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(
                    CGParameters.getInputSDFFile());
            if (mol.getProperty(DENOPTIMConstants.GRAPHTAG) != null)
            {
            
                String graphStr = mol.getProperty(
                        DENOPTIMConstants.GRAPHTAG).toString();
                System.err.println("Imported graph: " + graphStr);
        		GraphConversionTool gct = new GraphConversionTool();
        		DENOPTIMGraph grph = gct.getGraphFromString(graphStr);

                String mname = mol.getProperty("cdk:Title").toString();
                
                
                DENOPTIM3DMoleculeBuilder mbuild = 
                        new DENOPTIM3DMoleculeBuilder(mname, grph, 
                                        CGParameters.getWorkingDirectory());
// MF: commented out for multi-conf procedure which is going to be
// implemented at some time in the future. 
// Note that the method buildMulti3DStructure can be used also for 
// generating a single conformation.
/*                
                IAtomContainer nmol = mbuild.build3DStructure();
                nmol.setProperties(mol.getProperties());
                // write file
                DenoptimIO.writeMolecule(CGParameters.getOutputSDFFile(), nmol, false);
*/
//MF: writes more than one structure if needed
                ArrayList<IAtomContainer> nmols = mbuild.buildMulti3DStructure();
                for (int i = 0; i<nmols.size(); i++)
                {
                    String propVIDs = nmols.get(i).getProperty(
                            DENOPTIMConstants.ATMPROPVERTEXID).toString();
                    nmols.get(i).setProperties(mol.getProperties());
                    nmols.get(i).setProperty(
                            DENOPTIMConstants.ATMPROPVERTEXID, propVIDs);
                }
                // write file
                DenoptimIO.writeMoleculeSet(CGParameters.getOutputSDFFile(), nmols);
            }
        }
        catch (DENOPTIMException de)
        {
            de.printStackTrace(System.err);
            System.exit(-1);
        }
        
        System.exit(0);
    }
}
