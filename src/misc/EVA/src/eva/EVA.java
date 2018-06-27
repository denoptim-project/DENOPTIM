package eva;

import exception.DENOPTIMException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.MOPACReader;
import io.DenoptimIO;

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
public class EVA
{
    private static final Logger LOGGER = Logger.getLogger(EVA.class.getName());
    final static double SQRTINVPI = 1.0/Math.sqrt(Math.PI * 2);
    final static double INVPI = 1.0/Math.PI;
    
    Options opts = new Options();

    private String inputFile = "";
    private String outputFile = "";
    private double sigma = -1.;
    private double L = -1.;
    private double minVal = 1;
    private double maxVal = 4000;
    private boolean ISMOE = false;
    private boolean ISCHG = false;
    private boolean lorentizan = false;
    private double scalingFactor = 1.;
    
//------------------------------------------------------------------------------

    public String getInputFile()
    {
        return inputFile;
    }

//------------------------------------------------------------------------------

    public String getOutputFile()
    {
        return outputFile;
    }

//------------------------------------------------------------------------------

    public double getSigma()
    {
        return sigma;
    }

//------------------------------------------------------------------------------

    public double getIncrement()
    {
        return L;
    }

//------------------------------------------------------------------------------

    public double getMinValue()
    {
        return minVal;
    }
    
//------------------------------------------------------------------------------

    public double getScaleFactor()
    {
        return scalingFactor;
    }    

//------------------------------------------------------------------------------

    public double getMaxValue()
    {
        return maxVal;
    }

//------------------------------------------------------------------------------    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        EVA geva = new EVA();

        geva.setUp();
        geva.parseCommandLine(args);
        geva.checkOptions();
        
        try
        {
            MOPACReader mopread = new MOPACReader();
            ArrayList<Double> vals = null;
            
            mopread.readMOPACData(geva.getInputFile());
            if (geva.ISMOE)
            {
                vals = mopread.getOrbitalEnergies();
            }
            else if (geva.ISCHG)
            {
                vals = mopread.getAtomCharges();
            }
            else
                vals = mopread.getVibrationalFrequencies();
            
            
            if (vals == null)
            {
                LOGGER.log(Level.SEVERE, "No data found.");
                System.exit(-1);
            }
            
            ArrayList<Double> desc = new ArrayList<>();
            //System.err.println("Orbital Energies: " + vals.toString());
            
            // split BFS
            for (double x=geva.minVal; x<=geva.maxVal; x+=geva.L)
            {
                double f = geva.getValue(geva.sigma, vals, x);
                desc.add(f);
            }
            
            StringBuilder sb = new StringBuilder(512);
            
            for (int i=0; i<desc.size(); i++)
            {
                String str = String.format("%f ", desc.get(i).doubleValue());
                sb.append(str);
            }
            
            DenoptimIO.writeData(geva.getOutputFile(), sb.toString(), false);
            mopread.cleanup();
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

    private double getValue(double sigma, ArrayList<Double> freq, double xval)
    {
        double sum = 0;

        if (!lorentizan)
        {
            double s1 = 1.0/(2.0*sigma*sigma);
            double s0 = 1.0/sigma;

            for (int i=0; i<freq.size(); i++)
            {
                double f = freq.get(i);
                if (f > maxVal || f < minVal)
                    continue;
                double s2 = (xval - f)*(xval - f);

                sum += SQRTINVPI * s0 * Math.exp(-s2*s1);
            }
        }
        else
        {
            for (int i=0; i<freq.size(); i++)
            {
                // f if the location parameter specifying the location of the peak 
                // of the distribution
                double f = freq.get(i);
                if (f > maxVal || f < minVal)
                    continue;
                double s2 = (xval - f)*(xval - f);
                double s1 = sigma * 0.5;

                // sigma larger the scale/wdith parameter, the more spread out 
                // the distribution
                sum += INVPI * s1 * (1.0/(s2 + s1*s1));
            }
        }

        return sum;
    }
    
//------------------------------------------------------------------------------    
    
    private void parseCommandLine(String[] args)
    {
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(opts, args);
            inputFile = cmdLine.getOptionValue("i").trim();
            outputFile = cmdLine.getOptionValue("o").trim();

            sigma = Double.parseDouble(cmdLine.getOptionValue("s").trim());
            L = Double.parseDouble(cmdLine.getOptionValue("r").trim());
            if (cmdLine.hasOption("l"))
                lorentizan = true;
            
            if (cmdLine.hasOption("sf"))
            {
                scalingFactor = Double.parseDouble(cmdLine.getOptionValue("sf").trim());
                ISMOE = false;
            }
                    
            
            if (cmdLine.hasOption("e"))
            {
                ISMOE = true;
                minVal = -55;
                maxVal = 10.;
            }
            
            if (cmdLine.hasOption("c"))
            {
                ISCHG = true;
                minVal = -1;
                maxVal = 1.;
            }
            
            if (cmdLine.hasOption("min"))
            {
                minVal = Double.parseDouble(cmdLine.getOptionValue("min").trim());
            }
            
            if (cmdLine.hasOption("max"))
            {
                maxVal = Double.parseDouble(cmdLine.getOptionValue("max").trim());
            }
        }
        catch(ParseException | NumberFormatException poe)
        {
            LOGGER.log(Level.SEVERE, null, poe);
            printUsage();
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
                .withDescription("MOPAC output file.\n").isRequired().withLongOpt("input").
                    create('i'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Output file.").
                isRequired().
                withLongOpt("output").
                create('o'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Gaussian Kernel standard deviation.").
                isRequired().
                withLongOpt("sigma").
                create('s'));

            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Sampling resolution.").
                isRequired().
                withLongOpt("res").
                create('r'));
                
            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Minimum bounded range value.").
                withLongOpt("min").
                create());

            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Maximum bounded range value.").
                withLongOpt("max").
                create());
            
            opts.addOption(
                OptionBuilder.hasArg(true)
                .withDescription("Vibrtional frequency scaling factor").
                withLongOpt("sf").
                create());
                
            opts.addOption(
                OptionBuilder.hasArg(false)
                .withDescription("Data input contains orbital energies. Set the bounded range values accordingly.")
                .create('e'));
            
            opts.addOption(
                OptionBuilder.hasArg(false)
                .withDescription("Data input contains charges. Set the bounded range values accordingly.")
                .withLongOpt("chg")
                .create('c'));
                
            opts.addOption(
                OptionBuilder.hasArg(false)
                .withDescription("Use Lorentizan instead of Gaussian.")
                .create('l'));
            
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }

//------------------------------------------------------------------------------

    private void checkOptions()
    {
        String error = "";
        // check files
        if (inputFile.length() == 0)
        {
            error = "No input file specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }

        if (outputFile.length() == 0)
        {
            error = "No output file specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        if (sigma <= 0)
        {
            error = "Sigma must be +ve.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        if (L <= 0)
        {
            error = "Resolution must be +ve.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        if (ISMOE == true && ISCHG == true)
        {
            error = "Cannot use both charge and orbital energies.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
    }

//------------------------------------------------------------------------------

    // Gamma(x) = integral( t^(x-1) e^(-t), t = 0 .. infinity)    
    // Uses Lanczos approximation formula. See Numerical Recipes 6.1.

    private double logGamma(double x) 
    {
        double val = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173/(x + 0) - 86.50532033/(x + 1)
                       + 24.01409822/(x + 2) - 1.231739516/(x + 3)
                       + 0.00120858003/(x + 4) -  0.00000536382/(x + 5);
        return (val + Math.log(ser * Math.sqrt(2 * Math.PI)));
   }
   
//------------------------------------------------------------------------------   
   
    private double gamma(double x) 
    { 
        return Math.exp(logGamma(x)); 
    }

//------------------------------------------------------------------------------       

    private void printUsage()
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar EVA.jar <options>", opts);
        System.exit(-1);
    }
    
//------------------------------------------------------------------------------    
    
}