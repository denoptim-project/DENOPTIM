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
import java.util.TreeMap;

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
import denoptim.io.DenoptimIO;


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

    public static final String PROPERTY_RSU_ATMID = "RSU-ATMID";

    // Properties to store the details of the constrained rotatable bonds
    public static final String PROPERTY_ROTDBDCSTR_DEF = "ROTDBDCSTR_DEF";
    public static final String PROPERTY_ROTDBDCSTR_VALUE = "ROTDBDCSTR_VALUE";

//------------------------------------------------------------------------------    

    /**
     * Define the rotational space (a.k.a. torsional space) for a given molecule.
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
            for (int iAtm=0; iAtm<locMol.getAtomCount(); iAtm++)
            {
                locMol.getAtom(iAtm).setProperty(PROPERTY_RSU_ATMID, iAtm);
            }
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
        		//clone of mol but has no RCAs, so we take the atm index in the
                // oridinal mol fro mthe atm property
                        
                        int idAtmA = (int) locMol.getAtom(singleMatch[0]).getProperty(PROPERTY_RSU_ATMID);
                        int idAtmB = (int) locMol.getAtom(singleMatch[1]).getProperty(PROPERTY_RSU_ATMID);
        
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
              + " null! Cannot read definition of rotational space.");
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

    /**
     * Process the constrained rotatable bonds. This method will remove the 
     * rotatable bonds that are constrained and record the details of the 
     * constraint among the bond properties.
     * 
     * @param mol the molecule as IAtomContainer
     * @param rotatableBonds the list of rotatable bonds
     * @param defRotBndContrFile the file with the definition of the constrained 
     * rotatable bonds
     * @params logger the logging tool
     * @throws DENOPTIMException
     * @throws Error if the constraint is not defined by 4 atoms or if the 
     * attempt to match the constrained rotatable bonds returns an error
     */
    public static void processConstrainedRotatableBonds(
        IAtomContainer mol, ArrayList<ObjectPair> rotatableBonds, 
        String defRotBndContrFile, Logger logger) throws DENOPTIMException
    {
        // We'll use SMARTS so get rid of pseudoatoms that can create problems
	    // We'll use this modified IAtomContainer only when dealing with SMARTS
        IAtomContainer locMol = builder.newAtomContainer();
        try {
            locMol = mol.clone();
            for (int iAtm=0; iAtm<locMol.getAtomCount(); iAtm++)
            {
                locMol.getAtom(iAtm).setProperty(PROPERTY_RSU_ATMID, iAtm);
            }
        } catch (Throwable t) {
            throw new DENOPTIMException(t);
        }
        MoleculeUtils.removeRCA(locMol);

        if (defRotBndContrFile.equals(""))
        {
            // Nothing to be done
            return;
        }

        // Get definition of constrained rotatable bonds as list of SMARTS queries
        TreeMap<String,RotBndConstraint> mapOfConstraints = getRotationalConstraintsDefinition(
            defRotBndContrFile);

        TreeMap<String,String> listQueries = new TreeMap<String,String>();
        for (Map.Entry<String,RotBndConstraint> e : mapOfConstraints.entrySet())
        {
            listQueries.put(e.getKey(),e.getValue().getSmarts());
        }

        // Get bonds matching one of the definitions of rotatable bonds
        ManySMARTSQuery msq = new ManySMARTSQuery(locMol,listQueries);
        if (msq.hasProblems())
        {
            String msg = "ERROR! Attempt to match constrained rotatable bonds returned "
                         + "an error!";
            throw new Error(msg);
        } else {
            //Transform list of indeces
            for (Map.Entry<String,String> e : listQueries.entrySet())
            {
                String name = e.getKey();
                
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
                    if (singleMatch.length != 4)
                    {
                        throw new Error("Expecting a constrain to be defined by 4 atoms. "
                            + "Check this match of constraint '" + name + "': "
                            + singleMatch);
                    }
    
                    //NOTE the index refers to the IAtomContainer locMol that is a
                    //clone of mol but has no RCAs, so we take the atm index in the
                    // original mol from the atm property
                            
                    int idAtmA = (int) locMol.getAtom(singleMatch[0]).getProperty(PROPERTY_RSU_ATMID);
                    int idAtmB = (int) locMol.getAtom(singleMatch[1]).getProperty(PROPERTY_RSU_ATMID);
                    int idAtmC = (int) locMol.getAtom(singleMatch[2]).getProperty(PROPERTY_RSU_ATMID);
                    int idAtmD = (int) locMol.getAtom(singleMatch[3]).getProperty(PROPERTY_RSU_ATMID);
                    

                    // Compare with bonds already in the list
                    ObjectPair opToRemove = null;
                    for (ObjectPair op : rotatableBonds)
                    {
                        // this check also ensures that one and only one constraint is applied to any matched bond
                        int a1 = ((Integer)op.getFirst()).intValue();
                        int a2 = ((Integer)op.getSecond()).intValue();
                        if (((a1 == idAtmB) && (a2 == idAtmC)) ||
                            ((a2 == idAtmB) && (a1 == idAtmC)))
                        {
                            opToRemove = op;
                            break;
                        }
                    }
                    if (opToRemove!=null)
                    {
                        rotatableBonds.remove(opToRemove);
                        IBond bnd = mol.getBond(mol.getAtom(idAtmB), mol.getAtom(idAtmC));
                        bnd.setProperty(PROPERTY_ROTDBDCSTR_DEF, new IAtom[]{
                            mol.getAtom(idAtmA), mol.getAtom(idAtmB), 
                            mol.getAtom(idAtmC), mol.getAtom(idAtmD)});
                        bnd.setProperty(PROPERTY_ROTDBDCSTR_VALUE, 
                            mapOfConstraints.get(name).getValue());
                        bnd.setProperty(DENOPTIMConstants.BONDPROPROTATABLE, "false");
                        logger.log(Level.INFO, "Constraining dihedral along bond " 
                                + MoleculeUtils.getAtomRef(mol.getAtom(idAtmB), mol)
                                + "-" 
                                + MoleculeUtils.getAtomRef(mol.getAtom(idAtmC), mol)
                                + " (rotatable bond constrain: '" + name + "')");
                    }
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads the definition of constraints on the rotatable bonds.
     * @param filename the file to be read in.
     * @return the map with names and constraints
     * @throws DENOPTIMException should anything go wrong with the reading of the file
     */
    public static TreeMap<String,RotBndConstraint> getRotationalConstraintsDefinition(
        String filename) throws DENOPTIMException
    {
        TreeMap<String,RotBndConstraint> mapOfConstraints = new TreeMap<String,RotBndConstraint>();

        //Read the file
        if (filename.equals(null))
        {
            DENOPTIMException ex = new DENOPTIMException("Pointer to file is "
            + " null! Cannot read definition of rotational constraints.");
            throw ex;
        }
        File f = new File(filename);
        if (!f.exists())
        {
            DENOPTIMException ex = new DENOPTIMException("File '" + filename
                    + "' does not exist! Cannot find definition of rotational "
                + "constraints.");
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
                // Expectedf ormat: name smarts value
                if (parts.length != 3)
                {
                    throw new DENOPTIMException("Unable to understand "
                                        + "rotational constraints definition. "
                                        + "Check line '"+ line +"' in file "
                                        + filename);
                } else {
                    String key = parts[0];
                    String smarts = parts[1];
                    double value = Double.parseDouble(parts[2]);
                    if (mapOfConstraints.keySet().contains(key))
                    {
                        throw new DENOPTIMException("Duplicate definition of "
                                        + "rotational contraint named '" + key + "'. "
                                        + "Check line '"+ line +"' in file "
                                        + filename);
                    }

                    //Everything is OK, thus store this definition
                    mapOfConstraints.put(key, new RotBndConstraint(key,smarts,value));
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
        return mapOfConstraints;
    }

//------------------------------------------------------------------------------
}
