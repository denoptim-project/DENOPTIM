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
import java.io.File;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;

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
	
	private JmolPanel jmolPanel;
	private JScrollPane tabPanel;
	protected DefaultTableModel dataTabModel;
	protected JTable dataTable;
	
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
		JTableHeader dataTabHeader = dataTable.getTableHeader();
		dataTabHeader.setPreferredSize(new Dimension(100, 20));
		tabPanel = new JScrollPane(dataTable);
		tabPanel.setMinimumSize(new Dimension(100,30));
		this.setBottomComponent(tabPanel);
		
		//Find a proper tmp disk space
		tmpSDFFile = Utils.getTempFile("Denoptim_MolViewer_loadedMol.sdf");

	}
	
//-----------------------------------------------------------------------------

	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 */
	public void loadMolecularFromFile(String pathName)
	{
		File file = new File(pathName);
		loadMolecularFromFile(file);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param file the file to open
	 */
	public void loadMolecularFromFile(File file)
	{
		clearAll();
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			item = DenoptimIO.readDENOPTIMMolecules(file, false).get(0);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not load data from file <br>'" + file +"'.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;			
		}

		fillDataTable();
		
		jmolPanel.viewer.openFile(file.getAbsolutePath());
		setJmolViewer();
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	private void fillDataTable()
	{
		dataTabModel.addRow(new Object[]{"Name", item.getName()});
		dataTabModel.addRow(new Object[]{"UID", item.getMoleculeUID()});
		if (item.hasFitness())
		{
			dataTabModel.addRow(new Object[]{"Fitness", 
					item.getMoleculeFitness()});
		}
		else
		{
			dataTabModel.addRow(new Object[]{"Error", item.getError()});
		}
		dataTabModel.addRow(new Object[]{"Generation", item.getGeneration()});
		dataTabModel.addRow(new Object[]{"Source", item.getComments()});
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

}
