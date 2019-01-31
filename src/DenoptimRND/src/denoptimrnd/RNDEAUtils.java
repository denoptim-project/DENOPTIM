package denoptimrnd;

import exception.DENOPTIMException;
import io.DenoptimIO;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import logging.DENOPTIMLogger;
import molecule.*;
import utils.*;
import utils.GraphConversionTool;
import fragspace.FragmentSpace;


/**
 * Helper methods for random-based evolution. Implements some of the method of 
 * EAUtils helper for genetic algorithm.
 *
 * @author Marco Foscato
 */

class RNDEAUtils
{
    protected static DecimalFormat df = new DecimalFormat();
    
//------------------------------------------------------------------------------

    /**
     * Write out summary for the population
     * @param pop
     * @param filename
     * @throws DENOPTIMException
     */

    protected static void outputPopulationDetails
                            (ArrayList<DENOPTIMMolecule> pop, String filename)
                                                        throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);
        df.setMaximumFractionDigits(RNDParameters.getPrecisionLevel());

        for (int i=0; i<pop.size(); i++)
        {
            DENOPTIMMolecule mol = pop.get(i);
            if (mol != null)
            {
                sb.append(String.format("%-20s", 
					  mol.getMoleculeGraph().getGraphId()));
                sb.append(String.format("%-30s", mol.getMoleculeUID()));
                sb.append(df.format(mol.getMoleculeFitness()));
                sb.append(System.getProperty("line.separator"));
            }
        }

        // calculate descriptive statistics for the population
        String stats = getSummaryStatistics(pop);
        if (stats.trim().length() > 0)
            sb.append(stats);
        DenoptimIO.writeData(filename, sb.toString(), false);

        sb.setLength(0);
    }

//------------------------------------------------------------------------------

    private static String getSummaryStatistics(ArrayList<DENOPTIMMolecule> pop)
    {
        double[] fitness = getFitnesses(pop);
        String res = "";
        df.setMaximumFractionDigits(RNDParameters.getPrecisionLevel());

        StringBuilder sb = new StringBuilder(128);
        sb.append("\n\n#####POPULATION SUMMARY#####\n");
        int n = pop.size();
        sb.append(String.format("%-30s", "SIZE:"));
	sb.append(String.format("%12s", n));
        sb.append(System.getProperty("line.separator"));
        double f;
        f = DENOPTIMStatUtils.max(fitness);
        sb.append(String.format("%-30s", "MAX:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.min(fitness);
        sb.append(String.format("%-30s", "MIN:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.mean(fitness);
        sb.append(String.format("%-30s", "MEAN:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.median(fitness);
        sb.append(String.format("%-30s", "MEDIAN:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.stddev(fitness, true);
        sb.append(String.format("%-30s", "STDDEV:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.skewness(fitness, true);
        sb.append(String.format("%-30s", "SKEW:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));

        int sz = FragmentSpace.getScaffoldLibrary().size();
        HashMap<Integer, Integer> scf_cntr = new HashMap<>();
        for (int i=1; i<=sz; i++)
        {
            scf_cntr.put(i, 0);
        }
        for (int i=0; i<RNDParameters.getPopulationSize(); i++)
        {
            DENOPTIMMolecule mol = pop.get(i);
            DENOPTIMGraph g = mol.getMoleculeGraph();
            int scafIdx = g.getVertexAtPosition(0).getMolId() + 1;
            scf_cntr.put(scafIdx, scf_cntr.get(scafIdx)+1);
        }
        sb.append("\n\n#####SCAFFOLD ANALYSIS#####\n");
        for (Map.Entry pairs : scf_cntr.entrySet())
        {
            sb.append(pairs.getKey()).append(" ");
	    sb.append(pairs.getValue());
	    sb.append(System.getProperty("line.separator"));
        }
        res = sb.toString();
        sb.setLength(0);

        return res;
    }
    
//------------------------------------------------------------------------------

    /**
     * Simply copies the files from the previous directories into the specified
     * folder.
     * @param pop the final list of best molecules
     * @param destDir the name of the output directory
     */

    protected static void outputFinalResults(ArrayList<DENOPTIMMolecule> pop,
                            String destDir) throws DENOPTIMException
    {
        String genOutfile = destDir + System.getProperty("file.separator") +
                                "Final.txt";

        File fileDir = new File(destDir);

        try
        {
            for (int i=0; i<RNDParameters.getPopulationSize(); i++)
            {
                String sdfile = pop.get(i).getMoleculeFile();
                String imgfile = pop.get(i).getImageFile();

                if (sdfile != null && imgfile != null)
                {
                    FileUtils.copyFileToDirectory(new File(sdfile), fileDir);

                    FileUtils.copyFileToDirectory(new File(imgfile), fileDir);
                }
            }
            outputPopulationDetails(pop, genOutfile);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Get the fitness values for the list of molecules
     * @param mols
     * @return array of fitness values
     */

    protected static double[] getFitnesses(ArrayList<DENOPTIMMolecule> mols)
    {
        int k = mols.size();
        double[] arr = new double[k];

        for (int i=0; i<k; i++)
        {
            arr[i] = mols.get(i).getMoleculeFitness();
        }
        return arr;
    }
    
//------------------------------------------------------------------------------

    /**
     *
     * @param molPopulation
     * @return list of INCHI codes for the molecules in the population
     */

    protected static ArrayList<String> getInchiCodes
                                    (ArrayList<DENOPTIMMolecule> molPopulation)
    {
        int k = molPopulation.size();
        ArrayList<String> arr = new ArrayList<>();

        for (int i=0; i<k; i++)
        {
            arr.add(molPopulation.get(i).getMoleculeUID());
        }
        return arr;
    }

//------------------------------------------------------------------------------

    /**
     * Import the molecular population from a file.
     * @param fileName the pathname to the file ti read
     * @return the list of population members 
     * @throws DENOPTIMException
     */

    protected static ArrayList<DENOPTIMMolecule> readGraphsWithFitnessFromFile(
                                       String fileName) throws DENOPTIMException
    {
        ArrayList<IAtomContainer> mols;
        if (GenUtils.getFileExtension(fileName).compareToIgnoreCase(".sdf")==0)
        {
            mols = DenoptimIO.readSDFFile(fileName);
        }
        // everything else as a text file with links to individual molecules
        else
        {
            mols = DenoptimIO.readTxtFile(fileName);
        }

        String fsep = System.getProperty("file.separator");

        Set<String> lstUIDs = new HashSet();
        ArrayList<DENOPTIMMolecule> members = new ArrayList<DENOPTIMMolecule>();
        for (int i=0; i<mols.size(); i++)
        {
            DENOPTIMGraph graph = null;
            double fitness = 0, ad = 0;
            String molsmiles = null, molinchi = null, molfile = null;

            IAtomContainer mol = mols.get(i);
            Object apProperty = mol.getProperty("GraphENC");
            if (apProperty != null)
            {
                graph = GraphConversionTool.getGraphFromString(
                                                  apProperty.toString().trim());
            }
            else
            {
                DENOPTIMLogger.appLogger.log(Level.SEVERE,
                        "Molecule does not have the DENOPTIMGraph encoding.");
                throw new DENOPTIMException(
                        "Molecule does not have the DENOPTIMGraph encoding.");
            }

            apProperty = mol.getProperty("FITNESS");
            if (apProperty != null)
            {
                fitness = Double.parseDouble(apProperty.toString());
            }
            else
            {
                DENOPTIMLogger.appLogger.log(Level.SEVERE,
                            "Molecule does not have the associated fitness.");
                throw new DENOPTIMException(
                            "Molecule does not have the associated fitness.");
            }

            apProperty = mol.getProperty("SMILES");
            if (apProperty != null)
            {
                molsmiles = apProperty.toString().trim();
            }
            else
            {
                molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            }

            apProperty = mol.getProperty("InChi");
            if (apProperty != null)
            {
                molinchi = apProperty.toString();
            }
            if (mol.getProperty("UID") != null)
            {
                molinchi = mol.getProperty("UID").toString();
            }
            else
            {
                ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
                if (pr.getFirst() != null)
                {
                    molinchi = pr.getFirst().toString();
                }
            }

            // Add molecule to population, unless it has previously known UID
            if (lstUIDs.add(molinchi))
            {
                int ctr = GraphUtils.getUniqueMoleculeIndex();
                String molName = "M" + GenUtils.getPaddedString(8, ctr);
                int gctr = GraphUtils.getUniqueGraphIndex();
                graph.setGraphId(gctr);
                graph.setMsg("NEW");
                mol.setProperty("GCODE", gctr);
                mol.setProperty(CDKConstants.TITLE, molName);
                mol.setProperty("GraphENC", graph.toString());
                mol.setProperty("GraphMsg", "From Initial Population File");

                DENOPTIMMolecule pmol =
                    new DENOPTIMMolecule(graph, molinchi, molsmiles, fitness);
                pmol.setImageFile(null);
                members.add(pmol);
            }
        }

        if (members.isEmpty())
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                                        "No data found in file {0}", fileName);
            throw new DENOPTIMException("No data found in file " + fileName);
        }

        return members;
    }

//------------------------------------------------------------------------------

}
