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

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.smiles.SmilesGenerator;



/**
 * This app generates a browser driven overview of the DENOPTIM run. 
 * @author Vishwesh Venkatraman
 */
public class GenDENOPTIMFTree
{
    private static final Logger LOGGER = Logger.getLogger(GenDENOPTIMFTree.class.getName());
    private static final SmilesGenerator SMGEN = SmilesGenerator.unique();
    

    Options opts = new Options();

    private String inputDir = "";
    private String outputFileName = "";
    private String amolid = "";
    private String jsFile = "";
    private String cssFile = "";
    private int precisionLevel = 5;
    private String fragsFile = "";
    private boolean genDot = false;
    private boolean bygen = false;


    private static final DecimalFormat DFORMAT = new DecimalFormat();

//------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        GenDENOPTIMFTree gendtr = new GenDENOPTIMFTree();

        gendtr.setUp();
        gendtr.parseCommandLine(args);
        gendtr.checkOptions();

        try
        {
            // start processing of data
            ArrayList<MolData> lstMoldata = gendtr.processInput();
            
            if (gendtr.fragsFile.length() > 0)
            {
                ArrayList<IAtomContainer> mols = IO.readFragments(gendtr.fragsFile);
                
                int ndigits = String.valueOf(mols.size()).length(), k = 0;
                
                for (IAtomContainer cmol:mols)
                {
                    String fragname = String.format("%1$-10s", "F" + 
                        Utils.getPaddedString(ndigits, k+1));
                    
                    gendtr.evaluateFragment(cmol, fragname, lstMoldata);
                    k++;
                }
            }
            
            if (gendtr.amolid.length() > 0)
            {
                ArrayList<String> lstvisited = new ArrayList<>();
                
                gendtr.traceAncestry(gendtr.amolid, lstMoldata, lstvisited);
                
                StringBuilder sb = new StringBuilder(1024);
                sb.append("digraph G {").append(System.getProperty("line.separator"));
                
                
                for (String str:lstvisited)
                {
                    sb.append("\t").append(str).append(System.getProperty("line.separator"));
                }
                sb.append("}");
                
                // write to file
                String dotfile = gendtr.outputFileName + File.separator + 
                        gendtr.amolid + ".dot";
                IO.writeFile(dotfile, sb.toString());
                
                sb.setLength(0);
            }
            
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
            System.err.println("Exiting ...");
            System.exit(-1);
        }

        System.exit(0);
    }
    
    
//------------------------------------------------------------------------------

    /**
     * Recursive iterate through the hierarchical family tree for the given molecule
     * @param query
     * @param lstMoldata
     * @param lstvisited 
     */
    
    private void traceAncestry(String query, ArrayList<MolData> lstMoldata, 
        ArrayList<String> lstvisited)
    {
        int idx = getIndex(lstMoldata, query);
        
        if (idx == -1)
        {
            return;
        }
        
        MolData cmol = lstMoldata.get(idx);
        // add molecule
        StringBuilder sb = new StringBuilder(256);
        sb.append("\t");
        sb.append(cmol.getMolID()).append(" [shape=ellipse, color=black, style=bold, label=\"");
        sb.append("Gen ").append(cmol.getGeneration()).append("\\n")
            .append(cmol.getMolID()).append("\\n")
            .append(" ").append(DFORMAT.format(cmol.getFitness()));
        //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
        sb.append("\"").append(", image=\"").append(cmol.getImgFile())
            .append("\", labelloc=b];").append(System.getProperty("line.separator"));
        lstvisited.add(sb.toString().trim());
        sb.setLength(0);
        
        String conn;
        
        if (cmol.getParent_X_molID().length() > 0)
        {
            // add connection
            if (cmol.getParent_Y_molID().length() == 0)
            {
                conn = "\t" + cmol.getParent_X_molID() + " -> " +
                    cmol.getMolID() + " [style=bold, color=red]; " + 
                    System.getProperty("line.separator");
            }
            else
            {
                conn = "\t" + cmol.getParent_X_molID() + " -> " +
                    cmol.getMolID() + " [style=bold, color=blue]; " + 
                    System.getProperty("line.separator");
            }
            lstvisited.add(conn);
            traceAncestry(cmol.getParent_X_molID(), lstMoldata, lstvisited);
        }
        if (cmol.getParent_Y_molID().length() > 0)
        {
            conn = "\t" + cmol.getParent_Y_molID() + " -> " +
                cmol.getMolID() + " [style=bold, color=blue]; " + 
                    System.getProperty("line.separator");
            lstvisited.add(conn);
            traceAncestry(cmol.getParent_Y_molID(), lstMoldata, lstvisited);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * 
     * @param frag
     * @param lstMoldata 
     */
    
    private void evaluateFragment(IAtomContainer query, String fragname, 
        ArrayList<MolData> lstMoldata)
    {
        int hits = 0;
        Pattern pattern = VentoFoggia.findSubstructure(query);
        
        ArrayList<Double> lstFitness = new ArrayList<>();
        
        for (MolData cmol:lstMoldata)
        {
            IAtomContainer mol = cmol.getMolecule();
            int[] match = pattern.match(mol);
            if (match.length > 0)
            {
                hits++;
                lstFitness.add(cmol.getFitness());
            }
        }
        
        double stdev = Utils.findDeviation(lstFitness);
        double mean = Utils.findMean(lstFitness);
        double max = Collections.max(lstFitness);
        double min = Collections.min(lstFitness);
        
        StringBuilder sb = new StringBuilder(512);
        
        
        sb.append(fragname).append(DFORMAT.format(min)).append("\t").
            append(DFORMAT.format(mean)).append("\t").append(DFORMAT.format(stdev)).
            append("\t").append(DFORMAT.format(max)).append("\t").append(hits).
            append(System.getProperty("line.separator"));
        
        System.err.println(sb.toString().trim());
    }

//------------------------------------------------------------------------------

    /**
     * 
     * @return
     * @throws Exception 
     */
    private ArrayList<MolData> processInput() throws Exception
    {
        String smiles, molname, gmsg, imgloc, imgname, molerr, molhtmlfile;
        File imgfl;
        double fitness;
        int gen;
        
        ArrayList<MolData> lstMols = null;

        try
        {
            // obtain contents of the main level folder
            ArrayList<File> fldrcontents = listFolderContents(inputDir);

            // create a directory where all data is placed
            File fl = new File(outputFileName);
            if (!fl.exists())
                fl.mkdir();

            // create a directory to store the image output
            if (fl.exists())
            {
                imgfl = new File(outputFileName + File.separator + "images");
                imgfl.mkdir();
            }

            // write a html file with the directory name
            String htmlfile = outputFileName + File.separator + "index.html";
            
            if (this.cssFile.length() > 0 && this.jsFile.length() > 0)
            {
                // copy the files to the output folder
                File csFl = new File(cssFile);
                File jsFl = new File(jsFile);
                
                //System.err.println(csFl.getName());
                //System.err.println(jsFl.getName());
                
                IO.copyFile(cssFile, outputFileName + File.separator + csFl.getName());
                IO.copyFile(jsFile, outputFileName + File.separator + jsFl.getName());
                
                cssFile = csFl.getName();
                jsFile = jsFl.getName();
            }
            

            // container to hold all data
            lstMols = new ArrayList<>();

            for (File curfile:fldrcontents)
            {
                // for the current file, obtain contents
                if (curfile.isDirectory())
                {
                    System.err.println(curfile.getName());
                    // skip if folder starts with F, i.e. Final
                    if (! curfile.getName().startsWith("Gen"))
                        continue;

                    gen = Integer.parseInt(curfile.getName().substring(3));

                    ArrayList<File> contents_cfile = listFolderContents(curfile);
                    for (File xfile:contents_cfile)
                    {
                        if (xfile.getName().endsWith("_FIT.sdf"))
                        {
                            // read file, check if all ok with file, generate image
                            System.err.println("\t" + xfile.getName());

                            IAtomContainer mol = 
                                IO.readSingleSDFFile(xfile.getAbsolutePath());

                            // check for errors in the molecule if any
                            molerr = mol.getProperty(DENOPTIMConstants.MOLERRORTAG);

                            // if no error, create an image
                            if (molerr == null && mol.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
                            {
                                //molname = mol.getProperty("cdk:Title");
                                molname = xfile.getName().substring(0, xfile.getName().indexOf("_FIT.sdf"));
                                //System.err.println("\t" + molname);
                                

                                smiles = SMGEN.create(mol);

                                fitness = Double.valueOf(mol.getProperty(DENOPTIMConstants.FITNESSTAG).toString());
                                
                                imgloc = outputFileName + File.separator + "images";

                                imgname = molname + ".svg";

                                // create an svg file of the molecule
                                IO.writeImage(smiles, "svg", imgloc + File.separator + imgname);
                                
                                // declare instance and add details
                                MolData mdata;

                                // write if chemdoodle components are supplied
                                if (this.cssFile.length() > 0 &&
                                    this.jsFile.length() > 0)
                                {
                                    // create the HTML file for the molecule
                                    molhtmlfile = outputFileName + File.separator 
                                        + molname + ".html";                            
                                    HTMLUtils.moleculeToHTML(molhtmlfile, molname, mol, this.cssFile, this.jsFile);
                                    
                                    // store the mol
                                    mdata = new MolData(molname, 
                                        "images" + File.separator + imgname,
                                        molname + ".html", fitness, gen, mol);
                                }
                                else
                                {
                                    mdata = new MolData(molname, 
                                        "images" + File.separator + imgname,
                                        "", fitness, gen, mol);
                                }


                                gmsg = mol.getProperty(DENOPTIMConstants.GMSGTAG);
                                if (gmsg != null)
                                {
                                    // check if info about parents exists
                                    if (! gmsg.startsWith("NEW"))
                                    {
                                        if (gmsg.startsWith(Constants.XOVER_TAG))
                                        {
                                            String[] tokns = gmsg.split(":|\\||=");
                                            String X = tokns[1].substring(0, 
                                                tokns[1].indexOf("_FIT")).trim();
                                            
                                            String Y = tokns[3].substring(0, 
                                                tokns[3].indexOf("_FIT")).trim();
                                            mdata.setParent_X_molID(X);
                                            mdata.setParent_Y_molID(Y);
                                        }
                                        else if (gmsg.startsWith(Constants.MUTATION_TAG))
                                        {
                                            String[] tokns = gmsg.split(":|\\||=");
                                            String X = tokns[1].substring(0, 
                                                tokns[1].indexOf("_FIT")).trim();
                                            mdata.setParent_X_molID(X);
                                        }
                                    }
                                }
                                lstMols.add(mdata);
                            }
                        }
                    }                    
                }
            }
            
            if (lstMols.isEmpty())
            {
                throw new Exception("No relevant data in " + inputDir);
            }
            
            fldrcontents.clear();
            fldrcontents = null;

            // draw the family tree
            if (genDot)
            {
                String dotFile = outputFileName + File.separator + "family.dot";
                getDirectedFamilyTree(lstMols, dotFile);
            }
            
            if (bygen)
            {
                Collections.sort(lstMols);
                getGenerationTree(lstMols, outputFileName);
            }

            // write a html file summarizing the generational details
            HTMLUtils.createHTML(htmlfile, outputFileName, lstMols, DFORMAT, 
                cssFile, jsFile);
        }
        catch (Exception ex)
        {
            throw ex;
        }
        
        return lstMols;
    }


    
//------------------------------------------------------------------------------

    /**
     * For the molecules in the list, at each generation except the first (zeroeth) 
     * find the parents and create e GraphViz dot file.
     * @param lstMols
     * @param fldrname
     * @throws Exception 
     */
    
    private void getGenerationTree(ArrayList<MolData> lstMols, String fldrname) throws Exception
    {
        int oldGen = 1, curGen = 0, maxGen = Integer.MIN_VALUE;
        
        for (MolData mol:lstMols)
        {
            if (mol.getGeneration() >= maxGen)
                maxGen = mol.getGeneration();
        }
        
        int ndigits = String.valueOf(maxGen).length();
        
        ArrayList<Integer> clst = new ArrayList<>();
        
        for (int i=0; i<lstMols.size(); i++)
        {
            MolData mol = lstMols.get(i);
            curGen = mol.getGeneration();
            
            // skip the 0th generation
            if (curGen == 0)
                continue;
            
            //System.err.println("Gen: " + oldGen + " " + curGen);
            
            if (curGen == oldGen)
            {
                clst.add(i);
            }
            else
            {
                //Utils.pause();
                if (clst.size() > 0)
                {
                    String dotfile = fldrname + File.separator + "G" + 
                        Utils.getPaddedString(ndigits, oldGen) + ".dot";
                    // create the dot file for the current generation
                    createDOTForGeneration(lstMols, clst, dotfile);
                    clst.clear();
                }
                oldGen = curGen;
                clst.add(i);
            }
        }
        
        // the last one
        if (clst.size() > 0)
        {
            String dotfile = fldrname + File.separator + "G" + 
                Utils.getPaddedString(ndigits, oldGen) + ".dot";
            // create the dot file for the current generation
            createDOTForGeneration(lstMols, clst, dotfile);
            clst.clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Green indicates a man-woman (parent relation), Blue (man-children),
     * Red (woman-children);
     * @param lstMols
     * @param clst
     * @param dotfile
     * @throws Exception 
     */

    private void createDOTForGeneration(ArrayList<MolData> lstMols, 
        ArrayList<Integer> clst, String dotfile) throws Exception
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("graph G {").append(System.getProperty("line.separator"));
        
        String molID, molIDA, molIDB;
        int idx, idxA, idxB;
        StringBuilder sbRel = new StringBuilder(1024);
        
        // use this container to prevent repetition of nodes
        ArrayList<String> lstID = new ArrayList<>();
        
        
        for (int i=0; i<clst.size(); i++)
        {
            idx = clst.get(i);
            MolData mol = lstMols.get(idx);            
            molID = mol.getMolID();
            
            if (lstID.contains(molID))
                continue;
            else
                lstID.add(molID);
            
            sb.append("\t");
            sb.append(molID).append(" [shape=egg, color=black, style=bold, label=\"");
            sb.append("Gen ").append(mol.getGeneration()).append("\\n")
                .append(molID).append("\\n")
                .append(" ").append(DFORMAT.format(mol.getFitness()));
            //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
            sb.append("\"").append(", image=\"").append(mol.getImgFile())
                .append("\", labelloc=b];").append(System.getProperty("line.separator"));
            
            if (mol.getParent_X_molID().length() > 0)
            {
                idxA = getIndex(lstMols, mol.getParent_X_molID());
                MolData molA = lstMols.get(idxA);
                molIDA = molA.getMolID();
                if (! lstID.contains(molIDA))
                {    
                    sb.append("\t");
                    sb.append(molIDA).append(" [shape=box, color=black, style=bold, label=\"");
                    sb.append("Gen ").append(molA.getGeneration()).append("\\n")
                        .append(molIDA).append("\\n")
                        .append(" ").append(DFORMAT.format(molA.getFitness()));
                    //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
                    sb.append("\"").append(", image=\"").
                        append(molA.getImgFile()).append("\", labelloc=b];").
                        append(System.getProperty("line.separator"));
                    lstID.add(molIDA);
                }
                
                if (mol.getParent_Y_molID().length() > 0)
                {
                    idxB = getIndex(lstMols, mol.getParent_Y_molID());                    
                    MolData molB = lstMols.get(idxB);
                    molIDB = molB.getMolID();
                    
                    if (! lstID.contains(molIDB))
                    {
                        sb.append("\t");
                        sb.append(molIDB).append(" [shape=ellipse, color=black, style=bold, label=\"");
                        sb.append("Gen ").append(molB.getGeneration()).append("\\n")
                            .append(molIDB).append("\\n")
                            .append(" ").append(DFORMAT.format(molB.getFitness()));
                        //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
                        sb.append("\"").append(", image=\"").
                            append(molB.getImgFile()).append("\", labelloc=b];").
                            append(System.getProperty("line.separator"));
                        lstID.add(molIDB);
                    }
                    
                    
                    // indicates crossover
                    sbRel.append("\t");
                    sbRel.append(mol.getParent_X_molID()).append(" -- ")
                        .append(mol.getParent_Y_molID())
                        .append(" [style=bold, color=green]; ")
                        .append(System.getProperty("line.separator"));

                    sbRel.append("\t");
                    sbRel.append(mol.getParent_X_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=blue]; ")
                        .append(System.getProperty("line.separator"));

                    sbRel.append("\t");
                    sbRel.append(mol.getParent_Y_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=red]; ")
                        .append(System.getProperty("line.separator"));
                }
                else
                {
                    // show the mutation
                    sbRel.append("\t");
                    sbRel.append(mol.getParent_X_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=orange]; ")
                        .append(System.getProperty("line.separator"));
                }
            }
        }
        
        sb.append("\t").append(sbRel.toString().trim());

        sbRel.setLength(0);
        sbRel = null;

        sb.append(System.getProperty("line.separator"));
        sb.append("}").append(System.getProperty("line.separator"));
        
        IO.writeFile(dotfile, sb.toString().trim());
        lstID.clear();
    }

//------------------------------------------------------------------------------

    /**
     * 
     * @param lstMols
     * @param molid
     * @return index of the molecule ID
     */
    
    private int getIndex(ArrayList<MolData> lstMols, String molid)
    {
        int idx = 0;
        for (MolData mol:lstMols)
        {
            if (mol.getMolID().equals(molid))
            {
                break;
            }
            idx++;
        }
        return idx;
    }
    
//------------------------------------------------------------------------------

    private boolean existsParentLink(ArrayList<String> lstParents, String pA, 
        String pB, String splstr)
    {
        
        for (String pStr:lstParents)
        {
            String[] arr = pStr.split(splstr);
            if (arr[0].equals(pA) && arr[1].equals(pB))
                return true;
            if (arr[0].equals(pB) && arr[1].equals(pA))
                return true;
        }

        return false;
    }
    
//------------------------------------------------------------------------------    

    private void getDirectedFamilyTree(ArrayList<MolData> lstMols, 
        String dotFile) throws Exception
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("digraph G {").append(System.getProperty("line.separator"));
        sb.append("\t").append("nodesep=1.0;").append(System.getProperty("line.separator"));
        
        
        int oldGen = 1, curGen = 0;
        ArrayList<Integer> members = new ArrayList<>();
        String molid;
        
        for (int i=0; i<lstMols.size(); i++)
        {
            curGen = lstMols.get(i).getGeneration();
            
            //System.err.println("Gen: " + oldGen + " " + curGen);
            
            if (curGen == oldGen)
            {
                members.add(i);
            }
            else
            {
                //Utils.pause();
                if (members.size() > 0)
                {
                    // append to the dot file for the current generation
                    
                    // add individual member details
                    for (int j=0; j<members.size(); j++)
                    {
                        MolData cmol = lstMols.get(members.get(j));  
                        sb.append("\t");
                        sb.append(cmol.getMolID()).append(" [shape=ellipse, color=black, style=bold, label=\"");
                        sb.append("Gen ").append(cmol.getGeneration()).append("\\n")
                            .append(cmol.getMolID()).append("\\n")
                            .append(" ").append(DFORMAT.format(cmol.getFitness()));
                        //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
                        sb.append("\"").append(", image=\"").append(cmol.getImgFile())
                            .append("\", labelloc=b];").append(System.getProperty("line.separator"));
                        
                        // add links to parents
                        
                        if (cmol.getParent_X_molID().length() > 0)
                        {
                            sb.append("\t");
                            sb.append(cmol.getParent_X_molID()).append(" -> ")
                                .append(cmol.getMolID()).append(" [style=bold, color=blue]; ")
                                .append(System.getProperty("line.separator"));
                            
                            if (cmol.getParent_Y_molID().length() > 0)
                            {
                                sb.append("\t");
                                sb.append(cmol.getParent_Y_molID()).append(" -> ")
                                    .append(cmol.getMolID()).append(" [style=bold, color=blue]; ")
                                    .append(System.getProperty("line.separator"));
                            }
                        }
                    }
                    
                    // put these members at the same level
                    sb.append("\t").append("{rank=same;");
                    for (int j=0; j<members.size(); j++)
                    {
                        molid = lstMols.get(members.get(j)).getMolID();
                        if (j == (members.size()-1))
                            sb.append(molid);
                        else
                            sb.append(molid).append(" ");
                    }
                    sb.append("}").append(System.getProperty("line.separator"));

                    members.clear();
                }
                oldGen = curGen;
                members.add(i);
            }
        }
        
        
        // the last one
        if (members.size() > 0)
        {
            // append to the dot file for the current generation

            // add individual member details
            for (int j=0; j<members.size(); j++)
            {
                MolData cmol = lstMols.get(members.get(j));  
                sb.append("\t");
                sb.append(cmol.getMolID()).append(" [shape=ellipse, color=black, style=bold, label=\"");
                sb.append("Gen ").append(cmol.getGeneration()).append("\\n")
                    .append(cmol.getMolID()).append("\\n")
                    .append(" ").append(DFORMAT.format(cmol.getFitness()));
                //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
                sb.append("\"").append(", image=\"").append(cmol.getImgFile())
                    .append("\", labelloc=b];").append(System.getProperty("line.separator"));

                // add links to parents

                if (cmol.getParent_X_molID().length() > 0)
                {
                    sb.append("\t");
                    sb.append(cmol.getParent_X_molID()).append(" -> ")
                        .append(cmol.getMolID()).append(" [style=bold, color=blue]; ")
                        .append(System.getProperty("line.separator"));

                    
                    if (cmol.getParent_Y_molID().length() > 0)
                    {
                        sb.append("\t");
                        sb.append(cmol.getParent_Y_molID()).append(" -> ")
                            .append(cmol.getMolID()).append(" [style=bold, color=blue]; ")
                            .append(System.getProperty("line.separator"));
                    }
                }
            }

            // put these members at the same level
            sb.append("\t").append("{rank=same;");
            for (int j=0; j<members.size(); j++)
            {
                molid = lstMols.get(members.get(j)).getMolID();
                if (j == (members.size()-1))
                    sb.append(molid);
                else
                    sb.append(molid).append(" ");
            }
            sb.append("}").append(System.getProperty("line.separator"));

            members.clear();
        }
        
        
        sb.append("}").append(System.getProperty("line.separator"));

        IO.writeFile(dotFile, sb.toString().trim());

        sb.setLength(0);
        sb = null;
    }


//------------------------------------------------------------------------------

    /**
     *
     * @param lstMols List of the molecules containing the fitness and other
     * information.
     * @param dotFile The GraphViz dot file format to be generated.
     * @throws Exception
     */

    private void getFamilyTree(ArrayList<MolData> lstMols, String dotFile) throws Exception
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("graph G {").append(System.getProperty("line.separator"));

        StringBuilder sbRel = new StringBuilder(1024);
        
        ArrayList<String> lstParents = new ArrayList<>();
        
        String molID, parents;

        for (MolData mol:lstMols)
        {
            molID = mol.getMolID();
            sb.append("\t");
            sb.append(molID).append(" [shape=ellipse, color=black, style=bold, label=\"");
            sb.append("Gen ").append(mol.getGeneration()).append("\\n")
                .append(molID).append("\\n").append(" ").append(DFORMAT.format(mol.getFitness()));
            //sb.append("\", labelloc=b];").append(System.getProperty("line.separator"));
            sb.append("\"").append(", image=\"").append(mol.getImgFile())
                .append("\", labelloc=b];").append(System.getProperty("line.separator"));

            if (mol.getParent_X_molID().length() > 0)
            {
                if (mol.getParent_Y_molID().length() > 0)
                {
                    // indicates crossover
                    parents = mol.getParent_X_molID() + " -- " + mol.getParent_Y_molID();
                    if (!existsParentLink(lstParents, mol.getParent_X_molID(), 
                            mol.getParent_Y_molID(), "--"))
                    {
                        lstParents.add(parents);
                        
                        sbRel.append("\t");
                        sbRel.append(mol.getParent_X_molID()).append(" -- ")
                            .append(mol.getParent_Y_molID())
                            .append(" [style=bold, color=green]; ")
                            .append(System.getProperty("line.separator"));
                    }

                    
                    sbRel.append("\t");
                    sbRel.append(mol.getParent_X_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=blue]; ")
                        .append(System.getProperty("line.separator"));

                    sbRel.append("\t");
                    sbRel.append(mol.getParent_Y_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=red]; ")
                        .append(System.getProperty("line.separator"));
                }
                else
                {
                    // show the mutation
                    sbRel.append("\t");
                    sbRel.append(mol.getParent_X_molID()).append(" -- ")
                        .append(molID).append(" [style=bold, color=orange]; ")
                        .append(System.getProperty("line.separator"));
                }
            }
        }


        sb.append("\t").append(sbRel.toString().trim());

        sbRel.setLength(0);
        sbRel = null;

        sb.append(System.getProperty("line.separator"));
        sb.append("}").append(System.getProperty("line.separator"));

        IO.writeFile(dotFile, sb.toString().trim());

        sb.setLength(0);
        sb = null;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param filename
     * @return
     */

    private ArrayList<File> listFolderContents(String filename)
    {
        ArrayList<File> files;
        File fl = new File(filename);
        File[] fls = fl.listFiles();
        Arrays.sort(fls);
        files = new ArrayList<>(Arrays.asList(fls));
        return files;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param fl
     * @return
     */

    private ArrayList<File> listFolderContents(File fl)
    {
        ArrayList<File> files;
        File[] fls = fl.listFiles();
        Arrays.sort(fls);
        files = new ArrayList<>(Arrays.asList(fls));
        return files;
    }

//------------------------------------------------------------------------------

    /**
     *
     */

    private void printUsage()
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar GenDENOPTIMFTree <options>", opts);
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param args
     */

    private void parseCommandLine(String[] args)
    {
        try
        {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmdLine = parser.parse(opts, args);
            
            if (cmdLine.hasOption("h"))
            {
                printUsage();
                System.exit(0);
            }
            
            if (cmdLine.hasOption("d"))
                inputDir = cmdLine.getOptionValue("d").trim();
            
            
            if (cmdLine.hasOption("a"))
                amolid = cmdLine.getOptionValue("a").trim();
            
            if (cmdLine.hasOption("o"))
                outputFileName = cmdLine.getOptionValue("o").trim();

            if (cmdLine.hasOption("c"))
                cssFile = cmdLine.getOptionValue("c").trim();

            if (cmdLine.hasOption("j"))
                jsFile = cmdLine.getOptionValue("j").trim();

            if (cmdLine.hasOption("p"))
            {
                precisionLevel = Integer.parseInt(cmdLine.getOptionValue("p").trim());
            }

            if (cmdLine.hasOption("family"))
            {
                genDot = true;
            }

            if (cmdLine.hasOption("bygen"))
            {
                bygen = true;
            }


            if (cmdLine.hasOption("frags"))
            {
                fragsFile = cmdLine.getOptionValue("frags").trim();
            }

            if (precisionLevel > 0)
                DFORMAT.setMaximumFractionDigits(precisionLevel);
        }
        catch(ParseException | NumberFormatException poe)
        {
            LOGGER.log(Level.SEVERE, null, poe);
            printUsage();
            System.exit(-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     *
     */

    private void checkOptions()
    {
        String error = "";
        // check files
        if (inputDir.trim().length() == 0)
        {
            error = "Input directory not specified.";
            LOGGER.log(Level.SEVERE, error);
            printUsage();
            System.exit(-1);
        }


        if (this.cssFile.length() > 0)
        {
            if (! new File(cssFile).exists())
            {
                error = "ChemDoodle css component " + cssFile +
                        " does not seem to exist.";
                LOGGER.log(Level.SEVERE, error);
                printUsage();
                System.exit(-1);
            }
        }

        if (this.jsFile.length() > 0)
        {
            if (! new File(jsFile).exists())
            {
                error = "ChemDoodle js component " + jsFile +
                        " does not seem to exist.";
                LOGGER.log(Level.SEVERE, error);
                printUsage();
                System.exit(-1);
            }
        }

        if (outputFileName.trim().length() == 0)
        {
            error = "No output file name specified.";
            LOGGER.log(Level.SEVERE, error);
            printUsage();
            System.exit(-1);
        }

        if (inputDir.trim().length() > 0)
        {
            if (! new File(inputDir).isDirectory())
            {
                error = "Supplied option " + inputDir + " is not a directory.";
                LOGGER.log(Level.SEVERE, error);
                printUsage();
                System.exit(-1);
            }
        }

        if (this.fragsFile.trim().length() > 0)
        {
            if (! new File(fragsFile).exists())
            {
                error = "Fragment file " + fragsFile +
                        " does not seem to exist.";
                LOGGER.log(Level.SEVERE, error);
                printUsage();
                System.exit(-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Option setup for command line
     */

    private void setUp()
    {
        
        opts.addOption("h", false, "Usage information");

        opts.addOption("d", "dir", true, "Directory containing DENOPTIM output.");

        opts.addOption("o", "output", true, "Output file name without extension");

        opts.addOption("c", "css", true, "ChemDoodle CSS file.");

        opts.addOption("j", "js", true, "ChemDoodle JS file.");

        opts.addOption("p", "precision", true, "Set double precision level.");
        
        Option ftreeopt;
        ftreeopt = Option.builder().required(false).longOpt("family").
            hasArg(false).desc("Generate the complete family tree dot file.").build();
        opts.addOption(ftreeopt);
        
        Option ancestryopt;
        ancestryopt = Option.builder("a").required(false).longOpt("ancestry").
            hasArg(true).desc("Obtain ancestors for given moleculeID.").build();
        opts.addOption(ancestryopt);
        
        Option gentreeopt;
        gentreeopt = Option.builder().required(false).longOpt("bygen").
            hasArg(false).desc("Generate individual dot files for each generation.").build();
        opts.addOption(gentreeopt);
                
        opts.addOption("", "frags", true, "Evaluate fragments (in smiles/SDF format) based details."
            + "Provide a multi-SDF file or one containing fragment smiles on individual lines.");            
    }

//------------------------------------------------------------------------------

}
