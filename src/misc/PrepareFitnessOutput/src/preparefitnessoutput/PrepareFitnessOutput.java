package preparefitnessoutput;

import exception.DENOPTIMException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.DenoptimIO;

import org.openscience.cdk.interfaces.IAtomContainer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class PrepareFitnessOutput
{

    private static final Logger LOGGER = Logger.getLogger(PrepareFitnessOutput.class.getName());

    Options opts = new Options();

    String inpSDFFile = "";
    String fitFile = "";
    String descFile = "";
    double ADCutoff = -1;
    int violationsCutoff = 0;
    int ADViol = -1;
    int category = -1;
    double uncertaintyCutoff = -1;
    double molSim = 0;

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


            ArrayList<Double> fvals = new ArrayList<>();
            pfd.readFitness(pfd.fitFile, fvals);

            if (pfd.ADViol != 6)
                mol.setProperty("FITNESS", fvals.get(0));
            else
            {
                mol.setProperty("MODEL_FITNESS", fvals.get(0));
                mol.setProperty("FITNESS", pfd.molSim);
            }

            if (fvals.size() > 1)
                mol.setProperty("LEVERAGE", String.format("%-10.4f", fvals.get(1)));

            if (fvals.size() > 2)
            {
                String s = String.format("%-10.4f", fvals.get(2));
                mol.setProperty("UNCERTAINTY", s);
            }

            if (fvals.size() > 3)
            {
                StringBuilder sb = new StringBuilder();
                for (int i=3; i<fvals.size(); i++)
                    sb.append(fvals.get(i).intValue()).append(" ");
                mol.setProperty("AD_VIOLATIONS", sb.toString());
            }


            if (pfd.descFile.length() > 0)
            {
                ArrayList<Double> descriptors = new ArrayList<>();
                pfd.readDescriptors(pfd.descFile, descriptors);

                StringBuilder sb = new StringBuilder();
                for (int j=0; j<descriptors.size(); j++)
                {
                    sb.append(String.format("%6.3f", descriptors.get(j))).append(" ");
                }
                mol.setProperty("Descriptors", sb.toString().trim());
            }

            if (pfd.ADViol == 1)
            {
                if (pfd.ADCutoff > 0)
                {
                    if (fvals.get(1) > pfd.ADCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
            }

            if (pfd.ADViol == 2)
            {
                if (pfd.violationsCutoff > 0)
                {
                    if (fvals.get(3) > pfd.violationsCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
            }

            if (pfd.ADViol == 3)
            {
                if (pfd.violationsCutoff > 0 && pfd.ADCutoff > 0)
                {
                    if (fvals.get(1) > pfd.ADCutoff || fvals.get(3) > pfd.violationsCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
            }

            if (pfd.ADViol == 4)
            {
                if (fvals.size() > 3)
                {
                    int[] ct = new int[7];
                    for (int i=3; i<fvals.size(); i++)
                        ct[i-3] = fvals.get(i).intValue();

                    if (ct[pfd.category] == 1)
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                }
            }

            if (pfd.ADViol == 5)
            {
                if (pfd.uncertaintyCutoff > 0)
                {
                    if (fvals.get(2) > pfd.uncertaintyCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
            }

            if (pfd.ADViol == 6)
            {
                if (pfd.violationsCutoff > 0)
                {
                    if (fvals.get(3) > pfd.violationsCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
                if (pfd.uncertaintyCutoff > 0)
                {
                    if (fvals.get(2) > pfd.uncertaintyCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
                }
                if (pfd.ADCutoff > 0)
                {
                    if (fvals.get(1) > pfd.ADCutoff)
                    {
                        mol.setProperty("MOL_ERROR", "#AD_ISSUE: Estimated fitness is outside AD.");
                    }
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

    private void readFitness(String filename, ArrayList<Double> fvals) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }
                String[] vals = line.split("\\s+");
                for (String val : vals)
                {
                    fvals.add(Double.parseDouble(val));
                }
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
    }

//------------------------------------------------------------------------------

    private void readDescriptors(String filename, ArrayList<Double> desc) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;

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
                String[] vals = line.split("\\s+");
                for (String val : vals)
                {
                    desc.add(new Double(val));
                }
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
    }

//------------------------------------------------------------------------------

    private double readSimilarity(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;
        double val = 0;

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
                val = Double.parseDouble(line.trim());
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
        return val;
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
                    .withDescription("File containing fitness value, "
                    + "applicability domain estimate (if applicable)").
                    isRequired().
                    withLongOpt("fitness").
                    create('f'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Applicability domain leverage based cutoff.").
                    withLongOpt("cutoff").
                    create('c'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Applicability domain category violations cutoff.").
                    withLongOpt("violation").
                    create('v'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("File containing the descriptors.").
                    withLongOpt("desc").
                    create('d'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Uncertainty cutoff.").
                    withLongOpt("uncertainty").
                    create('u'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Use category #.").
                    withLongOpt("category").
                    create('g'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Use category #.").
                    withLongOpt("similarity").
                    create('s'));
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
        hf.printHelp("java -jar PrepareFitnessOutput.jar <options>", opts);
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
            fitFile = cmdLine.getOptionValue("f").trim();
            descFile = cmdLine.getOptionValue("d").trim();

            if (cmdLine.hasOption("c"))
            {
                ADCutoff = Double.parseDouble(cmdLine.getOptionValue("c").trim());
                ADViol = 1;
            }

            if (cmdLine.hasOption("v"))
            {
                violationsCutoff = Integer.parseInt(cmdLine.getOptionValue("v").trim());
                ADViol = 2;
            }

            if (cmdLine.hasOption("c") && cmdLine.hasOption("v"))
            {
                ADCutoff = Double.parseDouble(cmdLine.getOptionValue("c").trim());
                violationsCutoff = Integer.parseInt(cmdLine.getOptionValue("v").trim());
                ADViol = 3;
            }

            if (cmdLine.hasOption("g"))
            {
                category = Integer.parseInt(cmdLine.getOptionValue("g").trim());
                ADViol = 4;
            }

            if (cmdLine.hasOption("u"))
            {
                uncertaintyCutoff = Double.parseDouble(cmdLine.getOptionValue("u").trim());
                ADViol = 5;
            }

            if (cmdLine.hasOption("s"))
            {
                molSim = readSimilarity(cmdLine.getOptionValue("s").trim());
                ADViol = 6;
            }
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

        if (fitFile.length() == 0)
        {
            error = "File containing fitness values not specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }

    }

//------------------------------------------------------------------------------
}
