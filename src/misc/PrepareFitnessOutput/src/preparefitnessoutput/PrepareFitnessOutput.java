package preparefitnessoutput;

import exception.DENOPTIMException;
import io.DenoptimIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openscience.cdk.interfaces.IAtomContainer;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class PrepareFitnessOutput
{

    private static final Logger LOGGER = Logger.getLogger(PrepareFitnessOutput.class.getName());

    Options opts = new Options();

    String inpSDFFile = "";
    String property = "";
    String value = "";
    String propertyFile = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        PrepareFitnessOutput pfd = new PrepareFitnessOutput();
        pfd.setUp();

        try
        {
            pfd.parseCommandLine(args);
            pfd.checkOptions();

            IAtomContainer mol = DenoptimIO.readSingleSDFFile(pfd.inpSDFFile);
            
            if (pfd.property.length() > 0)
            {
                mol.setProperty(pfd.property, pfd.value);
            }
            
            if (pfd.propertyFile.length() > 0)
            {
                // read property file
                HashMap<String, String> properties = new HashMap<>();
                pfd.readProperties(pfd.propertyFile, properties);
                Iterator<Map.Entry<String, String>> iterator = properties.entrySet().iterator();
                while (iterator.hasNext())
                {
                    Map.Entry<String, String> entry = iterator.next();
                    mol.setProperty(entry.getKey(), entry.getValue());
                }
            }
            
            DenoptimIO.writeMolecule(pfd.inpSDFFile, mol, false);
        }
        catch (DENOPTIMException ioe)
        {
            LOGGER.log(Level.SEVERE, null, ioe);
            System.exit(-1);
        }

        System.exit(0);
    }

//------------------------------------------------------------------------------

    private void readProperties(String filename, 
                    HashMap<String, String> properties) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;
        String key, val;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }
                if (line.startsWith("#"))
                {
                    continue;
                }
                val = line.substring(line.indexOf("=") + 1).trim();
                key = line.substring(0, line.indexOf("="));
                properties.put(key, val);
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
        
        if (properties.isEmpty())
        {
            System.err.println("No data in " + filename);
            System.exit(-1);
        }
    }

//------------------------------------------------------------------------------    
    
    /**
     * Option setup for command line
     */

    private void setUp()
    {
        try
        {
            opts.addOption(
                OptionBuilder.hasArg(false)
                    .withDescription("usage information")
                    .withLongOpt("help")
                    .create('h'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Input SDF file containing coordinates.").
                    isRequired().
                    withLongOpt("input").
                    create('i'));
            
            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("File containing property values.").
                    withLongOpt("pfile").
                    create('f'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Name of the tag to be added.").
                    withLongOpt("property").
                    create('p'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Value to be associated with property.").
                    withLongOpt("value").
                    create('v'));
        }
        catch (IllegalArgumentException e)
        {
            LOGGER.log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }
    
//------------------------------------------------------------------------------

    private void printUsage()
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar SDFAddProperty.jar <options>", opts);
        System.exit(-1);
    }

//------------------------------------------------------------------------------

    private void parseCommandLine(String[] args) throws DENOPTIMException
    {
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(opts, args);
            inpSDFFile = cmdLine.getOptionValue("i").trim();
            if (cmdLine.hasOption("property"))
                property = cmdLine.getOptionValue("property").trim();
            if (cmdLine.hasOption("p"))
                property = cmdLine.getOptionValue("p").trim();
            if (cmdLine.hasOption("pfile"))
                propertyFile = cmdLine.getOptionValue("pfile").trim();
            if (cmdLine.hasOption("f"))
                propertyFile = cmdLine.getOptionValue("f").trim();
            if (cmdLine.hasOption("value"))
                value = cmdLine.getOptionValue("value").trim();
            if (cmdLine.hasOption("v"))
                value = cmdLine.getOptionValue("v").trim();
        }
        catch(ParseException | NumberFormatException poe)
        {
            LOGGER.log(Level.SEVERE, null, poe);
            printUsage();
        }
    }

//------------------------------------------------------------------------------

    private void checkOptions()
    {
        String error = "";
        // check files
        if (inpSDFFile.length() == 0)
        {
            error = "No input SDF file specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        int ctr = 0;
        if (property.length() == 0)
        {
            ctr++;
        }
        
        if (propertyFile.length() == 0)
        {
            ctr++;
        }

        if (ctr == 0)
        {
            error = "Either a property or file containing properties must be specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        if (property.length() > 0)
        {
            if (value.length() == 0)
            {
                error = "No value supplied.";
                LOGGER.log(Level.SEVERE, error);
                System.exit(-1);
            }
        }

    }

//------------------------------------------------------------------------------
}
