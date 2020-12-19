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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMMolecule;


/**
 * A panel with a molecular viewer and data table.
 * 
 * @author Marco Foscato
 */

public class MoleculeViewPanel extends JSplitPane
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded item
	 */
	private DENOPTIMMolecule item;
	
	/**
	 * Flag controlling behavior in case of partial data 
	 * (e.r., lack of fitness/error)
	 */
	private boolean toleratePartialData = false;
	
	private JmolPanel jmolPanel;
	private JScrollPane tabPanel;
	protected DefaultTableModel dataTabModel;
	protected JTable dataTable;
	private JPopupMenu dataTabPopMenu;
	
	private String tmpSDFFile;
	
	private final String NL = System.getProperty("line.separator");
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public MoleculeViewPanel()
	{
		initialize(340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the data table is editable or 
	 * not.
	 * @param dividerPosition allows to set the initial position of the divide
	 */
	public MoleculeViewPanel(int dividerPosition)
	{
		initialize(dividerPosition);
	}
	
//-----------------------------------------------------------------------------

	@SuppressWarnings("serial")
	private void initialize(int dividerPosition)
	{	
		this.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.setOneTouchExpandable(true);
		this.setDividerLocation(dividerPosition);
        
        // Jmol viewer panel
        jmolPanel = new JmolPanel();
        this.setTopComponent(jmolPanel);
        
		// Data table
		dataTabModel = new DefaultTableModel() {
			@Override
		    public boolean isCellEditable(int row, int column) {
				return false;
		    }
		};
		dataTabModel.setColumnCount(2);
		String column_names[]= {"<html><b>Property</b></html>", 
				"<html><b>Value</b></html>"};
		dataTabModel.setColumnIdentifiers(column_names);
		dataTable = new JTable(dataTabModel);
		dataTable.getColumnModel().getColumn(0).setMaxWidth(75);
		dataTable.setGridColor(Color.LIGHT_GRAY);
		dataTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				showPopup(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				showPopup(e);
			}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				showPopup(e);
			}
		});
		JTableHeader dataTabHeader = dataTable.getTableHeader();
		dataTabHeader.setPreferredSize(new Dimension(100, 20));
		dataTabHeader.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				showPopup(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				showPopup(e);
			}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				showPopup(e);
			}
		});
		tabPanel = new JScrollPane(dataTable);
		tabPanel.setMinimumSize(new Dimension(100,30));
		this.setBottomComponent(tabPanel);
		
		//Find a proper tmp disk space
		tmpSDFFile = Utils.getTempFile("Denoptim_MolViewer_loadedMol.sdf");

	}

//-----------------------------------------------------------------------------

	private void showPopup(MouseEvent e)
	{
		if (!e.isPopupTrigger() || item == null)
		{
			return;
		}
		// We take the list of potentially available properties
		// from the SDF of the item.
		IAtomContainer mol;
		TreeMap<String,String> availableProps = new TreeMap<String,String>();
		try {
			mol = DenoptimIO.readSingleSDFFile(item.getMoleculeFile());
		} catch (DENOPTIMException e1) {
			return;
		}
		for (Object propRef : mol.getProperties().keySet())
		{
			String key = propRef.toString();
			String val = key;
			if (GUIPreferences.defualtSDFTags.keySet().contains(key))
			{
				val = GUIPreferences.defualtSDFTags.get(key);
			}
			availableProps.put(key,val);
		}
		
		dataTabPopMenu = new JPopupMenu("Add/Remove Rows");
		for (Entry<String, String> entry : availableProps.entrySet())
		{
			JCheckBoxMenuItem mi = new JCheckBoxMenuItem(entry.getValue());
			if (GUIPreferences.chosenSDFTags.contains(entry.getKey()))
			{
				mi.setSelected(true);
			} else {
				mi.setSelected(false);
			}
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//At this point the menuItem is already selected
					// You can use this to verify:
					/*
					for (Component c : dataTabPopMenu.getComponents())
					{
						if (c instanceof JCheckBoxMenuItem)
						{
							JCheckBoxMenuItem i = (JCheckBoxMenuItem) c;
							System.out.println("   "+i.isSelected()+" "+i);
						}
					}
					*/
					if (!mi.isSelected())
					{
						mi.setSelected(false);
						GUIPreferences.chosenSDFTags.remove(entry.getKey());
					} else {
						mi.setSelected(true);
						GUIPreferences.chosenSDFTags.add(entry.getKey());
					}
					fillDataTable(mol);
				}
			});
			dataTabPopMenu.add(mi);
		}
		dataTabPopMenu.show(e.getComponent(), e.getX(), e.getY());
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Sets the behavior in case of request to visualize partial data. E.g.,
	 * SDF files that have neither fitness nor error field.
	 */
	public void enablePartialData(boolean enable)
	{
		toleratePartialData = enable;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 */
	public void loadChemicalStructure(IAtomContainer mol)
	{
		try {
			DenoptimIO.writeMolecule(tmpSDFFile, mol, false);
		} catch (DENOPTIMException e) {
			System.out.println("Could not write molecular representation to "
					+ "tmp file. Thus, could not load it into Jmol viewer.");
			return;
		}
		File file = new File(tmpSDFFile);
		loadChemicalStructureFromFile(file);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Loads a structure in the Jmol viewer.
	 * @param fitProvMol the structure to load
	 */
	public void loadChemicalStructureFromFile(String pathName)
	{
		File file = new File(pathName);
		loadChemicalStructureFromFile(file);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param file the file to open
	 */
	public void loadChemicalStructureFromFile(File file)
	{
		clearAll();
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			item = DenoptimIO.readDENOPTIMMolecules(file, false).get(0);
		} catch (DENOPTIMException e) {
			if (!toleratePartialData)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null,
		                "<html>Could not load data from file <br>'" + file +"'.",
		                "Error",
		                JOptionPane.PLAIN_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
				this.setCursor(Cursor.getPredefinedCursor(
						Cursor.DEFAULT_CURSOR));
				return;
			}
			else
			{
				try {
					
					//WARNING: here we always ignore the fragment space because
					// so far, there is no case where the fully defined graph 
					// is needed. We only need its string representation.
					
					item = new DENOPTIMMolecule(DenoptimIO.readMoleculeData(
							file.getAbsolutePath()).get(0),false,true);
				} catch (DENOPTIMException e1) {
					e1.printStackTrace();
					this.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
					return;
				}
			}
		}

		fillDataTable(file);
		
		jmolPanel.viewer.openFile(file.getAbsolutePath());
		setJmolViewer();
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

//-----------------------------------------------------------------------------

	/**
	 * @param molFile can be null, used only to get more data than what already
	 * collected in the DNEOPTIMMolecule class.
	 */
	private void fillDataTable(File molFile)
	{
		IAtomContainer mol = null;
		if (molFile != null)
		{
			try {
				mol = DenoptimIO.readSingleSDFFile(molFile.getAbsolutePath());
			} catch (DENOPTIMException e) {
				System.out.println("Could not read descriptors from '" 
						+ molFile + "': "+e.getLocalizedMessage());
			}
		}
		fillDataTable(mol);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * @param molFile can be null, used only to get more data than what already
	 * collected in the DNEOPTIMMolecule class.
	 */
	private void fillDataTable(IAtomContainer mol)
	{
		cleardataTable();
		
		Set<String> fromDnMol = new HashSet<String>();
		fromDnMol.add(CDKConstants.TITLE);
		fromDnMol.add(DENOPTIMConstants.UNIQUEIDTAG);
		fromDnMol.add(DENOPTIMConstants.FITNESSTAG);
		fromDnMol.add(DENOPTIMConstants.MOLERRORTAG);
		fromDnMol.add("Generation");
		fromDnMol.add(DENOPTIMConstants.GMSGTAG);
		
		TreeSet<String> chosen = GUIPreferences.chosenSDFTags;
		Map<String,String> defPropMap = GUIPreferences.defualtSDFTags;
		
		if (item.getName() != null && chosen.contains(CDKConstants.TITLE)) 
		{
			dataTabModel.addRow(new Object[] {
					defPropMap.get(CDKConstants.TITLE),
					item.getName() });
		}
		if (item.getMoleculeUID() != null && chosen.contains(
				DENOPTIMConstants.UNIQUEIDTAG)) 
		{
			dataTabModel.addRow(new Object[] {
					defPropMap.get(DENOPTIMConstants.UNIQUEIDTAG), 
					item.getMoleculeUID() });
		}
		if (item.hasFitness() && chosen.contains(
				DENOPTIMConstants.FITNESSTAG))
		{
			dataTabModel.addRow(new Object[]{
					defPropMap.get(DENOPTIMConstants.FITNESSTAG), 
					item.getMoleculeFitness()});
		}
		else
		{
			if (item.getError() != null && chosen.contains(
					DENOPTIMConstants.MOLERRORTAG)) 
			{
				dataTabModel.addRow(new Object[] {
						defPropMap.get(DENOPTIMConstants.MOLERRORTAG),
						item.getError() });
			}
		}
		if (item.getGeneration() > -1 && chosen.contains("Generation")) 
		{
			dataTabModel.addRow(new Object[] {
					defPropMap.get("Generation"),
					item.getGeneration() });
		}
		if (item.getComments() != null && chosen.contains(
				DENOPTIMConstants.GMSGTAG)) 
		{
			dataTabModel.addRow(new Object[] {
					defPropMap.get(DENOPTIMConstants.GMSGTAG),
					item.getComments() });
		}
		
		if (mol != null)
		{
			for (String key : GUIPreferences.chosenSDFTags)
			{
				Object p = mol.getProperty(key);
				if (p == null || fromDnMol.contains(key))
				{
					continue;
				}
				dataTabModel.addRow(new Object[] {key, p.toString()});
			}
		}
	}

//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 */
	public void clearAll()
	{
		cleardataTable();
		clearMolecularViewer();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the table of attachment points
	 */
	public void cleardataTable()
	{
		int initRowCount = dataTabModel.getRowCount();
        for (int i=0; i<initRowCount; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	dataTabModel.removeRow(0);
        }
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
	
	private void setJmolViewer()
	{
		StringBuilder sb = new StringBuilder();		
		sb.append("select none").append(NL); 
		sb.append("SelectionHalos ON").append(NL);
		sb.append("set picking ATOMS").append(NL);
		jmolPanel.viewer.evalString(sb.toString());
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

}
