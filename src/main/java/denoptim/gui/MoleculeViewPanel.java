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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jmol.awtjs.swing.BorderLayout;
import org.jmol.awtjs.swing.JPanel;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.Candidate;
import denoptim.io.DenoptimIO;


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
	private Candidate item;
	
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
				return true;
		    }
		};
		dataTabModel.setColumnCount(2);
		String column_names[]= {"<html><b>Property</b></html>", 
				"<html><b>Value</b></html>"};
		dataTabModel.setColumnIdentifiers(column_names);
		dataTable = new JTable(dataTabModel);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dataTable.getColumnModel().getColumn(0).setMaxWidth(75);
        dataTable.getColumnModel().getColumn(1).setMinWidth(750);
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
		tabPanel = new JScrollPane(dataTable, 
		        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
		        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
			mol = DenoptimIO.getFirstMolInSDFFile(item.getSDFFile());
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
			DenoptimIO.writeSDFFile(tmpSDFFile, mol, false);
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
	    // WARNING! You might be tempted to use 'false' to really clear ('zap') 
	    // the viewer from outdated content. However, that operation is extremely
	    // slow (2-3 seconds) and degrades user experience substantially. 
	    // Instead, we keep outdated data and let the incoming data overwrite
	    // the old ones.
	    // In case of timing, note that the 'zap' script is executed in another 
	    // thread, so the timing of the clear method does not reflect the actual
	    // time it takes to do the operation. The other thread dealing with the 
	    // 'zap' command will, however, stop the execution of this thread when
	    // the latter asks viewer.isScriptExecuting() (see below in this method)
		clearAll(true);
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			item = DenoptimIO.readCandidates(file, false).get(0);
		} catch (DENOPTIMException e) {
			if (!toleratePartialData)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
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
					
					item = new Candidate(DenoptimIO.readSDFFile(
							file.getAbsolutePath()).get(0),false,true);
				} catch (DENOPTIMException e1) {
				    try {
				        item = Candidate.fromAtomContainerNoGraph(
				                DenoptimIO.readSDFFile(
				                        file.getAbsolutePath()).get(0), true);
				    } catch (DENOPTIMException e2) {
    					e1.printStackTrace();
    					this.setCursor(Cursor.getPredefinedCursor(
    							Cursor.DEFAULT_CURSOR));
    					return;
				    }
				}
			}
		}

		fillDataTable(file);

		int i=0;
		// This is often needed to wait for the 'zap' script to finish.
        while (jmolPanel.viewer.isScriptExecuting())
        {
            i++;
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                // should never happen
                e.printStackTrace();
            }
        }
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
				mol = DenoptimIO.getFirstMolInSDFFile(molFile.getAbsolutePath());
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
		if (item.getUID() != null && chosen.contains(
				DENOPTIMConstants.UNIQUEIDTAG)) 
		{
			dataTabModel.addRow(new Object[] {
					defPropMap.get(DENOPTIMConstants.UNIQUEIDTAG), 
					item.getUID() });
		}
		if (item.hasFitness() && chosen.contains(
				DENOPTIMConstants.FITNESSTAG))
		{
			dataTabModel.addRow(new Object[]{
					defPropMap.get(DENOPTIMConstants.FITNESSTAG), 
					item.getFitness()});
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
	 * @param dataIsComing set <code>true</code> when there is incoming 
	 * molecular data to visualize. In such case we do not run the very slow
	 * <code>zap</code> script in JMol because the molecular data will be 
	 * overwritten anyway. 
	 */
	public void clearAll(boolean dataIsComing)
	{
		cleardataTable();
		clearMolecularViewer(dataIsComing);
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
