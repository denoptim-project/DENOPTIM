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

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.utils.ConnectedLigand;
import denoptim.utils.ConnectedLigandComparator;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.ObjectPair;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * Toolbox of utilities for Tinker style molecular representation.
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class TinkerUtils 
{
    private static String lsep = System.getProperty("line.separator");
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

    public static TinkerMolecule readTinkerIC(String filename)
                                                      throws DENOPTIMException
    {
        BufferedReader br = null;
        String line = null;
        int natom = 0;

        TinkerMolecule tinkermol = new TinkerMolecule();

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
                // set the name
                //System.err.println("Name: " + arr[1]);
                tinkermol.setName(arr[1]);
            }


            ArrayList<int[]> zadd = new ArrayList<>();
            ArrayList<int[]> zdel = new ArrayList<>();

            ArrayList<TinkerAtom> lstAtom = new ArrayList<>();

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

                // Atom number, name, type
                String name = arr[1];
                int type = Integer.parseInt(arr[2]);
                double zv[] = new double[3];
                int zi[] = new int[4];
                double d[] =
                {
                    0.0d, 0.0d, 0.0d
                };

                // Bond partner and bond value
                if (arr.length >= 5)
                {
                    zi[0] = Integer.parseInt(arr[3]);
                    zv[0] = Double.parseDouble(arr[4]);
                }
                else
                {
                    zi[0] = 0;
                    zv[0] = 0.0d;
                }
                // Angle partner and angle value
                if (arr.length >= 7)
                {
                    zi[1] = Integer.parseInt(arr[5]);
                    zv[1] = Double.parseDouble(arr[6]);
                }
                else
                {
                    zi[1] = 0;
                    zv[1] = 0.0d;
                }
                // Torsion partner and dihedral value
                if (arr.length >= 10)
                {
                    zi[2] = Integer.parseInt(arr[7]);
                    zv[2] = Double.parseDouble(arr[8]);
                    zi[3] = Integer.parseInt(arr[9]);
                }
                else
                {
                    zi[2] = 0;
                    zv[2] = 0.0d;
                    zi[3] = 0;
                }
                TinkerAtom tkatm = new TinkerAtom(i + 1,name,type,d,zi,zv);
                lstAtom.add(tkatm);
            } // end for

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
                                String msg = "Check Bond Pair to Remove: " 
                                    + (zadd.size() + 1) + " in " + filename;
                                throw new DENOPTIMException(msg);
                            }
                            int pair[] = new int[2];
                            pair[0] = Integer.parseInt(arr[0]);
                            pair[1] = Integer.parseInt(arr[1]);
                            zadd.add(pair);
                        }
                    }
                    // Parse bond pairs to be removed until EOF
                    while (br.ready())
                    {
                        line = br.readLine();
                        arr = line.trim().split(" +");
                        if (arr.length != 2)
                        {
                            String msg = "Check Bond Pair to Remove: " 
                                    + (zadd.size() + 1) + " in " + filename;
                            throw new DENOPTIMException(msg);
                        }
                        int pair[] = new int[2];
                        pair[0] = Integer.parseInt(arr[0]);
                        pair[1] = Integer.parseInt(arr[1]);
                        zdel.add(pair);
                    }
                }
            }

            tinkermol.setAtoms(lstAtom);
            tinkermol.setBondPairs(zdel, zadd);
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

        return tinkermol;
    }

//------------------------------------------------------------------------------
    
    /**
     * Read the tinker XYZ coordinate representation
     *
     * @param filename
     * @return the 3d coordinates
     * @throws DENOPTIMException
     */
    
    public static ArrayList<double[]> readTinkerXYZ(String filename)
                                                        throws DENOPTIMException
    {
        ArrayList<double[]> coords = new ArrayList<>();

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
            throw new DENOPTIMException("Incorrect number of atoms perceived in " + filename);
        }

        return coords;

    }

//------------------------------------------------------------------------------    
    /**
     * Write Tinker INT file.
     *
     * @param filename
     * @param tmol
     * @throws DENOPTIMException
     */

    public static void writeIC(String filename, TinkerMolecule tmol)
                                                        throws DENOPTIMException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename));
            int numatoms = tmol.getAtoms().size();
            // write out the number of atoms and the title
            String header = String.format("%6d  %s%n", numatoms, tmol.getName());
            fw.write(header);
            fw.flush();

            String line = "";

            for (int i = 0; i < numatoms; i++)
            {
                TinkerAtom atom = tmol.getAtoms().get(i);
                int[] d1 = atom.getAtomNeighbours();
                double[] d2 = atom.getDistAngle();
                // output of first three atoms is handled separately
                if (i == 0)
                {
                    line = String.format("%6d  %-3s%6d%n",
                            atom.getXYZIndex(), atom.getAtomString(),
                            atom.getAtomType());
                    fw.write(line);
                    fw.flush();
                }
                else
                {
                    if (i == 1)
                    {
                        line = String.format("%6d  %-3s%6d%6d%10.5f%n",
                                atom.getXYZIndex(), atom.getAtomString(),
                                atom.getAtomType(), d1[0], d2[0]);
                        fw.write(line);
                        fw.flush();
                    }
                    else
                    {
                        if (i == 2)
                        {
                            line = String.format("%6d  %-3s%6d%6d%10.5f%6d%10.4f%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType(), d1[0], d2[0],
                                    d1[1], d2[1]);
                            fw.write(line);
                            fw.flush();
                        } // output the fourth through final atoms
                        else
                        {
                            line = String.format("%6d  %-3s%6d%6d%10.5f%6d%10.4f%6d%10.4f%6d%n",
                                    atom.getXYZIndex(), atom.getAtomString(),
                                    atom.getAtomType(), d1[0], d2[0],
                                    d1[1], d2[1], d1[2], d2[2], d1[3]);
                            fw.write(line);
                            fw.flush();
                        }
                    }
                }
            }

            // addition and deletion of bonds as required
            ArrayList<int[]> zadd = tmol.getBondAdd();
            ArrayList<int[]> zdel = tmol.getBondDel();

            if (zadd.size() > 0 || zdel.size() > 0)
            {
                fw.write(lsep);
                fw.flush();

                for (int i = 0; i < zadd.size(); i++)
                {
                    int[] z = zadd.get(i);
                    line = String.format("%6d%6d%n", z[0], z[1]);
                    fw.write(line);
                    fw.flush();
                }

                if (zdel.size() > 0)
                {
                    fw.write(lsep);
                    fw.flush();

                    for (int i = 0; i < zdel.size(); i++)
                    {
                        int[] z = zdel.get(i);
                        line = String.format("%6d%6d%n", z[0], z[1]);
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
    public static ArrayList<Double> readPSSROTOutput(String filename)
            throws DENOPTIMException
    {
        ArrayList<Double> energies = new ArrayList<>();

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
                    energies.add(new Double(arr[1]));
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
                                                     ArrayList<String> initPars,
                                                     ArrayList<String> restPars)
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
            throw new DENOPTIMException(msg,nfe);
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
                    String sp = dq[2];
                    String[] str2 = sp.split("\\s+");

                    //Check the format by reading all parts of atom type def.
                    int atomType = Integer.parseInt(str1[1]);
                    String symbol = str1[2];
                    String label = dq[1];
                    int z = Integer.parseInt(str2[1]);
                    double atmWeight = Double.parseDouble(str2[2]);
                    int cn = Integer.parseInt(str2[3]);

                    //Store
                    atomTypesMap.put(symbol,atomType);                    
                }
                catch (Throwable t)
                {
                    String msg = "Format of Tinker's atom type definition not recognized. \nDetails:\n"+t.getMessage();
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
     * Convert <code>IAtomContainer</code> to <code>TinkerMolecule</code>.
     * Supports only containers where all atoms are reachable following
     * the connectivity and starting from any other atom in the container.
     * @param mol the CDK molecule to convert
     * @param tMap the atom type map
     * @return the molecule represented by internal coordinates
     */

    public static TinkerMolecule getICFromIAC(IAtomContainer mol, 
                       HashMap<String,Integer> tMap )  throws DENOPTIMException
    {
        TinkerMolecule tm = new TinkerMolecule();
        String doneBnd = "visitedBond";
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            double d = 0.0;
            double a = 0.0;
            double t = 0.0;

            int[] nbrs = new int[] {0, 0, 0, 0};

            IAtom atmI = mol.getAtom(i);
            if (debug)
            {
                System.err.println("Atom to IC: "+atmI.getSymbol()+" "+i);
            }

            // define the bond length
            if (i>0)
            {
                i2 = getFirstRefAtomId(i,mol);
                d = atmI.getPoint3d().distance(mol.getAtom(i2).getPoint3d());
                mol.getBond(atmI,mol.getAtom(i2)).setProperty(doneBnd,"T");
                if (debug)
                {
                    System.err.println(" i2 = " + i2 + " d: " + d);
                }
                nbrs[0] = i2+1;
            }

            // define the bond angle
            if (i>1)
            {
                i3 = getSecondRefAtomId(i,i2,mol);
                a = DENOPTIMMathUtils.angle(atmI.getPoint3d(),
                                            mol.getAtom(i2).getPoint3d(),
                                            mol.getAtom(i3).getPoint3d());
                if (debug)
                {
                    System.err.println(" i3 = "+ i3 + " a: " + a);
                }
                nbrs[0] = i2+1;
                nbrs[1] = i3+1;
            }

            // decide on dihedral or second angle
            if (i>2)
            {
                ObjectPair op = getThirdRefAtomId(i,i2,i3,mol,tm);
                i4 = (int) op.getFirst();
                i5 = (int) op.getSecond();
                if (i5==1)
                {
                    t = DENOPTIMMathUtils.angle(atmI.getPoint3d(),
                                                mol.getAtom(i2).getPoint3d(),
                                                mol.getAtom(i4).getPoint3d());
                    double sign = DENOPTIMMathUtils.torsion(
                                                atmI.getPoint3d(),
                                                mol.getAtom(i2).getPoint3d(),
                                                mol.getAtom(i3).getPoint3d(),
                                                mol.getAtom(i4).getPoint3d());
                    if (sign > 0.0)
                    {
                        i5 = -1;
                    } 
                }
                else
                {
                    t = DENOPTIMMathUtils.torsion(atmI.getPoint3d(),
                                                mol.getAtom(i2).getPoint3d(),
                                                mol.getAtom(i3).getPoint3d(),
                                                mol.getAtom(i4).getPoint3d());
                }
                if (debug)
                {
                    System.err.println(" i4 = "+ i4 + " t: " + t + " " + i5);
                }
                nbrs[0] = i2+1;
                nbrs[1] = i3+1;
                nbrs[2] = i4+1;
                nbrs[3] = i5;
            }

            String symb = atmI.getSymbol();
            int atyp = 0; // Tinker types are assigned later

            TinkerAtom ta = new TinkerAtom(i+1, symb, atyp,
                                           new double[] {atmI.getPoint3d().x,
                                                         atmI.getPoint3d().y, 
                                                         atmI.getPoint3d().z},
                                                                         nbrs,
                                                        new double[] {d,a,t});

            int vidx = (Integer) atmI.getProperty(
                                             DENOPTIMConstants.ATMPROPVERTEXID);
            ta.setVertexId(vidx);

            if (debug)
            {
                System.err.println(" TinkerAtom: "+ta.toString());
            }

            tm.addAtom(ta);

            if (debug)
            {
                System.err.println("TinkerMolecule: ");
                tm.printIC();
            }
        }

        // Add bonds not visited
        for (IBond b : mol.bonds())
        {
            if (b.getProperty(doneBnd) == null)
            {
                tm.addBond(mol.getAtomNumber(b.getAtom(0))+1,
                           mol.getAtomNumber(b.getAtom(1))+1);
            }
        }

        // Due to the assumption that all atoms are part of the same 
        // connected network, no bond has to be deleted

        // Fix TinkerAtom types
        setTinkerTypes(tm,tMap);

        return tm;
    }

//----------------------------------------------------------------------------
   
    private static int getFirstRefAtomId(int i1, IAtomContainer mol)
    {
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        for (IAtom nbr : mol.getConnectedAtomsList(mol.getAtom(i1)))
        {
            if (mol.getAtomNumber(nbr) < i1)
            {
                ConnectedLigand cl = new ConnectedLigand(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        int i2 = mol.getAtomNumber(candidates.get(0).getAtom());
        return i2;
    }

//----------------------------------------------------------------------------

    private static int getSecondRefAtomId(int i1, int i2, IAtomContainer mol)
    {
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        for (IAtom nbr : mol.getConnectedAtomsList(mol.getAtom(i2)))
        {
            if ((mol.getAtomNumber(nbr) < i1) && (nbr != mol.getAtom(i1)))
            {
                ConnectedLigand cl = new ConnectedLigand(nbr,1);
                candidates.add(cl);
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        int i3 = mol.getAtomNumber(candidates.get(0).getAtom());
        return i3;
    }

//----------------------------------------------------------------------------

    private static ObjectPair getThirdRefAtomId(int i1, int i2, int i3, 
               IAtomContainer mol, TinkerMolecule tm) throws DENOPTIMException
    {
        int i5 = 0;
        IAtom atmI1 = mol.getAtom(i1);
        IAtom atmI2 = mol.getAtom(i2);
        IAtom atmI3 = mol.getAtom(i3);
        List<ConnectedLigand> candidates = new ArrayList<ConnectedLigand>();
        if (tm.isTorsionUsed(i2+1, i3+1) || 
            countPredefinedNeighbours(i1,atmI3,mol)==1)
        {
            i5 = 1;
            for (IAtom nbr : mol.getConnectedAtomsList(atmI2))
            {
                if (debug)
                {
                   System.err.println("  Eval. 3rd (ANG): " + nbr.getSymbol()
                   + mol.getAtomNumber(nbr) + " " 
                   + (mol.getAtomNumber(nbr) < i1) + " "
                   + (nbr != atmI1) + " " 
                   + (nbr != atmI3));
                }
                if ((mol.getAtomNumber(nbr) < i1) && (nbr != atmI1) && 
                                                        (nbr != atmI3))
                {
                    double dbcAng = DENOPTIMMathUtils.angle(nbr.getPoint3d(),
                                                          atmI2.getPoint3d(),
                                                          atmI3.getPoint3d());
                    if(dbcAng > 1.0)
                    {
                        ConnectedLigand cl = new ConnectedLigand(nbr,1);
                        candidates.add(cl);
                    }
                    else
                    {
                        if (debug)
                        {
                            System.err.println("  ...but collinear with "
                                               + atmI3.getSymbol() + i3
                                               + " (i4-i2-i3: " + dbcAng 
                                               + ")");
                        }
                    }
                }
            }
        }
        else
        {
            i5 = 0;
            for (IAtom nbr : mol.getConnectedAtomsList(atmI3))
            {
                if (debug)
                {
                   System.err.println("  Eval. 3rd (TOR): " + nbr.getSymbol()
                   + mol.getAtomNumber(nbr) + " "
                   + (mol.getAtomNumber(nbr) < i1) + " "
                   + (nbr != atmI1) + " "
                   + (nbr != atmI2));
                }
                if ((mol.getAtomNumber(nbr) < i1) && (nbr != atmI1) &&
                                                        (nbr != atmI2))
                {
                    ConnectedLigand cl = new ConnectedLigand(nbr,1);
                    candidates.add(cl);
                }
            }
        }
        Collections.sort(candidates, new ConnectedLigandComparator());
        if (candidates.size() == 0)
        {
            String msg = "Unable to make internal coordinates. Please, "
                         + "consider the use of dummy atoms in proximity "
                         + "of atom " + tm.getAtom(i1+1);
            throw new DENOPTIMException(msg);
        }
        int i4 = mol.getAtomNumber(candidates.get(0).getAtom());

        ObjectPair op = new ObjectPair(i4,i5);
        
        return op;
    }

//------------------------------------------------------------------------------

    private static int countPredefinedNeighbours(int i, IAtom a, IAtomContainer mol)
    {
        int tot = 0;
        for (IAtom nbr : mol.getConnectedAtomsList(a))
        {
            if (mol.getAtomNumber(nbr) < i)
                tot++;
        }
        return tot;
    }

//------------------------------------------------------------------------------

   /**
     * Conversion to tinker IC may not always have the necessary atom types.
     * In order to fix this, we add user defined atom types.
     *
     * @param tmol The tinker IC representation
     * @throws DENOPTIMException
     */

    public static void setTinkerTypes(TinkerMolecule tmol, 
                        HashMap<String,Integer> tMap) throws DENOPTIMException
    {
        ArrayList<TinkerAtom> lstAtoms = tmol.getAtoms();
        int numberOfAtoms = lstAtoms.size();
        for (int i = 0; i < numberOfAtoms; i++)
        {
            TinkerAtom tatom = lstAtoms.get(i);

            String st = tatom.getAtomString().trim();
            if (!tMap.containsKey(st))
            {
                String msg = "Unable to assign atom type to atom '" + st +"'. ";
		if (st.equals("R"))
		{
		    msg = msg + "Unusual dummy atom symbols get atom symbol "
		      + "'R'. Please, add atom type 'R' in your atom type map.";
		}
                throw new DENOPTIMException(msg);
            }
            Integer val = tMap.get(st);
            if (val != null)
            {
                tatom.setAtomType(val.intValue());
                if (debug)
                    System.err.println("Set parameter for " + st + " " + val);
            }
            else
            {
                String msg = "No valid Tinker atom type assigned for atom "
                            + st + ".\n";
                throw new DENOPTIMException(msg);
            }
        }
    }

//------------------------------------------------------------------------------

}
