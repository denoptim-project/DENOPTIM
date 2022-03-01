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

package setupbrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.Mol2Reader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import denoptim.constants.DENOPTIMConstants;


public class SetupBRICS
{

//------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(SetupBRICS.class.getName());

//------------------------------------------------------------------------------

    public static void main(String[] args)
    {

        if (args.length < 2)
        {
            System.err.println("Usage: java -cp cdk.jar SetupBRICS infile(sdf/mol2) outsdffile");
            System.exit(-1);
        }
        
        SetupBRICS sbrics = new SetupBRICS();
        
        try
        {
            String infile = args[0];
            if (infile.endsWith(".sdf"))
            {
                ArrayList<IAtomContainer> mols = sbrics.readSDFFile(infile);
                ArrayList<ArrayList<Integer>> rdata = sbrics.readRData(infile);
                //System.err.println(rdata.size());
                boolean append = false;
                for (int i=0; i<mols.size(); i++)
                {
                    System.err.println("#" + (i+1));
                    IAtomContainer mol = mols.get(i);
					if (rdata.get(i).size() > 0)
					{
						sbrics.setupTags(mol, rdata.get(i));
						sbrics.writeMolecule(args[1], mol, append);
						append = true;
					}
                }
            }
            if (infile.endsWith(".mol2"))
            {
                ArrayList<IAtomContainer> mols = sbrics.readMOL2File(args[0]);
                boolean append = false;
                for (int i=0; i<mols.size(); i++)
                {
                    System.err.println("#" + (i+1));
                    IAtomContainer mol = mols.get(i);
                    sbrics.setupTags(mol);                
                    sbrics.writeMolecule(args[1], mol, append);
                    append = true;
                }                    
            }
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        System.exit(0);
    }    
    
//------------------------------------------------------------------------------
    
    private void setupTags(IAtomContainer mol)    
    {
        ArrayList<IAtom> atomsToRem = new ArrayList<>();
        ArrayList<IAtom> classAtoms = new ArrayList<>();
        ArrayList<String> atomIds = new ArrayList<>();
        
        for (int i = 0; i < mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            if (atm.getSymbol().equals("R"))
            {
                atomsToRem.add(atm);
                atomIds.add(atm.getID());
                List<IAtom> lst = mol.getConnectedAtomsList(atm);
                if (lst.size() > 1)
                    System.err.println("SIZE: " + lst.size());
                classAtoms.add(lst.get(0));
            }
        }
        
        for (int i=0; i<atomsToRem.size(); i++)
        {
            mol.removeAtomAndConnectedElectronContainers(atomsToRem.get(i));        
        }

        StringBuilder sb1 = new StringBuilder(512);
        StringBuilder sb2 = new StringBuilder(512);
        for (int i=0; i<classAtoms.size(); i++)
        {
            int pos = mol.getAtomNumber(classAtoms.get(i));
            sb1.append("" + (pos+1) + "#" + atomIds.get(i) + ":1 ");
            if (atomIds.get(i).equals("R7"))
                sb2.append("" + (pos+1) + ":" + "2" + " ");
            else
                sb2.append("" + (pos+1) + ":" + "1" + " ");
        }
       
        mol.setProperty(DENOPTIMConstants.APSTAG, sb1.toString().trim());
    }    
    
//------------------------------------------------------------------------------

    private void setupTags(IAtomContainer mol, ArrayList<Integer> rtypes)    
    {
        ArrayList<IAtom> atomsToRem = new ArrayList<>();
        ArrayList<IAtom> classAtoms = new ArrayList<>();
        
        for (int i = 0; i < mol.getAtomCount(); i++)
        {
            IAtom atm = mol.getAtom(i);
            if (atm.getSymbol().equals("R"))
            {
                atomsToRem.add(atm);
                List<IAtom> lst = mol.getConnectedAtomsList(atm);
                if (lst.size() > 1)
                    System.err.println("SIZE: " + lst.size());
                classAtoms.add(lst.get(0));
            }
        }
        
        for (int i=0; i<atomsToRem.size(); i++)
        {
            mol.removeAtomAndConnectedElectronContainers(atomsToRem.get(i));        
        }
        
        StringBuilder sb1 = new StringBuilder(512);
        StringBuilder sb2 = new StringBuilder(512);
        for (int i=0; i<classAtoms.size(); i++)
        {
            int pos = mol.getAtomNumber(classAtoms.get(i));
            sb1.append("" + (pos+1) + "#R" + rtypes.get(i) + ":1 ");
            if (rtypes.get(i).intValue() == 7)
                sb2.append("" + (pos+1) + ":" + "2" + " ");
            else
                sb2.append("" + (pos+1) + ":" + "1" + " ");
        }
       
        mol.setProperty(DENOPTIMConstants.APSTAG, sb1.toString().trim());
    }
    
//------------------------------------------------------------------------------

    private ArrayList<IAtomContainer> readMOL2File(String filename) throws Exception
    {
        Mol2Reader reader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();
        
        try
        {
            reader = new Mol2Reader(new FileReader(new File(filename)));
            ChemFile chemFile = (ChemFile)reader.read((ChemObject)new ChemFile());
            lstContainers.addAll(ChemFileManipulator.getAllAtomContainers(chemFile));
        }
        catch (CDKException | IOException cdke)
        {
            throw new Exception(cdke);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException ioe)
            {
                throw new Exception(ioe);
            }
        }
        
        if (lstContainers.isEmpty())
        {
            throw new Exception("No data found in " + filename);
        }
        
        System.err.println(lstContainers.size());

        return lstContainers;

    }  

//------------------------------------------------------------------------------

    private void writeMolecule(String filename, IAtomContainer mol, 
                                    boolean append) throws Exception
    {
        SDFWriter sdfWriter = null;
        try
        {
            sdfWriter = new SDFWriter(new FileWriter(new File(filename), append));
            sdfWriter.write(mol);
	    }
	    catch (CDKException cdke)
        {
            String error = "Error: " + cdke.getMessage();
			System.err.println(error);
			System.exit(-1);            
        }
        catch (IOException ioe)
		{
			String error = "Error: " + ioe.getMessage();
			System.err.println(error);
			System.exit(-1);
		}
        catch (Exception e)
		{
            String error = "Error: " + e.getMessage();
			System.err.println(error);
			System.exit(-1);
		}		
		finally
		{
		   try 
			{
				if(sdfWriter != null)
					sdfWriter.close();		
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
     * @return IAtomContainer[] an array of molecules
     * @throws Exception
     */
    private ArrayList<IAtomContainer> readSDFFile(String filename) throws Exception
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
            throw new Exception(cdke);
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
                throw new Exception(ioe);
            }
        }

        if (lstContainers.isEmpty())
        {
            throw new Exception("No data found in " + filename);
        }

        return lstContainers;
    }
    
//------------------------------------------------------------------------------

    private ArrayList<ArrayList<Integer>> readRData(String filename) throws Exception
    {
        BufferedReader br = null;
        String line = null;
        int nc = 0;
        int natom = 0;

        String[] vals = null;
        
        ArrayList<ArrayList<Integer>> data = new ArrayList<>();
        ArrayList<Integer> rtypes = new ArrayList<>();
        boolean lookForISO = false;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.startsWith("$$$$")) 
                {
                    //System.err.println("HERE");
                    nc = 0;
                    natom = 0;
                    data.add(rtypes);
                    //System.err.println("Added: " + data.size() + " " + rtypes.size());
                    rtypes = new ArrayList<>();
                    continue;
                }
                nc++;
                //System.err.println(line + " " + nc);
                if ((line.trim()).length() == 0)
                    continue;
                if (nc < 4)    
                    continue;
                if (nc == 4)    
                {
                    natom = Integer.parseInt(line.substring(0, 3).trim());
                    continue;
                }
                
                if (line.contains("M  RAD"))
                    continue;
                
                
                if (line.contains("R"))
                {
                    String element = line.substring(31, Math.min(line.length(), 34)).trim();

                    if (element.equals("R") || 
                               (element.length() > 0 && element.charAt(0) == 'R'))
                    {
                        String massDiffString = line.substring(34,36).trim();
                        //System.err.println("HERE " + element +  " " + massDiffString);
                        if (!lookForISO)
                        {
						    if (Integer.parseInt(massDiffString) > 0)
							    rtypes.add(Integer.parseInt(massDiffString));
                            else
                                lookForISO = true;
                        }
                    }
                }
                
                if (lookForISO)
                {
                    if (line.startsWith("M  ISO"))
                    {
                        String countString = line.substring(6,10).trim();
                        int infoCount = Integer.parseInt(countString);
                        StringTokenizer st = new StringTokenizer(line.substring(10));
                        for (int i=1; i <= infoCount; i++) 
                        {
                            int atomNumber = Integer.parseInt(st.nextToken().trim());
                            int absMass = Integer.parseInt(st.nextToken().trim());
                            rtypes.add(absMass);
                        }
                    }
                }
            }
        }            
        catch (NumberFormatException nfe)
        {
            throw nfe;
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if(br != null)
                    br.close();
            }
            catch (IOException ioe)
            {
                throw ioe;
            }
        }
        
        if (data.isEmpty())
        {
            throw new Exception("No R data in file.");
        }
        
        //System.err.println(data.toString());
        
        return data;
    }

//------------------------------------------------------------------------------    
    
    public void pause()
    {
        System.err.println("Press a key to continue");
        try
        {
            int inchar = System.in.read();
        }
        catch (IOException e)
        {
            System.err.println("Error reading from user");
        }
    }    
    
//------------------------------------------------------------------------------        


}
