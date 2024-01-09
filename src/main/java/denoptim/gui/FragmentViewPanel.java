/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.gui;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.vecmath.Point3d;

import org.jmol.viewer.Viewer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.io.DenoptimIO;
import denoptim.utils.MathUtils;
import denoptim.utils.MoleculeUtils;


/**
 * A panel with a molecular viewer and attachment point table.
 * 
 * @author Marco Foscato
 */

public class FragmentViewPanel extends JSplitPane implements IVertexAPSelection
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded fragment
	 */
	private Fragment fragment;
	
	/**
	 * Temporary list of attachment points of the current fragment
	 */
	protected Map<Integer,AttachmentPoint> mapAPs = null;
	
	/**
	 * Flag signalling that data about APs has been changed in the GUI
	 */
	public boolean alteredAPData = false;
	
	private JmolPanel jmolPanel;
	private JScrollPane tabPanel;
	protected DefaultTableModel apTabModel;
	protected JTable apTable;
	
	private boolean editableAPTable = false;
	
	private final String NL = System.getProperty("line.separator");
	
	private String tmpSDFFile;
	
	private JComponent parent;
	

//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public FragmentViewPanel(boolean editableTable)
	{
		this(null, editableTable);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param parent the parent component
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public FragmentViewPanel(JComponent parent, boolean editableTable)
	{
		this(parent,editableTable,340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param parent the parent component
	 * @param editableTable use <code>true</code> to make the AP table editable
	 * @param dividerPosition allows to set the initial position of the divide
	 */
	public FragmentViewPanel(JComponent parent, boolean editableTable, 
	        int dividerPosition)
	{
		this.parent = parent;
		editableAPTable = editableTable;
		initialize(dividerPosition);
	}
	
//-----------------------------------------------------------------------------

	@SuppressWarnings("serial")
	private void initialize(int dividerPosition)
	{	
		this.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.setOneTouchExpandable(true);
		this.setDividerLocation(dividerPosition);
		this.setResizeWeight(0.5);
        
        // Jmol viewer panel
        jmolPanel = new JmolPanel();
        this.setTopComponent(jmolPanel);
        
		// List of attachment points
		apTabModel = new DefaultTableModel() {
			@Override
		    public boolean isCellEditable(int row, int column) {
				if (column == 0)
				{
					return false;
				}
				else
			    {
					return editableAPTable;
			    }
		    }
		};
		apTabModel.setColumnCount(2);
		String column_names[]= {"<html><b>AP#</b></html>", "<html><b>APClass</b></html>"};
		apTabModel.setColumnIdentifiers(column_names);
		apTable = new JTable(apTabModel);
		apTable.putClientProperty("terminateEditOnFocusLost", true);
		apTable.getColumnModel().getColumn(0).setMaxWidth(75);
		apTable.setGridColor(Color.LIGHT_GRAY);
		JTableHeader apTabHeader = apTable.getTableHeader();
		apTabHeader.setPreferredSize(new Dimension(100, 20));
		apTabModel.addTableModelListener(new PausableTableModelListener());
		tabPanel = new JScrollPane(apTable);
		tabPanel.setMinimumSize(new Dimension(100,30));
		this.setBottomComponent(tabPanel);
		
		//Find a proper tmp disk space
		tmpSDFFile = Utils.getTempFile("Denoptim_FragViewer_loadedMol.sdf");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check for unsaved edits to the AP data
	 * @return <code>true</code> if there are unsaved edits
	 */
	public boolean hasUnsavedAPEdits()
	{
		return alteredAPData;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Overrides the flag signaling unsaved edits to saying that there are no
	 * altered data. Use this after processing/saving the changes.
	 */
	public void deprotectEdits()
	{
		alteredAPData = false;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Waits until Jmol is finished.
	 * @param milliSecFirst
	 */
	private void waitForJmolViewer(int milliSecFirst, String cause)
	{
		if (parent!=null)
		{
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		} else {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		
		// We wait
		try {
			Thread.sleep(milliSecFirst);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Waiting cycles
		Date date = new Date();
		long startTime = date.getTime();
		long wallTime = startTime + 5000; // 5 seconds
		while (jmolPanel.viewer.isScriptExecuting())
		{	
			Date newDate = new Date();
			long now = newDate.getTime();
			System.out.println("Waiting for Jmol (timespot "+now+")");
			if (now > wallTime)
			{
				if (parent!=null)
				{
					parent.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
				} else {
					this.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
				}
				String[] options = new String[]{"Yes","No"};
				int res = JOptionPane.showOptionDialog(this,
		                "<html>" + cause + ".<br>Keep waiting? (5 sec)</html>",
		                "Should we wait for another 5 seconds?",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[1]);
				if (res == 1)
				{
					System.out.println("Give up waiting");
					jmolPanel.viewer.haltScriptExecution();
					//no data is coming, so we have to 'zap'
					clearMolecularViewer(false);
					if (parent!=null)
					{
						parent.setCursor(Cursor.getPredefinedCursor(
								Cursor.DEFAULT_CURSOR));
					} else {
						this.setCursor(Cursor.getPredefinedCursor(
								Cursor.DEFAULT_CURSOR));
					}
					break;
				}
				else
				{
					System.out.println("Keep waiting...");
					if (parent!=null)
					{
						parent.setCursor(Cursor.getPredefinedCursor(
								Cursor.WAIT_CURSOR));
					} else {
						this.setCursor(Cursor.getPredefinedCursor(
								Cursor.WAIT_CURSOR));
					}
					newDate = new Date();
					wallTime = newDate.getTime() + 5000; // 5 seconds
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void make3DusingCDKgenerator(String smiles) throws CDKException, 
	CloneNotSupportedException, IOException
	{
		IAtomContainer mol = null;

		SmilesParser sp = new SmilesParser(
				DefaultChemObjectBuilder.getInstance());
		mol = sp.parseSmiles(smiles);
		
		// This seems to be needed in the opposite order since cdk-2.3
		/*
	    CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(
	    		mol.getBuilder());
	    adder.addImplicitHydrogens(mol);
		*/
		
		CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(
				mol.getBuilder());
	    for (IAtom atom : mol.atoms()) 
	    {
	        IAtomType type = matcher.findMatchingAtomType(mol, atom);
	        AtomTypeManipulator.configure(atom, type);
	    }
	    
	    CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(
                mol.getBuilder());
        adder.addImplicitHydrogens(mol);
	    
	    AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);

        ModelBuilder3D mb3d = ModelBuilder3D.getInstance(
                TemplateHandler3D.getInstance(), "mmff94", 
                DefaultChemObjectBuilder.getInstance());
		mol = mb3d.generate3DCoordinates(mol, false);

		// Load the system int oJmol
		loadPlainStructure(mol);
		
		// Run Jmol MM energy minimization
		jmolPanel.viewer.evalString("minimize");
		waitForJmolViewer(1500,"Energy minimization is taking some time.");
		
		// Re-load
		IAtomContainer iac;
		try {
			iac = getStructureFromJmolViewer();
			loadPlainStructure(iac);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			//no data is coming, so we have to 'zap'
			clearMolecularViewer(false);
		}
	}
	
//-----------------------------------------------------------------------------
	
	public String getDataFromJmol()
	{
	    String data = jmolPanel.viewer.getData("*", "txt");
	    data = data.replaceAll(" Xx  ", 
	            " " + DENOPTIMConstants.DUMMYATMSYMBOL + "  ");
		return data;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a molecule build from a smiles string. The 3D geometry is either 
	 * taken from remote CACTUS service (requires connection
	 * to the Internet) or built with CDK tools, as a fall-back. The CDK
	 * builder, however will produce a somewhat lower quality conformation than
	 * that obtained from on online generator.
	 * @param smiles the SMILES of the molecule to load
	 * @return <code>true</code> if the SMILES could be converted into a 3D 
	 * structure
	 * @throws DENOPTIMException 
	 */
	public boolean loadSMILES(String smiles)
	{	
		if (parent!=null)
		{
			parent.setCursor(Cursor.getPredefinedCursor(
					Cursor.WAIT_CURSOR));
		} else {
			this.setCursor(Cursor.getPredefinedCursor(
					Cursor.WAIT_CURSOR));
		}
		
		switch (GUIPreferences.smiTo3dResolver)
		{
		case CACTVS:
			jmolPanel.viewer.evalString("load $"+smiles);
			waitForJmolViewer(1500,
					"Slow response from https://cactus.nci.nih.gov");
			// NB: this is a workaround to the lack of try/catch mechanism when
			// executing Jmol commands.
			// We want to catch errors that prevent loading the structure
			// For example, offline mode and invalid SMILES
			String data = getDataFromJmol();
			
			if (data == null || data.equals(""))
			{
				String[] options = new String[] {"Build 3D guess","Abandon"}; 
				int res = JOptionPane.showOptionDialog(this,
		                "<html>Could not find a valid structure.<br>"
		                + "Possible reasons are:"
		                + "<ul>"
		                + "<li>unreachable remote service (we are offline)</li>"
		                + "<li>too slow response from online service</li>"
		                + "<li>no available structure for SMILES string<br>'" 
		                + smiles 
		                + "'</li></ul><br>"
		                + "You can try to build a guess 3D structure, or<br>"
		                + "prepare a refined structure elsewhere and import "
		                + "it as "
		                + "SDF file.</html>",
		                "Error",
		                JOptionPane.OK_CANCEL_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"),
		                options, 
		                options[0]);
				if (res == 0)
				{
					try {
						make3DusingCDKgenerator(smiles);
					} catch (CDKException | CloneNotSupportedException 
							| IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this,
				                "<html>Could not make 3D structure from SMILES."
				                + " Cause: '"+e1.getMessage()+"'</html>",
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
						if (parent!=null)
						{
							parent.setCursor(Cursor.getPredefinedCursor(
									Cursor.DEFAULT_CURSOR));
						} else {
							this.setCursor(Cursor.getPredefinedCursor(
									Cursor.DEFAULT_CURSOR));
						}
						return false;
					}
				} else {
					if (parent!=null)
					{
						parent.setCursor(Cursor.getPredefinedCursor(
								Cursor.DEFAULT_CURSOR));
					} else {
						this.setCursor(Cursor.getPredefinedCursor(
								Cursor.DEFAULT_CURSOR));
					}
					return false;
				}
			}
			break;
			
		case CDK:
			try {
				make3DusingCDKgenerator(smiles);
			} catch (CDKException | CloneNotSupportedException 
					| IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(this,
		                "<html>Could not make 3D structure from SMILES. "
		                + "Cause: '"+e1.getMessage()+"'</html>",
		                "Error",
		                JOptionPane.ERROR_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
				if (parent!=null)
				{
					parent.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
				} else {
					this.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
				}
				return false;
			}
			break;
		}
		
		// Now we should have a structure loaded in the viewer, 
		// so we take that one and put it in the IAtomContainer representation
		try	{ 
		    // WARNING! Here we assume we are always making undefined building
		    // blocks. Namely, neither a scaffold, nor a Fragment, nor a 
		    // capping group. This because when we make frags 
		    // from the GUI we are only saving an SDF file. The latter is later
		    // imported as part of the library of scaffolds/fragments/capping
		    // groups, and in that moment the BBType is re-assigned.
		    fragment = new Fragment(getStructureFromJmolViewer(),
		            Vertex.BBType.FRAGMENT);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
	                "<html>Could not understand Jmol system.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return false;
		}
		
		setJmolViewer();

        jmolPanel.viewer.evalString("zoom OUT");
		
		if (parent!=null)
		{
			parent.setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
		} else {
			this.setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
		}
		return true;
	}
	
//-----------------------------------------------------------------------------

	private IAtomContainer getStructureFromJmolViewer() 
			throws DENOPTIMException
	{
	    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
		IAtomContainer mol = builder.newAtomContainer();
		
		String strData = getDataFromJmol();
		if (strData.trim().equals(""))
		{
			throw new DENOPTIMException("Attempt to get data from viewer, but data is empty");
		}
		strData = strData.replaceAll("[0-9] \\s+ 999 V2000", 
				"0  0  0  0  0999 V2000");
		String[] lines = strData.split("\\n");
		if (lines.length < 5)
		{
		    //no data is coming, so we have to 'zap'
			clearMolecularViewer(false);
			clearAPTable();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: '" + strData + "'");
		}
		if (!lines[3].matches(".*999 V2000.*"))
		{
		    //no data is coming, so we have to 'zap'
			clearMolecularViewer(false);
			clearAPTable();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: " + strData);
		}
		int nAtms = Integer.parseInt(lines[3].substring(0,3).trim());
		int nBonds = Integer.parseInt(lines[3].substring(3,6).trim());
		
		StringBuilder sb = new StringBuilder();
		sb.append("Structure in JmolViewer").append(NL);
		sb.append("Jmol").append(NL).append(NL);
		sb.append(lines[3]).append(NL);
		for (int i=0; i<nAtms; i++)
		{
			sb.append(lines[3+i+1]).append("  0  0  0  0  0  0").append(NL);
		}
		for (int i=0; i<nBonds; i++)
		{
			sb.append(lines[3+nAtms+i+1]).append("  0").append(NL);
		}
		sb.append("M  END").append(NL).append("$$$$");
		
		DenoptimIO.writeData(tmpSDFFile, sb.toString(), false);
		try
        {
            mol = DenoptimIO.readAllAtomContainers(new File(tmpSDFFile)).get(0);
        } catch (Exception e)
        {
            throw new DENOPTIMException("Unable to fetch molecule from tmp SDF",
                    e);
        }
		return mol;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Returns the chemical representation of the currently loaded chemical
	 * object. In case of mismatch between the system loaded into the Jmol
	 * viewer and the one in the local memory, we take that from Jmol and
	 * made it be The 'current fragment'. Previously set references to the
	 * previous 'current fragment' will make no sense anymore.
	 * @return the chemical representation of what is currently visualised.
	 * Can be empty and null.
	 */
	public Fragment getLoadedStructure()
	{
		Fragment fromViewer = new Fragment();
		try {
			fromViewer = new Fragment(getStructureFromJmolViewer(),
			        Vertex.BBType.FRAGMENT);
			putAPsFromTableIntoIAtomContainer(fromViewer);
		} catch (DENOPTIMException e) {
			//e.printStackTrace();
			return fragment;
		}
		
		// NB: APs cannot be added directly into Jmol, so the only thing
		// that can change there is the molecular structure
		if (fromViewer.getAtomCount() != fragment.getAtomCount())
		{
			fragment = fromViewer;
			return fragment;
		}
		
		boolean sameSMILES = false;
		try {
		    Logger logger = Logger.getLogger(GUI.GUILOGGER);
			sameSMILES = MoleculeUtils.getSMILESForMolecule(
					fromViewer.getIAtomContainer(),logger)
					.equals(MoleculeUtils.getSMILESForMolecule(
							fragment.getIAtomContainer(), logger));
		} catch (DENOPTIMException e) {
			// we get false
		}
		if (!sameSMILES)
		{
			fragment = fromViewer;
			return fragment;
		}
		
		//Maybe the geometry is different
		double thrld = 0.0001;
		for (int i=0; i<fragment.getAtomCount(); i++)
		{
			Point3d pA = MoleculeUtils.getPoint3d(
			        fromViewer.getAtom(i));
			Point3d pB = MoleculeUtils.getPoint3d(
			        fragment.getAtom(i));
			if (pA.distance(pB)>thrld)
			{
				fragment = fromViewer;
				return fragment;
			}
		}

		setJmolViewer();

		return fragment;
	}
	
//-----------------------------------------------------------------------------
	
	private void putAPsFromTableIntoIAtomContainer(Fragment mol) 
			throws DENOPTIMException 
	{
		if (mapAPs == null || mapAPs.isEmpty())
		{
			return;
		}
		
		if (mol.getNumberOfAPs() == mapAPs.size())
		{
			return;
		}
		
        for (int apId : mapAPs.keySet())
        {
        	AttachmentPoint ap = mapAPs.get(apId);
        	int srcAtmId = ap.getAtomPositionNumber();
        	
        	//NB here the inequity considers two completely disjoint indexes
        	//but is the only thing that seems valid at the stage were the atoms
        	//contained in the Jmol viewer may vary freely from the APs 
        	//collected in mapAPs
        	if (srcAtmId > mol.getAtomCount())
        	{
        		throw new DENOPTIMException("The atom list has changed and is "
        				+ "no longer compatible with the list of attachment "
        				+ "points. Cannot convert the current system to a "
        				+ "valid fragment. "
        				+ "srcAtmId:" + srcAtmId 
        				+ " #atms:" + mol.getAtomCount());
        	}
        	mol.addAP(srcAtmId, ap.getAPClass(), 
        	        new Point3d(ap.getDirectionVector()));
        }
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 * @return the atom container created to visualize the molecular content.
	 * This is needed for operations involving the identification by means of 
	 * reference to the atoms selected/present in the viewer.
	 */
	public void loadPlainStructure(IAtomContainer mol)
	{
		try {
			fragment = new Fragment(mol, Vertex.BBType.UNDEFINED);
	        loadStructure();
		} catch (DENOPTIMException e) {
			//Should never happen
			e.printStackTrace();
		}
	}
		
//-----------------------------------------------------------------------------
    
    /**
     * Loads the structure of the currently loaded 'fragment' (i.e., our member)
     * into the Jmol viewer.
     */
    private void loadStructure()
    {		
		
		if (fragment == null)
		{
			JOptionPane.showMessageDialog(this,
	                "<html>No structure loaded.<br>This is most likely a bug "
			        + "in FragmentViewPanel. "
	                + "Please report it to the development team.</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;			
		}
		
		// NB: we use the nasty trick of a tmp file to by-pass the 
		// fragile/discontinued CDK-to-Jmol support.
		
		try {
			DenoptimIO.writeSDFFile(tmpSDFFile, fragment.getIAtomContainer(), 
			        false);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			System.out.println("Error writing TMP file '" + tmpSDFFile + "'");
			System.out.println("Please, report this to the DENOPTIM team.");
		}
		
		jmolPanel.viewer.openFile(tmpSDFFile);

		setJmolViewer();

        jmolPanel.viewer.evalString("zoom OUT");
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Loads the given fragments to this viewer.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 * @param frag the fragment to visualize
	 */
	public void loadFragmentToViewer(Fragment frag)
	{	
		this.fragment = frag;			
		loadStructure();		
		updateAPsMapAndTable();        
        updateAPsInJmolViewer();        
        preSelectAPs();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Uses the AP of the {@link Fragment} to create a new map and 
	 * table of APs.
	 */
	private void updateAPsMapAndTable()
	{
		clearAPTable();
		mapAPs = new HashMap<Integer,AttachmentPoint>();
		
		List<AttachmentPoint> lstAPs = fragment.getCurrentAPs();		
        if (lstAPs.size() == 0)
        {
			return;
        }
        
        activateTabEditsListener(false);
        int arrId = 0;
	    for (AttachmentPoint ap : lstAPs)
	    {
	    	arrId++;
	    	apTabModel.addRow(new Object[]{arrId, ap.getAPClass()});
	    	mapAPs.put(arrId,ap);
	    }
	    activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void preSelectAPs()
	{
		String PRESELPROP = GUIVertexSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIVertexSelector.PRESELECTEDAPSFIELDSEP;
		
		if (fragment.getProperty(PRESELPROP) == null)
		{
			return;
		}
		
		String prop = fragment.getProperty(PRESELPROP).toString();
		String[] parts =prop.split(PRESELPROPSEP);
		
		activateTabEditsListener(false);
		for (int i=0; i<parts.length; i++)
		{
			int apId = Integer.parseInt(parts[i]); //0-based
			apTable.getSelectionModel().addSelectionInterval(apId, apId);
		}
		activateTabEditsListener(true);
	}

//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 * @param dataIsComing set <code>true</code> when there is incoming 
     * molecular data to visualize. In such case we do not run the very slow
     * <code>zap</code> script in JMol because the molecular data will be 
     * overwritten anyway.
	 */
	public void clearAll(boolean dataIsComing)
	{
		clearAPTable();
		clearMolecularViewer(dataIsComing);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the table of attachment points
	 */
	public void clearAPTable()
	{
		activateTabEditsListener(false);
		int initRowCount = apTabModel.getRowCount();
        for (int i=0; i<initRowCount; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	apTabModel.removeRow(0);
        }
        activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the molecular viewer. <b>WARNING:</b> this is VERY SLOW: do not do
     * it unless you are sure you really need to clear the data. Typically,
     * if there is incoming data, you do not need to run this, as the old data 
     * will be overwritten anyway.
     * @param dataIsComing set <code>true</code> when there is incoming 
     * molecular data to visualize.
	 */
	public void clearMolecularViewer(boolean dataIsComing)
	{
	    if (!dataIsComing)
            jmolPanel.viewer.evalString("zap");
	}

//-----------------------------------------------------------------------------
	
	private void updateAPsInJmolViewer()
	{   
		if (mapAPs == null || mapAPs.isEmpty())
		{
			return;
		}
		
		StringBuilder sb = new StringBuilder();
        for (int arrId : mapAPs.keySet())
        {
        	AttachmentPoint ap = mapAPs.get(arrId);
        	int srcAtmId = ap.getAtomPositionNumber();
        	Point3d srcAtmPlace = MoleculeUtils.getPoint3d(
        			fragment.getAtom(srcAtmId));
        	double[] startArrow = new double[]{
        			srcAtmPlace.x,
        			srcAtmPlace.y,
        			srcAtmPlace.z};
        	double[] endArrow = new double[]{ap.getDirectionVector().x,
        	        ap.getDirectionVector().y, ap.getDirectionVector().z};
        	
        	if (startArrow == null || endArrow==null)
        	{
        	    System.out.println("WARNING: AP without geometrical data will "
        	            + "be ignored! Ignoring ap "+ap);
        	    continue;
        	}
        	
        	double[] offSet = MathUtils.scale(
        			MathUtils.subtract(endArrow,startArrow), 0.2);
        	double[] positionLabel = MathUtils.add(endArrow,offSet); 
        	sb.append("draw arrow").append(arrId).append(" arrow ");
        	sb.append(getJmolPositionStr(startArrow));
        	sb.append(getJmolPositionStr(endArrow));
        	sb.append(" width 0.1");
        	sb.append(NL);
        	sb.append("set echo apLab").append(arrId);
        	sb.append(getJmolPositionStr(positionLabel));
        	sb.append("; echo ").append(arrId);
        	sb.append("; color echo yellow");
        	sb.append(NL);
        }
        jmolPanel.viewer.evalString(sb.toString());
	}
	
//-----------------------------------------------------------------------------
	
	private String getJmolPositionStr(double[] position)
	{
		StringBuilder sb = new StringBuilder();
    	sb.append(" {");
    	sb.append(position[0]).append(" ");
    	sb.append(position[1]).append(" ");
    	sb.append(position[2]).append("} ");
		return sb.toString();
	}
	
//-----------------------------------------------------------------------------
	
	private void setJmolViewer()
	{
		StringBuilder sb = new StringBuilder();		
		sb.append("select none").append(NL); 
		sb.append("SelectionHalos ON").append(NL);
		sb.append("set picking ATOMS").append(NL);
		jmolPanel.viewer.evalString(sb.toString());
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Identifies which attachment points are selected in the visualized table
	 * @return the list of attachment points
	 */
	public ArrayList<AttachmentPoint> getSelectedAPs()
	{
		ArrayList<AttachmentPoint> selected = 
				new ArrayList<AttachmentPoint>();
		
		for (int rowId : apTable.getSelectedRows())
		{
			selected.add(mapAPs.get(apTable.getValueAt(rowId, 0)));
		}
		return selected;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Identifies which attachment points are selected in the visualized table
	 * @return the list of attachment points indexes
	 */
	public ArrayList<Integer> getSelectedAPIDs()
	{
		ArrayList<Integer> selected = new ArrayList<Integer>();
		for (int rowId : apTable.getSelectedRows())
		{
			selected.add(rowId);
		}
		return selected;
	}

//-----------------------------------------------------------------------------
    
	/**
	 * Identifies the atoms that are selected in the Jmol viewer
	 * @return the list of selected atoms
	 */
    public ArrayList<IAtom> getAtomsSelectedFromJMol()
    {
    	ArrayList<IAtom> selectedAtms = new ArrayList<IAtom>();
    	
    	if (fragment == null || fragment.getAtomCount()==0)
    	{
    		return selectedAtms;
    	}
    	
		for (int i =0; i< fragment.getAtomCount(); i++)
		{
			if (((Viewer) jmolPanel.viewer).slm.isSelected(i))
			{
				selectedAtms.add(fragment.getAtom(i));
			}
		}
		
		return selectedAtms;
    }
 	
//-----------------------------------------------------------------------------
	
	private class PausableTableModelListener implements TableModelListener
	{	
		private boolean isActive = false;
		
		public PausableTableModelListener() 
		{};

		@Override
		public void tableChanged(TableModelEvent e) 
		{
            if (isActive && !alteredAPData 
            		&& e.getType() == TableModelEvent.UPDATE)
            {
                alteredAPData = true;
                firePropertyChange(IVertexAPSelection.APDATACHANGEEVENT, false, 
                        true);
            }
		}
        
		public void setActive(boolean var)
		{
			isActive = var;
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Allows to activate and deactivate the listener.
	 * @param var use <code>true</code> to activate the listener
	 */
    public void activateTabEditsListener(boolean var)
    {
		try
		{
			PausableTableModelListener l = (PausableTableModelListener) 
					apTabModel.getTableModelListeners()[0];
    	    l.setActive(var);
		} catch (Throwable t) {
			//t.printStackTrace();
			System.out.println("Bad attempt to contro listener: " 
					+ t.getMessage());
			System.out.println(t.getCause());
		}
    }

//-----------------------------------------------------------------------------
    
    /*
     * This is needed to stop Jmol threads
     */
	public void dispose() 
	{
		jmolPanel.dispose();
	}
	
//-----------------------------------------------------------------------------
    
    @Override
    public Map<Integer, AttachmentPoint> getMapOfAPsInTable()
    {
        return mapAPs;
    }
    
//-----------------------------------------------------------------------------

    @Override
    public DefaultTableModel getAPTableModel()
    {
        return apTabModel;
    }
  	
//-----------------------------------------------------------------------------

}
