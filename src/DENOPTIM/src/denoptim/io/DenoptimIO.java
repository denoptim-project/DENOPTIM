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

package denoptim.io;

/**
 * Utility methods for input/output
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.vecmath.Point3d;

import java.util.Properties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;

import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.tools.FormatStringBuffer;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.listener.PropertiesListener;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.utils.DENOPTIMGraphEdit;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.smiles.InvPair;

import java.util.logging.Level;


public class DenoptimIO {
    private static final String NL = System.getProperty("line.separator");

    // A list of properties used by CDK algorithms which must never be
    // serialized into the SD file format.

    private static final ArrayList<String> cdkInternalProperties
            = new ArrayList<>(Arrays.asList(new String[]
            {InvPair.CANONICAL_LABEL, InvPair.INVARIANCE_PAIR}));

//------------------------------------------------------------------------------

    /**
     * Reads a text file containing links to multiple molecules mol/sdf format
     *
     * @param fileName the file containing the list of molecules
     * @return IAtomContainer[] an array of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readLinksToMols(String fileName)
            throws DENOPTIMException {
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();
        String sCurrentLine;

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((sCurrentLine = br.readLine()) != null) {
                sCurrentLine = sCurrentLine.trim();
                if (sCurrentLine.length() == 0) {
                    continue;
                }
                if (GenUtils.getFileExtension(sCurrentLine).
                        compareToIgnoreCase(".smi") == 0) {
                    throw new DENOPTIMException("Fragment files in SMILES format not supported.");
                }

                ArrayList<IAtomContainer> mols = readSDFFile(sCurrentLine);
                lstContainers.addAll(mols);
            }
        } catch (FileNotFoundException fnfe) {
            throw new DENOPTIMException(fnfe);
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } catch (DENOPTIMException de) {
            throw de;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        return lstContainers;

    }

//------------------------------------------------------------------------------

    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param fileName the file containing the molecules
     * @return IAtomContainer[] an array of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readSDFFile(String fileName)
            throws DENOPTIMException {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();

        try {
            mdlreader = new MDLV2000Reader(new FileReader(new File(fileName)));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) new ChemFile());
            lstContainers.addAll(
                    ChemFileManipulator.getAllAtomContainers(chemFile));
        } catch (CDKException | IOException cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (mdlreader != null) {
                    mdlreader.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lstContainers.isEmpty()) {
            throw new DENOPTIMException("No data found in " + fileName);
        }

        return lstContainers;
        //return lstContainers.toArray(new IAtomContainer[lstContainers.size()]);
    }

//------------------------------------------------------------------------------

    /**
     * Reads a file SDF file possible containing multiple molecules,
     * and returns only the first one.
     *
     * @param fileName the file containing the molecules
     * @return the first molecular object in the file
     * @throws DENOPTIMException
     */
    public static IAtomContainer readSingleSDFFile(String fileName)
            throws DENOPTIMException {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();

        try {
            if (!checkExists(fileName)) {
                String msg = "ERROR! file '" + fileName + "' not found!";
                throw new DENOPTIMException(msg);
            }
            mdlreader = new MDLV2000Reader(new FileReader(new File(fileName)));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) new ChemFile());
            lstContainers.addAll(
                    ChemFileManipulator.getAllAtomContainers(chemFile));
        } catch (Throwable cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (mdlreader != null) {
                    mdlreader.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lstContainers.isEmpty()) {
            throw new DENOPTIMException("No data found in " + fileName);
        }

        return lstContainers.get(0);
    }

//------------------------------------------------------------------------------

    /**
     * Writes the 2D/3D representation of the molecule to multi-SD file
     *
     * @param fileName The file to be written to
     * @param mols     The molecules to be written
     * @throws DENOPTIMException
     */
    public static void writeFragmentSet(String fileName,
                                        ArrayList<DENOPTIMFragment> mols)
            throws DENOPTIMException {
        SDFWriter sdfWriter = null;
        try {
            IAtomContainerSet molSet = new AtomContainerSet();
            for (int idx = 0; idx < mols.size(); idx++) {
                molSet.addAtomContainer(mols.get(idx).getIAtomContainer());
            }
            sdfWriter = new SDFWriter(new FileWriter(new File(fileName)));
            sdfWriter.write(molSet);
        } catch (CDKException | IOException cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (sdfWriter != null) {
                    sdfWriter.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes the 2D/3D representation of the molecule to multi-SD file
     *
     * @param fileName The file to be written to
     * @param mols     The molecules to be written
     * @throws DENOPTIMException
     */
    public static void writeMoleculeSet(String fileName,
                                        ArrayList<IAtomContainer> mols)
            throws DENOPTIMException {
        SDFWriter sdfWriter = null;
        try {
            IAtomContainerSet molSet = new AtomContainerSet();
            for (int idx = 0; idx < mols.size(); idx++) {
                molSet.addAtomContainer(mols.get(idx));
            }
            sdfWriter = new SDFWriter(new FileWriter(new File(fileName)));
            sdfWriter.write(molSet);
        } catch (CDKException | IOException cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (sdfWriter != null) {
                    sdfWriter.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a single molecule to the specified file
     *
     * @param fileName The file to be written to
     * @param mol      The molecule to be written
     * @param append
     * @throws DENOPTIMException
     */
    public static void writeMolecule(String fileName, IAtomContainer mol,
                                     boolean append) throws DENOPTIMException {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(new File(fileName), append));
            sdfWriter.write(mol);
        } catch (CDKException | IOException cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (sdfWriter != null) {
                    sdfWriter.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static void writeMol2File(String fileName, IAtomContainer mol,
                                     boolean append) throws DENOPTIMException {
        Mol2Writer mol2Writer = null;
        try {
            mol2Writer = new Mol2Writer(new FileWriter(new File(fileName), append));
            mol2Writer.write(mol);
        } catch (CDKException cdke) {
            throw new DENOPTIMException(cdke);
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (mol2Writer != null) {
                    mol2Writer.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static void writeXYZFile(String fileName, IAtomContainer mol,
                                    boolean append) throws DENOPTIMException {
        XYZWriter xyzWriter = null;
        try {
            xyzWriter = new XYZWriter(new FileWriter(new File(fileName), append));
            xyzWriter.write(mol);
        } catch (CDKException cdke) {
            throw new DENOPTIMException(cdke);
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (xyzWriter != null) {
                    xyzWriter.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes multiple smiles string array to the specified file
     *
     * @param fileName The file to be written to
     * @param smiles   array of smiles strings to be written
     * @param append   if
     *                 <code>true</code> append to the file
     * @throws DENOPTIMException
     */
    public static void writeSmilesSet(String fileName, String[] smiles,
                                      boolean append) throws DENOPTIMException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(fileName), append);
            for (int i = 0; i < smiles.length; i++) {
                fw.write(smiles[i] + NL);
                fw.flush();
            }
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a single smiles string to the specified file
     *
     * @param fileName The file to be written to
     * @param smiles
     * @param append   if
     *                 <code>true</code> append to the file
     * @throws DENOPTIMException
     */
    public static void writeSmiles(String fileName, String smiles,
                                   boolean append) throws DENOPTIMException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(fileName), append);
            fw.write(smiles + NL);
            fw.flush();
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write a data file
     *
     * @param fileName
     * @param data
     * @param append
     * @throws DENOPTIMException
     */
    public static void writeData(String fileName, String data, boolean append)
            throws DENOPTIMException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(fileName), append);
            fw.write(data + NL);
            fw.flush();
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Serialize an object into a given file
     *
     * @param fileName
     * @param obj
     * @param append
     * @throws DENOPTIMException
     */
    public static void serializeToFile(String fileName, Object obj,
                                       boolean append)
            throws DENOPTIMException {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(fileName, append);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.close();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new DENOPTIMException("Cannot serialize object.", t);
        } finally {
            try {
                fos.flush();
                fos.close();
                fos = null;
            } catch (Throwable t) {
                throw new DENOPTIMException("Cannot close FileOutputStream", t);
            }
            try {
                oos.close();
                oos = null;
            } catch (Throwable t) {
                throw new DENOPTIMException("Cannot close ObjectOutputStream", t);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Deserialize a <code>DENOPTIMGraph</code> from a given file
     *
     * @param file the given file
     * @return the graph
     * @throws DENOPTIMException if anything goes wrong
     */

    public static DENOPTIMGraph deserializeDENOPTIMGraph(File file)
            throws DENOPTIMException {
        DENOPTIMGraph graph = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            graph = (DENOPTIMGraph) ois.readObject();
            ois.close();
        } catch (InvalidClassException ice) {
            String msg = "Attempt to deserialized old graph generated by an "
                    + "older version of DENOPTIM. A serialized graph "
                    + "can only be read by the version of DENOPTIM that "
                    + "has generate the serialized file.";
            throw new DENOPTIMException(msg);
        } catch (Throwable t) {
            throw new DENOPTIMException(t);
        } finally {
            try {
                fis.close();
            } catch (Throwable t) {
                throw new DENOPTIMException(t);
            }
        }
        
        // Serialization creates independent clones of the APClasses, but
        // we want to APClasses to be unique, so we replace the clones with the
        // reference to our unique APClass
        for (DENOPTIMAttachmentPoint ap : graph.getAttachmentPoints())
        {
            APClass a = ap.getAPClass();
            if (a!=null)
            {
                ap.setAPClass(APClass.make(a.toString()));
            }
        }
        for (DENOPTIMEdge e : graph.getEdgeList())
        {
            APClass srcApc = e.getSrcAPClass();
            APClass trgApc = e.getTrgAPClass();
            if (srcApc!=null && trgApc!=null)
            {
                e.setSrcAPClass(APClass.make(srcApc.toString()));
                e.setTrgAPClass(APClass.make(trgApc.toString()));
            }
        }

        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a zip file
     *
     * @param zipOutputFileName
     * @param filesToZip
     * @throws Exception
     */
    public static void createZipFile(String zipOutputFileName,
                                     String[] filesToZip) throws Exception {
        FileOutputStream fos = new FileOutputStream(zipOutputFileName);
        ZipOutputStream zos = new ZipOutputStream(fos);
        int bytesRead;
        byte[] buffer = new byte[1024];
        CRC32 crc = new CRC32();
        for (int i = 0, n = filesToZip.length; i < n; i++) {
            String fname = filesToZip[i];
            File cFile = new File(fname);
            if (!cFile.exists()) {
                //System.err.println("Skipping: " + fname);
                continue;
            }

            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(cFile));
            crc.reset();
            while ((bytesRead = bis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
            bis.close();
            // Reset to beginning of input stream
            bis = new BufferedInputStream(new FileInputStream(cFile));
            ZipEntry ze = new ZipEntry(fname);
            // DEFLATED setting for a compressed version
            ze.setMethod(ZipEntry.DEFLATED);
            ze.setCompressedSize(cFile.length());
            ze.setSize(cFile.length());
            ze.setCrc(crc.getValue());
            zos.putNextEntry(ze);
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            bis.close();
        }
        zos.close();
    }

//------------------------------------------------------------------------------

    /**
     * Delete the file
     *
     * @param fileName
     * @throws DENOPTIMException
     */
    public static void deleteFile(String fileName) throws DENOPTIMException {
        File f = new File(fileName);
        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) {
            //System.err.println("Delete: no such file or directory: " + fileName);
            return;
        }

        if (!f.canWrite()) {
            //System.err.println("Delete: write protected: " + fileName);
            return;
        }

        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            //System.err.println("Delete operation on directory not supported");
            return;
        }

        // Attempt to delete it
        boolean success = f.delete();

        if (!success) {
            throw new DENOPTIMException("Deletion of " + fileName + " failed.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Delete all files with pathname containing a given string
     *
     * @param path
     * @param pattern
     * @throws DENOPTIMException
     */
    public static void deleteFilesContaining(String path, String pattern)
            throws DENOPTIMException {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String name = listOfFiles[i].getName();
                if (name.contains(pattern)) {
                    deleteFile(listOfFiles[i].getAbsolutePath());
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * @param fileName
     * @return <code>true</code> if directory is successfully created
     */
    public static boolean createDirectory(String fileName) {
        return (new File(fileName)).mkdir();
    }

//------------------------------------------------------------------------------

    /**
     * @param fileName
     * @return <code>true</code> if file exists
     */
    public static boolean checkExists(String fileName) {
        if (fileName.length() > 0) {
            return (new File(fileName)).exists();
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of lines in the file
     *
     * @param fileName
     * @return number of lines in the file
     * @throws DENOPTIMException
     */
    public static int countLinesInFile(String fileName) throws DENOPTIMException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(fileName));
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            while ((readChars = bis.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return count;
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * @param fileName
     * @return list of fingerprints in bit representation
     * @throws DENOPTIMException
     */
    public static ArrayList<BitSet> readFingerprintData(String fileName)
            throws DENOPTIMException {
        ArrayList<BitSet> fps = new ArrayList<>();

        BufferedReader br = null;
        String sCurrentLine;

        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.trim().length() == 0) {
                    continue;
                }
                String[] str = sCurrentLine.split(", ");
                int n = str.length - 1;
                BitSet bs = new BitSet(n);
                for (int i = 0; i < n; i++) {
                    bs.set(i, Boolean.parseBoolean(str[i + 1]));
                }
                fps.add(bs);
            }
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (fps.isEmpty()) {
            throw new DENOPTIMException("No data found in file: " + fileName);
        }

        return fps;
    }

//------------------------------------------------------------------------------

    /**
     * Perform a deep copy of the object
     *
     * @param oldObj
     * @return a deep copy of an object
     * @throws DENOPTIMException
     */

    public static Object deepCopy(Object oldObj) throws DENOPTIMException {
        Object newObj = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            // serialize and pass the object
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bin);

            oos.close();

            newObj = ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
        return newObj;
    }

//------------------------------------------------------------------------------

    /**
     * Read the min, max, mean, and median of a population from
     * "Gen.*\.txt" file
     *
     * @param fileName
     * @return list of data
     * @throws DENOPTIMException
     */
    public static double[] readPopulationProps(File file)
            throws DENOPTIMException {
        double[] vals = new double[4];
        ArrayList<String> txt = readList(file.getAbsolutePath());
        for (String line : txt) {
            if (line.trim().length() < 8) {
                continue;
            }

            String key = line.toUpperCase().trim().substring(0, 8);
            switch (key) {
                case ("MIN:    "):
                    vals[0] = Double.parseDouble(line.split("\\s+")[1]);
                    break;

                case ("MAX:    "):
                    vals[1] = Double.parseDouble(line.split("\\s+")[1]);
                    break;

                case ("MEAN:   "):
                    vals[2] = Double.parseDouble(line.split("\\s+")[1]);
                    break;

                case ("MEDIAN: "):
                    vals[3] = Double.parseDouble(line.split("\\s+")[1]);
                    break;
            }
        }
        return vals;
    }

//------------------------------------------------------------------------------

    /**
     * Read list of data as text
     *
     * @param fileName
     * @return list of data
     * @throws DENOPTIMException
     */
    public static ArrayList<String> readList(String fileName) throws DENOPTIMException {
        ArrayList<String> lst = new ArrayList<>();
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                lst.add(line.trim());
            }
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lst.isEmpty()) {
            throw new DENOPTIMException("No data found in file: " + fileName);
        }

        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Write the coordinates in XYZ format
     *
     * @param fileName
     * @param atom_symbols
     * @param atom_coords
     * @throws DENOPTIMException
     */
    public static void writeXYZFile(String fileName, ArrayList<String> atom_symbols,
                                    ArrayList<Point3d> atom_coords) throws DENOPTIMException {
        FileWriter fw = null;
        FormatStringBuffer fsb = new FormatStringBuffer("%-8.6f");
        try {
            String molname = fileName.substring(0, fileName.length() - 4);
            fw = new FileWriter(new File(fileName));
            int numatoms = atom_symbols.size();
            fw.write("" + numatoms + NL);
            fw.flush();
            fw.write(molname + NL);

            String line = "", st = "";

            for (int i = 0; i < atom_symbols.size(); i++) {
                st = atom_symbols.get(i);
                Point3d p3 = atom_coords.get(i);

                line = st + "        " + (p3.x < 0 ? "" : " ") + fsb.format(p3.x) + "        "
                        + (p3.y < 0 ? "" : " ") + fsb.format(p3.y) + "        "
                        + (p3.z < 0 ? "" : " ") + fsb.format(p3.z);
                fw.write(line + NL);
                fw.flush();
            }
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Generate the ChemDoodle representation of the molecule
     *
     * @param mol
     * @return molecule as a formatted string
     * @throws DENOPTIMException
     */
    public static String getChemDoodleString(IAtomContainer mol)
            throws DENOPTIMException {
        StringWriter stringWriter = new StringWriter();
        MDLV2000Writer mw = null;
        try {
            mw = new MDLV2000Writer(stringWriter);
            mw.write(mol);
        } catch (CDKException cdke) {
            throw new DENOPTIMException(cdke);
        } finally {
            try {
                if (mw != null) {
                    mw.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        String MoleculeString = stringWriter.toString();

        //System.out.print(stringWriter.toString());
        //now split MoleculeString into multiple lines to enable explicit printout of \n
        String Moleculelines[] = MoleculeString.split("\\r?\\n");

        StringBuilder sb = new StringBuilder(1024);
        sb.append("var molFile = '");
        for (int i = 0; i < Moleculelines.length; i++) {
            sb.append(Moleculelines[i]);
            sb.append("\\n");
        }
        sb.append("';");
        return sb.toString();
    }

//------------------------------------------------------------------------------

    public static void writeMolecule2D(String fileName, IAtomContainer mol)
            throws DENOPTIMException {
        MDLV2000Writer writer = null;

        try {
            writer = new MDLV2000Writer(new FileWriter(new File(fileName)));
            Properties customSettings = new Properties();
            customSettings.setProperty("ForceWriteAs2DCoordinates", "true");
            PropertiesListener listener = new PropertiesListener(customSettings);
            writer.addChemObjectIOListener(listener);
            writer.writeMolecule(mol);
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } catch (Exception ex) {
            throw new DENOPTIMException(ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static Set<APClass> readAllAPClasses(File fragLib) {
        Set<APClass> allCLasses = new HashSet<APClass>();
        try {
            for (IAtomContainer mol : DenoptimIO.readMoleculeData(
                    fragLib.getAbsolutePath())) {
                DENOPTIMFragment frag = new DENOPTIMFragment(mol,
                        BBType.UNDEFINED);
                for (DENOPTIMAttachmentPoint ap : frag.getAttachmentPoints()) {
                    allCLasses.add(ap.getAPClass());
                }
            }
        } catch (DENOPTIMException e) {
            System.out.println("Could not read data from '" + fragLib + "'. "
                    + "Cause: " + e.getMessage());
        }

        return allCLasses;
    }

//------------------------------------------------------------------------------

    /**
     * The class compatibility matrix
     *
     * @param fileName    the file to be read
     * @param cpMap container for the APClass compatibility rules
     * @param boMap       container for the APClass-to-bond order
     * @param capMap     container for the capping rules
     * @param ends     container for the definition of forbidden ends
     */
    public static void writeCompatibilityMatrix(String fileName, 
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<String, BondType> boMap, HashMap<APClass, APClass> capMap,
            HashSet<APClass> ends) throws DENOPTIMException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date date = new Date();
        String dateStr = dateFormat.format(date);

        StringBuilder sb = new StringBuilder();
        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" Compatibility matrix data").append(NL);
        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" Written by DENOPTIM-GUI on ").append(dateStr).append(NL);
        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" APCLass Compatibility rules").append(NL);
        SortedSet<APClass> keysCPMap = new TreeSet<APClass>();
        keysCPMap.addAll(cpMap.keySet());
        for (APClass srcAPC : keysCPMap) {
            sb.append(DENOPTIMConstants.APCMAPCOMPRULE).append(" ");
            sb.append(srcAPC).append(" ");
            for (int i = 0; i < cpMap.get(srcAPC).size(); i++) {
                APClass trgAPC = cpMap.get(srcAPC).get(i);
                sb.append(trgAPC);
                if (i != (cpMap.get(srcAPC).size() - 1)) {
                    sb.append(",");
                } else {
                    sb.append(NL);
                }
            }
        }

        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" APClass-to-BondOrder").append(NL);
        SortedSet<String> keysBO = new TreeSet<String>();
        keysBO.addAll(boMap.keySet());
        for (String apc : keysBO) {
            sb.append(DENOPTIMConstants.APCMAPAP2BO).append(" ");
            sb.append(apc).append(" ").append(
                    boMap.get(apc).toOldString()).append(NL);
        }

        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" Capping rules").append(NL);
        SortedSet<APClass> keysCap = new TreeSet<APClass>();
        keysCap.addAll(capMap.keySet());
        for (APClass apc : keysCap) {
            sb.append(DENOPTIMConstants.APCMAPCAPPING).append(" ");
            sb.append(apc).append(" ").append(capMap.get(apc)).append(NL);
        }

        sb.append(DENOPTIMConstants.APCMAPIGNORE);
        sb.append(" Forbidden ends").append(NL);
        SortedSet<APClass> sortedFE = new TreeSet<APClass>();
        sortedFE.addAll(ends);
        for (APClass apc : sortedFE) {
            sb.append(DENOPTIMConstants.APCMAPFORBIDDENEND).append(" ");
            sb.append(apc).append(" ").append(NL);
        }

        DenoptimIO.writeData(fileName, sb.toString(), false);
    }

//------------------------------------------------------------------------------

    /**
     * Read the APclass compatibility matrix data from file.
     *
     * @param fileName    the file to be read
     * @param compatMap container for the APClass compatibility rules
     * @param boMap       container for the APClass-to-bond type rules
     * @param cappingMap     container for the capping rules
     * @param forbiddenEndList     container for the definition of forbidden ends
     * @throws DENOPTIMException
     */
    public static void readCompatibilityMatrix(String fileName,HashMap<APClass, 
            ArrayList<APClass>> compatMap, HashMap<String, BondType> boMap, 
            HashMap<APClass, APClass> cappingMap, Set<APClass> forbiddenEndList)
            throws DENOPTIMException {

        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPIGNORE)) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPCOMPRULE)) {
                    String str[] = line.split("\\s+");
                    if (str.length < 3) {
                        String err = "Incomplete APClass compatibility line '"
                                + line + "'.";
                        throw new DENOPTIMException(err + " " + fileName);
                    }

                    APClass srcAPC = APClass.make(str[1]);
                    ArrayList<APClass> trgAPCs = new ArrayList<APClass>();
                    for (String s : str[2].split(","))
                    {
                        trgAPCs.add(APClass.make(s.trim()));
                    }
                    compatMap.put(srcAPC, trgAPCs);
                } else {
                    if (line.startsWith(DENOPTIMConstants.APCMAPAP2BO)) {
                        String str[] = line.split("\\s+");
                        if (str.length != 3) {
                            String err = "Incomplete reaction bondorder line '"
                                    + line + "'.";
                            throw new DENOPTIMException(err + " " + fileName);
                        }
                        boMap.put(str[1], BondType.parseStr(str[2]));
                    } else {
                        if (line.startsWith(DENOPTIMConstants.APCMAPCAPPING)) {
                            String str[] = line.split("\\s+");
                            if (str.length != 3) {
                                String err = "Incomplete capping line '"
                                        + line +"'.";
                                throw new DENOPTIMException(err + " "+fileName);
                            }
                            APClass srcAPC = APClass.make(str[1]);
                            APClass trgAPC = APClass.make(str[2]);
                            cappingMap.put(srcAPC, trgAPC);
                        } else {
                            if (line.startsWith(
                                    DENOPTIMConstants.APCMAPFORBIDDENEND)) {
                                String str[] = line.split("\\s+");
                                if (str.length != 2) {
                                    for (int is = 1; is < str.length; is++) {
                                        forbiddenEndList.add(
                                                APClass.make(str[is]));
                                    }
                                } else {
                                    forbiddenEndList.add(APClass.make(str[1]));
                                }
                            }
                        }
                    }
                }
            }
        } catch (NumberFormatException | IOException nfe) {
            throw new DENOPTIMException(nfe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (compatMap.isEmpty()) {
            String err = "No reaction compatibility data found in file: ";
            throw new DENOPTIMException(err + " " + fileName);
        }

        if (boMap.isEmpty()) {
            String err = "No bond data found in file: ";
            throw new DENOPTIMException(err + " " + fileName);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads the APclass compatibility matrix for ring-closing connections
     * (the RC-CPMap).
     * Note that RC-CPMap is by definition symmetric. Though, <code>true</code>
     * entries can be defined either from X:Y or Y:X, and we make sure
     * such entries are stored in the map. This method assumes
     * that the APclasses reported in the RC-CPMap are defined, w.r.t bond
     * order, in the regular compatibility matrix as we wont
     * check it this condition is satisfied.
     *
     * @param fileName
     * @param rcCompatMap
     * @throws DENOPTIMException
     */
    public static void readRCCompatibilityMatrix(String fileName, 
            HashMap<APClass, ArrayList<APClass>> rcCompatMap)
            throws DENOPTIMException {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPIGNORE)) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPCOMPRULE)) {
                    String str[] = line.split("\\s+");
                    if (str.length < 3) {
                        String err = "Incomplete reaction compatibility data.";
                        throw new DENOPTIMException(err + " " + fileName);
                    }
                    
                    APClass srcAPC = APClass.make(str[1]);
                    /*
                    ArrayList<APClass> trgAPCs = new ArrayList<APClass>();
                    for (String s : str[2].split(","))
                    {
                        trgAPCs.add(APClass.make(s.trim()));
                    }
*/

                    String strRcn[] = str[2].split(",");
                    for (int i = 0; i < strRcn.length; i++) {
                        strRcn[i] = strRcn[i].trim();

                        APClass trgAPC = APClass.make(strRcn[i]);
                        if (rcCompatMap.containsKey(srcAPC)) {
                            rcCompatMap.get(srcAPC).add(trgAPC);
                        } else {
                            ArrayList<APClass> list = new ArrayList<APClass>();
                            list.add(trgAPC);
                            rcCompatMap.put(srcAPC, list);
                        }

                        if (rcCompatMap.containsKey(trgAPC)) {
                            rcCompatMap.get(trgAPC).add(srcAPC);
                        } else {
                            ArrayList<APClass> list = new ArrayList<APClass>();
                            list.add(srcAPC);
                            rcCompatMap.put(trgAPC, list);
                        }
                    }
                }
            }
        } catch (NumberFormatException | IOException nfe) {
            throw new DENOPTIMException(nfe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        if (rcCompatMap.isEmpty()) {
            String err = "No reaction compatibility data found in file: ";
            throw new DENOPTIMException(err + " " + fileName);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads SDF files that represent one or more tested candidates. Candidates
     * are provided with a graph representation, a unique identifier, and
     * either a fitness value or a mol_error defining why this candidate could
     * not be evaluated.
     *
     * @param file         the SDF file to read
     * @param useFragSpace use <code>true</code> if a fragment space is defined
     *                     and we can use it to interpret the graph finding a full meaning for the
     *                     nodes in the graph.
     * @return the list of candidates
     * @throws DENOPTIMException is something goes wrong while reading the file
     *                           or interpreting its content
     */
    public static ArrayList<DENOPTIMMolecule> readDENOPTIMMolecules(File file, 
            boolean useFragSpace) throws DENOPTIMException {
        String filename = file.getAbsolutePath();
        ArrayList<DENOPTIMMolecule> mols = new ArrayList<>();
        ArrayList<IAtomContainer> iacs = readMoleculeData(filename);
        for (IAtomContainer iac : iacs) {
            DENOPTIMMolecule mol = new DENOPTIMMolecule(iac, useFragSpace);
            mol.setMoleculeFile(filename);
            mols.add(mol);
        }

        return mols;
    }

//------------------------------------------------------------------------------

    /**
     * Reads the molecules in a file. Expects filenames with commonly accepted
     * extensions (i.e., .smi and .sdf). Unrecognized extensions will be
     * interpreted as links (i.e., pathnames) to SDF files.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readMoleculeData(String fileName)
            throws DENOPTIMException {
        ArrayList<IAtomContainer> mols;
        // check file extension
        if (GenUtils.getFileExtension(fileName).
                compareToIgnoreCase(".smi") == 0) {
            throw new DENOPTIMException("Fragment files in SMILES format not"
                    + " supported.");
        } else if (GenUtils.getFileExtension(fileName).
                compareToIgnoreCase(".sdf") == 0) {
            mols = DenoptimIO.readSDFFile(fileName);
        }
        // process everything else as a text file with links to individual 
        // molecules
        else {
            mols = DenoptimIO.readLinksToMols(fileName);
        }
        return mols;
    }

//------------------------------------------------------------------------------

    /**
     * Reads the molecules in a file with specifies format. Acceptable formats
     * are TXT, SD, and SDF.
     *
     * @param fileName the pathname of the file to read.
     * @param format   a string defining how to interpret the file.
     * @return the list of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readMoleculeData(String fileName,
                                                             String format) throws DENOPTIMException {
        ArrayList<IAtomContainer> mols;
        switch (format) {
            case "SDF":
                mols = DenoptimIO.readSDFFile(fileName);
                break;

            case "SD":
                mols = DenoptimIO.readSDFFile(fileName);
                break;

            case "TXT":
                mols = DenoptimIO.readLinksToMols(fileName);
                break;

            default:
                throw new DENOPTIMException("Molecular file format '" + format
                        + "' is not recognized.");
        }
        return mols;
    }

//------------------------------------------------------------------------------

    /**
     * Writes a PNG representation of the molecule
     *
     * @param mol      the molecule
     * @param fileName output file
     * @throws DENOPTIMException
     */

    public static void moleculeToPNG(IAtomContainer mol, String fileName)
            throws DENOPTIMException {
        IAtomContainer iac = null;
        if (!GeometryTools.has2DCoordinates(mol)) {
            iac = DENOPTIMMoleculeUtils.generate2DCoordinates(mol);
        } else {
            iac = mol;
        }

        if (iac == null) {
            throw new DENOPTIMException("Failed to generate 2D coordinates.");
        }

        try {
            int WIDTH = 500;
            int HEIGHT = 500;
            // the draw area and the image should be the same size
            Rectangle drawArea = new Rectangle(WIDTH, HEIGHT);
            Image image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

            // generators make the image elements
            ArrayList<IGenerator<IAtomContainer>> generators = new ArrayList<>();
            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new BasicAtomGenerator());


            GeometryTools.translateAllPositive(iac);

            // the renderer needs to have a toolkit-specific font manager
            AtomContainerRenderer renderer =
                    new AtomContainerRenderer(generators, new AWTFontManager());

            RendererModel model = renderer.getRenderer2DModel();
            model.set(BasicSceneGenerator.UseAntiAliasing.class, true);
            //model.set(BasicAtomGenerator.KekuleStructure.class, true);
            model.set(BasicBondGenerator.BondWidth.class, 2.0);
            model.set(BasicAtomGenerator.ColorByType.class, true);
            model.set(BasicAtomGenerator.ShowExplicitHydrogens.class, false);
            model.getParameter(BasicSceneGenerator.FitToScreen.class).setValue(Boolean.TRUE);


            // the call to 'setup' only needs to be done on the first paint
            renderer.setup(iac, drawArea);

            // paint the background
            Graphics2D g2 = (Graphics2D) image.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, WIDTH, HEIGHT);


            // the paint method also needs a toolkit-specific renderer
            renderer.paint(iac, new AWTDrawVisitor(g2),
                    new Rectangle2D.Double(0, 0, WIDTH, HEIGHT), true);

            ImageIO.write((RenderedImage) image, "PNG", new File(fileName));
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write the molecule in V3000 format.
     *
     * @param outfile
     * @param mol
     * @throws Exception
     */

    @SuppressWarnings({ "ConvertToTryWithResources", "unused" })
    private static void writeV3000File(String outfile, IAtomContainer mol)
            throws DENOPTIMException {
        StringBuilder sb = new StringBuilder(1024);

        String title = (String) mol.getProperty(CDKConstants.TITLE);
        if (title == null)
            title = "";
        if (title.length() > 80)
            title = title.substring(0, 80);
        sb.append(title).append("\n");

        sb.append("  CDK     ").append(new SimpleDateFormat("MMddyyHHmm").
                format(System.currentTimeMillis()));
        sb.append("\n\n");

        sb.append("  0  0  0     0  0            999 V3000\n");

        sb.append("M  V30 BEGIN CTAB\n");
        sb.append("M  V30 COUNTS ").append(mol.getAtomCount()).append(" ").
                append(mol.getBondCount()).append(" 0 0 0\n");
        sb.append("M  V30 BEGIN ATOM\n");
        for (int f = 0; f < mol.getAtomCount(); f++) {
            IAtom atom = mol.getAtom(f);
            sb.append("M  V30 ").append((f + 1)).append(" ").append(atom.getSymbol()).
                    append(" ").append(atom.getPoint3d().x).append(" ").
                    append(atom.getPoint3d().y).append(" ").
                    append(atom.getPoint3d().z).append(" ").append("0");
            sb.append("\n");
        }
        sb.append("M  V30 END ATOM\n");
        sb.append("M  V30 BEGIN BOND\n");

        Iterator<IBond> bonds = mol.bonds().iterator();
        int f = 0;
        while (bonds.hasNext()) {
            IBond bond = bonds.next();
            int bondType = bond.getOrder().numeric();
            String bndAtoms = "";
            if (bond.getStereo() == IBond.Stereo.UP_INVERTED ||
                    bond.getStereo() == IBond.Stereo.DOWN_INVERTED ||
                    bond.getStereo() == IBond.Stereo.UP_OR_DOWN_INVERTED) {
                // turn around atom coding to correct for inv stereo
                bndAtoms = mol.getAtomNumber(bond.getAtom(1)) + 1 + " ";
                bndAtoms += mol.getAtomNumber(bond.getAtom(0)) + 1;
            } else {
                bndAtoms = mol.getAtomNumber(bond.getAtom(0)) + 1 + " ";
                bndAtoms += mol.getAtomNumber(bond.getAtom(1)) + 1;
            }

//            String stereo = "";
//            switch(bond.getStereo())
//            {
//                case UP:
//                    stereo += "1";
//                    break;
//       		case UP_INVERTED:
//                    stereo += "1";
//                    break;
//                case DOWN:
//                    stereo += "6";
//                    break;
//                case DOWN_INVERTED:
//                    stereo += "6";
//                    break;
//                case UP_OR_DOWN:
//                    stereo += "4";
//                    break;
//                case UP_OR_DOWN_INVERTED:
//                    stereo += "4";
//                    break;
//                case E_OR_Z:
//                    stereo += "3";
//                    break;
//                default:
//                    stereo += "0";
//            }

            sb.append("M  V30 ").append((f + 1)).append(" ").append(bondType).
                    append(" ").append(bndAtoms).append("\n");
            f = f + 1;
        }

        sb.append("M  V30 END BOND\n");
        sb.append("M  V30 END CTAB\n");
        sb.append("M  END\n\n");

        Map<Object, Object> sdFields = mol.getProperties();
        if (sdFields != null) {
            for (Object propKey : sdFields.keySet()) {
                if (!cdkInternalProperties.contains((String) propKey)) {
                    sb.append("> <").append(propKey).append(">");
                    sb.append("\n");
                    sb.append("").append(sdFields.get(propKey));
                    sb.append("\n\n");
                }
            }
        }


        sb.append("$$$$\n");

        //System.err.println(sb.toString());

        try {

            FileWriter fw = new FileWriter(outfile);
            fw.write(sb.toString());
            fw.close();
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of graph editing tasks from a text file
     *
     * @param fileName the pathname of the file to read
     * @return the list of editing tasks
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraphEdit> readDENOPTIMGraphEditFromFile(
            String fileName)
            throws DENOPTIMException {
        ArrayList<DENOPTIMGraphEdit> lst = new ArrayList<>();
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPIGNORE)) {
                    continue;
                }

                DENOPTIMGraphEdit graphEdit;
                try {
                    graphEdit = new DENOPTIMGraphEdit(line.trim());
                } catch (Throwable t) {
                    String msg = "Cannot convert string to DENOPTIMGraphEdit. "
                            + "Check line '" + line.trim() + "'";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg, t);
                }
                lst.add(graphEdit);
            }
        } catch (IOException ioe) {
            String msg = "Cannot read file " + fileName;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg, ioe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from file
     *
     * @param fileName the pathname of the file to read
     * @param format   the file format to expect
     * @param useFS    set to <code>true</code> when there is a defined
     *                 fragment space that contains the fragments used to build the graphs.
     *                 Otherwise, use <code>false</code>. This will create only as many APs as
     *                 needed to satisfy the graph representation, thus creating a potential
     *                 mismatch between fragment space and graph representation.
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromFile(
            String fileName, String format, boolean useFS)
            throws DENOPTIMException {
        switch (format) {
            case "TXT":
                return DenoptimIO.readDENOPTIMGraphsFromFile(fileName, useFS);

            case "SDF":
                return DenoptimIO.readDENOPTIMGraphsFromSDFile(fileName, useFS);

            case "SER":
                return DenoptimIO.readDENOPTIMGraphsFromSerFile(fileName);
        }
        return new ArrayList<DENOPTIMGraph>();
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a serialized graph.
     *
     * @param fileName the pathname of the file to read.
     *                 mismatch between fragment space and graph representation.
     * @return the list of graphs
     * @throws DENOPTIMException
     */

    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromSerFile(
            String fileName) throws DENOPTIMException {
        ArrayList<DENOPTIMGraph> list = new ArrayList<DENOPTIMGraph>();
        list.add(deserializeDENOPTIMGraph(new File(fileName)));
        return list;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a SDF file
     *
     * @param fileName the pathname of the file to read
     * @param useFS    set to <code>true</code> when there is a defined
     *                 fragment space that contains the fragments used to build the graphs.
     *                 Otherwise, use <code>false</code>. This will create only as many APs as
     *                 needed to satisfy the graph representation, thus creating a potential
     *                 mismatch between fragment space and graph representation.
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromSDFile(
            String fileName, boolean useFS)
            throws DENOPTIMException {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<DENOPTIMGraph>();
        ArrayList<IAtomContainer> mols = DenoptimIO.readSDFFile(fileName);
        int i = 0;
        for (IAtomContainer mol : mols) {
            i++;
            Object prop = mol.getProperty(DENOPTIMConstants.GRAPHTAG);
            if (prop == null) {
                throw new DENOPTIMException("Attempt to load graph form "
                        + "SDF that lacks a '" + DENOPTIMConstants.GRAPHTAG
                        + "' tag. Check molecule " + i);
            }
            DENOPTIMGraph g = GraphConversionTool.getGraphFromString(
                    prop.toString().trim(), useFS);
            lstGraphs.add(g);
        }
        return lstGraphs;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a text file
     *
     * @param fileName the pathname of the file to read
     * @param useFS    set to <code>true</code> when there is a defined
     *                 fragment space that contains the fragments used to build the graphs.
     *                 Otherwise, use <code>false</code>. This will create only as many APs as
     *                 needed to satisfy the graph representation, thus creating a potential
     *                 mismatch between fragment space and graph representation.
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromFile(
            String fileName, boolean useFS) throws DENOPTIMException {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<DENOPTIMGraph>();
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                if (line.startsWith(DENOPTIMConstants.APCMAPIGNORE)) {
                    continue;
                }

                DENOPTIMGraph g;
                try {
                    g = GraphConversionTool.getGraphFromString(
                            line.trim(), useFS);
                } catch (Throwable t) {
                    String msg = "Cannot convert string to DENOPTIMGraph. "
                            + "Check line '" + line.trim() + "'";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg, t);
                }
                lstGraphs.add(g);
            }
        } catch (IOException ioe) {
            String msg = "Cannot read file " + fileName;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg, ioe);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
        return lstGraphs;
    }

//------------------------------------------------------------------------------

    /**
     * Writes the string representation of the graphs to file.
     *
     * @param fileName the file where to print
     * @param graphs   the list of graphs to print
     * @param append   use <code>true</code> to append
     * @throws DENOPTIMException
     */
    public static void writeGraphsToFile(String fileName,
                                         ArrayList<DENOPTIMGraph> graphs, boolean append)
            throws DENOPTIMException {
        StringBuilder sb = new StringBuilder();
        for (DENOPTIMGraph g : graphs) {
            sb.append(g.toString()).append(NL);
        }
        writeData(fileName, sb.toString(), append);
    }

//------------------------------------------------------------------------------

    /**
     * Writes the string representation of a graph to file.
     *
     * @param fileName the file where to print
     * @param graph    the graph to print
     * @param append   use <code>true</code> to append
     * @throws DENOPTIMException
     */
    public static void writeGraphToFile(String fileName, DENOPTIMGraph graph,
                                        boolean append) throws DENOPTIMException {
        writeData(fileName, graph.toString(), append);
    }

//------------------------------------------------------------------------------

    /**
     * Looks for a writable location where to put temporary files and returns
     * an absolute pathname to the folder where tmp files can be created.
     *
     * @return a  writable absolute path
     */
    public static String getTempFolder() {

        ArrayList<String> tmpFolders = new ArrayList<String>();
        tmpFolders.add(System.getProperty("file.separator") + "tmp");
        tmpFolders.add(System.getProperty("file.separator") + "scratch");
        tmpFolders.add(System.getProperty("java.io.tmpdir"));

        String tmpPathName = "";
        String tmpFolder = "";
        for (String t : tmpFolders) {
            tmpFolder = t;
            tmpPathName = tmpFolder + System.getProperty("file.separator")
                    + "Denoptim_tmpFile";
            if (DenoptimIO.canWriteAndReadTo(tmpPathName)) {
                break;
            }
        }
        return tmpFolder;
    }

//------------------------------------------------------------------------------

    /**
     * Check whether we can write and read to a given pathname
     *
     * @param pathName
     * @return <code>true</code> if we can write and read in that pathname
     */
    public static boolean canWriteAndReadTo(String pathName) {
        boolean res = true;
        try {
            DenoptimIO.writeData(pathName, "TEST", false);
            DenoptimIO.readList(pathName);
        } catch (DENOPTIMException e) {
            res = false;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a library of fragments from SDF file and checks that every fragment
     * has the AP tag.
     *
     * @param fileName name of the SDF file
     * @param kindStr  the kind of fragment to be read. Used for logging.
     * @return the list of fragments as atom containers.
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readInLibraryOfFragments(
            String fileName, String kindStr) throws DENOPTIMException {
        ArrayList<IAtomContainer> lib = new ArrayList<IAtomContainer>();
        int i = 0;
        for (IAtomContainer mol : readMoleculeData(fileName)) {
            i++;
            Object ap = mol.getProperty(DENOPTIMConstants.APTAG);
            if (ap == null) {
                DENOPTIMLogger.appLogger.log(Level.WARNING,
                        "No attachment point information for " + kindStr + " "
                                + i + " in file '" + fileName + "'. I'm ignoring it!");
            } else {
                lib.add(mol);
            }
        }
        if (lib.isEmpty()) {
            throw new DENOPTIMException("Scaffold library has no entries.");
        }
        return lib;
    }

//------------------------------------------------------------------------------

}
