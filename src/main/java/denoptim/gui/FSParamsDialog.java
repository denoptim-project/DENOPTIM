/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.programs.RunTimeParameters.ParametersType;

/**
 * A modal dialog to define a fragment space and load it.
 */

class FSParamsDialog extends GUIModalDialog
{
	/**
     * Version ID
     */
    private static final long serialVersionUID = 1L;

    private FSParametersForm fsParsForm;
	
	private GUICardPanel parent;
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public FSParamsDialog(GUICardPanel parentPanel)
	{
		super();
		this.parent = parentPanel;
		
		fsParsForm = new FSParametersForm(this.getSize());
		addToCentralPane(fsParsForm);
		
		btnDone.setText("Create BB Space");
		btnDone.setToolTipText(String.format("<html><body width='%1s'>"
		        + "Uses the parameters defined above to define a space"
		        + "of graph building blocks (BB Space) "
		        + "and makes it available to the graph handler.</html>",400));
		
		// NB: Assumption: 1 action listener inherited from superclass.
		// We want to remove it because we need to acquire full control over
		// when the modal panel has to be closed.
		btnDone.removeActionListener(btnDone.getActionListeners()[0]);
		btnDone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {					
				try {
					fsParsForm.possiblyReadParamsFromFSParFile();
					makeFragSpace();
				} catch (Exception e1) {
				    e1.printStackTrace();
					String msg = "<html><body width='%1s'>"
					        + "These parameters did not allow to "
							+ "build a space of graph building blocks.<br>"
							+ "Possible cause of this problem: " 
							+ "<br>";
							
					if (e1.getCause() != null)
					{
						msg = msg + e1.getCause();
					}
					if (e1.getMessage() != null)
					{
						msg = msg + " " + e1.getMessage();
					}
					msg = msg + "<br>Please alter the "
							+ "settings and try again.</html>";
							
					JOptionPane.showMessageDialog(btnDone,
					        String.format(msg,400),
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				close();
			}
		});
		
		this.btnCanc.setToolTipText("Exit without creating any BB Space.");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Reads all the parameters, calls the interpreters, and eventually
	 * creates the static FragmentSpace object.
	 * @return 
	 * @throws Exception
	 */
	public FragmentSpace makeFragSpace() throws Exception
	{
		if (fsParsForm.txtPar1.getText().trim().equals(""))
		{
			throw new DENOPTIMException("No library of fragments");
		}
		
		StringBuilder sbPars = new StringBuilder();
		fsParsForm.putParametersToString(sbPars);
		
		String[] lines = sbPars.toString().split(System.getProperty(
		        "line.separator"));

        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
		for (String line : lines)
		{
		    fsParams.readParameterLine(line);
		}
		// This creates the FragmentSpace object
	    fsParams.checkParameters();
	    fsParams.processParameters();
	    return fsParams.getFragmentSpace();
	}
	
//-----------------------------------------------------------------------------

}