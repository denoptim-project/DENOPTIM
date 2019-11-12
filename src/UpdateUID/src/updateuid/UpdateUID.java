/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package updateuid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class UpdateUID
{

    
    private static final java.util.logging.Logger LOGGER = 
                java.util.logging.Logger.getLogger(UpdateUID.class.getName());
    
    Options opts = new Options();
    
    private String sdfFile = "";
    private String molKeyFile = "";
    private String uniqueKeyFile = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        UpdateUID objUpdate = new UpdateUID();

        objUpdate.setUp();
        objUpdate.parseCommandLine(args);
        objUpdate.checkOptions();
        
        RandomAccessFile rafile = null; // The file we'll lock
        FileChannel channel = null; // The channel to the file
        FileLock lock = null; // The lock object we hold
        File lockfile = null;
        
        try
        {
            
            IAtomContainer mol = objUpdate.readSingleSDFFile(objUpdate.sdfFile);
            String keyToVerify = objUpdate.readKeyToVerify(objUpdate.molKeyFile);
            
            
            //System.err.println("To verfiy: " + keyToVerify);
            
            lockfile = new File(objUpdate.uniqueKeyFile);
            // Creates a random access file stream to read from, and optionally to write to
            // Create a FileChannel that can read and write that file.
            // Note that we rely on the java.io package to open the file,
            // in read/write mode, and then just get a channel from it.
            // This will create the file if it doesn't exit. We'll arrange
            // for it to be deleted below, if we succeed in locking it.
            rafile = new RandomAccessFile(lockfile, "rw");
            
            channel = rafile.getChannel();
            
            // Try to get an exclusive lock on the file.
            // This method will return a lock or null, but will not block.
            // See also FileChannel.lock() for a blocking variant.
            
            while (true)
            {
                // Attempts to acquire an exclusive lock on this channel's file (returns null or throws
                // an exception if the file is already locked.
                try 
                {
                    lock = channel.tryLock();
                    if (lock != null)
                        break;
                }
                catch (OverlappingFileLockException e) 
                {
                    // thrown when an attempt is made to acquire a lock on a a file that overlaps
                    // a region already locked by the same JVM or when another thread is already
                    // waiting to lock an overlapping region of the same file
                    System.err.println("Overlapping File Lock Error: " + e.getMessage());
                }
            }
            
            
            boolean found = false;
            for ( String line; (line = rafile.readLine()) != null; )
            {
                if (line.trim().length() == 0)
                    continue;
                if (line.trim().equalsIgnoreCase(keyToVerify))
                {
                    found = true;
                    break;
                }
            }
            
            
            // do all the checks and update the keyfile as required
            // move the cursor to the end of the file
            if (!found)
            {
                rafile.seek(channel.position());
		        String msg = "Writing UID: " + keyToVerify 
			     + " from " + objUpdate.sdfFile;
                LOGGER.log(Level.INFO, msg); 
	
                rafile.writeBytes(keyToVerify + "\n");
                channel.force(true);
            }
            else
            {
                mol.setProperty("MOL_ERROR", "#Duplicate: UID Already exists.");
            }

            mol.setProperty("UID", keyToVerify);
            objUpdate.writeMolecule(objUpdate.sdfFile, mol, false);
        }
        catch (Exception de)
        {
            LOGGER.log(Level.SEVERE, null, de); 
            System.exit(-1);
        }
        finally
        {
            try 
            {
                // close the channel
                if (channel != null)
                    channel.close();
                if (rafile!= null)
                    rafile.close();
                // release the lock
                if (lock != null && lock.isValid())
                    lock.release();
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
        
        System.exit(0);
    }
    
//------------------------------------------------------------------------------
    
    private String readKeyToVerify(String infile) throws Exception
    {
        BufferedReader br = null;
        String line = null, key = null;
        try
        {
            br = new BufferedReader(new FileReader(infile));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }
                key = line.trim();
            }
        }
        catch (IOException nfe)
        {
            throw nfe;
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
                throw ioe;
            }
        }
        
        return key;
    }
    

//------------------------------------------------------------------------------    
    
    private void parseCommandLine(String[] args)
    {
        try
        {
            CommandLineParser parser = new PosixParser();    
            CommandLine cmdLine = parser.parse(opts, args);
            
            if (cmdLine.hasOption("s"))
                sdfFile = cmdLine.getOptionValue("s").trim();
            
            if (cmdLine.hasOption("m"))
                molKeyFile = cmdLine.getOptionValue("m").trim();
            
            if (cmdLine.hasOption("k"))
                uniqueKeyFile = cmdLine.getOptionValue("k").trim();
        }
        catch(ParseException poe)
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
        if (sdfFile.length() == 0)
        {
            error = "No SDF file specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }

        if (molKeyFile.length() == 0)
        {
            error = "No file containing key to verify specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }
        
        if (uniqueKeyFile.length() == 0)
        {
            error = "No file containing the list of uniques keys specified.";
            LOGGER.log(Level.SEVERE, error);
            System.exit(-1);
        }        
    }
    
//------------------------------------------------------------------------------       

    private void printUsage()
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar UpdateFile.jar <options>", opts);
        System.exit(-1);
    }    
    
//------------------------------------------------------------------------------     
    
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
                    .withDescription("File containing unique keys.")
                    .isRequired().withLongOpt("keydb")
                    .create('k'));
            
            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("Key to verify against database.")
                    .isRequired().withLongOpt("molid")
                    .create('m'));
            
            opts.addOption(
                OptionBuilder.hasArg(true)
                    .withDescription("The SDF format of the molecule.")
                    .isRequired().withLongOpt("sdf")
                    .create('s'));
        }
        catch (IllegalArgumentException e)
        {
            LOGGER.log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }

//------------------------------------------------------------------------------   
    
    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param filename the file containing the molecules
     * @return IAtomContainer[] an array of molecules
     * @throws Exception
     */
    private IAtomContainer readSingleSDFFile(String filename) throws Exception
    {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();

        try
        {
            mdlreader = new MDLV2000Reader(new FileReader(new File(filename)));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) new ChemFile());
            lstContainers.addAll(
                    ChemFileManipulator.getAllAtomContainers(chemFile));
        }
        catch (CDKException | IOException cdke)
        {
            LOGGER.log(Level.SEVERE, null, cdke);
	        throw cdke;
        }
        finally
        {
            try
            {
                if (mdlreader != null)
                {
                    mdlreader.close();
                }
            }
            catch (IOException ioe)
            {
                LOGGER.log(Level.SEVERE, null, ioe);
  		        throw ioe;
            }
        }

        if (lstContainers.isEmpty())
        {
            LOGGER.log(Level.SEVERE, "No data in file: {0}", filename);
            throw new Exception("No data in file: " + filename);
        }

        return lstContainers.get(0);
    }
    
//------------------------------------------------------------------------------    

    /**
     * Writes a single molecule to the specified file
     *
     * @param filename The file to be written to
     * @param mol The molecule to be written
     * @throws Exception
     */
    private void writeMolecule(String filename, IAtomContainer mol,
            boolean append) throws Exception
    {
        SDFWriter sdfWriter = null;
        try
        {
            sdfWriter = new SDFWriter(new FileWriter(new File(filename), append));
            sdfWriter.write(mol);
        }
        catch (CDKException | IOException cdke)
        {
            throw cdke;
        }
        finally
        {
            try
            {
                if (sdfWriter != null)
                {
                    sdfWriter.close();
                }
            }
            catch (IOException ioe)
            {
                throw ioe;
            }
        }
    }
    
//------------------------------------------------------------------------------
    
}
