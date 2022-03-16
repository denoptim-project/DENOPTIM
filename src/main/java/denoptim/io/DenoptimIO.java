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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.InvPair;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.CandidateLW;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.json.DENOPTIMgson;
import denoptim.logging.DENOPTIMLogger;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMGraphEdit;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;


/**
 * Utility methods for input/output
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class DenoptimIO
{

	public static final String FS = System.getProperty("file.separator");
    public static final String NL = System.getProperty("line.separator");

    // A list of properties used by CDK algorithms which must never be
    // serialized into the SD file format.

    private static final ArrayList<String> cdkInternalProperties
            = new ArrayList<>(Arrays.asList(new String[]
            {InvPair.CANONICAL_LABEL, InvPair.INVARIANCE_PAIR}));
    
    private static final IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();

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
    public static IAtomContainer getFirstMolInSDFFile(String fileName)
            throws DENOPTIMException {
        return readSDFFile(fileName).get(0);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes vertexes to file. Always overwrites.
     *
     * @param file the file where to print
     * @param format how to print vertexes on file
     * @param vertexes the list of vertexes to print
     * @throws DENOPTIMException
     */
    public static File writeVertexesToFile(File file, FileFormat format,
            ArrayList<DENOPTIMVertex> vertexes) throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile()+"."+format.getExtension());
        }
        switch (format)
        {
            case VRTXJSON:
                writeVertexesToJSON(file, vertexes);
                break;
                
            case VRTXSDF:
                writeVertexesToSDF(file, vertexes, false);
                break;
                
            default:
                throw new DENOPTIMException("Cannot write vertexes with format '" 
                        + format + "'.");
        }
        return file;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Writes vertexes to JSON file. Always overwrites.
     *
     * @param file the file where to print.
     * @param vertexes the list of vertexes to print.
     * @throws DENOPTIMException
     */
    public static void writeVertexesToJSON(File file,
            ArrayList<DENOPTIMVertex> vertexes) throws DENOPTIMException
    {
        Gson writer = DENOPTIMgson.getWriter();
        writeData(file.getAbsolutePath(), writer.toJson(vertexes), false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes a vertex to an SDF file.
     *
     * @param pathName pathname where to write.
     * @param vertices list of vertices to write.
     * @throws DENOPTIMException
     */
    
    public static void writeVertexToSDF(String pathName, DENOPTIMVertex vertex) 
            throws DENOPTIMException 
    {
        ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        lst.add(vertex);
        writeVertexesToSDF(new File(pathName), lst, false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Write a list of vertexes to file.
     * 
     * @param file the file to write to.
     * @param vertexes the vertexes to be written.
     * @param append use <code>true</code> to append instead of overwriting.
     * @throws DENOPTIMException
     */
    public static void writeVertexesToSDF(File file, 
            ArrayList<DENOPTIMVertex> vertexes, boolean append) 
                    throws DENOPTIMException 
    {
        ArrayList<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        for (DENOPTIMVertex v : vertexes) 
        {
            lst.add(v.getIAtomContainer());
        }
        writeSDFFile(file.getAbsolutePath(), lst, append);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes {@link IAtomContainer}s to SDF file.
     *
     * @param fileName The file to be write to.
     * @param mols     The molecules to be written.
     * @throws DENOPTIMException
     */
    public static void writeSDFFile(String fileName,
            ArrayList<IAtomContainer> mols) throws DENOPTIMException {
        writeSDFFile(fileName,mols, false);
    }

//------------------------------------------------------------------------------

    /**
     * Writes a set of {@link IAtomContainer}s to SDF file.
     *
     * @param fileName The file to be written to
     * @param mols     The molecules to be written
     * @param append use <code>true</code> to append to the file
     * @throws DENOPTIMException
     */
    public static void writeSDFFile(String fileName,
            ArrayList<IAtomContainer> mols, boolean append) 
                    throws DENOPTIMException 
    {
        SDFWriter sdfWriter = null;
        try {
            IAtomContainerSet molSet = new AtomContainerSet();
            for (int idx = 0; idx < mols.size(); idx++) {
                molSet.addAtomContainer(mols.get(idx));
            }
            sdfWriter = new SDFWriter(new FileWriter(new File(fileName),append));
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
     *  Writes an {@link IAtomContainer} to SDF file.
     *
     * @param fileName The file to be written to
     * @param mol      The molecule to be written
     * @param append
     * @throws DENOPTIMException
     */
    public static void writeSDFFile(String fileName, IAtomContainer mol,
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
     * Write text-like data file.
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
    
    @Deprecated
    public static void serializeToFile(String fileName, Object obj, 
            boolean append) throws DENOPTIMException {
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

    @Deprecated
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

        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Read only selected data from a GA produced items. This is a light-weight
     * reader of SDF files containing an item, which can be successfully or 
     * Unsuccessfully evaluated.
     *
     * @param fileName the pathname to SDF file
     * @return the light-weight item description.
     * @throws DENOPTIMException 
     */
    public static List<CandidateLW> readLightWeightCandidate(File file) 
            throws DENOPTIMException
    {
        List<String> propNames = new ArrayList<String>(Arrays.asList(
                DENOPTIMConstants.UNIQUEIDTAG,
                CDKConstants.TITLE
                ));
        List<String> optionalPropNames = new ArrayList<String>(Arrays.asList(
                DENOPTIMConstants.FITNESSTAG,
                DENOPTIMConstants.MOLERRORTAG,
                DENOPTIMConstants.GMSGTAG,
                DENOPTIMConstants.GRAPHLEVELTAG
                ));
        propNames.addAll(optionalPropNames);
        List<Map<String, Object>> propsPerItem = readSDFProperties(
                file.getAbsolutePath(), propNames);
        
        List<CandidateLW> items = new ArrayList<CandidateLW>();
        for (Map<String, Object> props : propsPerItem)
        {
            Object uidObj = props.get(DENOPTIMConstants.UNIQUEIDTAG);
            if (uidObj==null )
            {
                throw new DENOPTIMException("Cannot create item if SDF tag "
                        + DENOPTIMConstants.UNIQUEIDTAG + " is null!");
            }
            Object nameObj = props.get(CDKConstants.TITLE);
            if (nameObj==null)
            {
                throw new DENOPTIMException("Cannot create item is SDF tag "
                        + CDKConstants.TITLE + " is null!");
            }
            CandidateLW item = new CandidateLW(uidObj.toString(),
                    nameObj.toString(),file.getAbsolutePath());
            
            for (String propName : optionalPropNames)
            {
                Object obj = props.get(propName);
                if (obj != null)
                {
                    switch (propName)
                    {
                        case DENOPTIMConstants.FITNESSTAG:
                            item.setFitness(Double.parseDouble(obj.toString()));
                            break;
                        
                        case DENOPTIMConstants.MOLERRORTAG:
                            item.setError(obj.toString());
                            break;
                        
                        case DENOPTIMConstants.GMSGTAG:
                            item.setGeneratingSource(obj.toString());
                            break;
                            
                        case DENOPTIMConstants.GRAPHLEVELTAG:
                            item.setLevel(Integer.parseInt(obj.toString()));
                            break;
                    }
                }
            }
            items.add(item);
        }
        return items;
    }

//------------------------------------------------------------------------------

    /**
     * Read the min, max, mean, and median of a population from
     * "Gen.*\.txt" file
     *
     * @param fileName  the pathname to the file to read.
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
     * Read list of data as text.
     *
     * @param fileName the pathname to the file to read.
     * @return list of data
     * @throws DENOPTIMException
     */
    public static ArrayList<String> readList(String fileName) 
            throws DENOPTIMException {
        return readList(fileName, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Read list of data as text.
     *
     * @param fileName the pathname to the file to read.
     * @param allowEmpty if <code>true</code> the method is allowed to return
     * an empty list if the file exists and is empty
     * @return list of data
     * @throws DENOPTIMException
     */
    public static ArrayList<String> readList(String fileName, 
            boolean allowEmpty) throws DENOPTIMException {
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

        if (lst.isEmpty() && !allowEmpty) {
            throw new DENOPTIMException("No data found in file: " + fileName);
        }

        return lst;
    }
    
//------------------------------------------------------------------------------

    /**
     * Read text from file.
     *
     * @param fileName  the pathname to the file to read.
     * @return the text.
     * @throws DENOPTIMException
     */
    public static String readText(String fileName) throws DENOPTIMException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                sb.append(line).append(NL);
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

        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Extract selected properties from SDF files.
     * @param pathName to the file to read
     * @param propNames the list of property names to extract. All the rest will be
     * ignored.
     * @return a corresponding map of results.
     * @throws DENOPTIMException if the file cannot be read.
     */
    public static List<Map<String, Object>> readSDFProperties(String pathName, 
            List<String> propNames) throws DENOPTIMException 
    {
        List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
        ArrayList<IAtomContainer> iacs =  DenoptimIO.readSDFFile(pathName);
        for (IAtomContainer iac : iacs)
        {
            Map<String,Object> properties = new HashMap<String,Object>();
            for (String propName : propNames)
            {
                properties.put(propName, iac.getProperty(propName));
            }
            results.add(properties);
        }
        return results;
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

    public static Set<APClass> readAllAPClasses(File fragLib) {
        Set<APClass> allCLasses = new HashSet<APClass>();
        try {
            for (DENOPTIMVertex v : DenoptimIO.readVertexes(fragLib, 
                    BBType.UNDEFINED)) 
            {
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints()) {
                    allCLasses.add(ap.getAPClass());
                }
            }
        } catch (DENOPTIMException | IllegalArgumentException 
                | UndetectedFileFormatException | IOException e) {
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
     * @param capMap     container for the capping rules
     * @param ends     container for the definition of forbidden ends
     */
    public static void writeCompatibilityMatrix(String fileName, 
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<APClass, APClass> capMap,
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
     * @param cappingMap     container for the capping rules
     * @param forbiddenEndList     container for the definition of forbidden ends
     * @throws DENOPTIMException
     */
    public static void readCompatibilityMatrix(String fileName,HashMap<APClass, 
            ArrayList<APClass>> compatMap, 
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
     * @param file         the SDF file to read.
     * @return the list of candidates.
     * @throws DENOPTIMException is something goes wrong while reading the file
     *                           or interpreting its content
     */
    public static ArrayList<Candidate> readCandidates(File file) 
            throws DENOPTIMException {
        return readCandidates(file,false);
    }

//------------------------------------------------------------------------------

    /**
     * Reads SDF files that represent one or more tested or to be tested 
     * candidates. 
     *
     * @param file the SDF file to read.
     * @param allowNoUID use <code>true</code> if candidates should be allowed 
     * to have no unique identifier.
     * @return the list of candidates.
     * @throws DENOPTIMException is something goes wrong while reading the file
     *                           or interpreting its content
     */
    
    public static ArrayList<Candidate> readCandidates(File file, 
            boolean allowNoUID) throws DENOPTIMException {
        String filename = file.getAbsolutePath();
        ArrayList<Candidate> candidates = new ArrayList<>();
        ArrayList<IAtomContainer> iacs = readSDFFile(file.getAbsolutePath());
        for (IAtomContainer iac : iacs) {
            Candidate mol = new Candidate(iac, allowNoUID);
            mol.setSDFFile(filename);
            candidates.add(mol);
        }
        return candidates;
    }

//------------------------------------------------------------------------------

    /**
     * Writes candidate items to file. Always overwrites.
     * @param file the file where to print.
     * @param candidates the list of candidates to print to file.
     * @param append use <code>true</code> to append if the file exist
     * @throws DENOPTIMException
     */
    public static void writeCandidatesToFile(File file, 
            ArrayList<Candidate> candidates, boolean append) 
                    throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile() + "." 
                    + FileFormat.CANDIDATESDF.getExtension());
        }
        ArrayList<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        for (Candidate g : candidates) 
        {
            lst.add(g.getFitnessProviderOutputRepresentation());
        }
        writeSDFFile(file.getAbsolutePath(), lst, append);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes one candidate item to file. Always overwrites.
     * @param file the file where to print.
     * @param candidate the candidate to print to file.
     * @param append use <code>true</code> to append if the file exist
     * @throws DENOPTIMException
     */
    public static void writeCandidateToFile(File file, Candidate candidate,
            boolean append) 
            throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile() + "." 
                    + FileFormat.CANDIDATESDF.getExtension());
        }
        writeSDFFile(file.getAbsolutePath(), 
                candidate.getFitnessProviderOutputRepresentation(), append);
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
     * Reads a list of graph editing tasks from a JSON file
     *
     * @param fileName the pathname of the file to read
     * @return the list of editing tasks
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraphEdit> readDENOPTIMGraphEditFromFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DENOPTIMGraphEdit> graphEditTasks = new ArrayList<>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            graphEditTasks = reader.fromJson(br, 
                    new TypeToken<ArrayList<DENOPTIMGraphEdit>>(){}.getType());
        }
        catch (FileNotFoundException fnfe)
        {
            throw new DENOPTIMException("File '" + fileName + "' not found.");
        }
        catch (JsonSyntaxException jse)
        {
            String msg = "Expected BEGIN_ARRAY but was BEGIN_OBJECT";
            if (jse.getMessage().contains(msg))
            {
                // The file contains a single object, not a list. We try to read
                // that single object as a DENOPTIMGraph
                try 
                {
                    br.close();
                    br = new BufferedReader(new FileReader(fileName));
                }
                catch (FileNotFoundException fnfe)
                {
                    //cannot happen
                } catch (IOException ioe) 
                {
                    throw new DENOPTIMException(ioe);
                }
                DENOPTIMGraphEdit graphEditTask = reader.fromJson(br,
                        DENOPTIMGraphEdit.class);
                graphEditTasks.add(graphEditTask);
            }
        }
        finally 
        {
            try {
                if (br != null)
                {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
        
        return graphEditTasks;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from file.
     *
     * @param fileName the pathname of the file to read
     * @return the list of graphs
     * @throws Exception 
     */
    
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromFile(
            File inFile) throws Exception 
    {
        FileFormat ff = FileUtils.detectFileFormat(inFile);
        switch (ff) 
        {
            case GRAPHJSON:
                return DenoptimIO.readDENOPTIMGraphsFromJSONFile(
                        inFile.getAbsolutePath());

            case GRAPHSDF:
                return DenoptimIO.readDENOPTIMGraphsFromSDFile(
                        inFile.getAbsolutePath());
                
            case GRAPHTXT:
                return DenoptimIO.readDENOPTIMGraphsFromTxtFile(
                        inFile.getAbsolutePath());
                
            case CANDIDATESDF:
                return DenoptimIO.readDENOPTIMGraphsFromSDFile(
                    inFile.getAbsolutePath());
                
            case VRTXSDF:
                ArrayList<DENOPTIMGraph> graphs = new ArrayList<DENOPTIMGraph>();
                ArrayList<DENOPTIMVertex> vertexes = 
                        readVertexes(inFile, BBType.UNDEFINED);
                for (DENOPTIMVertex v : vertexes)
                {
                    if (v instanceof DENOPTIMTemplate)
                    {
                        graphs.add(((DENOPTIMTemplate)v).getInnerGraph());
                    }
                }
                System.out.println("WARNING: Reading graphs from " 
                        + FileFormat.VRTXSDF + " file can only read the "
                        + "templates' inner graphs. Importing " 
                        + graphs.size() + " graphs "
                        + "from " + vertexes.size() + " vertexes.");
                return graphs;
                
            default:
                throw new Exception("Format '" + ff + "' could not be used to "
                        + "read graphs from file '" + inFile + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a SDF file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromSDFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<DENOPTIMGraph>();
        ArrayList<IAtomContainer> mols = DenoptimIO.readSDFFile(fileName);
        int i = 0;
        for (IAtomContainer mol : mols) 
        {
            i++;
            DENOPTIMGraph g = readGraphFromSDFileIAC(mol,i,fileName);
            lstGraphs.add(g);
        }
        return lstGraphs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Converts an atom container read in from an SDF file into a graph, if 
     * possible. Otherwise, throws an exception.
     * @param mol the atom container coming from SDF representation
     * @param molId identified used only for logging purposed
     * @return the corresponding graph or null.
     * @throws DENOPTIMException is the atom container cannot be converted due
     * to lack of the proper SDF tags, or failure in the conversion.
     */
    public static DENOPTIMGraph readGraphFromSDFileIAC(IAtomContainer mol, 
            int molId) throws DENOPTIMException
    {
        return readGraphFromSDFileIAC(mol, molId, "unKnown");
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Converts an atom container read in from an SDF file into a graph, if 
     * possible. Otherwise, throws an exception.
     * @param mol the atom container coming from SDF representation
     * @param molId identified used only for logging purposes.
     * @param fileName a pathname used only for logging errors. This is usually
     * the pathname to the file from which we took the atom container.
     * @return the corresponding graph or null.
     * @throws DENOPTIMException is the atom container cannot be converted due
     * to lack of the proper SDF tags, or failure in the conversion.
     */
    public static DENOPTIMGraph readGraphFromSDFileIAC(IAtomContainer mol, 
            int molId, String fileName) throws DENOPTIMException
    {
        // Something very similar is done also in Candidate
        DENOPTIMGraph g = null;
        Object json = mol.getProperty(DENOPTIMConstants.GRAPHJSONTAG);
        Object graphEnc = mol.getProperty(DENOPTIMConstants.GRAPHTAG);
        if (graphEnc == null && json == null) {
            throw new DENOPTIMException("Attempt to load graph form "
                    + "SDF that has neither '" + DENOPTIMConstants.GRAPHTAG
                    + "' nor '" + DENOPTIMConstants.GRAPHJSONTAG 
                    + "' tag. Check molecule " + molId + " in the SDF file.");
        } else if (json != null) {
            String js = json.toString();
            try
            {
                g = DENOPTIMGraph.fromJson(js);
            } catch (Exception e)
            {
                throw new DENOPTIMException(e.getMessage()+" Check file '" 
                        + fileName + "'", e);
            }
        } else {
            g = GraphConversionTool.getGraphFromString(
                    graphEnc.toString().trim());
        }
        return g;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a text file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of graphs.
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromTxtFile(
            String fileName) throws DENOPTIMException 
    {
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
                    g = GraphConversionTool.getGraphFromString(line.trim());
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

    //TODO-v3+ this method should is almost a copy of the one working on graphs.
    // It should be possible to have one method do both tasks.
    
    /**
     * Reads a list of {@link DENOPTIMVertex}es from a JSON file.
     * @param fileName the pathname of the file to read.
     * @return the list of vertexes.
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMVertex>  readDENOPTIMVertexesFromJSONFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DENOPTIMVertex> result = new ArrayList<DENOPTIMVertex>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            result = reader.fromJson(br, 
                    new TypeToken<ArrayList<DENOPTIMVertex>>(){}.getType());
        }
        catch (FileNotFoundException fnfe)
        {
            throw new DENOPTIMException("File '" + fileName + "' not found.");
        }
        catch (JsonSyntaxException jse)
        {
            String msg = "Expected BEGIN_ARRAY but was BEGIN_OBJECT";
            if (jse.getMessage().contains(msg))
            {
                // The file contains a single object, not a list. We try to read
                // that single object as a DENOPTIMVertex
                try 
                {
                    br.close();
                    br = new BufferedReader(new FileReader(fileName));
                }
                catch (FileNotFoundException fnfe)
                {
                    //cannot happen
                } catch (IOException ioe) 
                {
                    throw new DENOPTIMException(ioe);
                }
                DENOPTIMVertex v = reader.fromJson(br,DENOPTIMVertex.class);
                result.add(v);
            }
        }
        finally 
        {
            try {
                if (br != null)
                {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a JSON file.
     * @param fileName the pathname of the file to read
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph>  readDENOPTIMGraphsFromJSONFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DENOPTIMGraph> list_of_graphs = new ArrayList<DENOPTIMGraph>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            list_of_graphs = reader.fromJson(br, 
                    new TypeToken<ArrayList<DENOPTIMGraph>>(){}.getType());
        }
        catch (FileNotFoundException fnfe)
        {
            throw new DENOPTIMException("File '" + fileName + "' not found.");
        }
        catch (JsonSyntaxException jse)
        {
            String msg = "Expected BEGIN_ARRAY but was BEGIN_OBJECT";
            if (jse.getMessage().contains(msg))
            {
                // The file contains a single object, not a list. We try to read
                // that single object as a DENOPTIMGraph
                try 
                {
                    br.close();
                    br = new BufferedReader(new FileReader(fileName));
                }
                catch (FileNotFoundException fnfe)
                {
                    //cannot happen
                } catch (IOException ioe) 
                {
                    throw new DENOPTIMException(ioe);
                }
                DENOPTIMGraph g = reader.fromJson(br,DENOPTIMGraph.class);
                list_of_graphs.add(g);
            }
        }
        finally 
        {
            try {
                if (br != null)
                {
                    br.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }

        return list_of_graphs;
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to file. Always overwrites.
     *
     * @param file the file where to print
     * @param format how to print graphs on file
     * @param graphs the list of graphs to print
     * @throws DENOPTIMException
     */
    public static File writeGraphsToFile(File file, FileFormat format,
            ArrayList<DENOPTIMGraph> graphs) throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile()+"."+format.getExtension());
        }
        switch (format)
        {
            case GRAPHJSON:
                writeGraphsToJSON(file, graphs);
                break;
                
            case GRAPHSDF:
                writeGraphsToSDF(file, graphs, false, true);
                break;
                
            default:
                throw new DENOPTIMException("Cannot write graphs with format '" 
                        + format + "'.");
        }
        return file;
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to SDF file.
     *
     * @param file the file where to print
     * @param graphs the list of graphs to print
     * @throws DENOPTIMException
     */
    public static void writeGraphsToSDF(File file,
            ArrayList<DENOPTIMGraph> graphs) throws DENOPTIMException
    {
        writeGraphsToSDF(file, graphs, false);
    }

//------------------------------------------------------------------------------

    /**
     * Writes the graph to SDF file.
     *
     * @param file the file where to print.
     * @param graph the of graph to print.
     * @param append use <code>true</code> to append to the file.
     * @param make3D use <code>true</code> to convert graph to 3d. 
     * @throws DENOPTIMException
     */
    public static void writeGraphToSDF(File file, DENOPTIMGraph graph,
            boolean append, boolean make3D) throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraph> lst = new ArrayList<>(1);
        lst.add(graph);
        writeGraphsToSDF(file, lst, append, make3D);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graph to SDF file.
     *
     * @param file the file where to print
     * @param graph the of graph to print
     * @param append use <code>true</code> to append to the file
     * @throws DENOPTIMException
     */
    public static void writeGraphToSDF(File file, DENOPTIMGraph graph,
            boolean append) throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraph> lst = new ArrayList<>(1);
        lst.add(graph);
        writeGraphsToSDF(file, lst, append);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to SDF file.
     *
     * @param file the file where to print
     * @param graphs the list of graphs to print
     * @param append use <code>true</code> to append to the file
     * @throws DENOPTIMException
     */
    public static void writeGraphsToSDF(File file,
            ArrayList<DENOPTIMGraph> graphs, boolean append) throws DENOPTIMException
    {
        writeGraphsToSDF(file, graphs, append, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to SDF file.
     *
     * @param file the file where to print
     * @param graphs the list of graphs to print
     * @param append use <code>true</code> to append to the file
     * @param make3D use <code>true</code> to convert graph to 3d. 
     * If false an empty molecule will be used.
     * @throws DENOPTIMException
     */
    public static void writeGraphsToSDF(File file,
            ArrayList<DENOPTIMGraph> graphs, boolean append, boolean make3D) 
                    throws DENOPTIMException
    {
        ArrayList<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        for (DENOPTIMGraph g : graphs) 
        {
            ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder();
            IAtomContainer iac = builder.newAtomContainer();
            if (make3D)
            {
                try {
                    iac = tb.convertGraphTo3DAtomContainer(g,true);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.out.println("Couldn't make 3D-tree representation: "
                            + t.getMessage());
                }
            } else {
                GraphUtils.writeSDFFields(iac,g);
            }
            lst.add(iac);
        }
        writeSDFFile(file.getAbsolutePath(), lst, append);
    }

//------------------------------------------------------------------------------

    /**
     * Writes the graph to JSON file.
     *
     * @param file the file where to print
     * @param graph the graphs to print
     * @throws DENOPTIMException
     */
    public static void writeGraphToJSON(File file, DENOPTIMGraph graph) 
            throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraph> graphs = new ArrayList<DENOPTIMGraph>();
        graphs.add(graph);
        writeGraphsToJSON(file,graphs);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to JSON file.
     *
     * @param file the file where to print
     * @param graphs the list of graphs to print
     * @throws DENOPTIMException
     */
    public static void writeGraphsToJSON(File file,
            ArrayList<DENOPTIMGraph> graphs) throws DENOPTIMException
    {
        Gson writer = DENOPTIMgson.getWriter();
        writeData(file.getAbsolutePath(), writer.toJson(graphs), false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to JSON file.
     *
     * @param file the file where to print
     * @param graphs the list of graphs to print
     * @param append   use <code>true</code> to append
     * @throws DENOPTIMException
     */
    public static void writeGraphsToJSON(File file,
            ArrayList<DENOPTIMGraph> graphs, boolean append) throws DENOPTIMException
    {
        Gson writer = DENOPTIMgson.getWriter();
        writeData(file.getAbsolutePath(), writer.toJson(graphs), append);
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
     * Reads the file defined in {@link DENOPTIMConstants#RECENTFILESLIST} and
     * makes a map that contains the pathname and the format of each recent 
     * file. Ignores any entry that is present in the file but cannot be found 
     * in the system.
     * @return the recent files map
     */
    public static Map<File, FileFormat> readRecentFilesMap()
    {
        Map<File, FileFormat> map = new LinkedHashMap<File, FileFormat>();
        if (!DENOPTIMConstants.RECENTFILESLIST.exists())
        {
            return map;
        }
        try
        {
            for (String line : DenoptimIO.readList(
                    DENOPTIMConstants.RECENTFILESLIST.getAbsolutePath(), true))
            {
                line = line.trim();
                String[] parts = line.split("\\s+");
                String ffStr = parts[0];
                FileFormat ff = null;
                try
                {
                    ff = FileFormat.valueOf(FileFormat.class, ffStr);
                } catch (Exception e)
                {
                    throw new DENOPTIMException("Unable to convert '" + ffStr
                            + "' to a known file format.");
                }
                String fileName = line.substring(ffStr.length()).trim();
                if (FileUtils.checkExists(fileName))
                {
                    map.put(new File(fileName), ff);
                }
            }
        } catch (DENOPTIMException e)
        {
            DENOPTIMLogger.appLogger.log(Level.WARNING, "WARNING: unable to "
                    + "fetch list of recent files.", e);
            map = new HashMap<File, FileFormat>();
        }
        return map;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads {@link DENOPTIMVertex}es from any file that can contain such items.
     *  
     * @param file the file we want to read.
     * @param bbt the type of building blocks assigned to each new vertex, if
     * not already specified by the content of the file, i.e., this method does
     * not overwrite the {@link DENOPTIMVertex.BBType} defined in the file.
     * @throws IOException 
     * @throws UndetectedFileFormatException 
     * @throws DENOPTIMException 
     * @throws IllegalArgumentException 
     * @throws Exception
     */
    public static ArrayList<DENOPTIMVertex> readVertexes(File file,
            DENOPTIMVertex.BBType bbt) throws UndetectedFileFormatException, 
    IOException, IllegalArgumentException, DENOPTIMException
    {
        ArrayList<DENOPTIMVertex> vertexes = new ArrayList<DENOPTIMVertex>();
        FileFormat ff = FileUtils.detectFileFormat(file);
        switch (ff)
        {
            case VRTXSDF:
                vertexes = readDENOPTIMVertexesFromSDFile(
                        file.getAbsolutePath(),bbt);
                break;
                
            case VRTXJSON:
                vertexes = readDENOPTIMVertexesFromJSONFile(
                        file.getAbsolutePath());
                break;
                    
            case GRAPHSDF:
                ArrayList<DENOPTIMGraph> lstGraphs = 
                    readDENOPTIMGraphsFromSDFile(file.getAbsolutePath());
                for (DENOPTIMGraph g : lstGraphs)
                {
                    DENOPTIMTemplate t = new DENOPTIMTemplate(bbt);
                    t.setInnerGraph(g);
                    vertexes.add(t);
                }
                break;
                    
            case GRAPHJSON:
                ArrayList<DENOPTIMGraph> lstGraphs2 = 
                readDENOPTIMGraphsFromJSONFile(file.getAbsolutePath());
                for (DENOPTIMGraph g : lstGraphs2)
                {
                    DENOPTIMTemplate t = new DENOPTIMTemplate(bbt);
                    t.setInnerGraph(g);
                    vertexes.add(t);
                }
                break;
                
            default:
                throw new DENOPTIMException("Format '" + ff 
                        + "' could not be used to "
                        + "read in vertices from file '" + file + "'.");
        }
        return vertexes;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a list of  {@link DENOPTIMVertex}es from a SDF file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of vertexes.
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMVertex> readDENOPTIMVertexesFromSDFile(
            String fileName, DENOPTIMVertex.BBType bbt) throws DENOPTIMException 
    {
        ArrayList<DENOPTIMVertex> vertexes = new ArrayList<DENOPTIMVertex>();
        int i=0;
        Gson reader = DENOPTIMgson.getReader();
        for (IAtomContainer mol : readSDFFile(fileName)) 
        {
            i++;
            DENOPTIMVertex v = null;
            try
            {
                v = DENOPTIMVertex.parseVertexFromSDFFormat(mol, reader, bbt);
            } catch (DENOPTIMException e)
            {
                throw new DENOPTIMException("Unable to read vertex " + i 
                        + " in file " + fileName,e);
            }
            vertexes.add(v);
        }
        return vertexes;
    }

//------------------------------------------------------------------------------

}