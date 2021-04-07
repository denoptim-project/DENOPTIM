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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import denoptim.molecule.DENOPTIMVertex;


/**
 * A modal dialog with a viewer that understands the different types of 
 * DENOPTIM vertex 
 * and allows to select vertices and, if needed, attachment points.
 * 
 * @author Marco Foscato
 */

public class GUIVertexSelector extends GUIModalDialog
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded list of vertices
	 */
	private ArrayList<DENOPTIMVertex> vertexLibrary =
			new ArrayList<DENOPTIMVertex>();
	
	/**
	 * The currently loaded vertex
	 */
	private DENOPTIMVertex vertex;
	
	/**
	 * The index of the currently loaded vertex [0–(n-1)}
	 */
	private int currVrtxIdx = 0;
	
	/**
	 * The index of the selected AP [0-(n-1)]
	 */
	private int currApIdx = -1;
	
	private VertexViewPanel vertexViewer;
	private JPanel ctrlPane;
	private JPanel navigPanel;
	private JPanel navigPanel2;
	
	private JSpinner navigSpinner;
	private JLabel totalVerticesLabel;
	private final VrtxSpinnerChangeEvent vrtxSpinnerListener = 
			new VrtxSpinnerChangeEvent();
	
	private boolean enforceAPSelection = false;
	
	/**
	 * Property used to pre-select APs.
	 */
	public static final String PRESELECTEDAPSFIELD = "pre-SelectedAPs";
	
	/**
	 * Separator in property used to pre-select APs
	 */
	public static final String PRESELECTEDAPSFIELDSEP = "pre-SelectedAPs";

//-----------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param vrtxLib the library of vertex to load
	 */
	public GUIVertexSelector(ArrayList<DENOPTIMVertex> vrtxLib)
	{
		this(vrtxLib,0, false);
	}
	
//-----------------------------------------------------------------------------

    /**
     * Constructor
     * @param vrtxLib the library of vertex to load
     */
    public GUIVertexSelector(ArrayList<DENOPTIMVertex> vrtxLib, boolean use3rd)
    {
        this(vrtxLib, 0, use3rd);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param vrtxLib the library of vertex to load
     * @param initialId the 0-based index of the vertex to open 
     * when displaying the dialog
     */
    public GUIVertexSelector(ArrayList<DENOPTIMVertex> vrtxLib, 
            int initialId)
    {
        this(vrtxLib, initialId, false);
    }
    
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param vrtxLib the library of vertex to load
	 * @param initialId the 0-based index of the vertex to open 
	 * when displaying the dialog
	 */
	public GUIVertexSelector(ArrayList<DENOPTIMVertex> vrtxLib, 
			int initialId, boolean use3rd)
	{
		super(use3rd);
		vertexLibrary = vrtxLib;
		this.setBounds(150, 150, 400, 550);
		this.setTitle("Select Vertex and AP");
		/*
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		*/
		
		// The viewer with Jmol and APtable (not editable)
		vertexViewer = new VertexViewPanel(false);
		addToCentralPane(vertexViewer);
		
		// Controls for navigating the vertices list
        ctrlPane = new JPanel();
        ctrlPane.setVisible(true);
		
        // NB: avoid GroupLayout because it interferes with Jmol viewer and causes exception
        
        navigPanel = new JPanel();
        navigPanel2 = new JPanel();
        JLabel navigationLabel1 = new JLabel("vertex # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalVerticesLabel = new JLabel("0");
        
		navigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		navigSpinner.setToolTipText("Move to vertex number # in the currently "
		        + "loaded library.");
		navigSpinner.setPreferredSize(new Dimension(75,20));
		navigSpinner.addChangeListener(vrtxSpinnerListener);
        navigPanel.add(navigationLabel1);
		navigPanel.add(navigSpinner);
		ctrlPane.add(navigPanel);
		
        navigPanel2.add(navigationLabel2);
        navigPanel2.add(totalVerticesLabel);
		ctrlPane.add(navigPanel2);
		addToNorthPane(ctrlPane);
		
		if (use3rd)
		{
	        this.btnExtra.setText("Select current vertex");
            this.btnExtra.setToolTipText("<html>Selects this fragment. "
                    + "Multiple selections are allowed before hitting the "
                    + "'Done' button.</html>");
            this.btnExtra.addActionListener(new ActionListener() {
                
                @SuppressWarnings("unchecked")
                @Override
                public void actionPerformed(ActionEvent e) {
                    ArrayList<Integer> ids = vertexViewer.getSelectedAPIDs();
                    if (ids.size() > 0)
                    {
                        // WARNING: we take only the first, if many are selected.
                        currApIdx = ids.get(0);
                    }
                    else
                    {
                        if (vertex.getNumberOfAP()==1)
                        {
                            currApIdx=0;
                        }
                        else if (enforceAPSelection)
                        {
                            JOptionPane.showMessageDialog(null,"<html>"
                                    + "No attachment point (AP) selected.<br>"
                                    + "Please select an AP in the table."
                                    + "</html>",
                                    "Error",
                                    JOptionPane.PLAIN_MESSAGE,
                                    UIManager.getIcon("OptionPane.errorIcon"));
                            return;
                        }
                    }
                    appendToResult(currVrtxIdx,currApIdx);
                }
            });
		} else {
    		this.btnDone.setText("Select current vertex");
    		this.btnDone.setToolTipText("<html>Process the currently displayed "
    				+ "vertex<br>and the currently selected AP, "
    				+ "if any.</html>");
    		this.btnDone.addActionListener(new ActionListener() {
    			
    			@SuppressWarnings("unchecked")
                @Override
    			public void actionPerformed(ActionEvent e) {
    				ArrayList<Integer> ids = vertexViewer.getSelectedAPIDs();
    				if (ids.size() > 0)
    				{
    					// WARNING: we take only the first, if many are selected.
    					currApIdx = ids.get(0);
    				}
    				else
    				{
    					if (vertex.getNumberOfAP()==1)
    					{
    						currApIdx=0;
    					}
    					else if (enforceAPSelection)
    					{
    						JOptionPane.showMessageDialog(null,"<html>"
    								+ "No attachment point (AP) selected.<br>"
    								+ "Please select an AP in the table."
    								+ "</html>",
    				                "Error",
    				                JOptionPane.PLAIN_MESSAGE,
    				                UIManager.getIcon("OptionPane.errorIcon"));
    						return;
    					}
    				}
    				appendToResult(currVrtxIdx,currApIdx);
    				vertexViewer.dispose();
    				close();
    			}
    		});
		}
		
		// Load the first vertex
		currVrtxIdx = initialId;
		loadCurrentVrtxIdxToViewer();
		updateVrtxListSpinner();	
	}
	
//-----------------------------------------------------------------------------
	
    private void appendToResult(int vrtxId, int apId)
    {
        if (result == null)
        {
            result = new ArrayList<ArrayList<Integer>>();
        }
        boolean alreadySelected = false;
        for (ArrayList<Integer> p : 
            ((ArrayList<ArrayList<Integer>>)result))
        {
            if ((p.get(0) == vrtxId) 
                    && (p.get(1) == apId))
            {
                alreadySelected = true;
                break;
            }
        }
        if (!alreadySelected)
        {
            ArrayList<Integer> pair = new ArrayList<Integer>();
            pair.add(vrtxId);
            pair.add(apId);
            ((ArrayList<ArrayList<Integer>>)result).add(pair);
        }
    }

//-----------------------------------------------------------------------------

	/**
	 * Allows to control whether confirming the selection of a vertex without
	 * having selected an attachment point is permitted or not.
	 * @param enforced use <code>true</code> to enforce the selection of an AP.
	 */
	public void setRequireApSelection(boolean enforced)
	{
		this.enforceAPSelection = enforced;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the vertices corresponding to the field index.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 */
	private void loadCurrentVrtxIdxToViewer()
	{
		if (vertexLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of vertices loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		clearCurrentSystem();
		vertex = vertexLibrary.get(currVrtxIdx);
		vertexViewer.loadVertexToViewer(vertex);
	}
	
//-----------------------------------------------------------------------------

	private void updateVrtxListSpinner()
	{		
		navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 1, 
				vertexLibrary.size(), 1));
		totalVerticesLabel.setText(Integer.toString(vertexLibrary.size()));
	}
	
//-----------------------------------------------------------------------------
	
	private void clearCurrentSystem()
	{
		// Get rid of currently loaded mol
		vertex = null;
		
		// Clear viewer?
		// No, its clears upon loading of a new system.
		// The exception (i.e., removal of the last vertex) is dealt with by
		// submitting "zap" only in that occasion.
		
		vertexViewer.clearCurrentSystem();
	}
	
//-----------------------------------------------------------------------------
	
	private class VrtxSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public VrtxSpinnerChangeEvent()
		{}
		
		/**
		 * Enables/disable the listener
		 * @param var <code>true</code> to activate listener, 
		 * <code>false</code> to disable.
		 */
		public void setEnabled(boolean var)
		{
			this.inEnabled = var;
		}
		
        @Override
        public void stateChanged(ChangeEvent event)
        {
        	if (!inEnabled)
        	{
        		return;
        	}
      
        	activateTabEditsListener(false);
        	
        	//NB here we convert from 1-based index in GUI to 0-based index
        	currVrtxIdx = ((Integer) navigSpinner.getValue()).intValue() - 1;
        	loadCurrentVrtxIdxToViewer();
        	
        	activateTabEditsListener(true);
        }
	}
	
//-----------------------------------------------------------------------------

    private void activateTabEditsListener(boolean var)
    {
        vertexViewer.activateTabEditsListener(var);
    }
		
//-----------------------------------------------------------------------------
  	
}
