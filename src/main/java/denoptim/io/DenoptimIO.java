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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.io.formats.CIFFormat;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Candidate;
import denoptim.graph.CandidateLW;
import denoptim.graph.DGraph;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.json.DENOPTIMgson;
import denoptim.logging.StaticLogger;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphEdit;
import denoptim.utils.GraphUtils;
import denoptim.utils.Randomizer;


/**
 * Utility methods for input/output
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class DenoptimIO
{

    /**
     * File separator from system.
     */
	public static final String FS = System.getProperty("file.separator");
    
	/**
	 * Newline character from system.
	 */
	public static final String NL = System.getProperty("line.separator");
    
    private static final IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();
    
//------------------------------------------------------------------------------
    
    /**
     * Reads any content of a given pathname and tries to read DENOPTIM data 
     * from it. As the type of the returned value if not defined this method
     * is meant of untyped coding context, like the interfacing with Python. 
     * Please, avoid to use it in typed code context, like within JAVA code.
     * @param pathname the pathname to read.
     * @return any DENOPTIM-like data that could be found in the given pathname.
     * @throws Exception if for any reason no data can be returned.
     */
    public static Object readDENOPTIMData(String pathname) 
            throws Exception
    {
        Object data = null;
        File file = new File(pathname);
        FileFormat format = FileFormat.UNRECOGNIZED;
        format = FileUtils.detectFileFormat(file);
        if (format == FileFormat.UNRECOGNIZED)
        {
            throw new UndetectedFileFormatException(file);
        }
        switch (format)
        {
            case VRTXSDF:
            {
                ArrayList<Vertex> vrtxs = readVertexes(file, BBType.FRAGMENT);
                if (vrtxs.size()>1)
                    data = vrtxs;
                else
                    data = vrtxs.get(0);
                break; 
            }
            
            case VRTXJSON:
            {
                ArrayList<Vertex> vrtxs = readVertexes(file, BBType.FRAGMENT);
                if (vrtxs.size()>1)
                    data = vrtxs;
                else
                    data = vrtxs.get(0);
                break;  
            }
                
            case CANDIDATESDF:
            {
                ArrayList<Candidate> cands = readCandidates(file, true);
                if (cands.size()>1)
                    data = cands;
                else
                    data = cands.get(0);
                break;
            }
                
            case GRAPHSDF:
            {
                ArrayList<DGraph> graphs = readDENOPTIMGraphsFromFile(file, 
                        format);
                if (graphs.size()>1)
                    data = graphs;
                else
                    data = graphs.get(0);
                break;
            }
                
            case GRAPHJSON:
            {
                ArrayList<DGraph> graphs = readDENOPTIMGraphsFromFile(file, 
                        format);
                if (graphs.size()>1)
                    data = graphs;
                else
                    data = graphs.get(0);
                break;
            }
            
            case GENSUMMARY:
            {
                List<Candidate> cands = readGenerationFromSummary(file);
                if (cands.size()>1)
                    data = cands;
                else
                    data = cands.get(0);
                break;
            }
                
            default:
                throw new DENOPTIMException("Data from file of format '"
                        + format + "' cannot be loaded yet. Please, contact"
                        + "the development team.");
        }
        return data;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a {@link FileFormat#GENSUMMARY} file and searches all the files
     * defining each member of the population. If the pathnames found in
     * {@link FileFormat#GENSUMMARY} do not exist, this method will try to
     * build the candidate's files pathnames relative to the location of the 
     * {@link FileFormat#GENSUMMARY}. This assumes that the original folder
     * structure of a {@link FileFormat#GA_RUN} has been preserved.
     * 
     * @param file the generation summary file ({@link FileFormat#GENSUMMARY}) 
     * to read
     * @return population members
     * @throws DENOPTIMException
     */
    public static List<Candidate> readGenerationFromSummary(File file) 
            throws DENOPTIMException
    {
        ArrayList<Candidate> cands = new ArrayList<Candidate>();
        List<String> pathnames = readPopulationMemberPathnames(file);
        String genSummaryParentDir = file.getParent(); // GenXXX folder
        for (String candPathname : pathnames)
        {
            File candFile = new File(candPathname);
            if (candFile.exists())
            {
                cands.add(readCandidates(candFile).get(0));
            } else if (genSummaryParentDir!=null) {
                // Extract the possibly non-existing pathname where the
                // candidates files were originally generated
                String runFolderPathname = 
                        candFile.getParentFile() // GenXXX
                        .getParent(); // RUN###
                if (runFolderPathname==null)
                    throw new DENOPTIMException("Unable to find parent "
                            + "folder for '"+ genSummaryParentDir +"'");
                
                String genAndMolPath = candFile.getAbsolutePath()
                        .substring(runFolderPathname.length());
                
                cands.add(readCandidates(new File(
                        genSummaryParentDir+genAndMolPath)).get(0));
            }
        }
        return cands;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a single collection with all atom containers found in a file of
     * any format. CIF files are internally converted to SDF files, causing loss
     * of information beyond molecular structure in Cartesian coordinates. So,
     * if you need more than molecular structure from CIF files, do not use this
     * method.
     * @param file the file to read.
     * @return the list of containers found in the file.
     * @throws IOException if the file is not found or not readable.
     * @throws CDKException if the reading of the file goes wrong.
     */
    public static List<IAtomContainer> readAllAtomContainers(File file) 
            throws IOException, CDKException
    {
        List<IAtomContainer> results = null;
     
        FileReader formatReader = new FileReader(file);
        IChemFormat chemFormat = new FormatFactory().guessFormat(
                new BufferedReader(formatReader));
        formatReader.close();
        
        if (chemFormat instanceof CIFFormat)
        {
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //                            WARNING
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //
            // * CDK's CIFReader is broken (skips lines _audit, AND ignores 
            //   connectivity)
            // * BioJava's fails on CIF files that do not complain to mmCIF 
            //   format. Try this:
            //   CifStructureConverter.fromPath(f.toPath());
            //
            // The workaround unit CDK's implementation is fixed, is to use Jmol
            // to read the CIF and make an SDF that can then be read normally. 
            // In fact, Jmol's reader is more robust than CDK's and more 
            // versatile than BioJava's. 
            // Moreover it reads also the bonds defined in the CIF 
            // (which OpenBabel does not read).
            // Yet, Jmol's methods are not visible and require spinning a 
            // a viewer.
            
            Map<String, Object> info = new Hashtable<String, Object>();
            info.put("adapter", new SmarterJmolAdapter());
            info.put("isApp", false);
            info.put("silent", "");
            Viewer v =  new Viewer(info);
            v.loadModelFromFile(null, file.getAbsolutePath(), null, null,
                    false, null, null, null, 0, " ");
            String tmp = FileUtils.getTempFolder() + FS + "convertedCIF.sdf";
            v.scriptWait("write " + tmp + " as sdf");
            v.dispose();
            
            results = readAllAtomContainers(new File(tmp));
        } else {
            FileReader fileReader = new FileReader(file);
            ReaderFactory readerFact = new ReaderFactory();
            ISimpleChemObjectReader reader = readerFact.createReader(fileReader);
            IChemFile chemFile = (IChemFile) reader.read(
                    (IChemObject) new ChemFile());
            results = ChemFileManipulator.getAllAtomContainers(chemFile);
            fileReader.close();
        }
        
        return results;
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
            List<Vertex> vertexes) throws DENOPTIMException 
    {
        return writeVertexesToFile(file, format, vertexes, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes vertexes to file. Always overwrites.
     *
     * @param file the file where to print
     * @param format how to print vertexes on file
     * @param vertex the vertex to print
     * @throws DENOPTIMException
     */
    public static File writeVertexToFile(File file, FileFormat format,
            Vertex vertex, boolean append) throws DENOPTIMException 
    {
        ArrayList<Vertex> lst = new ArrayList<Vertex>();
        lst.add(vertex);
        return writeVertexesToFile(file, format, lst, append);
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
            List<Vertex> vertexes, boolean append) throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile()+"."+format.getExtension());
        }
        switch (format)
        {
            case VRTXJSON:
                writeVertexesToJSON(file, vertexes, append);
                break;
                
            case VRTXSDF:
                writeVertexesToSDF(file, vertexes, append);
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
            List<Vertex> vertexes) throws DENOPTIMException
    {
        writeVertexesToJSON(file, vertexes, false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Writes vertexes to JSON file. Always overwrites.
     *
     * @param file the file where to print.
     * @param vertexes the list of vertexes to print.
     * @param append use <code>true</code> to append to the existing list of 
     * vertexes found in the file.
     * @throws DENOPTIMException
     */
    public static void writeVertexesToJSON(File file,
            List<Vertex> vertexes, boolean append) throws DENOPTIMException
    {
        Gson writer = DENOPTIMgson.getWriter();
        if (append)
        {
            ArrayList<Vertex> allVertexes = readDENOPTIMVertexesFromJSONFile(
                    file.getAbsolutePath());
            allVertexes.addAll(vertexes);
            writeData(file.getAbsolutePath(), writer.toJson(allVertexes), false);
        } else {
            writeData(file.getAbsolutePath(), writer.toJson(vertexes), false);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes a vertex to an SDF file.
     *
     * @param pathName pathname where to write.
     * @param vertices list of vertices to write.
     * @throws DENOPTIMException
     */
    
    public static void writeVertexToSDF(String pathName, Vertex vertex) 
            throws DENOPTIMException 
    {
        List<Vertex> lst = new ArrayList<Vertex>();
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
            List<Vertex> vertexes, boolean append) 
                    throws DENOPTIMException 
    {
        List<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        for (Vertex v : vertexes) 
        {
            lst.add(v.getIAtomContainer());
        }
        writeSDFFile(file.getAbsolutePath(), lst, append);
    }

//------------------------------------------------------------------------------

    /**
     * Writes {@link IAtomContainer} to SDF file.
     *
     * @param fileName The file to be write to.
     * @param mol     The molecules to be written.
     * @throws DENOPTIMException
     */
    public static void writeSDFFile(String fileName, IAtomContainer mol) 
            throws DENOPTIMException {
        List<IAtomContainer>  mols = new ArrayList<IAtomContainer>();
        mols.add(mol);
        writeSDFFile(fileName, mols, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes {@link IAtomContainer}s to SDF file.
     *
     * @param fileName The file to be write to.
     * @param mols     The molecules to be written.
     * @throws DENOPTIMException
     */
    public static void writeSDFFile(String fileName, List<IAtomContainer> mols) 
            throws DENOPTIMException {
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
    public static void writeSDFFile(String fileName, List<IAtomContainer> mols, 
            boolean append) throws DENOPTIMException 
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
                DENOPTIMConstants.PROVENANCE,
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
                        
                        case DENOPTIMConstants.PROVENANCE:
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
     * {@link FileFormat#GENSUMMARY} file.
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
     * Read the pathnames to the population members from a 
     * {@link FileFormat#GENSUMMARY} file.
     *
     * @param fileName  the pathname to the file to read.
     * @return list of data
     * @throws DENOPTIMException
     */
    public static List<String> readPopulationMemberPathnames(File file) 
            throws DENOPTIMException 
    {
        List<String>  vals = new ArrayList<String>();
        ArrayList<String> txt = readList(file.getAbsolutePath());
        for (String line : txt) 
        {
            if (!line.contains(FS))
                continue;
            
            String[] words = line.trim().split("\\s+");
            if (words.length < 5)
                continue;
            
            // NB: there is no keyword for population members!
            vals.add(words[4]);
        }
        return vals;
    }
    
//------------------------------------------------------------------------------

    /**
     * Read the minimal info that can be found in a
     * {@link FileFormat#GENSUMMARY} file about the members of a population.
     *
     * @param fileName  the pathname to the {@link FileFormat#GENSUMMARY} file 
     * to read.
     * @return list of population members defined to the extent that information
     * is available in the {@link FileFormat#GENSUMMARY} file.
     * @throws DENOPTIMException if the file cannot be read.
     */
    
    public static List<CandidateLW> readPopulationMembersTraces(File file) 
           throws DENOPTIMException 
    {
        List<CandidateLW> members = new ArrayList<CandidateLW>();
        List<String> txt = readList(file.getAbsolutePath());
        
        // Skip the header, i.e., the first line
        for (int i=1; i<txt.size(); i++) 
        {
            String line = txt.get(i);
            
            // WARNING: here we set strong expectation on the format of the 
            // gensummary files!
            
            if (line.startsWith("#"))
                break;
            
            if (line.isBlank())
                continue;
           
            String[] words = line.trim().split("\\s+");
            String pathname = "nofile";
            if (words.length >= 5)
            {
                pathname = words[4];
            }
            CandidateLW member = new CandidateLW(words[2], words[0], pathname);
            member.setFitness(Double.parseDouble(words[3]));
            members.add(member);
        }
        return members;
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
            for (Vertex v : DenoptimIO.readVertexes(fragLib, BBType.UNDEFINED)) 
            {
                for (AttachmentPoint ap : v.getAttachmentPoints()) {
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
        
        // Try to identify the generation at which this candidate was generated
        int genID = -1;
        if (file.getParentFile()!=null 
                && file.getParentFile().getName().startsWith(
                        DENOPTIMConstants.GAGENDIRNAMEROOT))
        {
            String genFolderName = file.getParentFile().getName();
            genID = Integer.valueOf(genFolderName.substring(
                    DENOPTIMConstants.GAGENDIRNAMEROOT.length()));
        }
        
        for (IAtomContainer iac : iacs) {
            Candidate cand = new Candidate(iac, false, allowNoUID);
            cand.setSDFFile(filename);
            if (genID!=-1)
                cand.setGeneration(genID);
            candidates.add(cand);
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
     * Reads a list of graph editing tasks from a JSON file
     *
     * @param fileName the pathname of the file to read
     * @return the list of editing tasks
     * @throws DENOPTIMException
     */
    public static ArrayList<GraphEdit> readDENOPTIMGraphEditFromFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<GraphEdit> graphEditTasks = new ArrayList<>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            graphEditTasks = reader.fromJson(br, 
                    new TypeToken<ArrayList<GraphEdit>>(){}.getType());
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
                GraphEdit graphEditTask = reader.fromJson(br,
                        GraphEdit.class);
                graphEditTasks.add(graphEditTask);
            } else {
                jse.printStackTrace();
                throw new DENOPTIMException("ERROR! Unable to read JSON file "
                        + "that defines a graph enditing task.",jse);
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
     * Reads a list of <{@link DGraph}s from file.
     *
     * @param fileName the pathname of the file to read
     * @return the list of graphs
     * @throws Exception 
     */
    
    public static ArrayList<DGraph> readDENOPTIMGraphsFromFile(File inFile) 
            throws Exception 
    {
        FileFormat ff = FileUtils.detectFileFormat(inFile);
        return readDENOPTIMGraphsFromFile(inFile, ff);
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads a list of {@link DGraph}s from file.
     *
     * @param inFile the file to read
     * @param format the format of the file to read. We assume this has been
     * detected from previous calls of {@link FileUtils#detectFileFormat(File)}
     *  and that now we want to avoid having to call that method again.
     * @return the list of graphs
     * @throws Exception 
     */
    
    public static ArrayList<DGraph> readDENOPTIMGraphsFromFile(File inFile,
            FileFormat format) throws Exception 
    {
        switch (format) 
        {
            case GRAPHJSON:
                return DenoptimIO.readDENOPTIMGraphsFromJSONFile(
                        inFile.getAbsolutePath());

            case GRAPHSDF:
                return DenoptimIO.readDENOPTIMGraphsFromSDFile(
                        inFile.getAbsolutePath());
                
            case GRAPHTXT:
                throw new DENOPTIMException("Use of string representation '" 
                        + DENOPTIMConstants.GRAPHTAG + "' is deprecated. Use "
                        + "JSON format instead.");
                
            case CANDIDATESDF:
                return DenoptimIO.readDENOPTIMGraphsFromSDFile(
                    inFile.getAbsolutePath());
                
            case VRTXSDF:
                ArrayList<DGraph> graphs = new ArrayList<DGraph>();
                ArrayList<Vertex> vertexes = readVertexes(inFile, 
                        BBType.UNDEFINED);
                for (Vertex v : vertexes)
                {
                    if (v instanceof Template)
                    {
                        graphs.add(((Template)v).getInnerGraph());
                    }
                }
                System.out.println("WARNING: Reading graphs from " 
                        + FileFormat.VRTXSDF + " file can only read the "
                        + "templates' inner graphs. Importing " 
                        + graphs.size() + " graphs "
                        + "from " + vertexes.size() + " vertexes.");
                return graphs;
                
            default:
                throw new Exception("Format '" + format + "' could not be used "
                        + "to read graphs from file '" + inFile + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <{@link DGraph}s from a SDF file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DGraph> readDENOPTIMGraphsFromSDFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DGraph> lstGraphs = new ArrayList<DGraph>();
        ArrayList<IAtomContainer> mols = DenoptimIO.readSDFFile(fileName);
        int i = 0;
        for (IAtomContainer mol : mols) 
        {
            i++;
            DGraph g = readGraphFromSDFileIAC(mol,i,fileName);
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
    public static DGraph readGraphFromSDFileIAC(IAtomContainer mol) 
            throws DENOPTIMException
    {
        return readGraphFromSDFileIAC(mol, -1, "");
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
    public static DGraph readGraphFromSDFileIAC(IAtomContainer mol, 
            int molId) throws DENOPTIMException
    {
        return readGraphFromSDFileIAC(mol, molId, "");
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Converts an atom container read in from an SDF file into a graph, if 
     * possible. Otherwise, throws an exception.
     * @param mol the atom container coming from SDF representation
     * @param molId identified used only for logging purposes. If negative it is
     *  ignored.
     * @param fileName a pathname used only for logging errors. This is usually
     * the pathname to the file from which we took the atom container. If empty
     * it is ignored
     * @return the corresponding graph or null.
     * @throws DENOPTIMException is the atom container cannot be converted due
     * to lack of the proper SDF tags, or failure in the conversion.
     */
    public static DGraph readGraphFromSDFileIAC(IAtomContainer mol, 
            int molId, String fileName) throws DENOPTIMException
    {
        // Something very similar is done also in Candidate
        DGraph g = null;
        Object json = mol.getProperty(DENOPTIMConstants.GRAPHJSONTAG);
        if (json == null) {
            Object graphEnc = mol.getProperty(DENOPTIMConstants.GRAPHTAG);
            if (graphEnc!=null)
            {
                throw new DENOPTIMException("Use of '" 
                        + DENOPTIMConstants.GRAPHTAG + "' is deprecated. SDF "
                        + "files containing graphs must include the "
                        + "tag '" + DENOPTIMConstants.GRAPHJSONTAG + "'.");
            }
            String msg = "Attempt to load graph form "
                    + "SDF that has no '" + DENOPTIMConstants.GRAPHJSONTAG 
                    + "' tag.";
            if (molId>-1)
            {
                msg = msg + " Check molecule " + molId;
                if (!fileName.isEmpty())
                {
                    msg = msg + " in the SDF file '" + fileName + "'";
                } else {
                    msg = msg + ".";
                }
            }
            throw new DENOPTIMException(msg);
        } else {
            String js = json.toString();
            try
            {
                g = DGraph.fromJson(js);
            } catch (Exception e)
            {
                String msg = e.getMessage();
                if (molId>-1)
                {
                    msg = msg + " Check molecule " + molId;
                    if (!fileName.isEmpty())
                    {
                        msg = msg + " in the SDF file '" + fileName + "'";
                    } else {
                        msg = msg + ".";
                    }
                }
                throw new DENOPTIMException(msg, e);
            }
        }
        return g;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <{@link DGraph}s from a text file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of graphs.
     * @throws DENOPTIMException
     */
    public static ArrayList<DGraph> readDENOPTIMGraphsFromTxtFile(
            String fileName, FragmentSpace fragSpace, Logger logger)
                    throws DENOPTIMException 
    {
        ArrayList<DGraph> lstGraphs = new ArrayList<DGraph>();
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

                DGraph g;
                try {
                    g = GraphConversionTool.getGraphFromString(line.trim(), 
                            fragSpace);
                } catch (Throwable t) {
                    String msg = "Cannot convert string to DENOPTIMGraph. "
                            + "Check line '" + line.trim() + "'";
                    logger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg, t);
                }
                lstGraphs.add(g);
            }
        } catch (IOException ioe) {
            String msg = "Cannot read file " + fileName;
            logger.log(Level.SEVERE, msg);
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

    //TODO-v3+ this method should be almost a copy of the one working on graphs.
    // It should be possible to have one method do both tasks.
    
    /**
     * Reads a list of {@link Vertex}es from a JSON file.
     * @param fileName the pathname of the file to read.
     * @return the list of vertexes.
     * @throws DENOPTIMException
     */
    public static ArrayList<Vertex>  readDENOPTIMVertexesFromJSONFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<Vertex> result = new ArrayList<Vertex>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            result = reader.fromJson(br, 
                    new TypeToken<ArrayList<Vertex>>(){}.getType());
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
                Vertex v = reader.fromJson(br,Vertex.class);
                result.add(v);
            } else {
                throw new DENOPTIMException("ERROR! Unable to read vertex from '"
                        + fileName + "'.", jse);
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
     * Reads a list of {@link DGraph}s from a JSON file.
     * @param fileName the pathname of the file to read
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DGraph>  readDENOPTIMGraphsFromJSONFile(
            String fileName) throws DENOPTIMException 
    {
        ArrayList<DGraph> list_of_graphs = new ArrayList<DGraph>();
        Gson reader = DENOPTIMgson.getReader();

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            list_of_graphs = reader.fromJson(br, 
                    new TypeToken<ArrayList<DGraph>>(){}.getType());
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
                DGraph g = reader.fromJson(br,DGraph.class);
                list_of_graphs.add(g);
            } else {
                throw new DENOPTIMException("ERROR! Unable to read graph from "
                        + "JSON '" + fileName + "'", jse);
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
     * Writes the a graph to file. Always overwrites.
     *
     * @param file the file where to print.
     * @param format how to print graphs on file.
     * @param graph the graph to print.
     * @throws DENOPTIMException
     */
    public static File writeGraphToFile(File file, FileFormat format,
            DGraph graph, Logger logger, Randomizer randomizer) 
                    throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile()+"."+format.getExtension());
        }
        switch (format)
        {
            case GRAPHJSON:
                writeGraphToJSON(file, graph);
                break;
                
            case GRAPHSDF:
                writeGraphToSDF(file, graph, false, true, logger, randomizer);
                break;
                
            default:
                throw new DENOPTIMException("Cannot write graph with format '" 
                        + format + "'.");
        }
        return file;
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to file. Always overwrites.
     *
     * @param file the file where to print
     * @param format how to print graphs on file
     * @param modGraphs the list of graphs to print
     * @return the reference to the file written.
     * @throws DENOPTIMException
     */
    public static File writeGraphsToFile(File file, FileFormat format,
            List<DGraph> modGraphs, Logger logger, Randomizer randomizer) 
                    throws DENOPTIMException 
    {
        if (FilenameUtils.getExtension(file.getName()).equals(""))
        {
            file = new File(file.getAbsoluteFile()+"."+format.getExtension());
        }
        switch (format)
        {
            case GRAPHJSON:
                writeGraphsToJSON(file, modGraphs);
                break;
                
            case GRAPHSDF:
                writeGraphsToSDF(file, modGraphs, false, true, logger, randomizer);
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
            List<DGraph> graphs, Logger logger, Randomizer randomizer) 
                    throws DENOPTIMException
    {
        writeGraphsToSDF(file, graphs, false, logger, randomizer);
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
    public static void writeGraphToSDF(File file, DGraph graph,
            boolean append, boolean make3D, Logger logger, Randomizer randomizer)
                    throws DENOPTIMException
    {
        List<DGraph> lst = new ArrayList<>(1);
        lst.add(graph);
        writeGraphsToSDF(file, lst, append, make3D, logger, randomizer);
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
    public static void writeGraphToSDF(File file, DGraph graph,
            boolean append, Logger logger, Randomizer randomizer) 
                    throws DENOPTIMException
    {
        ArrayList<DGraph> lst = new ArrayList<>(1);
        lst.add(graph);
        writeGraphsToSDF(file, lst, append, logger, randomizer);
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
            List<DGraph> graphs, boolean append,
            Logger logger, Randomizer randomizer) throws DENOPTIMException
    {
        writeGraphsToSDF(file, graphs, append, false, logger, randomizer);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to SDF file.
     *
     * @param file the file where to print
     * @param modGraphs the list of graphs to print
     * @param append use <code>true</code> to append to the file
     * @param make3D use <code>true</code> to convert graph to 3d. 
     * If false an empty molecule will be used.
     * @throws DENOPTIMException
     */
    public static void writeGraphsToSDF(File file,
            List<DGraph> modGraphs, boolean append, boolean make3D, 
            Logger logger, Randomizer randomizer) throws DENOPTIMException
    {
        ArrayList<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        for (DGraph g : modGraphs) 
        {
            ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, randomizer);
            IAtomContainer iac = builder.newAtomContainer();
            if (make3D)
            {
                try {
                    iac = tb.convertGraphTo3DAtomContainer(g, true);
                } catch (Throwable t) {
                    t.printStackTrace();
                    logger.log(Level.WARNING,"Couldn't make 3D-tree "
                            + "representation: " + t.getMessage());
                }
            } else {
                GraphUtils.writeSDFFields(iac, g);
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
    public static void writeGraphToJSON(File file, DGraph graph) 
            throws DENOPTIMException
    {
        ArrayList<DGraph> graphs = new ArrayList<DGraph>();
        graphs.add(graph);
        writeGraphsToJSON(file, graphs);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes the graphs to JSON file.
     *
     * @param file the file where to print
     * @param modGraphs the list of graphs to print
     * @throws DENOPTIMException
     */
    public static void writeGraphsToJSON(File file,
            List<DGraph> modGraphs) throws DENOPTIMException
    {
        Gson writer = DENOPTIMgson.getWriter();
        writeData(file.getAbsolutePath(), writer.toJson(modGraphs), false);
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
            List<DGraph> graphs, boolean append) throws DENOPTIMException
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
    public static void writeGraphToFile(String fileName, DGraph graph, 
            boolean append) throws DENOPTIMException 
    {
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
            StaticLogger.appLogger.log(Level.WARNING, "WARNING: unable to "
                    + "fetch list of recent files.", e);
            map = new HashMap<File, FileFormat>();
        }
        return map;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads {@link Vertex}es from any file that can contain such items.
     *  
     * @param file the file we want to read.
     * @param bbt the type of building blocks assigned to each new vertex, if
     * not already specified by the content of the file, i.e., this method does
     * not overwrite the {@link Vertex.BBType} defined in the file.
     * @throws IOException 
     * @throws UndetectedFileFormatException 
     * @throws DENOPTIMException 
     * @throws IllegalArgumentException 
     * @throws Exception
     */
    public static ArrayList<Vertex> readVertexes(File file,
            Vertex.BBType bbt) throws UndetectedFileFormatException, 
    IOException, IllegalArgumentException, DENOPTIMException
    {
        ArrayList<Vertex> vertexes = new ArrayList<Vertex>();
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
                ArrayList<DGraph> lstGraphs = 
                    readDENOPTIMGraphsFromSDFile(file.getAbsolutePath());
                for (DGraph g : lstGraphs)
                {
                    Template t = new Template(bbt);
                    t.setInnerGraph(g);
                    vertexes.add(t);
                }
                break;
                    
            case GRAPHJSON:
                ArrayList<DGraph> lstGraphs2 = 
                readDENOPTIMGraphsFromJSONFile(file.getAbsolutePath());
                for (DGraph g : lstGraphs2)
                {
                    Template t = new Template(bbt);
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
     * Reads a list of {@link Vertex}es from a SDF file.
     *
     * @param fileName the pathname of the file to read.
     * @return the list of vertexes.
     * @throws DENOPTIMException
     */
    public static ArrayList<Vertex> readDENOPTIMVertexesFromSDFile(
            String fileName, Vertex.BBType bbt) throws DENOPTIMException 
    {
        ArrayList<Vertex> vertexes = new ArrayList<Vertex>();
        int i=0;
        Gson reader = DENOPTIMgson.getReader();
        for (IAtomContainer mol : readSDFFile(fileName)) 
        {
            i++;
            Vertex v = null;
            try
            {
                v = Vertex.parseVertexFromSDFFormat(mol, reader, bbt);
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
    
    /**
     * Read molecular formula from TXT data representation produced by Cambridge
     * Structural Database tools (such as Conquest). Essentially, this method 
     * reads a text file expecting to find lines as the following among lines 
     * with other kinds of information:
     * <pre>
     * REFCODE: ABEWOT
     * [...]
     * Formula:           C36 H44 Cl1 P2 Ru1 1+,F6 P1 1-
     * </pre>
     * @param file the text file to read
     * @return the mapping of CSD's REFCODE as key to their respective 
     * molecular formula as formatted in the input.
     * @throws DENOPTIMException if any exception occurs during the reading of 
     * the file or if the file does not exist.
     */
    public static LinkedHashMap<String, String> readCSDFormulae(File file) 
            throws DENOPTIMException
    {   
        LinkedHashMap<String, String> allFormulae = new LinkedHashMap<String,String>();
        BufferedReader buffRead = null;
        try {
            //Read the file line by line
            buffRead = new BufferedReader(new FileReader(file));
            String lineAll = null;
            String refcode = "";
            String formula = "";
            while ((lineAll = buffRead.readLine()) != null) 
            {
                String[] lineArgs = lineAll.split(":");
                //Get the name
                if (lineArgs[0].equals("REFCODE")) 
                 refcode = lineArgs[1].trim();

                //Get the formula
                if (lineArgs[0].equals("  Formula")) 
                {
                    formula = lineArgs[1].trim();
                    //Store formula
                    allFormulae.put(refcode,formula);
                    //Clean fields
                    refcode = "";
                    formula = "";
                }
            }
        } catch (FileNotFoundException fnf) {
            throw new DENOPTIMException("File Not Found: " + file, fnf);
        } catch (IOException ioex) {
            throw new DENOPTIMException("Error reading file: " + file, ioex);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException e) {
                throw new DENOPTIMException("Error closing buffer to "+file, e);
            }
        }
       
        return allFormulae;
    }

//------------------------------------------------------------------------------
    
    /**
     * Read cutting rules from a stream
     * @param reader the stream providing text lines.
     * @param cutRules the collector of the cutting rules. The collection is 
     * already sorted by priority when this method returns.
     * @param source the source of text. This is used only in logging as the
     * source of the text that we are trying to convert into cutting rules.
     * For example, a file name. 
     * @throws DENOPTIMException in case there are errors in the formatting of 
     * the text contained in the file.
     */
    public static void readCuttingRules(BufferedReader reader, 
            List<CuttingRule> cutRules, String source) throws DENOPTIMException
    {
        ArrayList<String> cutRulLines = new ArrayList<String>();
        try
        {
            String line = null;
            while ((line = reader.readLine()) != null) 
            {
                if (line.trim().startsWith(DENOPTIMConstants.CUTRULKEYWORD))
                    cutRulLines.add(line.trim());
            }
        } catch (IOException e)
        {
            throw new DENOPTIMException(e);
        } finally {
            if (reader != null)
                try
                {
                    reader.close();
                } catch (IOException e)
                {
                    throw new DENOPTIMException(e);
                }
        }
        readCuttingRules(cutRulLines, cutRules, source);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Read cutting rules from a properly formatted text file.
     * @param file the file to read.
     * @param cutRules the collector of the cutting rules. The collection is 
     * already sorted by priority when this method returns.
     * @throws DENOPTIMException in case there are errors in the formatting of 
     * the text contained in the file.
     */
    public static void readCuttingRules(File file, 
            List<CuttingRule> cutRules) throws DENOPTIMException
    {   
        ArrayList<String> allLines = readList(file.getAbsolutePath());
        
        //Now get the list of cutting rules
        ArrayList<String> cutRulLines = new ArrayList<String>();
        allLines.stream()
            .filter(line -> line.trim().startsWith(
                    DENOPTIMConstants.CUTRULKEYWORD))
            .forEach(line -> cutRulLines.add(line.trim()));
        
        readCuttingRules(cutRulLines, cutRules, "file '" 
                + file.getAbsolutePath()+ "'");
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Read cutting rules from a properly formatted text file.
     * @param cutRulLines text lines containing cutting rules. These are 
     * expected to be lines starting with the 
     * {@link DENOPTIMConstants.CUTRULKEYWORD} keyword.
     * @param cutRules the collector of the cutting rules. The collection is 
     * already sorted by priority when this method returns.
     * @param source the source of text. This is used only in logging as the
     * source of the text that we are trying to convert into cutting rules.
     * For example, a file name. 
     * @throws DENOPTIMException in case there are errors in the formatting of 
     * the text contained in the file.
     */
    public static void readCuttingRules(ArrayList<String> cutRulLines,
            List<CuttingRule> cutRules, String source) throws DENOPTIMException
    {   
        Set<Integer> usedPriorities = new HashSet<Integer>();
        for (int i = 0; i<cutRulLines.size(); i++)
        {
            String[] words = cutRulLines.get(i).split("\\s+");
            String name = words[1]; //name of the rule
            if (words.length < 6)
            {
                throw new DENOPTIMException("ERROR in getting cutting rule."
                        + " Found " + words.length + " parts inctead of 6."
                        + "Check line '" + cutRulLines.get(i) + "'"
                        + "in " + source + ".");
            }

            // further details in map of options
            ArrayList<String> opts = new ArrayList<String>();
            if (words.length >= 7)
            {
                for (int wi=6; wi<words.length; wi++)
                {
                    opts.add(words[wi]);
                }
            }

            int priority = Integer.parseInt(words[2]);
            if (usedPriorities.contains(priority))
            {
                throw new DENOPTIMException("ERROR in getting cutting rule."
                        + " Duplicate priority index " + priority + ". "
                        + "Check line '" + cutRulLines.get(i) + "'"
                        + "in " + source + ".");
            } else {
                usedPriorities.add(priority);
            }

            CuttingRule rule = new CuttingRule(name,
                            words[3],  //atom1
                            words[4],  //atom2
                            words[5],  //bond between 1 and 2
                            priority,  
                            opts);     

            cutRules.add(rule);
        }
        
        Collections.sort(cutRules, new Comparator<CuttingRule>() {

            @Override
            public int compare(CuttingRule r1, CuttingRule r2)
            {
                return Integer.compare(r1.getPriority(), r2.getPriority());
            }
            
        });
    }

//------------------------------------------------------------------------------
    
    /**
     * Writes a formatted text file that collects cutting rule.
     * @param file the file where to write.
     * @param cutRules the cutting rules to write.
     * @throws DENOPTIMException 
     */
    public static void writeCuttingRules(File file,
            List<CuttingRule> cutRules) throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder();
        for (CuttingRule r : cutRules)
        {
            sb.append(DENOPTIMConstants.CUTRULKEYWORD).append(" ");
            sb.append(r.getName()).append(" ");
            sb.append(r.getPriority()).append(" ");
            sb.append(r.getSMARTSAtom0()).append(" ");
            sb.append(r.getSMARTSAtom1()).append(" ");
            sb.append(r.getSMARTSBnd()).append(" ");
            if (r.getOptions()!=null)
            {
                for (String opt : r.getOptions())
                    sb.append(opt).append(" ");
            }
            sb.append(NL);
        }
        writeData(file.getAbsolutePath(), sb.toString(), false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Appends the second file to the first.
     * @param f1 file being elongated
     * @param list files providing the content to place in f1.
     * @throws IOException 
     */
    public static void appendTxtFiles(File f1, List<File> files) throws IOException
    {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw = null;
        try
        {
            fw = new FileWriter(f1, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            for (File inFile : files)
            {
                FileReader fr;
                BufferedReader br = null;
                try
                {
                    fr = new FileReader(inFile);
                    br = new BufferedReader(fr);
                    String line = null;
                    while ((line = br.readLine()) != null) 
                    {
                        pw.println(line);
                    }
                } finally {
                    if (br != null)
                        br.close();
                }
            }
        } finally {
            if (pw!=null)
                pw.close();
        }
    }

//------------------------------------------------------------------------------

}
