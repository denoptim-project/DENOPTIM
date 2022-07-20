package denoptim.fragmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.FormulaUtils;

public class FragmenterTools
{
    
//------------------------------------------------------------------------------
    
    /**
     * Processes all molecules analyzing the composition of the structure in
     * the chemical representation as compared to the molecular formula 
     * declared in the
     * {@link DENOPTIMConstants#FORMULASTR} property, and extracts those 
     * molecules where the declared formula matches the composition of
     * the chemical representation.
     * @param input the source of chemical structures.
     * @param output the file where to write extracted structures.
     * @param logger a task-dedicated logger where we print messages for the 
     * user.
     * @throws DENOPTIMException
     * @throws IOException
     */
    public static void checkElementalAnalysisAgainstFormula(File input,
            File output, Logger logger) 
                    throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int index = -1;
        int maxBufferSize = 500;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Checking elemental analysis of "
                            + "structure " + index);
                }
                IAtomContainer mol = reader.next();
                if (mol.getProperty(DENOPTIMConstants.FORMULASTR)==null)
                {
                    throw new Error("Property '" + DENOPTIMConstants.FORMULASTR 
                            + "' not found in molecule " + index + " in file "
                            + input + ". Cannot compare formula with elemental"
                            + "analysis.");
                }
                String formula = mol.getProperty(DENOPTIMConstants.FORMULASTR)
                        .toString();
                
                if (FormulaUtils.compareFormulaAndElementalAnalysis(formula, 
                        mol, logger))
                {
                    buffer.add(mol);
                } else {
                    if (logger!=null)
                    {
                        logger.log(Level.INFO,"Inconsistency between elemental "
                                + "analysis of structure and molecular formula."
                                + " Rejecting structure " + index + ": " 
                                + mol.getTitle());
                    }
                }
                
                // If max buffer size is reached, then bump to file
                if (buffer.size() >= maxBufferSize)
                {
                    DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, 
                            true);
                    buffer.clear();
                }
            }
        }
        finally {
            reader.close();
        }
        if (buffer.size() < maxBufferSize)
        {
            DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, true);
            buffer.clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    //TODO-gg del
    public static void templateMethod(File input,
            FragmenterParameters settings, File output) throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int index = -1;
        int bufferSize = 0;
        int maxBufferSize = 500;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                bufferSize++;
                IAtomContainer mol = reader.next();
                if (mol.getProperty(DENOPTIMConstants.FORMULASTR)==null)
                {
                    throw new Error("Property '" + DENOPTIMConstants.FORMULASTR 
                            + "' not found in molecule " + index + " in file "
                            + input + ". Cannot compare formula with elemental"
                            + "analysis.");
                }
                String formula = mol.getProperty(DENOPTIMConstants.FORMULASTR)
                        .toString();
                
                
                
                
                
                
                // If max buffer size is reached, then bump to file
                if (bufferSize >= maxBufferSize)
                {
                    bufferSize = 0;
                    DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, true);
                    buffer.clear();
                }
            }
        }
        finally {
            reader.close();
        }
        if (bufferSize < maxBufferSize)
        {
            DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, true);
            buffer.clear();
        }
    }

}
