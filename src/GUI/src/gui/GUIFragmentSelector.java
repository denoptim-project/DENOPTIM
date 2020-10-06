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

import java.awt.Cursor;
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

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMFragment;


/**
 * A modal dialog with a molecular viewer that understands DENOPTIM fragments
 * and allows to select fragments.
 * 
 * @author Marco Foscato
 */

public class GUIFragmentSelector extends GUIModalDialog
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * The currently loaded list of fragments
	 */
	private ArrayList<DENOPTIMFragment> fragmentLibrary =
			new ArrayList<DENOPTIMFragment>();
	
	/**
	 * The currently loaded fragment
	 */
	private DENOPTIMFragment fragment;
	
	/**
	 * The index of the currently loaded fragment [0–(n-1)}
	 */
	private int currFrgIdx = 0;
	
	/**
	 * The index of the selected AP [0-(n-1)]
	 */
	private int currApIdx = -1;
	
	private FragmentViewPanel fragmentViewer;
	private JPanel fragCtrlPane;
	private JPanel fragNavigPanel;
	private JPanel fragNavigPanel2;
	
	private JSpinner fragNavigSpinner;
	private JLabel totalFragsLabel;
	private final FragSpinnerChangeEvent fragSpinnerListener = 
			new FragSpinnerChangeEvent();
	
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
	 * @param fragLib the library of fragment to load
	 */
	public GUIFragmentSelector(ArrayList<IAtomContainer> fragLib)
	{
		this(fragLib,0);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param fragLib the library of fragment to load
	 * @param initialFragId the 0-based index of the fragment to open 
	 * when displaying the dialog
	 */
	public GUIFragmentSelector(ArrayList<IAtomContainer> fragLib, 
			int initialFragId)
	{
		super();
		this.setBounds(150, 150, 400, 550);
		this.setTitle("Select fragment and AP");
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// Define the list of frags among which we are selecting
		for (IAtomContainer mol : fragLib)
		{				
			try {
				DENOPTIMFragment frg = new DENOPTIMFragment(mol);
				if (mol.getProperty(PRESELECTEDAPSFIELD) != null)
				{
				    frg.setProperty(PRESELECTEDAPSFIELD,
				    		mol.getProperty(PRESELECTEDAPSFIELD));
				}
				fragmentLibrary.add(frg);
			} catch (DENOPTIMException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null,"<html>Error importing "
						+ "a fragment.<br>The list of fragment is incomplete."
						+ "<br>Please report this to the DENOPTIM "
						+ "team.</html>",
		                "Error",
		                JOptionPane.PLAIN_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
			}
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			
		// The viewer with Jmol and APtable (not editable)
		fragmentViewer = new FragmentViewPanel(false);
		addToCentralPane(fragmentViewer);
		
		// Controls for navigating the fragments list
        fragCtrlPane = new JPanel();
        fragCtrlPane.setVisible(true);
		
        // NB: avoid GroupLayout because it interferes with Jmol viewer and causes exception
        
        fragNavigPanel = new JPanel();
        fragNavigPanel2 = new JPanel();
        JLabel navigationLabel1 = new JLabel("Fragment # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalFragsLabel = new JLabel("0");
        
		fragNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		fragNavigSpinner.setToolTipText("Move to fragment number # in the currently loaded library.");
		fragNavigSpinner.setPreferredSize(new Dimension(75,20));
		fragNavigSpinner.addChangeListener(fragSpinnerListener);
        fragNavigPanel.add(navigationLabel1);
		fragNavigPanel.add(fragNavigSpinner);
		fragCtrlPane.add(fragNavigPanel);
		
        fragNavigPanel2.add(navigationLabel2);
        fragNavigPanel2.add(totalFragsLabel);
		fragCtrlPane.add(fragNavigPanel2);
		addToNorthPane(fragCtrlPane);
		
		// Edit global dialog controls
		this.btnDone.setText("Select current fragment");
		this.btnDone.setToolTipText("<html>Process the currently displayed "
				+ "fragment<br>and the currently selected AP, if any.</html>");
		this.btnDone.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<Integer> ids = fragmentViewer.getSelectedAPIDs();
				if (ids.size() > 0)
				{
					// WARNING: we take only the first, if many are selected.
					currApIdx = ids.get(0);
				}
				else
				{
					if (fragment.getAPCount()==1)
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
				result = new Integer[]{currFrgIdx, currApIdx};
				fragmentViewer.clearAPTable();
				fragmentViewer.clearMolecularViewer();
				close();
			}
		});
		
		// Load the first fragment
		currFrgIdx = initialFragId;
		loadCurrentFragIdxToViewer();
		updateFragListSpinner();	
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Allows to control whether confirming the selection of a fragment without
	 * having selected an attachment point is permitted or not.
	 * @param enforced use <code>true</code> to enforce the selection of an AP.
	 */
	public void setRequireApSelection(boolean enforced)
	{
		this.enforceAPSelection = enforced;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the fragments corresponding to the field index.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 */
	private void loadCurrentFragIdxToViewer()
	{
		if (fragmentLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of fragments loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		clearCurrentSystem();

		fragment = fragmentLibrary.get(currFrgIdx);
		fragmentViewer.loadFragImentToViewer(fragment);
	}
	
//-----------------------------------------------------------------------------

	private void updateFragListSpinner()
	{		
		fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
				fragmentLibrary.size(), 1));
		totalFragsLabel.setText(Integer.toString(fragmentLibrary.size()));
	}
	
//-----------------------------------------------------------------------------
	
	private void clearCurrentSystem()
	{
		// Get rid of currently loaded mol
		fragment = null;
		
		// Clear viewer?
		// No, its clears upon loading of a new system.
		// The exception (i.e., removal of the last fragment) is dealt with by
		// submitting "zap" only in that occasion.
		
		// Remove tmp storage of APs
		fragmentViewer.mapAPs = null;
		
		// Remove table of APs
		fragmentViewer.clearAPTable();
	}
	
//-----------------------------------------------------------------------------
	
	private class FragSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public FragSpinnerChangeEvent()
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
        	currFrgIdx = ((Integer) fragNavigSpinner.getValue()).intValue() - 1;
        	loadCurrentFragIdxToViewer();
        	
        	activateTabEditsListener(true);
        }
	}
	
//-----------------------------------------------------------------------------

    private void activateTabEditsListener(boolean var)
    {
		fragmentViewer.activateTabEditsListener(var);
    }
		
//-----------------------------------------------------------------------------
  	
}
