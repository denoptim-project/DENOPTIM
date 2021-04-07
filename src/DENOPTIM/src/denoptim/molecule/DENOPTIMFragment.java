package denoptim.molecule;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.DENOPTIMMoleculeUtils;

/**
 * Class representing a continuously connected portion of chemical object
 * holding attachment points.
 *
 * @author Marco Foscato
 */

public class DENOPTIMFragment extends DENOPTIMVertex
{
    /**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;

    /**
     * attachment points on this vertex
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private ArrayList<SymmetricSet> lstSymAPs;
    
	/**
	 * Molecular representation of this fragment
	 */
	private IAtomContainer mol;
	
	/**
	 * Field distinguishing type DENOPTIMFragment from other types of vertrexes.
	 * The existence of this field triggers interpretation of the JSON string
	 * as a DENOPTIMFragment.
	 */
	private final String fragmentType = "Molecular_fragment";
	// NB: Don't make static or Gson will ignore it!
	
//-----------------------------------------------------------------------------

    /**
     * Constructor of an empty fragment
     */
    
    public DENOPTIMFragment()
    {
        super();
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a molecular fragment kind of vertex.
     * @param vertexId unique identified of the vertex
     */

    public DENOPTIMFragment(int vertexId)
    {
        super(vertexId);
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties. WARNING: other properties of the atom container
     * are not imported!
     * @param vertexId the identifier of the vertex to construct
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public DENOPTIMFragment(int vertexId, IAtomContainer mol, DENOPTIMVertex.BBType bbt)
            throws DENOPTIMException
    {     
        super (vertexId);
        
        this.setBuildingBlockType(bbt);
        
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        this.mol = builder.newAtomContainer();
        
        for (IAtom oAtm : mol.atoms())
        {
            IAtom nAtm = DENOPTIMMoleculeUtils.makeSameAtomAs(oAtm);
            this.mol.addAtom(nAtm);
        }
        
        for (IBond oBnd : mol.bonds())
        {
            if (oBnd.getAtomCount() != 2)
            {
                throw new DENOPTIMException("Unable to deal with bonds "
                        + "involving more than two atoms.");
            }
            int ia = mol.indexOf(oBnd.getAtom(0));
            int ib = mol.indexOf(oBnd.getAtom(1));
            this.mol.addBond(ia,ib,oBnd.getOrder());
        }


        Object prop = mol.getProperty(DENOPTIMConstants.APCVTAG);
        if (prop != null)
        {
            projectPropertyToAP(prop.toString());
        }
        
        ArrayList<SymmetricSet> simAP = identifySymmetryRelatedAPSets(this.mol, 
                getAttachmentPoints());
        setSymmetricAPSets(simAP);
        
        this.setAsRCV(getNumberOfAP() == 1 
                && APClass.RCAAPCLASSSET.contains(
                        getAttachmentPoints().get(0).getAPClass()));
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties. WARNING: properties of the atom container are not
     * imported!
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public DENOPTIMFragment(IAtomContainer mol, DENOPTIMVertex.BBType bbt) 
            throws DENOPTIMException
    {    	
    	this(-1,mol,bbt);
    }
    
//------------------------------------------------------------------------------
    
    private void updateSymmetryRelations()
    {
        setSymmetricAPSets(identifySymmetryRelatedAPSets(mol, 
                getAttachmentPoints()));
    }
    
//------------------------------------------------------------------------------
    
    public static ArrayList<SymmetricSet> identifySymmetryRelatedAPSets(
            IAtomContainer mol,
            ArrayList<DENOPTIMAttachmentPoint> daps)
    {
        ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
        for (int i=0; i<daps.size()-1; i++)
        {
            ArrayList<Integer> lst = new ArrayList<>();
            Integer i1 = i;
            lst.add(i1);
            
            boolean alreadyFound = false;
            for (SymmetricSet previousSS : lstCompatible)
            {
                if (previousSS.contains(i1))
                {
                    alreadyFound = true;
                    break;
                }
            }
            if (alreadyFound)
            {
                continue;
            }
        
            DENOPTIMAttachmentPoint d1 = daps.get(i);
            for (int j=i+1; j<daps.size(); j++)
            {
                DENOPTIMAttachmentPoint d2 = daps.get(j);
                if (atomsAreCompatible(mol, d1.getAtomPositionNumber(),
                                    d2.getAtomPositionNumber()))
                {
                    // check if reactions are compatible w.r.t. symmetry
                    if (d1.getAPClass().compareTo(d2.getAPClass()) == 0)
                    {
                        Integer i2 = j;
                        lst.add(i2);
                    }
                }
            }
            
            if (lst.size() > 1)
            {
                lstCompatible.add(new SymmetricSet(lst));
            }
        }
        
        return lstCompatible;
    }
 
//------------------------------------------------------------------------------

    /**
     * Checks if the atoms at the given positions have similar environments
     * i.e. are similar in atom types etc.
     * @param mol
     * @param a1 atom position
     * @param a2 atom position
     * @return <code>true</code> if atoms have similar environments
     */

    private static boolean atomsAreCompatible(IAtomContainer mol, int a1, int a2)
    {
        // check atom types
        IAtom atm1 = mol.getAtom(a1);
        IAtom atm2 = mol.getAtom(a2);

        if (atm1.getSymbol().compareTo(atm2.getSymbol()) != 0)
            return false;

        // check connected bonds
        if (mol.getConnectedBondsCount(atm1)!=mol.getConnectedBondsCount(atm2))
            return false;


        // check connected atoms
        if (mol.getConnectedAtomsCount(atm1)!=mol.getConnectedAtomsCount(atm2))
            return false;

        List<IAtom> la1 = mol.getConnectedAtomsList(atm2);
        List<IAtom> la2 = mol.getConnectedAtomsList(atm2);

        int k = 0;
        for (int i=0; i<la1.size(); i++)
        {
            IAtom b1 = la1.get(i);
            for (int j=0; j<la2.size(); j++)
            {
                IAtom b2 = la2.get(j);
                if (b1.getSymbol().compareTo(b2.getSymbol()) == 0)
                {
                    k++;
                    break;
                }
            }
        }

        return k == la1.size();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtmId the index of the source atom in the atom list of this 
     * chemical representation. Index must be 0-based.
     * @param apc the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @param valence the valences used by this AP.
     * @throws DENOPTIMException 
     */

    public void addAP(int srcAtmId, APClass apc, Point3d vector, int valence) 
            throws DENOPTIMException
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        addAPOnAtom(srcAtm, apc, vector, valence);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtm the source atom in the atom list of this 
     * chemical representation.
     * @param apc the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @throws DENOPTIMException 
     */

    public void addAPOnAtom(IAtom srcAtm, APClass apc, Point3d vector)
            throws DENOPTIMException
    {
        addAPOnAtom(srcAtm,apc,vector,-1);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtm the source atom in the atom list of this 
     * chemical representation.
     * @param apc the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @param valence the valences used by this AP.
     * @throws DENOPTIMException 
     */

    private void addAPOnAtom(IAtom srcAtm, APClass apc, Point3d vector,
                             int valence)
            throws DENOPTIMException
    {
        int atmId = mol.indexOf(srcAtm);
        this.addAP(atmId, valence, valence,
                new double[] {vector.x, vector.y, vector.z}, apc);
        ArrayList<DENOPTIMAttachmentPoint> aps = this.getAttachmentPoints();
        DENOPTIMAttachmentPoint ap = aps.get(aps.size() - 1);

        ArrayList<DENOPTIMAttachmentPoint> apList = new ArrayList<>();
        if (getAPCountOnAtom(srcAtm) > 0) {
            apList = getAPListFromAtom(srcAtm);
        }
        apList.add(ap);
        srcAtm.setProperty(DENOPTIMConstants.APTAG, apList);
        
        updateSymmetryRelations();
    }

//-----------------------------------------------------------------------------
    
    private ArrayList<DENOPTIMAttachmentPoint> getAPListFromAtom(IAtom srcAtm)
    {
        @SuppressWarnings("unchecked")
		ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
        		(ArrayList<DENOPTIMAttachmentPoint>) srcAtm.getProperty(
        				DENOPTIMConstants.APTAG);
        return apsOnAtm;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns the number of APs currently defined on a specific atom source.
     * @param srcAtmId the index of the atom
     * @return the number of APs
     * @throws DENOPTIMException 
     */
    
    public int getAPCountOnAtom(int srcAtmId)
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        return getAPCountOnAtom(srcAtm);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the number of APs currently defined on a specific atom source.
     * @param srcAtm the source atom
     * @return the number of APs
     * @throws DENOPTIMException 
     */
    
    public int getAPCountOnAtom(IAtom srcAtm)
    {
    	int num = 0;
    	if (srcAtm.getProperty(DENOPTIMConstants.APTAG) != null)
    	{
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
					getAPListFromAtom(srcAtm);
        	num = apsOnAtm.size();
    	}
    	return num;
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Changes the properties of each APs as to reflect the current atom list.
     * DENOPTIMAttachmentPoint include the index of their source atom, and this
     * method updates such indexes to reflect the current atom list.
     * This method is needed upon reordering of the atom list.
     */
    
    public void updateAPs()
    {
        for (int atmId = 0; atmId<mol.getAtomCount(); atmId++)
        {
            IAtom srcAtm = mol.getAtom(atmId);
            if (srcAtm.getProperty(DENOPTIMConstants.APTAG) != null)
            {
            	ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
            			getAPListFromAtom(srcAtm);
	            for (int i = 0; i < apsOnAtm.size(); i++)
	            {
	                DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
	                ap.setAtomPositionNumber(atmId);
	            }
            }
        }
        
        updateSymmetryRelations();
    }

//-----------------------------------------------------------------------------
    
    /**
     * Collects APs currently defined as properties of the atoms.
     * Converts the internal notation defining APs (i.e., APs are stored in
     * as atom-specific properties) to the standard DENOPTIM formalism (i.e.,
     * APs are collected in a molecular property).
     * @return the list of APs. Note that these APs cannot respond to changes
     * in the atom list!
     */
    
    public ArrayList<DENOPTIMAttachmentPoint> getCurrentAPs()
    {
    	updateAPs();
    	
    	ArrayList<DENOPTIMAttachmentPoint> allAPs =
                new ArrayList<>();
    	
        for (IAtom srcAtm : mol.atoms())
        {
        	if (srcAtm.getProperty(DENOPTIMConstants.APTAG) != null)
            {
        		ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
        				getAPListFromAtom(srcAtm);
                allAPs.addAll(apsOnAtm);
            }
        }

        //Reorder according to DENOPTIMAttachmentPoint priority
        allAPs.sort(new DENOPTIMAttachmentPointComparator());
        
        //Sync the list of APs stored in superclass
        setAttachmentPoints(allAPs);
        
        return allAPs;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Uses the molecular property defining attachment points to create the 
     * DENOPTIMAttachmentPoint objects in the Atom objects. Does not
     * overwrite existing APs in the atoms.
     * @throws DENOPTIMException
     */
    
    public void projectPropertyToAP() throws DENOPTIMException
    {

	    String allAtomsProp = "";    
	    if (mol.getProperty(DENOPTIMConstants.APCVTAG) == null)
	    {
	    	System.out.println("WARNING: no tag " 
	    			+ DENOPTIMConstants.APCVTAG + "found in fragment."
	    			+ " No AP created.");
	    	return;
        }
	    allAtomsProp = mol.getProperty(DENOPTIMConstants.APCVTAG).toString();
	    projectPropertyToAP(allAtomsProp);
    }
	    
//-----------------------------------------------------------------------------
    
    /**
     * Uses a string formatted like the molecular property used for 
     * defining attachment points to create the 
     * DENOPTIMAttachmentPoint objects in the Atom objects. Does not
     * overwrite existing APs in the atoms.
     * @param allAtomsProp the content of the molecular property.
     * @throws DENOPTIMException
     */
    
    public void projectPropertyToAP(String allAtomsProp) throws DENOPTIMException
    {
    	if (allAtomsProp.trim().equals(""))
    	{
    		return;
    	}
    	
    	// Cleanup current APs in atom objects
    	for (int ii=0 ; ii<mol.getAtomCount(); ii++)
    	{
    		IAtom atm = mol.getAtom(ii);   		
    		atm.removeProperty(DENOPTIMConstants.APTAG);
    	}

	    // Temp storage for APs
	    ArrayList<DENOPTIMAttachmentPoint> allAPs = 
	    		new ArrayList<DENOPTIMAttachmentPoint>();
	   
	    // Collect all the APs as objects
	    String[] atomsProp = allAtomsProp.split(
	    		DENOPTIMConstants.SEPARATORAPPROPATMS);
	    for (int i = 0; i< atomsProp.length; i++)
	    {
			String onThisAtm = atomsProp[i];
			if (onThisAtm.contains(DENOPTIMConstants.SEPARATORAPPROPAPS))
			{
			    String[] moreAPonThisAtm = onThisAtm.split(
			    		DENOPTIMConstants.SEPARATORAPPROPAPS);
			    
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(this, moreAPonThisAtm[0],
                                "SDF");
			    
			    int atmID = ap.getAtomPositionNumber();
			    //WARNING the atmID is already 0-based
	            allAPs.add(ap);
			    for (int j = 1; j<moreAPonThisAtm.length; j++ )
			    {
			    	//WARNING here we have to switch to 1-based enumeration
			    	// because we import from SDF string
					DENOPTIMAttachmentPoint apMany = 
							new DENOPTIMAttachmentPoint(this, atmID+1
									+ DENOPTIMConstants.SEPARATORAPPROPAAP
									+ moreAPonThisAtm[j], "SDF");
					allAPs.add(apMany);
			    }
			} 
			else 
			{
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(this, onThisAtm,"SDF");
			    allAPs.add(ap);
			}
	    }

		// Write attachment points in the atoms
        for (int i = 0; i < allAPs.size(); i++)
        {
            DENOPTIMAttachmentPoint ap = allAPs.get(i);
            int atmID = ap.getAtomPositionNumber();
            
            if (atmID > mol.getAtomCount())
            {
            	throw new DENOPTIMException("Fragment property defines AP "
            			+ "with out-of-borders atom index (" + atmID + ").");
            }
            
            IAtom atm = mol.getAtom(atmID);
            if (atm.getProperty(DENOPTIMConstants.APTAG) != null)
            {
				ArrayList<DENOPTIMAttachmentPoint> oldAPs = 
						getAPListFromAtom(atm);
                oldAPs.add(ap);
                atm.setProperty(DENOPTIMConstants.APTAG,oldAPs);
            } 
            else
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = 
                		new ArrayList<DENOPTIMAttachmentPoint>();
                aps.add(ap);
                
                atm.setProperty(DENOPTIMConstants.APTAG,aps);
            }
        }

        //Overwrite the list of APs of the superclass
        setAttachmentPoints(allAPs);
        
        updateSymmetryRelations();
    }

//-----------------------------------------------------------------------------
    
    /**
     * Finds the DENOPTIMAttachmentPoint objects defined as properties of
     * the atoms in this container, and defines the string-based molecular
     * property used to print attachment points in SDF files.
     */
    public void projectAPsToProperties()
    {
        //WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n

    	String propAPClass = "";
        String propAttchPnt = "";
        for (IAtom atm : mol.atoms())
        {
        	//WARNING: here is the 1-based criterion implemented
        	int atmID = mol.getAtomNumber(atm)+1;
        	
        	if (atm.getProperty(DENOPTIMConstants.APTAG) == null)
            {
        		continue;
            }
        	
        	ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
        			getAPListFromAtom(atm);
	        
	        boolean firstCL = true;
	        for (int i = 0; i<apsOnAtm.size(); i++)
	        {
			    DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
	
			    //Build SDF property "CLASS"
			    String stingAPP = ""; //String Attachment Point Property
			    if (firstCL)
			    {
					firstCL = false;
					stingAPP = ap.getSingleAPStringSDF(true);
			    } 
			    else 
			    {
			    	stingAPP = DENOPTIMConstants.SEPARATORAPPROPAPS 
			    			+ ap.getSingleAPStringSDF(false);
			    }
			    propAPClass = propAPClass + stingAPP;
	
			    //Build SDF property "ATTACHMENT_POINT"
			    String sBO = FragmentSpace.getBondOrderForAPClass(
                        ap.getAPClass().toString()).toOldString();
			    String stBnd = " " + atmID +":"+sBO;
			    if (propAttchPnt.equals(""))
			    {
	                stBnd = stBnd.substring(1);
			    }
			    propAttchPnt = propAttchPnt + stBnd;
			}
	        propAPClass = propAPClass + DENOPTIMConstants.SEPARATORAPPROPATMS;
	    }

        mol.setProperty(DENOPTIMConstants.APCVTAG,propAPClass);
        mol.setProperty(DENOPTIMConstants.APTAG,propAttchPnt);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a deep copy of this fragments.
     * @throws CloneNotSupportedException 
     */
    
    @Override
    public DENOPTIMFragment clone()
    {   
    	DENOPTIMFragment clone = new DENOPTIMFragment();
		try {
		    this.projectAPsToProperties();
		    //deep copy of mol is created in the DENOPTIMFragment constructor
			clone = new DENOPTIMFragment(getVertexId(),mol,getBuildingBlockType());
		} catch (Exception e) {
		    e.printStackTrace();
			String msg = "Failed to clone DENOPTIMFragment! " +clone;
			System.err.println(msg);
		}
		clone.setBuildingBlockId(this.getBuildingBlockId());
		clone.setBuildingBlockType(this.getBuildingBlockType());
		
		ArrayList<SymmetricSet> cLstSymAPs = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : this.getSymmetricAPSets())
        {
            cLstSymAPs.add(ss.clone());
        }
        clone.setSymmetricAPSets(cLstSymAPs);
        
        clone.setAsRCV(this.isRCV());
        clone.setLevel(this.getLevel());
        
		return clone;
    }

//-----------------------------------------------------------------------------

    @Override
    public IAtomContainer getIAtomContainer()
    {
        this.projectAPsToProperties();
        for (int atmPos=0; atmPos<mol.getAtomCount(); atmPos++)
        {
            IAtom atm = mol.getAtom(atmPos);
            atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID, getVertexId());
            atm.setProperty(DENOPTIMConstants.ATMPROPORIGINALATMID, atmPos);
        }
        return mol;
    }
    
//-----------------------------------------------------------------------------

    public Iterable<IAtom> atoms()
    {
        return mol.atoms();
    }
    
//-----------------------------------------------------------------------------

    public Iterable<IBond> bonds()
    {
        return mol.bonds();
    }

//-----------------------------------------------------------------------------

    public void addAtom(IAtom atom)
    {
        mol.addAtom(atom);
    }   
    
//-----------------------------------------------------------------------------

    public IAtom getAtom(int number)
    {
        return mol.getAtom(number);
    }

//-----------------------------------------------------------------------------

    public int getAtomNumber(IAtom atom)
    {
        return mol.getAtomNumber(atom);
    }

//-----------------------------------------------------------------------------

    public int getAtomCount()
    {
        return mol.getAtomCount();
    }
    
//-----------------------------------------------------------------------------

    public void addBond(IBond bond)
    {
        mol.addBond(bond);
    }
    
//-----------------------------------------------------------------------------
    
    public IBond removeBond(int position)
    {
        return mol.removeBond(position);
    }

//-----------------------------------------------------------------------------
   
    public IBond removeBond(IAtom atom1, IAtom atom2)
    {
       return mol.removeBond(atom1, atom2);
    }
    
//-----------------------------------------------------------------------------
    
    public void removeBond(IBond bond)
    {
        mol.removeBond(bond);
    }
    
//-----------------------------------------------------------------------------
    
    public void removeAtomAndConnectedElectronContainers(IAtom atom)
    {
        mol.removeAtomAndConnectedElectronContainers(atom);
    }
    
//-----------------------------------------------------------------------------
    
    public List<IAtom> getConnectedAtomsList(IAtom atom)
    {
        return mol.getConnectedAtomsList(atom);
    }
    
//-----------------------------------------------------------------------------
    
    public int getConnectedAtomsCount(IAtom atom)
    {
        return mol.getConnectedAtomsCount(atom);
    }
 
//-----------------------------------------------------------------------------

    @Override
    public Object getProperty(Object description)
    {
        return mol.getProperty(description);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public void setProperty(Object description, Object property)
    {
        mol.setProperty(description, property);
    }
    
//-----------------------------------------------------------------------------
    
    public void setProperties(Map<Object, Object> properties)
    {
        mol.setProperties(properties);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another fragment ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMFragment other, StringBuilder reason)
    {
        if (this.containsAtoms() && other.containsAtoms())
        {
            IAtomContainer tMol = this.getIAtomContainer();
            IAtomContainer oMol = other.getIAtomContainer();
            if (tMol.getAtomCount() != oMol.getAtomCount())
            {
                reason.append("Different atom count (" 
                        + tMol.getAtomCount()+":"
                        + oMol.getAtomCount()+"); ");
            }
            if (tMol.getBondCount() != oMol.getBondCount())
            {
                reason.append("Different bond count (" 
                        + tMol.getBondCount()+":"
                        + oMol.getBondCount()+"); ");
            }
            /*
            //TODO: use fragment comparator from GM3DFragmenter
            for (int i=0; i<tMol.getAtomCount(); i++)
            {
                IAtom tAtm = tMol.getAtom(i);
                IAtom oAtm = oMol.getAtom(i);
            }
            */
        }
        return sameVertexFeatures(other, reason);
    }
  
//------------------------------------------------------------------------------

    public int getHeavyAtomsCount()
    {
        return DENOPTIMMoleculeUtils.getHeavyAtomCount(mol);
    }

//------------------------------------------------------------------------------

    public boolean containsAtoms()
    {
        if (mol.getAtomCount() > 0)
            return true;
        else
            return false;
    }
    
//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return getVertexId()+ "_" + (getBuildingBlockId() + 1) + "_" +
                getBuildingBlockType().toOldInt() + "_" + getLevel();
    }
    
//------------------------------------------------------------------------------

    @Override
    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> lstAP)
    {
        this.lstAPs = lstAP;
    }
    
//------------------------------------------------------------------------------

    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs)
    {
        this.lstSymAPs = sAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        return lstSymAPs;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<DENOPTIMVertex> getMutationSites()
    {
        Set<DENOPTIMVertex> set = new HashSet<DENOPTIMVertex>();
        switch (getBuildingBlockType())
        {
            case CAP:
                break;
                
            case SCAFFOLD:
                break;
                
            default:
                set.add(this);
                break;
        }
        return set;
    }
    
//------------------------------------------------------------------------------

}
