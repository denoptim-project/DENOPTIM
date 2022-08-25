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

package denoptim.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.silent.RingSet;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;


/**
 * Tool box for definition and management of the rotational space, which is given
 * by the list of rotatable bonds.
 *
 * @author Marco Foscato 
 */

public class RotationalSpaceUtils  
{
    private static final  IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------    

    /**
     * Define the rotational space (also torsional space) for a given molecule.
     * The method identifies the rotatable bonds by using a provided set of 
     * SMARTS queries and can consider all fragment-fragment bonds 
     * (i.e., interfragment connections) while excluding cyclic bonds.
     * The propety <code>DENOPTIMConstants.BONDPROPROTATABLE</code> is used 
     * to store the rotatability in the <code>Bond</code>s.
     * 
     * @param mol the molecular structure
     * @param defRotBndsFile name of a text file with a list of SMARTS queries
     * that defines which bonds are considered rotatable bonds. This can be 
     * an empty string, in which case, only inter-fragment connections will be 
     * rotatable.
     * @param addIterfragBonds if <code>true</code> includes all inter-fragment 
     * connections
     * @param excludeRings if <code>true</code> cyclic bonds will be excluded
     * @throws DENOPTIMException
     */

    public static ArrayList<ObjectPair> defineRotatableBonds(IAtomContainer mol,
          String defRotBndsFile, boolean addIterfragBonds, boolean excludeRings,
          Logger logger) throws DENOPTIMException
    {
        ArrayList<ObjectPair> rotatableBonds = new ArrayList<ObjectPair>();

    	// Set all bond flags
    	for (IBond b : mol.bonds())
    	{
    	    b.setProperty(DENOPTIMConstants.BONDPROPROTATABLE,"false");
    	}

        // Deal with inter-fragment bonds
        if (addIterfragBonds)
        {
            rotatableBonds.addAll(getInterVertexBonds(mol));
        }

        // We'll use SMARTS so get rid of pseudoatoms that can create problems
	    // We'll use this modified IAtomContainer only when dealing with SMARTS
        IAtomContainer locMol = builder.newAtomContainer();
        try {
            locMol = mol.clone();
        } catch (Throwable t) {
            throw new DENOPTIMException(t);
        }
        MoleculeUtils.removeRCA(locMol);

        
        if (!defRotBndsFile.equals(""))
        {
            // Get definition of rotational space as list of SMARTS queries
            Map<String,String> listQueries = getRotationalSpaceDefinition(
                                                                   defRotBndsFile);
    
            // Get bonds matching one of the definitions of rotatable bonds
            ManySMARTSQuery msq = new ManySMARTSQuery(locMol,listQueries);
            if (msq.hasProblems())
            {
                String msg = "WARNING! Attempt to match rotatable bonds returned "
                             + "an error! Selecting only fragment-fragment "
                             + "bonds. Details: " + msq.getMessage();
                logger.log(Level.WARNING, msg);
            } else {
                //Transform list of indeces
                for (String name : listQueries.keySet())
                {
                    //Skip if no match
                    if (msq.getNumMatchesOfQuery(name) == 0)
                    {
                        continue;
                    }
        
                    //Put all matches in one list
                    Mappings matches = msq.getMatchesOfSMARTS(name);
                    for (int[] singleMatch : matches)
                    {
                        //Check assumption on number of atoms involved in each bond
                        if (singleMatch.length != 2)
                        {
                            throw new Error("DENOPTIM can only deal with bonds "
                                    + "involving 2 atoms. Check bond " 
                                    + singleMatch);
                        }
        
        		//NOTE the index refers to the IAtomContainer locMol that is a
        		//clone of mol and has the same atom order. Thus we use the
        		//atom idenx to identify atoms in mol rather than locMol.
                        
                        int idAtmA = singleMatch[0];
                        int idAtmB = singleMatch[1];
        
                        // Compare with bonds already in the list
                        boolean alreadyThere = false;
                        for (ObjectPair op : rotatableBonds)
                        {
                            int a1 = ((Integer)op.getFirst()).intValue();
                            int a2 = ((Integer)op.getSecond()).intValue();
                            if (((a1 == idAtmA) && (a2 == idAtmB)) ||
                                ((a2 == idAtmA) && (a1 == idAtmB)))
                            {
                                alreadyThere = true;
                                break;
                            }
                        }
                        if (!alreadyThere)
                        {
                            ObjectPair newRotBnd = new ObjectPair(
                                    Integer.valueOf(idAtmA),
                                    Integer.valueOf(idAtmB));
                            rotatableBonds.add(newRotBnd);
                        }
                    }
                }
            }
        }

        // Find all rings to identify cyclic bonds
        SpanningTree st = new SpanningTree(mol);
        IRingSet allRings = new RingSet();
        if (excludeRings)
        {
            try {
                allRings = st.getAllRings();
            } catch (Exception ex) {
                throw new DENOPTIMException(ex);
            }
        }

        // Purge list and store information as bond property
        ArrayList<ObjectPair> toRemove = new ArrayList<ObjectPair>();
        for (ObjectPair op : rotatableBonds)
        {
            int a1 = ((Integer)op.getFirst()).intValue();
            int a2 = ((Integer)op.getSecond()).intValue();
       
            IBond bnd = mol.getBond(mol.getAtom(a1),mol.getAtom(a2));

		    if (excludeRings)
		    {
                IRingSet rs = allRings.getRings(bnd);
                if (!rs.isEmpty())
                {
				    logger.log(Level.FINE, "Ignoring cyclic bond: "+bnd);
                    toRemove.add(op);
                    continue;
                }
		    }
            bnd.setProperty(DENOPTIMConstants.BONDPROPROTATABLE, "true");
        }

		if (excludeRings)
		{
            for (ObjectPair op : toRemove)
            {
                rotatableBonds.remove(op);
            }
        }

        return rotatableBonds;
    }

//------------------------------------------------------------------------------    

    /**
     * Search for all bonds connecting atoms corresponding to two different 
     * vertices in the DENOPTIMGraph. Vertex membership is evaluated by 
     * comparing the <code>DENOPTIMConstants.ATMPROPVERTEXID</code> property
     * of each <code>Atom</code>.
     * @param mol the molecule as IAtomContainer
     * @throws DENOPTIMException
     */
    public static ArrayList<ObjectPair> getInterVertexBonds(IAtomContainer mol) 
                                                        throws DENOPTIMException
    {
        ArrayList<ObjectPair> interfragBonds = new ArrayList<ObjectPair>();

        String p = DENOPTIMConstants.ATMPROPVERTEXPATH;
        for (IBond bnd : mol.bonds())
        {
            if (bnd.getAtomCount() == 2)
            {
                IAtom atmA = bnd.getAtom(0);
                IAtom atmB = bnd.getAtom(1);
        		String fragIdA = atmA.getProperty(p).toString();
        		String fragIdB = atmB.getProperty(p).toString();

                if (!fragIdA.equals(fragIdB) &&
                    (mol.getConnectedBondsCount(atmA) != 1) &&
                    (mol.getConnectedBondsCount(atmB) != 1))
                {
                    ObjectPair newRotBnd = new ObjectPair();
                    int idAtmA = mol.indexOf(atmA);
                    int idAtmB = mol.indexOf(atmB);
                    if (idAtmA < idAtmB)
                    {
                        newRotBnd = new ObjectPair(Integer.valueOf(idAtmA),
                                Integer.valueOf(idAtmB));
                    } else {
                        newRotBnd = new ObjectPair(Integer.valueOf(idAtmB),
                                Integer.valueOf(idAtmA));
                    }
                    interfragBonds.add(newRotBnd);
                }
            }
            else
            {
                String str = "ERROR! Unable to handle bonds involving other "
                             + "than two atoms.";
                throw new DENOPTIMException(str);
            }
        }
        return interfragBonds;
    }
    
//------------------------------------------------------------------------------    
    /**
     * Read a formatted file and return a map with all the SMARTS queries 
     * identifying rotatable bonds
     *
     * @param filename the name of the text file to read
     * @throws DENOPTIMException
     */

    public static Map<String,String> getRotationalSpaceDefinition(
                                String filename) throws DENOPTIMException
    {
        Map<String,String> mapOfSMARTS = new HashMap<String,String>();

        //Read the file
        if (filename.equals(null))
        {
            DENOPTIMException ex = new DENOPTIMException("Pointer to file is "
              + " null! annot read definition of rotational space.");
            throw ex;
        }
        File f = new File(filename);
        if (!f.exists())
        {
            DENOPTIMException ex = new DENOPTIMException("File '" + filename
                    + "' does not exist! Cannot find definition of rotational "
	            + "space.");
            throw ex;
        }
        BufferedReader br = null;
        String line;
        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                    continue;

                if (line.trim().startsWith("#"))
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 2)
                {
                    throw new DENOPTIMException("Unable to understand "
                                        + "rotational Space definition. "
                                        + "Check line '"+ line +"' in file "
                                        + filename);
                } else {
                    String key = parts[0];
                    String smarts = parts[1];
                    if (mapOfSMARTS.keySet().contains(key))
                    {
                        throw new DENOPTIMException("Duplicate definition of "
                                        + "rotatabe bond named '" + key + "'. "
                                        + "Check line '"+ line +"' in file "
                                        + filename);
                    }

                    //Everything is OK, thus store this definition
                    mapOfSMARTS.put(key,smarts);
                }
            }
        }
        catch (IOException nfe)
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

        return mapOfSMARTS;
    }

//------------------------------------------------------------------------------
}
