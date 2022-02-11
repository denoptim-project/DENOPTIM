/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptimrnd;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.DENOPTIMStatUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.ObjectPair;


/**
 * Helper methods for random-based evolution. Implements some of the method of 
 * EAUtils helper for genetic algorithm.
 *
 * @author Marco Foscato
 */

class RNDEAUtils
{
    private static Locale enUsLocale = new Locale("en", "US");
    private static DecimalFormat df = initialiseFormatter();
    private static DecimalFormat initialiseFormatter() {
    	DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(
    			enUsLocale);
    	df.setGroupingUsed(false);
    	return df;
    }
    
//------------------------------------------------------------------------------

    /**
     * Write out summary for the population
     * @param pop
     * @param filename
     * @throws DENOPTIMException
     */

    protected static void outputPopulationDetails
                            (ArrayList<Candidate> pop, String filename)
                                                        throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(512);
        df.setMaximumFractionDigits(RNDParameters.getPrecisionLevel());

        for (int i=0; i<pop.size(); i++)
        {
            Candidate mol = pop.get(i);
            if (mol != null)
            {
                sb.append(String.format("%-20s", 
					  mol.getGraph().getGraphId()));
                sb.append(String.format("%-30s", mol.getUID()));
                sb.append(df.format(mol.getFitness()));
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

    private static String getSummaryStatistics(ArrayList<Candidate> pop)
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
        f = DENOPTIMStatUtils.stddev(fitness,true);
        sb.append(String.format("%-30s", "STDDEV:"));
	sb.append(String.format("%12.3f", f));
        sb.append(System.getProperty("line.separator"));
        f = DENOPTIMStatUtils.skewness(fitness,true);
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
            Candidate mol = pop.get(i);
            DENOPTIMGraph g = mol.getGraph();
            int scafIdx = g.getVertexAtPosition(0).getBuildingBlockId() + 1;
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

    protected static void outputFinalResults(ArrayList<Candidate> pop,
                            String destDir) throws DENOPTIMException
    {
        String genOutfile = destDir + System.getProperty("file.separator") +
                                "Final.txt";

        File fileDir = new File(destDir);

        try
        {
            for (int i=0; i<RNDParameters.getPopulationSize(); i++)
            {
                String sdfile = pop.get(i).getSDFFile();
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

    protected static double[] getFitnesses(ArrayList<Candidate> mols)
    {
        int k = mols.size();
        double[] arr = new double[k];

        for (int i=0; i<k; i++)
        {
            arr[i] = mols.get(i).getFitness();
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
                                    (ArrayList<Candidate> molPopulation)
    {
        int k = molPopulation.size();
        ArrayList<String> arr = new ArrayList<>();

        for (int i=0; i<k; i++)
        {
            arr.add(molPopulation.get(i).getUID());
        }
        return arr;
    }

//------------------------------------------------------------------------------

    /**
     * Import the molecular population from a file.
     * @param fileName the pathname to the file to read
     * @return the list of population members 
     * @throws DENOPTIMException
     * @Deprecated use DenoptimIO
     */
    @Deprecated
    protected static ArrayList<Candidate> readGraphsWithFitnessFromFile(
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
            throw new DENOPTIMException("ERROR! Unable to read file '" 
                    + fileName + "'");
        }

        String fsep = System.getProperty("file.separator");

        Set<String> lstUIDs = new HashSet();
        ArrayList<Candidate> members = new ArrayList<Candidate>();
        for (int i=0; i<mols.size(); i++)
        {
            DENOPTIMGraph graph = null;
            double fitness = 0, ad = 0;
            String molsmiles = null, molinchi = null, molfile = null;

            IAtomContainer mol = mols.get(i);
            Object apProperty = mol.getProperty(DENOPTIMConstants.GRAPHTAG);
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

            apProperty = mol.getProperty(DENOPTIMConstants.FITNESSTAG);
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

            apProperty = mol.getProperty(DENOPTIMConstants.SMILESTAG);
            if (apProperty != null)
            {
                molsmiles = apProperty.toString().trim();
            }
            else
            {
                molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            }

            apProperty = mol.getProperty(DENOPTIMConstants.INCHIKEYTAG);
            if (apProperty != null)
            {
                molinchi = apProperty.toString();
            }
            if (mol.getProperty(DENOPTIMConstants.UNIQUEIDTAG) != null)
            {
                molinchi = mol.getProperty(
                        DENOPTIMConstants.UNIQUEIDTAG).toString();
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
                graph.setLocalMsg("NEW");
                mol.setProperty(DENOPTIMConstants.GCODETAG, gctr);
                mol.setProperty(CDKConstants.TITLE, molName);
                mol.setProperty(DENOPTIMConstants.GRAPHTAG, graph.toString());
                mol.setProperty(DENOPTIMConstants.GMSGTAG, 
                        "From Initial Population File");

                Candidate pmol = new Candidate(molName, graph, fitness, 
                        molinchi, molsmiles);
                
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
