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

package gui;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.vecmath.Point3d;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.FragmentUtils;


/**
 * A panel with a molecular viewer and attachment point table.
 * 
 * @author Marco Foscato
 */

public class FragmentViewPanel extends JSplitPane
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded fragment
	 */
	private DENOPTIMFragment fragment;
	
	/**
	 * Temporary list of attachment points of the current fragment
	 */
	protected Map<Integer,DENOPTIMAttachmentPoint> mapAPs = null;
	
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
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public FragmentViewPanel()
	{
		initialize(340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public FragmentViewPanel(boolean editableTable)
	{
		editableAPTable = editableTable;
		initialize(340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 * @param dividerPosition allows to set the initial position of the divide
	 */
	public FragmentViewPanel(boolean editableTable, int dividerPosition)
	{
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
	 * altered data.
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
	private void waitForJmolViewer(int milliSecFirst)
	{
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
			System.out.println("Waiting "+now);
			if (now > wallTime)
			{
				this.setCursor(Cursor.getPredefinedCursor(
						Cursor.DEFAULT_CURSOR));
				String[] options = new String[]{"Yes","No"};
				int res = JOptionPane.showOptionDialog(null,
		                "<html>Slow response from Jmol.<br>Keep waiting?</html>",
		                "Should we wait for another 5 seconds?",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[1]);
				if (res == 1)
				{
					break;
				}
				else
				{
					this.setCursor(Cursor.getPredefinedCursor(
							Cursor.WAIT_CURSOR));
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
	
	public String getDataFromJmol()
	{
		return jmolPanel.viewer.getData("*", "txt");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a molecule based on the remote CACTUS service. Requires connection
	 * to the Internet.
	 * @param smiles the SMILES of the molecule to load
	 * @throws DENOPTIMException 
	 */
	public void loadSMILESFromRemote(String smiles) throws DENOPTIMException
	{
		// Load the structure using CACTUS service via Jmol
		jmolPanel.viewer.evalString("load $"+smiles);
		waitForJmolViewer(1500);
		
		// NB: this is a workaround to the lack of try/catch mechanism when
		// executing Jmol commands.
		// We want to catch errors that prevent loading the structure
		// For example, offline mode and invalid SMILES
		String data = getDataFromJmol();		
		if (data == null || data.equals(""))
		{
			System.out.println("DATA: "+data);
			JOptionPane.showMessageDialog(null,
	                "<html>Could not find a valid structure.<br>"
	                + "Possible reasons are:"
	                + "<ul>"
	                + "<li>unreachable remote service (we are offline)</li>"
	                + "<li>no available structure for SMILES string<br>'" 
	                + smiles 
	                + "'</li></ul><br>"
	                + "Please create such structure and import it as SDF file."
	                + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			throw new DENOPTIMException("No SMILES imported");
		}
		
		// Now we should have a structure loaded in the viewer, 
		// so we take that one and put it in the IAtomContainer representation
		try	{
		    fragment = new DENOPTIMFragment(getStructureFromJmolViewer());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not understand Jmol system.</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		setJmolViewer();
	}
	
//-----------------------------------------------------------------------------

	private IAtomContainer getStructureFromJmolViewer() 
			throws DENOPTIMException
	{
		IAtomContainer mol = new AtomContainer();
		
		String strData = getDataFromJmol();
		strData = strData.replaceAll("[0-9] \\s+ 999 V2000", 
				"0  0  0  0  0999 V2000");
		String[] lines = strData.split("\\n");
		if (lines.length < 5)
		{
			clearMolecularViewer();
			clearAPTable();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: " + strData);
		}
		if (!lines[3].matches(".*999 V2000.*"))
		{
			clearMolecularViewer();
			clearAPTable();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: " + strData);
		}
		String[] counters = lines[3].trim().split("\\s+");
		int nAtms = Integer.parseInt(counters[0]);
		int nBonds = Integer.parseInt(counters[1]);
		
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
		mol = DenoptimIO.readSingleSDFFile(tmpSDFFile);
		return mol;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Returns the chemical representation of the currently loaded chemical
	 * object.
	 * @return the chemical representation of what is currently visualized.
	 */
	public DENOPTIMFragment getLoadedStructure()
	{
		return fragment;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 */
	public void loadPlainStructure(IAtomContainer mol)
	{
		fragment = (DENOPTIMFragment) mol;
		if (fragment == null)
		{
			JOptionPane.showMessageDialog(null,
	                "<html>No structure loaded.<br>This is most likely a bug!"
	                + "Please report it to the development team.</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;			
		}
		
		// NB: we use the nasty trick of a tmp file to by-pass the 
		// fragile/discontinued CDK-to-Jmol support.
		
		try {
			DenoptimIO.writeMolecule(tmpSDFFile, fragment.getIAtomContainer(), 
			        false);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			System.out.println("Error writing TMP file '" + tmpSDFFile + "'");
			System.out.println("Please, report this to the DENOPTIM team.");
		}
		jmolPanel.viewer.openFile(tmpSDFFile);
		setJmolViewer();
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
	public void loadFragImentToViewer(DENOPTIMFragment frag)
	{		
		clearAPTable();
		
		this.fragment = frag;
			
		loadPlainStructure(fragment.getIAtomContainer());
		
		updateAPsMapAndTable();
        
        updateAPsInJmolViewer();
        
        preSelectAPs();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Uses the AP of the {@link DENOPTIMFragment} to create a new map and 
	 * table of APs.
	 */
	private void updateAPsMapAndTable()
	{
		clearAPTable();
		mapAPs = new HashMap<Integer,DENOPTIMAttachmentPoint>();
		
		ArrayList<DENOPTIMAttachmentPoint> lstAPs = fragment.getCurrentAPs();		
        if (lstAPs.size() == 0)
        {
			return;
        }
        
        activateTabEditsListener(false);
        int arrId = 0;
	    for (DENOPTIMAttachmentPoint ap : lstAPs)
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
		String PRESELPROP = GUIFragmentSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIFragmentSelector.PRESELECTEDAPSFIELDSEP;
		
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
	 */
	public void clearAll()
	{
		clearAPTable();
		clearMolecularViewer();
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
	 * Clears the molecular viewer
	 */
	public void clearMolecularViewer()
	{
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
        	DENOPTIMAttachmentPoint ap = mapAPs.get(arrId);
        	int srcAtmId = ap.getAtomPositionNumber();
        	Point3d srcAtmPlace = FragmentUtils.getPoint3d(
        			fragment.getAtom(srcAtmId));
        	double[] startArrow = new double[]{
        			srcAtmPlace.x,
        			srcAtmPlace.y,
        			srcAtmPlace.z};
        	double[] endArrow = ap.getDirectionVector();
        	
        	double[] offSet = DENOPTIMMathUtils.scale(
        			DENOPTIMMathUtils.subtract(endArrow,startArrow), 0.2);
        	double[] positionLabel = DENOPTIMMathUtils.add(endArrow,offSet); 
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
	
	private class JmolPanel extends JPanel 
	{

        /**
		 * Version UID
		 */
		private static final long serialVersionUID = 1699908697703788097L;

		JmolViewer viewer;

        private final Dimension hostPanelSize = new Dimension();

        public JmolPanel() {
            viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
            null, null, null, null, null); //NB: can add listener here
        }
        
        //---------------------------------------------------------------------

		@Override
        public void paint(Graphics g) {
            getSize(hostPanelSize);
            viewer.renderScreenImage(g, hostPanelSize.width, hostPanelSize.height);
        }
    }
	
//-----------------------------------------------------------------------------

	/**
	 * Identifies which attachment points are selected in the visualized table
	 * @return the list of attachment points
	 */
	public ArrayList<DENOPTIMAttachmentPoint> getSelectedAPs()
	{
		ArrayList<DENOPTIMAttachmentPoint> selected = 
				new ArrayList<DENOPTIMAttachmentPoint>();
		
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
                firePropertyChange("APDATA", false, true);
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

}
