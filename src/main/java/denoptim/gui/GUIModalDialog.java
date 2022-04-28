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


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class GUIModalDialog extends JDialog
{
	
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 8562693062507200623L;
	
	/**
	 * The button that is used to launch the processing of the data given
	 * to the open dialog, and close the dialog window.
	 */
	protected JButton btnDone;
	
	/**
	 * The button that is used to close the dialog without processing any
	 * input.
	 */
	protected JButton btnCanc;
	
	/**
     * The button that can be used for any action that does not close the 
     * dialog.
     */
    protected JButton btnExtra;
    
	private JPanel pnlControls;
	
	/**
	 * The result to be returned once the dialog is closed
	 */
	protected Object result = null;
	
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public GUIModalDialog()
    {
        initialize(false);
    }

//------------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * @param useExtraButton set <code>true</code> to request three buttons
	 * in the control panel.
	 */
    public GUIModalDialog(boolean useExtraButton)
    {
        initialize(useExtraButton);
    }
    
//------------------------------------------------------------------------------
    
	private void initialize(boolean useExtraButton)
	{
		this.setModal(true);
		this.setLayout(new BorderLayout());
		//this.setBounds(150, 150, 800, 450);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		btnDone = new JButton("Done");
		btnDone.setToolTipText("Processes data and closes dialog");
		btnDone.addActionListener(new ActionListener() {
	            
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
		
		btnCanc = new JButton("Cancel");
		btnCanc.setToolTipText("Exit dialog.");
		btnCanc.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				result = null;
				close();
			}
		});
		
		pnlControls = new JPanel();
		if (useExtraButton)
		{
		    btnExtra = new JButton("Extra");
            pnlControls.add(btnExtra);
		}
		pnlControls.add(btnDone);
		pnlControls.add(btnCanc);
		this.add(pnlControls, BorderLayout.SOUTH);
	}

//-------------------------------------------------------------------------

	/**
	 * Shows the dialog and restrains the modality to it, until the dialog
	 *  gets closed
	 */
	public Object showDialog()
	{
	    pack();
		setVisible(true);
		return result;
	}

//-------------------------------------------------------------------------

	/**
	 * Adds a component to the topmost part of this dialog frame.
	 * @param comp the component to be added.
	 */
	public void addToNorthPane(JComponent comp)
	{
		this.add(comp, BorderLayout.NORTH);
	}
	
//-------------------------------------------------------------------------

	/**
	 * Adds a component to the central part of this dialog frame.
	 * @param comp the component to be added.
	 */
	public void addToCentralPane(JComponent comp)
	{
		this.add(comp, BorderLayout.CENTER);
	}
	
//-------------------------------------------------------------------------
	
	/**
	 * Closes the dialog window
	 */
	protected void close()
	{
		GUIModalDialog.this.setVisible(false);
		GUIModalDialog.this.dispatchEvent(new WindowEvent(
				GUIModalDialog.this, WindowEvent.WINDOW_CLOSING));
		dispose();
	}

//-------------------------------------------------------------------------
}
