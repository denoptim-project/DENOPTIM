package denoptim.fragmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.smiles.FixBondOrdersTool;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.FormulaUtils;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;

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
        int maxBufferSize = 2000;
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
    
    /**
     * Removes from the structures anyone that matches any of the given SMARTS 
     * queries.
     * @param input the source of chemical structures.
     * @param smarts the queries leading to rejection.
     * @param output the file where to write extracted structures.
     * @param logger a task-dedicated logger where we print messages for the 
     * user.
     * @throws DENOPTIMException
     * @throws IOException
     */
    public static void filterStrucutresBySMARTS(File input, Set<String> smarts,
            File output, Logger logger) 
                    throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int i = -1;
        Map<String, String> smartsMap = new HashMap<String, String>();
        for (String s : smarts)
        {
            i++;
            smartsMap.put("prefilter-"+i, s);
        }
        
        int index = -1;
        int maxBufferSize = 2000;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Prefiltering structure " + index);
                }
                IAtomContainer mol = reader.next();
                
                ManySMARTSQuery msq = new ManySMARTSQuery(mol, smartsMap);
                if (msq.hasProblems())
                {
                    String msg = "WARNING! Problems while searching for "
                            + "specific atoms/bonds using SMARTS: " 
                            + msq.getMessage();
                    throw new DENOPTIMException(msg,msq.getProblem());
                }
                Map<String, Mappings> allMatches = msq.getAllMatches();
                
                if (allMatches.size()==0)
                {
                    buffer.add(mol);
                } else {
                    String hits = "";
                    for (String s : allMatches.keySet())
                        hits = hits + DenoptimIO.NL + smartsMap.get(s);
                    if (logger!=null)
                    {
                        logger.log(Level.INFO,"Found match for " + hits
                                + "Rejecting structure " + index + ": " 
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
 //TODO-GG del   
    public static void codeTemplare(File input,
            File output, Logger logger) 
                    throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int index = -1;
        int maxBufferSize = 2000;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Checking _______ of "
                            + "structure " + index);
                }
                IAtomContainer mol = reader.next();
                
                //TODO
                
                
                /*
                if (FormulaUtils.____()
                        mol, logger))
                {
                    buffer.add(mol);
                } else {
                    if (logger!=null)
                    {
                        logger.log(Level.INFO,"______."
                                + " Rejecting structure " + index + ": " 
                                + mol.getTitle());
                    }
                }
                */
                
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

}
