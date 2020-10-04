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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import denoptim.logging.DENOPTIMLogger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.FragmentUtils;

/**
 * Class representing a continuously connected portion of molecular object
 * holding attachment points.
 * 
 * @author Marco Foscato
 */

public class DENOPTIMFragment extends DENOPTIMVertex
{ 	
    /**
	 * Version UID
	 */
	private static final long serialVersionUID = 4415462924969433010L;
	
    /**
     * Index of the graph building block contained in the vertex
     */
    private int buildingBlockId;
    
    //TODO-V3 to enum
    /*
     * 0:scaffold, 1:fragment, 2:capping group
     */
    private int buildingBlockType;
    
	/**
	 * Molecular representation of this fragment
	 */
	private final IAtomContainer mol;

//-----------------------------------------------------------------------------

    /**
     * Constructor of an empty fragment
     */
    
    public DENOPTIMFragment()
    {
        super();
        this.mol = new AtomContainer();
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a molecular fragment kind of vertex.
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbType the type of building block 0:scaffold, 1:fragment, 2:capping group
     */
    @Deprecated
    public DENOPTIMFragment(int vertexId, int bbId, int bbType)
    {
        super(vertexId);
       
        //TODO-V3 : get rid of this constructor
        // FragmentSpace.getFragment(bbType,bbId) already return a clone of the original
        

      //TODO-V3 delete. only until FRagSpace will not return mol
        this.mol = new AtomContainer();
        
        //TODO-V3 get rid of interface
        DENOPTIMVertex bb = null;
        try
        {
            bb = FragmentSpace.getFragment(bbType,bbId).clone();
            
            //TODO-V3 get IAC from library
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
            String msg = "Fatal error! Cannot continue. " + e.getMessage();
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            System.exit(0);
        }
        
        this.setAttachmentPoints(bb.getAttachmentPoints());
        this.setSymmetricAP(bb.getSymmetricAP());
        this.setAsRCV(getAPCount() == 1 
                && DENOPTIMConstants.RCAAPCLASSSET.contains(
                        this.getAttachmentPoints().get(0).getAPClass()));
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties. WARNING: properties of the atom container are not
     * imported!
     * @param mol the molecular representation
     * @throws DENOPTIMException 
     */
    
    public DENOPTIMFragment(IAtomContainer mol) throws DENOPTIMException
    {    	
    	// WARNING: atom container properties are not imported
        this.mol = new AtomContainer(mol);
        
        Object prop = mol.getProperty(DENOPTIMConstants.APCVTAG);
        if (prop != null)
        {
            projectPropertyToAP(prop.toString());
        }
    }
    
//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a fragment
     */

    public int getFragmentType()
    {
        return buildingBlockType;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return the id of the molecule
     */
    public int getMolId()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------

    public void setMolId(int m_molId)
    {
        buildingBlockId = m_molId;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtmId the index of the source atom in the atom list of this 
     * chemical representation. Index must be 0-based.
     * @param propAPClass the attachment point class, or null, if class should not 
     * be defined.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @throws DENOPTIMException 
     */

    public void addAP(int srcAtmId, String propAPClass, double[] vector) 
    		throws DENOPTIMException
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        Point3d p3d = new Point3d(vector[0], vector[1], vector[2]);
        addAP(srcAtm, propAPClass, p3d);
    }

//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtmId the index of the source atom in the atom list of this 
     * chemical representation. Index must be 0-based.
     * @param propAPClass the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @throws DENOPTIMException 
     */

    public void addAP(int srcAtmId, String propAPClass, Point3d vector) 
    		throws DENOPTIMException
    {
        IAtom srcAtm = mol.getAtom(srcAtmId);
        addAP(srcAtm, propAPClass, vector);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Add an attachment point to the specifies atom
     * @param srcAtm the source atom in the atom list of this 
     * chemical representation.
     * @param propAPClass the attachment point class, or null, if class should not 
     * be defines.
     * @param vector the coordinates of the 3D point representing the end of 
     * the attachment point direction vector, or null. The coordinates must be
     * consistent with the coordinates of the atoms.
     * @throws DENOPTIMException 
     */

    public void addAP(IAtom srcAtm, String propAPClass, Point3d vector) 
    		throws DENOPTIMException
    {
    	int atmId = mol.getAtomNumber(srcAtm);
    	DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint();
    	ap.setAtomPositionNumber(atmId);
    	ap.setAPClass(propAPClass);
    	ap.setDirectionVector(new double[]{vector.x, vector.y, vector.z});
    	
    	//This adds the AP to the list of the superclass
    	addAttachmentPoint(ap);
    	
    	ArrayList<DENOPTIMAttachmentPoint> apList;
        if (getAPCountOnAtom(srcAtm) > 0)
        {
        	apList = getAPListFromAtom(srcAtm);
        	apList.add(ap);
        }
        else
        {
        	apList = new ArrayList<DENOPTIMAttachmentPoint>();
        	apList.add(ap);
    	}
        
        srcAtm.setProperty(DENOPTIMConstants.APTAG, apList);
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
     * Returns the number of APs currently defined.
     * @return the number of APs
     */
    
    public int getAPCount()
    {
    	int num = 0;
        for (int atmId = 0; atmId<mol.getAtomCount(); atmId++)
        {
            num = num + getAPCountOnAtom(atmId);
        }
    	return num;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns the list of all APClasses on present on this fragment.
     * @return the list of APClassess
     */
    
    //TODO-V3 remove, moved to DENOPTIMVertex
    
    @Deprecated
    public ArrayList<String> getAllAPClassess()
    {
    	ArrayList<String> lst = new ArrayList<String>();
    	for (DENOPTIMAttachmentPoint ap : getCurrentAPs())
    	{
    		String apCls = ap.getAPClass();
    		if (!lst.contains(apCls))
    		{
    			lst.add(apCls);
    		}
    	}
    	return lst;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Returns clones of all attachment points currently defined on this 
     * fragment.
     * @return the list of AP clones
     */

	public ArrayList<DENOPTIMAttachmentPoint> getAPs() 
	{
	    //TODO-V3 use or overwrite the method from vertex
	    //TODO-V3 clarify if and when we return copy/original
		ArrayList<DENOPTIMAttachmentPoint> original = getCurrentAPs();
		ArrayList<DENOPTIMAttachmentPoint> copy =
                new ArrayList<>(original.size());
		for (DENOPTIMAttachmentPoint ap : original) {
		    copy.add(ap.clone());
        }
		return copy;
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
        
        //TODO-V3 make sure the update affects also the reference hosted in vertex.lstAPs
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
			    		new DENOPTIMAttachmentPoint(moreAPonThisAtm[0],"SDF");
			    
			    int atmID = ap.getAtomPositionNumber();
			    //WARNING the atmID is already 0-based
	            allAPs.add(ap);
			    for (int j = 1; j<moreAPonThisAtm.length; j++ )
			    {
			    	//WARNING here we have to switch to 1-based enumeration
			    	// because we import from SDF string
					DENOPTIMAttachmentPoint apMany = 
							new DENOPTIMAttachmentPoint(atmID+1 
									+ DENOPTIMConstants.SEPARATORAPPROPAAP
									+ moreAPonThisAtm[j], "SDF");
					allAPs.add(apMany);
			    }
			} 
			else 
			{
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(onThisAtm,"SDF");
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
        		//System.out.println("No property "+DENOPTIMConstants.APTAG);
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
			    //TODO-V3 del
			    
			    int BndOrd = 1;
                try
                {
                    BndOrd = FragmentSpace.getBondOrderForAPClass(
                    		ap.getAPClass());
                } catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    System.out.println("HERE HERE: "+ap);
                    System.out.println("BO-MAP: "+FragmentSpace.getBondOrderMap());
                    
                }
			    String sBO = Integer.toString(BndOrd);
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
    public DENOPTIMFragment clone()
    {   
    	DENOPTIMFragment frg = new DENOPTIMFragment();
		try {
		    this.projectAPsToProperties();
			frg = new DENOPTIMFragment(mol);
		} catch (Exception e) {
			//TODO: by-pass by copying all atoms, bonds and el. containers,
			// atom properties and atomcontainer properties (what else?)
			String msg = "Failed to clone DENOPTIMFragment! " +frg;
			System.err.println(msg);
		}
		
		//TODO-V3: check consistency with content of vertex.lstAPs
		return frg;
    }
    
//-----------------------------------------------------------------------------
   
    /**
     * Returns the list of attachment points that can be treated the same
     * way (i.e., symmetry-related attachment points). 
     * Note that symmetry, in this context, is
     * a defined by the topological environment surrounding the attachment 
     * point and has nothing to to with symmetry in three-dimensional space.  
     * @return the list of symmetry-related attachment points.
     */
    public ArrayList<SymmetricSet> getSymmetricAPsSets() {
        ArrayList<DENOPTIMAttachmentPoint> aps = getAPs();
        IAtomContainer iac = this.getIAtomContainer();
        ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
        for (int i = 0; i < aps.size() - 1; i++) {
            ArrayList<Integer> lst = new ArrayList<>();
            Integer i1 = i;
            lst.add(i1);

            boolean alreadyFound = false;
            for (SymmetricSet previousSS : lstCompatible) {
                if (previousSS.contains(i1)) {
                    alreadyFound = true;
                    break;
                }
            }

            if (alreadyFound) {
                continue;
            }

            DENOPTIMAttachmentPoint d1 = aps.get(i);
            for (int j = i + 1; j < aps.size(); j++) {
                DENOPTIMAttachmentPoint d2 = aps.get(j);
                if (FragmentUtils.isCompatible(iac, d1.getAtomPositionNumber(),
                        d2.getAtomPositionNumber())) {
                    // check if reactions are compatible
                    if (FragmentUtils.isFragmentClassCompatible(d1, d2)) {
                        Integer i2 = j;
                        lst.add(i2);
                    }
                }
            }

            if (lst.size() > 1) {
                lstCompatible.add(new SymmetricSet(lst));
            }
        }

        return lstCompatible;
    }
    
//-----------------------------------------------------------------------------

    @Override
    public IAtomContainer getIAtomContainer()
    {
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

    public Object getProperty(Object description)
    {
        return mol.getProperty(description);
    }
    
//-----------------------------------------------------------------------------
    
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
     * @return <code>true</code> if the two vertexes represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {
        if (other instanceof DENOPTIMFragment)
            return sameAs((DENOPTIMFragment) other, reason);
        else
            return false;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another fragment ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertexes represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMFragment other, StringBuilder reason)
    {
        if (this.getFragmentType() != other.getFragmentType())
        {
            reason.append("Different fragment type ("+this.getFragmentType()+":"
                    +other.getFragmentType()+"); ");
            return false;
        }
        
        if (this.getMolId() != other.getMolId())
        {
            reason.append("Different molID ("+this.getMolId()+":"
                    +other.getMolId()+"); ");
            return false;
        }
        
        return ((DENOPTIMVertex) this).sameAs(((DENOPTIMVertex)other), reason);
    }
    
//-----------------------------------------------------------------------------
    
}
