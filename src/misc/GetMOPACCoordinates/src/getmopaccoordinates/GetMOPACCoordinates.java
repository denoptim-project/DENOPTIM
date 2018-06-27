package getmopaccoordinates;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtomContainer;

import exception.DENOPTIMException;
import io.DenoptimIO;
import io.MOPACReader;
import utils.DENOPTIMMoleculeUtils;
import utils.GenUtils;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class GetMOPACCoordinates
{
    private static final Logger LOGGER = Logger.getLogger(GetMOPACCoordinates.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 3)
        {
            System.err.println("Usage: java -jar GetMOPACCoordinates.jar inpSDFFile MOPAC.OUT outSDFFile/outMOL2File/outXYZFile");
            System.err.println("The coordinates are written to the output based on the extension.");
            System.exit(-1);
        }
        
        String sdfFile = args[0];
        String mopFile = args[1];
        String outFile = args[2];
        
        MOPACReader mopread = new MOPACReader();
        
        try
        {
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(sdfFile);
            mopread.readMOPACData(mopFile);
            
            int status = mopread.getStatus();
            if (status == -1)
            {
                mol.setProperty("MOL_ERROR", "MOPAC Optimization failed.");                
            }
            else
            {
                ArrayList<Point3d> atom_coords = mopread.getAtomCoordinates();
                //System.err.println(atom_coords.size() + " " + mol.getAtomCount());
                if (hasValidCoordinates(atom_coords))
                {
                    for (int i = 0; i<atom_coords.size(); i++)
                    {
                        Point3d coords = atom_coords.get(i);
                        mol.getAtom(i).setPoint3d(coords);
                    }
                }

                ArrayList<Double> atom_charges = mopread.getAtomCharges();
                if (atom_charges != null)
                {
                    for (int i = 0; i<atom_charges.size(); i++)
                    {
                        mol.getAtom(i).setCharge(atom_charges.get(i));
                    }
                }

                String smi = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
                if (smi != null && smi.length() > 0)
                {
                    if (smi.contains("."))
                    {
                        mol.setProperty("MOL_ERROR", "Broken structure.");
                    }
                }
            }
            
            mopread.cleanup();
            
            //System.err.println("EXTENSION: " + GenUtils.getFileExtension(outFile));
            
            // write the file
            if (GenUtils.getFileExtension(outFile).equalsIgnoreCase(".xyz"))
            {
                DenoptimIO.writeXYZFile(outFile, mol, false);
            }
            if (GenUtils.getFileExtension(outFile).equalsIgnoreCase(".sdf"))
            {
                DenoptimIO.writeMolecule(outFile, mol, false);
            }
            if (GenUtils.getFileExtension(outFile).equalsIgnoreCase(".mol2"))
            {
                DenoptimIO.writeMol2File(outFile, mol, false);
            }
        }
        catch (DENOPTIMException de)
        {
            LOGGER.log(Level.SEVERE, null, de); 
            System.exit(-1);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, null, ex); 
            System.exit(-1);
        }
        
        System.exit(0);
        
    }
    
//------------------------------------------------------------------------------
    
    private static boolean hasValidCoordinates(ArrayList<Point3d> atom_coords) throws DENOPTIMException
    {
        if (atom_coords == null)
        {
            throw new DENOPTIMException("No coordinates found.");
        }
        if (atom_coords.isEmpty())
        {
            throw new DENOPTIMException("No coordinates found.");
        }
        return true;
    }
    
//------------------------------------------------------------------------------    
}
