package denoptimcg;

import java.util.ArrayList;

import exception.DENOPTIMException;
import io.DenoptimIO;
import molecule.DENOPTIMGraph;
import org.openscience.cdk.interfaces.IAtomContainer;
import utils.GenUtils;
import utils.GraphConversionTool;

/**
 *
 * @author VISHWESH
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
            IAtomContainer mol = 
                    DenoptimIO.readSingleSDFFile(CGParameters.getInputSDFFile());
            if (mol.getProperty("GraphENC") != null)
            {
            
                String graphStr = mol.getProperty("GraphENC").toString();
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
                for (int inmol = 0; inmol<nmols.size(); inmol++)
		{
                    nmols.get(inmol).setProperties(mol.getProperties());
		}
                // write file
                DenoptimIO.writeMoleculeSet(CGParameters.getOutputSDFFile(), nmols);

            }
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }
        
        System.exit(0);
    }
}
