/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.integration.tinker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.molecularmodeling.MMBuilderUtils;
import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;
import denoptim.utils.ConnectedLigand;
import denoptim.utils.ConnectedLigandComparator;
import denoptim.utils.GeneralUtils;
import denoptim.utils.MathUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RotationalSpaceUtils;


/**
 * Toolbox of utilities for Tinker style molecular representation.
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class TinkerUtils 
{
    private static final String NL = System.getProperty("line.separator");
    private static boolean debug = false;
    
    
//------------------------------------------------------------------------------    
    /**
     * Reads a Tinker INT file.
     * Example of Tinker INT file (i.e., internal coordinates)
     * <br>
     * 8 Ethane <br>
     * 1 C 1  <br>
     * 2 C 1 1 1.60000 <br>
     * 3 H 5 1 1.10000 2 109.4700 <br>
     * 4 H 5 1 1.10000 2 109.4700 3 109.4700  1 <br>
     * 5 H 5 1 1.10000 2 109.4700 3 109.4700 -1 <br>
     * 6 H 5 2 1.10000 1 109.4700 3  10.0000  0 <br>
     * 7 H 5 2 1.10000 1 109.4700 6 109.4700  1 <br>
     * 8 H 5 2 1.10000 1 109.4700 6 109.4700 -1 <br>
     * <br>
     * The first line, says that there
     * are eight atoms in the system, and that the name of the molecule is
     * Ethane Second line set the origin of the system, i.e., from which atom
     * start to build the molecule: first atom is atom 1, which is a carbon
     * atom, C, and its atom type for the force field is 1 Third line says
     * that the second atom in the molecule is a carbon atom, C, having 
     * atom type 1, bonded to atom 1 placed at a distance of 1.6 Angstroms from
     * the atom 1 Fourth line says that the third atom in the structure is an
     * hydrogen atoms, H, having atom type 5, bonded to atom 1, placed at
     * a distance of 1.1 Angstroms from atom 1 and forming an angle of 109.47
     * degrees with atom 2 Fifth line says that the fourth atom in the structure
     * is an hydrogen atoms, H, having atom type 5, bonded to atom 1,
     * placed at a distance of 1.1 Angstroms from atom 1, and forming an angle
     * of 109.47 degrees with atom 2 Also, the dihedral angles between the
     * planes defined by atoms 1 2 3 and 2 3 4 is 109.47 degrees The last column
     * indicates the chirality flag The structure of this line is then repeated
     * for all the remain atoms. Atom 1 has no internal coordinates at all. The
     * coordinates of atom 1 are, by definition, Cartesian. Normally, the
     * coordinates of atom 1 are (0,0,0), but can be set to any value desired.
     * Atom 2 must be connected to atom 1 by an interatomic distance only. If
     * atom 1 is not at the origin, then the care must be taken in defining atom
     * 2: if internal coordinates are used, then the connectivity must be given.
     * If the connectivity is not specified, then the coordinate of atom 2 is,
     * by definition, Cartesian. Atom 3 can be connected to atom 1 or 2, and
     * must make an angle with atom 2 or 1 (thus 3-2-1 or 3-1-2); no dihedral is
     * possible for atom 3. For any one atom (i) this consists of an interatomic
     * distance in Angstroms from an already-defined atom (j), an interatomic
     * angle in degrees between atoms i and j and an already defined k, (k and j
     * must be different atoms), and finally a torsional angle in degrees
     * between atoms i, j, k, and an already defined atom l (l cannot be the
     * same as k or j).
     *
     * @param filename
     */

    public static ZMatrix readTinkerINT(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line = null;
        int natom = 0;

        ZMatrix zmat = new ZMatrix();

        try
        {
            br = new BufferedReader(new FileReader(filename));
            line = br.readLine().trim();

            // read the number of atoms and store
            String[] arr = line.split(" +");
            natom = Integer.parseInt(arr[0]);
            if (natom < 1)
            {
                String msg = "Invalid number of atoms in " + filename;
                throw new DENOPTIMException(msg);
            }

            if (arr.length >= 2)
            {
                arr = line.split(" +", 2);
                zmat.setId(arr[1]);
            }

            for (int i = 0; i < natom; i++)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }

                arr = line.trim().split(" +");
                if (arr == null || arr.length < 3)
                {
                    String msg = "Check atom " + (i + 1) + " in " + filename;
                    throw new DENOPTIMException(msg);
                }

                int id = Integer.parseInt(arr[0])-1;
                String symbol = arr[1];
                String type = arr[2];
                ZMatrixAtom bndRef = null;
                ZMatrixAtom angRef = null;
                ZMatrixAtom ang2Ref = null;
                Double bondLength = null;
                Double angleValue = null;
                Double angle2Value = null;
                Integer chiralFlag = null;

                // Bond partner and bond value
                if (arr.length >= 5)
                {
                    bndRef = zmat.getAtom(Integer.parseInt(arr[3])-1);
                    bondLength = Double.parseDouble(arr[4]);
                }
                // Angle partner and angle value
                if (arr.length >= 7)
                {
                    angRef = zmat.getAtom(Integer.parseInt(arr[5])-1);
                    angleValue = Double.parseDouble(arr[6]);
                }
                // Torsion partner and dihedral value
                if (arr.length >= 10)
                {
                    ang2Ref = zmat.getAtom(Integer.parseInt(arr[7])-1);
                    angle2Value = Double.parseDouble(arr[8]);
                    chiralFlag = Integer.parseInt(arr[9]);
                }
                ZMatrixAtom zatm = new ZMatrixAtom(id, symbol, type,
                        bndRef, angRef, ang2Ref, 
                        bondLength, angleValue, angle2Value, 
                        chiralFlag);
                zmat.addAtom(zatm);
                if (bndRef!=null)
                {
                    zmat.addBond(zatm, bndRef);
                }
            }

            if (br.ready())
            {
                line = br.readLine();
                // Check for a first blank line
                if (line.trim().equalsIgnoreCase(""))
                {
                    // Parse bond pairs to add until EOF or a blank line is
                    // reached
                    boolean blank = false;
                    while (br.ready() && !blank)
                    {
                        line = br.readLine();
                        if (line.trim().equalsIgnoreCase(""))
                        {
                            blank = true;
                        }
                        else
                        {
                            arr = line.trim().split(" +");
                            if (arr.length != 2)
                            {
                                String msg = "Check Bond Pair to Remove: '" 
                                    + line + "' in " + filename;
                                throw new DENOPTIMException(msg);
                            }
                            zmat.addBond(Integer.parseInt(arr[0])-1, 
                                    Integer.parseInt(arr[1])-1);
                        }
                    }
                    // Parse bond pairs to be removed until EOF
                    while (br.ready())
                    {
                        line = br.readLine();
                        arr = line.trim().split(" +");
                        if (arr.length != 2)
                        {
                            String msg = "Check Bond Pair to Remove: '" 
                                    + line + "' in " + filename;
                            throw new DENOPTIMException(msg);
                        }
                        zmat.delBond(Integer.parseInt(arr[0])-1, 
                                Integer.parseInt(arr[1])-1);
                    }
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
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

        return zmat;
    }

//------------------------------------------------------------------------------
    
    /**
     * Read the tinker XYZ coordinate representation
     *
     * @param filename
     * @return the 3d coordinates
     * @throws DENOPTIMException
     */
    
    public static List<double[]> readTinkerXYZ(String filename)
                                                        throws DENOPTIMException
    {
        List<double[]> coords = new ArrayList<>();

        BufferedReader br = null;
        String line = null;

        int natom = 0;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            line = br.readLine().trim();

            // read the number of atoms and store
            String[] arr = line.split("\\s+");
            natom = Integer.parseInt(arr[0]);
            if (natom < 1)
            {
               String msg = "Invalid number of atoms in " + filename;
               throw new DENOPTIMException(msg);
            }

            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                arr = line.trim().split("\\s+");
                if (arr.length >= 5)
                {
                    // Atom number, name, type
                    //String name = arr[1];
                    double[] f = new double[3];
                    f[0] = Double.parseDouble(arr[2]);
                    f[1] = Double.parseDouble(arr[3]);
                    f[2] = Double.parseDouble(arr[4]);

                    coords.add(f);
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
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

        if (coords.size() != natom)
        {
            throw new DENOPTIMException("Incorrect number of atoms perceived "
                    + "in " + filename);
        }

        return coords;

    }

//------------------------------------------------------------------------------   
    
    /**
     * Write Tinker INT file.
     *
     * @param filename
     * @param zmat
     * @throws DENOPTIMException
     */

    public static void writeTinkerINT(String filename, ZMatrix zmat)
            throws DENOPTIMException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename));
            int numatoms = zmat.getAtoms().size();
            // write out the number of atoms and the title
            String header = String.format("%6d  %s%n", numatoms, zmat.getId());
            fw.write(header);
            fw.flush();

            String line = "";

            for (int i = 0; i < numatoms; i++)
            {
                ZMatrixAtom atom = zmat.getAtoms().get(i);
                if (i == 0)
                {
                    line = String.format("%6d  %-3s%6s%n",
                            atom.getId()+1,  // 1-based indexing
                            atom.getSymbol(),
                            atom.getType());
                    fw.write(line);
                    fw.flush();
                }
                else
                {
                    if (i == 1)
                    {
                        line = String.format("%6d  %-3s%6s%6d%10.5f%n",
                                atom.getId()+1, atom.getSymbol(),
                                atom.getType(), 
                                zmat.getBondRefAtomIndex(i)+1, // 1-based indexing
                                zmat.getBondLength(i));
                        fw.write(line);
                        fw.flush();
                    }
                    else
                    {
                        if (i == 2)
                        {
                            line = String.format("%6d  %-3s%6s%6d%10.5f%6d%10.4f%n",
                                    atom.getId()+1, atom.getSymbol(),
                                    atom.getType(), 
                                    zmat.getBondRefAtomIndex(i)+1,
                                    zmat.getBondLength(i),
                                    zmat.getAngleRefAtomIndex(i)+1,
                                    zmat.getAngleValue(i));
                            fw.write(line);
                            fw.flush();
                        } // output the fourth through final atoms
                        else
                        {
                            line = String.format("%6d  %-3s%6s%6d%10.5f%6d%10.4f%6d%10.4f%6d%n",
                                    atom.getId()+1, atom.getSymbol(),
                                    atom.getType(), 
                                    zmat.getBondRefAtomIndex(i)+1,
                                    zmat.getBondLength(i),
                                    zmat.getAngleRefAtomIndex(i)+1,
                                    zmat.getAngleValue(i),
                                    zmat.getAngle2RefAtomIndex(i)+1,
                                    zmat.getAngle2Value(i),
                                    zmat.getChiralFlag(i));
                            fw.write(line);
                            fw.flush();
                        }
                    }
                }
            }

            // addition and deletion of bonds as required
            List<int[]> zadd = zmat.getBondsToAdd();
            List<int[]> zdel = zmat.getBondsToDel();

            if (zadd.size() > 0 || zdel.size() > 0)
            {
                fw.write(NL);
                fw.flush();

                for (int i = 0; i < zadd.size(); i++)
                {
                    int[] z = zadd.get(i);
                    line = String.format("%6d%6d%n", z[0]+1, z[1]+1);
                    fw.write(line);
                    fw.flush();
                }

                if (zdel.size() > 0)
                {
                    fw.write(NL);
                    fw.flush();

                    for (int i = 0; i < zdel.size(); i++)
                    {
                        int[] z = zdel.get(i);
                        line = String.format("%6d%6d%n", z[0]+1, z[1]+1);
                        fw.write(line);
                        fw.flush();
                    }
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
                if (fw != null)
                {
                    fw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Read the PSSROT output file
     *
     * @param filename
     * @return the list of energies
     * @throws DENOPTIMException
     */
    public static List<Double> readPSSROTOutput(String filename)
            throws DENOPTIMException
    {
        List<Double> energies = new ArrayList<>();

        BufferedReader br = null;
        String line;
        try
        {
            br = new BufferedReader(new FileReader(filename));

            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.contains("Final Function Value and Deformation"))
                {
                    String str = line.substring(38);
                    // read the number of atoms and store
                    String[] arr = str.split("\\s+");
                    energies.add(Double.parseDouble(arr[1]));
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
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

        if (energies.isEmpty())
        {
            String msg = "No data found in file: " + filename;
            throw new DENOPTIMException(msg);
        }
        return energies;
    }

//------------------------------------------------------------------------------

    /**
     * Read the parameter settings to be used by PSSROT
     *
     * @param filename
     * @return list of data
     * @throws DENOPTIMException
     */
    public static void readPSSROTParams(String filename, 
            List<String> initPars, List<String> restPars)
                    throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;
        int fnd = -1;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                    continue;
                if (line.contains("INIT"))
                {
                    fnd = 0;
                    continue;
                }
                if (line.contains("REST"))
                {
                    fnd = 1;
                    continue;
                }
                if (fnd == 0)
                    initPars.add(line.trim());
                if (fnd == 1)
                    restPars.add(line.trim());
            }
        }
        catch (IOException nfe)
        {
            String msg = "File '" + filename + "' not found.";
            throw new DENOPTIMException(msg, nfe);
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

        if (initPars.isEmpty())
        {
            String msg = "No data found in file: " + filename;
            throw new DENOPTIMException(msg);
        }
        if (restPars.isEmpty())
        {
            String msg = "No data found in file: " + filename;
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Read the Tinker atom mapping from Tinker Force Field.
     *
     * @param filename
     * @return map of atom symbol and Tinker type
     * @throws DENOPTIMException
     */

    public static HashMap<String, Integer> readTinkerAtomTypes(String filename)
            throws DENOPTIMException
    {
        HashMap<String, Integer> atomTypesMap = new HashMap<>();

        BufferedReader br = null;
        String line;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                //Read only lines starting with keyword "atom"
                line = line.trim();
                if (!line.startsWith("atom"))
                {
                    continue;
                }

                //Format:
                //key, class, symbol, "label", Z, atomic weight, connectivity
                try
                {
                    //extract atom type (or 'class' according to Tinker's 
                    // nomenclature) and atom symbol
                    String[] dq = line.split("\"");
                    String fp = dq[0];
                    String[] str1 = fp.split("\\s+");

                    //Check the format by reading all parts of atom type def.
                    int atomType = Integer.parseInt(str1[1]);
                    String symbol = str1[2];
                    /* Not needed so far...
                    String sp = dq[2];
                    String[] str2 = sp.split("\\s+");
                    String label = dq[1];
                    int z = Integer.parseInt(str2[1]);
                    double atmWeight = Double.parseDouble(str2[2]);
                    int cn = Integer.parseInt(str2[3]);
                    */

                    //Store
                    atomTypesMap.put(symbol,atomType);                    
                }
                catch (Throwable t)
                {
                    String msg = "Format of Tinker's atom type definition not "
                            + "recognized. " + NL + "Details: " + NL 
                            + t.getMessage();
                    throw new DENOPTIMException(msg);
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
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

        if (atomTypesMap.isEmpty())
        {
            String msg = "No data found in file: " + filename;
            throw new DENOPTIMException(msg);
        }

        return atomTypesMap;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check for the existence of an output file for a Tinker job and, if the 
     * output file is not found, this method throws an exception that contains 
     * the error message from Tinker job log.
     * @param outputPathName pathname of the Tinker output file that we expect
     * to exist.
     * @param logPathName pathname to the log of the Tinker job that was
     * supposed to generate the output.
     * @param taskName a string identifying the task that Tinker was supposed to
     * perform.
     * @throws TinkerException if the output file is not found. This exception
     * will contain the error from Tinker's log.
     */
    
    public static void ensureOutputExistsOrRelayError(String outputPathName, 
            String logPathName, String taskName) throws TinkerException
    {
        File output = new File(outputPathName);
        if (output.exists() && output.canRead())
        {
            return;
        }
        
        String errMsg = "TINKER ERROR: ";
        ReversedLinesFileReader fr = null;
        try
        {
            fr = new ReversedLinesFileReader(new File(
                    logPathName), null);

            int numEmpty = 0;
            for (int i=0; i<100; i++) //at most 100 lines are read
            {
                String line = fr.readLine();
                if (line==null)
                    break;
                
                if (line.trim().isEmpty())
                    numEmpty++;
                
                if (numEmpty==2)
                    break;
                errMsg = errMsg + NL + line;
            }
            fr.close();
        } catch (IOException e)
        {
            throw new TinkerException("Missing Tinker log '" + logPathName + "'",
                    taskName);
        }
        
        throw new TinkerException(errMsg, taskName);
    }

//------------------------------------------------------------------------------
    
    /**
     * Identifies how many iteration Tinker has done by looking into the log
     * file, searching for a given pattern. The resulting number of iterations
     * is translated in the pathname of the last iteration file. The iteration
     * files have names like  
     * <code>filename.000</code>, <code>filename.001</code>, 
     * <code>filename.002</code>. So, if Tinker took 56 iteration, this method
     * will return <code>filename.055</code>.
     * @param workDir the directory expected to contain the iteration files.
     * @param fname the basename of the iteration file.
     * @param tinkerLog the log file where we count the iterations by searching 
     * the patters.
     * @param pattern the pattern to search in the log file
     * @return the pathname of the last cycle-file.
     * @throws DENOPTIMException
     */
    public static String getNameLastCycleFile(String workDir, String fname, 
            String tinkerLog, String pattern) throws DENOPTIMException
    {
        int lastI = MMBuilderUtils.countLinesWKeywordInFile(tinkerLog, pattern);
        String xyzfile = workDir + System.getProperty("file.separator") + fname 
                + "." + GeneralUtils.getPaddedString(3, lastI - 1);
        return xyzfile;
    }

    
//------------------------------------------------------------------------------

}
