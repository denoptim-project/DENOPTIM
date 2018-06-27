package checkcatalystconstraints;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point3d;


import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;

import utils.DENOPTIMMathUtils;
import utils.DoubleVector;
import utils.GenUtils;
import exception.DENOPTIMException;
import io.DenoptimIO;
import io.MOPACReader;
import java.util.HashMap;
import java.util.Map;
import org.openscience.cdk.exception.CDKException;
import task.ProcessHandler;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class CheckCatalystConstraints
{

    private static final Logger LOGGER = Logger.getLogger(CheckCatalystConstraints.class.getName());

    private static String inpsdfFile;
    private static String outsdfFile;
    private static String mopFile;
    private static double MinNonBondedDistance = Double.MAX_VALUE;
    private static double MaxBondedDistance = Double.MIN_VALUE;
    private static double MaxTorsion = Double.MIN_VALUE;
    private static double MinAngle = Double.MAX_VALUE;
    private static String REXEC;
    private static String RPredictScript;
    private static String scaffoldLib;
    private static String wrkDir;
    private static String id;
    

    private static String fsep = System.getProperty("file.separator");

//------------------------------------------------------------------------------    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar CheckCatalystConstraints.jar parameterFile");
            System.exit(-1);
        }

        String paramFile = args[0];

        try
        {
            readParameters(paramFile);
            checkParameters();
            
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(inpsdfFile);
            String fname = new File(inpsdfFile).getName();
            int pos = fname.lastIndexOf(".");
            if (pos > 0)
                fname = fname.substring(0, pos);


            if (mol.getProperty("GraphENC") != null)
            {
                String genc = mol.getProperty("GraphENC").toString();
                int scfidx = getScaffoldIndex(genc);
                IAtomContainer scf = DenoptimIO.readSDFFile(scaffoldLib).get(scfidx);

                MOPACReader mopread = new MOPACReader();
                mopread.readMOPACData(mopFile);
                
                if (mopread.getStatus() == -1)
                {
                    mol.setProperty("MOL_ERROR", "MOPAC Optimization failed."); 
                    DenoptimIO.writeMolecule(outsdfFile, mol, false);
                }
                else
                {

                    ArrayList<Point3d> atom_coords = mopread.getAtomCoordinates();
                    if (hasValidCoordinates(atom_coords))
                    {
                        for (int i = 0; i<atom_coords.size(); i++)
                        {
                            Point3d coords = atom_coords.get(i);
                            mol.getAtom(i).setPoint3d(coords);
                        }
                    }
                    

                    ArrayList<Double> atom_charges = mopread.getAtomCharges();
                    if (atom_charges != null)
                    {
                        // calculate the descriptors
                        DoubleVector descriptors = new DoubleVector(6);
                        //calculateDescriptors(scf, mol, descriptors, atom_charges);
                        
                        calculateDescriptorsUsingMCS(scf, mol, descriptors, atom_charges);
                        
                        StringBuilder sb = new StringBuilder();
                        for (int j=0; j<descriptors.length(); j++)
                        {
                            sb.append(String.format("%6.3f", descriptors.getValue(j))).append(" ");
                        }
                        mol.setProperty("Descriptors", sb.toString().trim());

                        String status = checkMolecule(descriptors, scf, mol);
                        if (!status.equalsIgnoreCase("OK"))
                        {
                            // write MOL_ERROR tag
                            mol.setProperty("MOL_ERROR", status);
                            DenoptimIO.writeMolecule(outsdfFile, mol, false);
                        }
                        else
                        {
                            String descOutFile = wrkDir + fsep + fname + "_desc.txt";
                            
                            String predOutFile = wrkDir + fsep + fname + "_pred.txt";
                            
                            // write descriptors
                            StringBuilder descStr = new StringBuilder();
                            for (int j=0; j<descriptors.length(); j++)
                            {
                                descStr.append(descriptors.getValue(j)).append(" ");
                            }
                            DenoptimIO.writeData(descOutFile, descStr.toString(), false);

                            String cmdStr = getCommandLineForR(REXEC, RPredictScript,
                                                        descOutFile, predOutFile);
                            

                            ProcessHandler ph_sc = new ProcessHandler(cmdStr, id);
        
                            try
                            {
                                ph_sc.runProcess();

                                if (ph_sc.getExitCode() != 0)
                                {
                                    DenoptimIO.deleteFile(descOutFile);
                                    LOGGER.log(Level.SEVERE, "Failed to execute: {0}", cmdStr);
                                    LOGGER.log(Level.SEVERE, ph_sc.getErrorOutput());
                                    System.exit(-1);
                                }
                            }
                            catch (Exception ex)
                            {
                                LOGGER.log(Level.SEVERE, ex.getMessage());
                                System.exit(-1);
                            }

                            double[] res = readResponseData(predOutFile);

                            if (res == null)
                            {
                                String msg = "Error occured while executing SAR prediction routine.";
                                LOGGER.severe(msg);
                                System.exit(-1);
                            }

                            mol.setProperty("FITNESS", GenUtils.roundValue(res[0], 3));
                            if (res.length > 1)
                                mol.setProperty("AD", GenUtils.roundValue(res[1], 3));


                            DenoptimIO.deleteFile(descOutFile);
                            DenoptimIO.deleteFile(predOutFile);


                            DenoptimIO.writeMolecule(outsdfFile, mol, false);
                        }
                    }
                }
            }
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
    
    private static void checkParameters() throws DENOPTIMException
    {
        if (inpsdfFile == null || inpsdfFile.length() == 0)
        {
            throw new DENOPTIMException("Input SDF file not supplied. Check parameter file.");
        }
        if (wrkDir == null || wrkDir.length() == 0)
        {
            throw new DENOPTIMException("Working directory not supplied. Check parameter file.");
        }
        if (outsdfFile == null || outsdfFile.length() == 0)
        {
            throw new DENOPTIMException("Output SDF file not supplied. Check parameter file.");
        }
        if (mopFile == null || mopFile.length() == 0)
        {
            throw new DENOPTIMException("MOPAC output file not supplied. Check parameter file.");
        }
        if (RPredictScript == null || RPredictScript.length() == 0)
        {
            throw new DENOPTIMException("R routine not supplied. Check parameter file.");
        }
        if (REXEC == null || REXEC.length() == 0)
        {
            throw new DENOPTIMException("REXEC not supplied. Check parameter file.");
        }
        if (scaffoldLib == null || scaffoldLib.length() == 0)
        {
            throw new DENOPTIMException("Scaffold library file not supplied. Check parameter file.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Read the predictions from file
     *
     * @param filename
     * @return array of predicted values
     * @throws DENOPTIMException
     */
    private static double[] readResponseData(String filename) throws DENOPTIMException
    {
        String sCurrentLine = null;

        double[] f = new double[2];

        int lc = 0;
        BufferedReader br = null;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null)
            {
                if (sCurrentLine.trim().length() == 0)
                {
                    continue;
                }
                lc++;

                if (lc == 1)
                {
                    String[] str = sCurrentLine.trim().split("\\s+");
                    f[0] = Double.parseDouble(str[0]);
                    f[1] = Double.parseDouble(str[1]);
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

        return f;
    }



//------------------------------------------------------------------------------

    private static String getCommandLineForR(String swPath, String script,
                                            String infile, String outputfile)
    {
        String cmdStr = swPath + " " + script + " " + infile + " " + outputfile;
        return cmdStr;
    }

//------------------------------------------------------------------------------

    private static int getScaffoldIndex(String grphstr)
    {
        String s1[] = grphstr.split("\\s+");
        String vStr = s1[1];

        // split vertices on the comma
        String s2[] = vStr.split(",");

        // for 1st vertex
        String s3[] = s2[0].split("_");

        // molid
        int molid = Integer.parseInt(s3[1]) - 1;

        return molid;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate descriptors for the molecule
     *
     * @param scaffold
     * @param mol
     * @param descriptors
     * @param atom_charges
     * @throws DENOPTIMException
     */
    private static void calculateDescriptors(IAtomContainer scaffold, IAtomContainer mol,
            DoubleVector descriptors, ArrayList<Double> atom_charges)
            throws DENOPTIMException
    {
        String str = scaffold.getProperty("ATOM_INDICES").toString();
        String[] st = str.split(":");

        int h1 = Integer.parseInt(st[0]) - 1, h2 = Integer.parseInt(st[1]) - 1;
        int Cl1 = Integer.parseInt(st[2]) - 1, Cl2 = Integer.parseInt(st[3]) - 1;
        int Ru = Integer.parseInt(st[4]) - 1;
        int C = Integer.parseInt(st[5]) - 1;
        int X = Integer.parseInt(st[6]) - 1;

        double distRuCl = getDistance(mol, Ru, Cl1);
        distRuCl += getDistance(mol, Ru, Cl2);
        distRuCl /= 2;
        descriptors.setValue(0, distRuCl);

        double angleClRuCl = getAngle(mol, Cl1, Ru, Cl2);
        descriptors.setValue(1, angleClRuCl);

        double angleClRuC = getAngle(mol, Cl1, Ru, C);
        angleClRuC += getAngle(mol, Cl2, Ru, C);
        angleClRuC /= 2;
        descriptors.setValue(2, angleClRuC);

        double d1 = Math.abs(getTorsion(mol, h1, C, Ru, X));
        double d2 = Math.abs(getTorsion(mol, h2, C, Ru, X));
        double torsionXRuCH = Math.min(Math.abs(d1), Math.abs(d2));
        descriptors.setValue(3, torsionXRuCH);

        double chgCl = 0;
        chgCl += atom_charges.get(Cl1).doubleValue();
        chgCl += atom_charges.get(Cl2).doubleValue();
        chgCl /= 2;
        descriptors.setValue(4, chgCl);

        double chgHyd = 0;
        chgHyd += atom_charges.get(h1).doubleValue();
        chgHyd += atom_charges.get(h2).doubleValue();
        chgHyd /= 2;
        descriptors.setValue(5, chgHyd);
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the minimum non-bonded Distance
     *
     * @param scaffold
     * @param mol
     * @return the distance
     */
    private static double getMinimumNonBondedAtomDistance(IAtomContainer scaffold,
            IAtomContainer mol)
    {
        String str = scaffold.getProperty("ATOM_INDICES").toString();
        String[] st = str.split(":");
        int Ru = Integer.parseInt(st[4]) - 1;

        IAtom atm = mol.getAtom(Ru);

        // get core atoms
        ArrayList<IAtom> lst = new ArrayList<>();
        for (int i = 0; i < st.length; i++)
        {
            lst.add(mol.getAtom(Integer.parseInt(st[i]) - 1));
        }
        // to the list add all connected atoms
        List<IAtom> clst = mol.getConnectedAtomsList(atm);
        for (int i = 0; i < clst.size(); i++)
        {
            if (!lst.contains(clst.get(i)))
            {
                lst.add(clst.get(i));
            }
        }

        // measure distances for all non-bonded atoms
        double d = Double.MAX_VALUE;

        int k = 0;
        for (IAtom atom : mol.atoms())
        {
            if (!lst.contains(atom))
            {
                double f = getDistance(mol, Ru, k);
                if (f < d)
                {
                    d = f;
                }
            }
            k++;
        }
        return d;
    }

//------------------------------------------------------------------------------

    private static double getDistance(IAtomContainer mol, int i, int j)
    {
        Point3d p1 = mol.getAtom(i).getPoint3d();
        Point3d p2 = mol.getAtom(j).getPoint3d();
        return DENOPTIMMathUtils.distance(p1, p2);
    }

//------------------------------------------------------------------------------

    private static double getAngle(IAtomContainer mol, int i, int j, int k)
    {
        Point3d p1 = mol.getAtom(i).getPoint3d();
        Point3d p2 = mol.getAtom(j).getPoint3d();
        Point3d p3 = mol.getAtom(k).getPoint3d();
        return DENOPTIMMathUtils.angle(p1, p2, p3);
    }

//------------------------------------------------------------------------------

    private static double getTorsion(IAtomContainer mol, int i, int j, int k, int l)
    {
        Point3d p1 = mol.getAtom(i).getPoint3d();
        Point3d p2 = mol.getAtom(j).getPoint3d();
        Point3d p3 = mol.getAtom(k).getPoint3d();
        Point3d p4 = mol.getAtom(l).getPoint3d();
        return DENOPTIMMathUtils.torsion(p1, p2, p3, p4);
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param descriptors
     * @param scaffold
     * @param mol
     * @return the error message due to constraint violation or "OK" if the
     * constraints are satisfied
     */
    private static String checkMolecule(DoubleVector descriptors,
        IAtomContainer scaffold, IAtomContainer mol) throws DENOPTIMException
    {
        //threshold_dist=Ru-X|(0,2.5); X-X|(0,2.5); Ru~X@n|(2.9,)
        //threshold_angle=Cl-Ru-C|(90,)
        //threshold_dihedral=H-C-Ru-X@n|(,10)
        String msg = null;
        if (descriptors.getValue(0) > MaxBondedDistance)
        {
            msg = ": Maximum bonded distance threshold violated. "
                    + String.format("%4f", descriptors.getValue(0));
            LOGGER.info(msg);
            return "Maximum bonded distance threshold violated. " + 
                    String.format("%2f", descriptors.getValue(0));
        }

        if (descriptors.getValue(2) < MinAngle)
        {
            msg = ": Minimum angle threshold violated. "
                    + String.format("%4f", descriptors.getValue(2));
            LOGGER.info(msg);
            return "Minimum angle threshold violated. " + 
                    String.format("%2f", descriptors.getValue(2));
        }

        if (descriptors.getValue(3) > MaxTorsion)
        {
            msg = ": Torsion angle threshold violated. "
                    + String.format("%4f", descriptors.getValue(3));
            LOGGER.info(msg);
            return "Torsion angle threshold violated. " + 
                    String.format("%2f", descriptors.getValue(3));
        }

        double f = getMinimumNonBondedAtomDistanceMCS(scaffold, mol);
        if (f < MinNonBondedDistance)
        {
            msg = ": Minimum non-bonded distance threshold violated. "
                    + String.format("%4f", f);
            LOGGER.info(msg);
            return "Minimum non-bonded distance threshold violated. " + 
                    String.format("%2f", f);
        }

        return "OK";
    }

//------------------------------------------------------------------------------

    private static void readParameters(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String option, line;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.toUpperCase().startsWith("INPSDF"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        inpsdfFile = option;
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("OUTSDF"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        outsdfFile = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("MOPOUT"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        mopFile = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("MAXBNDDIST"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        MaxBondedDistance = Double.parseDouble(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("MINANGLE"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        MinAngle = Double.parseDouble(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("MAXTORSION"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        MaxTorsion = Double.parseDouble(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("MINNBDIST"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        MinNonBondedDistance = Double.parseDouble(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("SCAFFOLDLIB"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        scaffoldLib = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RSCRIPTPATH"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        REXEC = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("SARPREDICTSCRTIPT"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        RPredictScript = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("WORKDIR"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        wrkDir = option;
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("TASKID"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        id = option;
                    }
                    continue;
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

   private static boolean hasValidCoordinates(ArrayList<Point3d> atom_coords)
                                                        throws DENOPTIMException
    {
        if (atom_coords == null)
        {
            throw new DENOPTIMException("No coordinates found.");
        }
        if (atom_coords.isEmpty())
        {
            throw new DENOPTIMException("No coordinates found.");
        }
        return true;
    }

//------------------------------------------------------------------------------
   
    private static void calculateDescriptorsUsingMCS(IAtomContainer scaffold, 
            IAtomContainer mol, DoubleVector descriptors, 
            ArrayList<Double> atom_charges)
            throws DENOPTIMException
    {
        String str = scaffold.getProperty("ATOM_INDICES").toString();
        String[] st = str.split(":");

        int h1 = Integer.parseInt(st[0]) - 1, h2 = Integer.parseInt(st[1]) - 1;
        int Cl1 = Integer.parseInt(st[2]) - 1, Cl2 = Integer.parseInt(st[3]) - 1;
        int Ru = Integer.parseInt(st[4]) - 1;
        int C = Integer.parseInt(st[5]) - 1;
        int X = Integer.parseInt(st[6]) - 1;
        
        HashMap<Integer, Integer> lstMatch = new HashMap<>();
        lstMatch.put(Ru, -1);
        lstMatch.put(C, -1);
        lstMatch.put(Cl1, -1);
        lstMatch.put(Cl2, -1);
        lstMatch.put(X, -1);
        lstMatch.put(h1, -1);
        lstMatch.put(h2, -1);
        
        
        
        boolean bondSensitive = true;        
        boolean stereoMatch = false;
        boolean fragmentMinimization = false;
        boolean energyMinimization = false;
        
        try
        {
            Isomorphism comparison = new Isomorphism(Algorithm.DEFAULT, bondSensitive);
            // set molecules, remove hydrogens, clean and configure molecule
            comparison.init(scaffold, mol, false, false);
            comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
            
            if (comparison.getFirstAtomMapping() != null)
            {
                //IAtomContainer Mol1 = comparison.getReactantMolecule();
                //IAtomContainer Mol2 = comparison.getProductMolecule();
                
                for (Map.Entry  mapping : comparison.getFirstMapping().entrySet()) 
                {
                    int i2 = ((Integer)mapping.getValue()).intValue();
                    int i1 = ((Integer)mapping.getKey()).intValue();
                    //IAtom eAtom = Mol1.getAtom(i1);
                    //IAtom pAtom = Mol2.getAtom(i2);
                    
                    Integer id1 = ((Integer)mapping.getKey()).intValue();
                    Integer id2 = ((Integer)mapping.getValue()).intValue();
                    
                    if (lstMatch.containsKey(id1))
                    {
                        lstMatch.put(id1, id2);
                    }

                    //System.out.println(eAtom.getSymbol() + 
                    //(((Integer)mapping.getKey()).intValue() + 1) + " " + 
                    //pAtom.getSymbol() + (((Integer)mapping.getValue()).intValue() + 1));
                }
            }
        }
        catch (CDKException ex)
        {
            throw new DENOPTIMException(ex);
        }


        double distRuCl = getDistance(mol, lstMatch.get(Ru).intValue(), 
                                            lstMatch.get(Cl1).intValue());
        distRuCl += getDistance(mol, lstMatch.get(Ru).intValue(), 
                                            lstMatch.get(Cl2).intValue());
        distRuCl /= 2;
        descriptors.setValue(0, distRuCl);

        double angleClRuCl = getAngle(mol, lstMatch.get(Cl1).intValue(), 
                    lstMatch.get(Ru).intValue(), lstMatch.get(Cl2).intValue());
        descriptors.setValue(1, angleClRuCl);

        double angleClRuC = getAngle(mol, lstMatch.get(Cl1).intValue(), 
                        lstMatch.get(Ru).intValue(), lstMatch.get(C).intValue());
        
        angleClRuC += getAngle(mol, lstMatch.get(Cl2).intValue(), 
                        lstMatch.get(Ru).intValue(), lstMatch.get(C).intValue());
        angleClRuC /= 2;
        descriptors.setValue(2, angleClRuC);

        double d1 = Math.abs(getTorsion(mol, lstMatch.get(h1).intValue(), 
                lstMatch.get(C).intValue(), lstMatch.get(Ru).intValue(), 
                lstMatch.get(X).intValue()));
        double d2 = Math.abs(getTorsion(mol, lstMatch.get(h2).intValue(), 
                lstMatch.get(C).intValue(), lstMatch.get(Ru).intValue(), 
                lstMatch.get(X).intValue()));
        double torsionXRuCH = Math.min(Math.abs(d1), Math.abs(d2));
        descriptors.setValue(3, torsionXRuCH);

        double chgCl = 0;
        chgCl += atom_charges.get(lstMatch.get(Cl1).intValue()).doubleValue();
        chgCl += atom_charges.get(lstMatch.get(Cl2).intValue()).doubleValue();
        chgCl /= 2;
        descriptors.setValue(4, chgCl);

        double chgHyd = 0;
        chgHyd += atom_charges.get(lstMatch.get(h1).intValue()).doubleValue();
        chgHyd += atom_charges.get(lstMatch.get(h2).intValue()).doubleValue();
        chgHyd /= 2;
        descriptors.setValue(5, chgHyd);        
    }

//------------------------------------------------------------------------------       

    /**
     * Calculate the minimum non-bonded Distance
     *
     * @param scaffold
     * @param mol
     * @return the distance
     */
    private static double getMinimumNonBondedAtomDistanceMCS(IAtomContainer scaffold,
                            IAtomContainer mol) throws DENOPTIMException
    {
        String str = scaffold.getProperty("ATOM_INDICES").toString();
        String[] st = str.split(":");
        int h1 = Integer.parseInt(st[0]) - 1, h2 = Integer.parseInt(st[1]) - 1;
        int Cl1 = Integer.parseInt(st[2]) - 1, Cl2 = Integer.parseInt(st[3]) - 1;
        int Ru = Integer.parseInt(st[4]) - 1;
        int C = Integer.parseInt(st[5]) - 1;
        int X = Integer.parseInt(st[6]) - 1;
        
        HashMap<Integer, Integer> lstMatch = new HashMap<>();
        lstMatch.put(Ru, -1);
        lstMatch.put(C, -1);
        lstMatch.put(Cl1, -1);
        lstMatch.put(Cl2, -1);
        lstMatch.put(X, -1);
        lstMatch.put(h1, -1);
        lstMatch.put(h2, -1);
        
        
        
        boolean bondSensitive = true;        
        boolean stereoMatch = false;
        boolean fragmentMinimization = false;
        boolean energyMinimization = false;
        
        try
        {
            Isomorphism comparison = new Isomorphism(Algorithm.DEFAULT, bondSensitive);
            // set molecules, remove hydrogens, clean and configure molecule
            comparison.init(scaffold, mol, false, false);
            comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
            
            if (comparison.getFirstAtomMapping() != null)
            {
                //IAtomContainer Mol1 = comparison.getReactantMolecule();
                //IAtomContainer Mol2 = comparison.getProductMolecule();
                
                for (Map.Entry  mapping : comparison.getFirstMapping().entrySet()) 
                {
                    int i2 = ((Integer)mapping.getValue()).intValue();
                    int i1 = ((Integer)mapping.getKey()).intValue();
                    //IAtom eAtom = Mol1.getAtom(i1);
                    //IAtom pAtom = Mol2.getAtom(i2);
                    
                    Integer id1 = ((Integer)mapping.getKey()).intValue();
                    Integer id2 = ((Integer)mapping.getValue()).intValue();
                    
                    if (lstMatch.containsKey(id1))
                    {
                        lstMatch.put(id1, id2);
                    }

                    //System.out.println(eAtom.getSymbol() + 
                    //(((Integer)mapping.getKey()).intValue() + 1) + " " + 
                    //pAtom.getSymbol() + (((Integer)mapping.getValue()).intValue() + 1));
                }
            }
        }
        catch (CDKException ex)
        {
            throw new DENOPTIMException(ex);
        }

        

        // get minimum non-bonded distance
        IAtom atm = mol.getAtom(lstMatch.get(Ru).intValue());

        // get core atoms
        ArrayList<IAtom> lst = new ArrayList<>();
        for (int i = 0; i < st.length; i++)
        {
            int id = Integer.parseInt(st[i]) - 1;
            lst.add(mol.getAtom(lstMatch.get(id).intValue()));
        }
        // to the list add all connected atoms
        List<IAtom> clst = mol.getConnectedAtomsList(atm);
        for (int i = 0; i < clst.size(); i++)
        {
            if (!lst.contains(clst.get(i)))
            {
                lst.add(clst.get(i));
            }
        }

        // measure distances for all non-bonded atoms
        double d = Double.MAX_VALUE;

        int k = 0;
        for (IAtom atom : mol.atoms())
        {
            if (!lst.contains(atom))
            {
                double f = getDistance(mol, lstMatch.get(Ru).intValue(), k);
                if (f < d)
                {
                    d = f;
                }
            }
            k++;
        }
        
        return d;
    }

//------------------------------------------------------------------------------    
   
}
