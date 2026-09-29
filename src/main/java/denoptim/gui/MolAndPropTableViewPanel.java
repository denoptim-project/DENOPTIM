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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * Base panel with an unspecified molecular viewer (subclass-specific) and a property data
 * table.
 * 
 * @author Marco Foscato
 */

public abstract class MolAndPropTableViewPanel extends JSplitPane
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded container
	 */
	protected IAtomContainer container;
	
	/**
	 * Flag controlling behavior in case of partial data 
	 * (e.r., lack of fitness/error)
	 */
	protected boolean toleratePartialData = false;
	
	protected JScrollPane tabPanel;
	protected DefaultTableModel dataTabModel;
	protected JTable dataTable;
	private JPopupMenu dataTabPopMenu;
	
//-----------------------------------------------------------------------------

	/**
	 * Initializes the split pane layout and the shared property data table.
	 * Subclasses may set up their molecular viewer via 
	 * {@link #initializeViewer()}.
	 * The divider is placed at 50% of the available height and keeps that
	 * proportion when this panel is resized.
	 */
	@SuppressWarnings("serial")
	protected void initialize()
	{	
		this.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.setOneTouchExpandable(true);
		this.setResizeWeight(0.75);
		// Proportional divider location only works once the split pane has a
		// real size (e.g. after a CardLayout shows this panel).
		this.addComponentListener(new ComponentAdapter() {
		    private boolean dividerInitialized = false;
            @Override
            public void componentResized(ComponentEvent e)
            {
                if (!dividerInitialized && getHeight() > 0)
                {
                    setDividerLocation(0.75);
                    dividerInitialized = true;
                }
            }
        });
        
		initializeViewer();
        
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
	}

//-----------------------------------------------------------------------------

	/**
	 * Hook for subclasses to initialize their molecular viewer component.
	 * Called from {@link #initialize()} before the data table is created.
	 */
	protected void initializeViewer()
	{
		// subclasses may override
	}

//-----------------------------------------------------------------------------

	private void showPopup(MouseEvent e)
	{
		if (!e.isPopupTrigger() || container == null)
		{
			return;
		}
		// We take the list of potentially available properties from the container
		TreeMap<String,String> availableProps = new TreeMap<String,String>();
		for (Object propRef : container.getProperties().keySet())
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
					fillDataTable(container);
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
	 * Loads a structure in the mol viewer.
	 * @param mol the structure to load
	 */
	public void loadContent(IAtomContainer mol)
	{
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        container = mol;
        displayLoadedStructure(mol);
        fillDataTable(mol);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

//-----------------------------------------------------------------------------

	/**
	 * Displays the already-loaded candidate in the subclass-specific molecular
	 * viewer.
	 * @param mol the container that was loaded
	 */
	protected abstract void displayLoadedStructure(IAtomContainer mol);
	
//-----------------------------------------------------------------------------
	
	/**
	 * Fills the data table with the properties of the container.
	 * @param mol the container providing the data to fill the table with.
	 */
	protected void fillDataTable(IAtomContainer mol)
	{
		clearDataTable();
		
		Map<String,String> defPropMap = GUIPreferences.defualtSDFTags;
        for (String key : GUIPreferences.chosenSDFTags)
        {
            Object p = mol.getProperty(key);
            if (p == null)
            {
                continue;
            }
            if (defPropMap.containsKey(key))
            {
                dataTabModel.addRow(new Object[] {defPropMap.get(key), p.toString()});
            } else {
                dataTabModel.addRow(new Object[] {key, p.toString()});
            }
        }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the property data table.
	 */
	public void clearDataTable()
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
	 * Removes the currently visualized molecule and property table content.
	 * @param dataIsComing set <code>true</code> when there is incoming 
	 * molecular data to visualize.
	 */
	public abstract void clearAll(boolean dataIsComing);

//-----------------------------------------------------------------------------

	/**
	 * Releases resources held by this panel.
	 */
	public abstract void dispose();
  	
//-----------------------------------------------------------------------------

}
