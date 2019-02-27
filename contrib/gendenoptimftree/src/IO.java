/*******************************************************************************
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 ******************************************************************************/


package gendenoptimftree;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoObject;
import com.epam.indigo.IndigoRenderer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class IO 
{

//------------------------------------------------------------------------------

    /**
     *
     * @param filename
     * @param dataToWrite
     * @throws java.lang.Exception
     */

    public static void writeFile(String filename, String dataToWrite) throws Exception
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename), false);
            fw.write(dataToWrite);
            fw.write(System.getProperty("line.separator"));
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if(fw != null)
                    fw.close();
            }
            catch (IOException ioe)
            {
                throw ioe;
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param filename the file containing the molecules
     * @return an array of molecules
     * @throws Exception
     */
    
    private static ArrayList<IAtomContainer> readSDFFile(String filename) throws Exception
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
                throw ioe;
            }
        }

        if (lstContainers.isEmpty())
        {
            throw new Exception("No data in file: " + filename);
        }

        return lstContainers;
    }       
    
//------------------------------------------------------------------------------

    /**
     *
     * @param smiles
     * @param type
     * @param imgfile
     */

    public static void writeImage(String smiles, String type, String imgfile)
    {
        Indigo indigo = new Indigo();
        IndigoRenderer renderer = new IndigoRenderer(indigo);

        IndigoObject mol = indigo.loadMolecule(smiles);
        mol.dearomatize();
        mol.foldHydrogens();
        indigo.setOption("render-output-format", type);
        indigo.setOption("render-margins", 5, 5);
        mol.layout(); // if not called, will be done automatically by the renderer
        indigo.setOption("render-relative-thickness", 1.5);
        indigo.setOption("render-coloring", true);
        indigo.setOption("render-bond-line-width", 1.5);
        indigo.setOption("render-label-mode", "terminal-hetero");
        indigo.setOption("render-implicit-hydrogens-visible", false);
        indigo.setOption("render-stereo-style", "none");
        indigo.setOption("render-image-width", 300);
        indigo.setOption("render-image-height", 300);
        indigo.setOption("render-background-color", "1, 1, 1");
        renderer.renderToFile(mol, imgfile);
    }    
    
//------------------------------------------------------------------------------

    /**
     * 
     * @param mfile
     * @return
     * @throws Exception 
     */
    
    public static ArrayList<IAtomContainer> readFragments(String mfile) throws Exception
    {
        ArrayList<IAtomContainer> mols = null;
        
        if (mfile.endsWith(".sdf"))
        {
            mols = IO.readSDFFile(mfile);
        }
        else if (mfile.endsWith(".smi"))
        {
            mols = IO.readSMILESFile(mfile);
        }
        
        return mols;
    }
    
    
    
//------------------------------------------------------------------------------
    
    /**
     * Read molecules from a smiles format file
     * @param filename
     * @return list of molecules
     * @throws Exception 
     */

    private static ArrayList<IAtomContainer> readSMILESFile(String filename) throws Exception
    {
        BufferedReader br = null;
        String sCurrentLine;

        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();
        SmilesParser SMPARSER = new SmilesParser(DefaultChemObjectBuilder.getInstance());

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null)
            {
                if (sCurrentLine.trim().length() == 0)
                    continue;
                String[] str = sCurrentLine.split("\\s+");
                String smiles = str[0];
                IAtomContainer mol = SMPARSER.parseSmiles(smiles);
                if (str.length > 1)
                    mol.setProperty("cdk:Title", str[1]);
                lstContainers.add(mol);
            }
        }
        catch (CDKException | IOException cdke)
        {
            throw cdke;
        }
        finally
        {
            try
            {
                if (br != null)
                    br.close();
            }
            catch (IOException ioe)
            {
                throw ioe;
            }
        }
		
        if (lstContainers.isEmpty())
        {
            throw new Exception("No data in file: " + filename);	    
        }
		
        return lstContainers;
    }    
    
    
//------------------------------------------------------------------------------

    /**
     * 
     * @param src
     * @param dest
     * @throws Exception 
     */
    
    public static void copyFile(String src, String dest) throws Exception
    {
        File sourceFile = new File(src);
        File destFile = new File(dest);
        
        if (!destFile.exists()) 
        {
            destFile.createNewFile();
        }
        
        FileChannel srcChannel = null;
        FileChannel destChannel = null;

        try 
        {
            srcChannel = new FileInputStream(sourceFile).getChannel();
            destChannel = new FileOutputStream(destFile).getChannel();
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally 
        {
            if (srcChannel != null) 
            {
                srcChannel.close();
            }
            if (destChannel != null) 
            {
                destChannel.close();
            }
        }
    }
    
    
//------------------------------------------------------------------------------

    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param filename the file containing the molecules
     * @return IAtomContainer[] an array of molecules
     * @throws java.lang.Exception
     */

    public static IAtomContainer readSingleSDFFile(String filename) throws Exception
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
                throw ioe;
            }
        }

        if (lstContainers.isEmpty())
        {
            throw new Exception("No data in file: " + filename);
        }

        return lstContainers.get(0);
    }    
    
//------------------------------------------------------------------------------
    
    public static ArrayList<String> readList(String filename) throws Exception
    {
        BufferedReader br = null;
        String sCurrentLine;

        ArrayList<String> lst = new ArrayList<>();

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null)
            {
                if (sCurrentLine.trim().length() == 0)
                    continue;
                lst.add(sCurrentLine);
            }
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if (br != null)
                    br.close();
            }
            catch (IOException ioe)
            {
                throw ioe;
            }
        }
		
        if (lst.isEmpty())
        {
            throw new Exception("No data in file: " + filename);	    
        }
		
        return lst;
    }
    
    
//------------------------------------------------------------------------------    


    
}
