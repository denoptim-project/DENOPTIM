package denoptim.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;

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
	
	private ILoadFragSpace parent;
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public FSParamsDialog(ILoadFragSpace parentPanel)
	{
		super();
		this.parent = parentPanel;
		
		fsParsForm = new FSParametersForm(this.getSize());
		addToCentralPane(fsParsForm);
		
		btnDone.setText("Create Fragment Space");
		btnDone.setToolTipText(String.format("<html><body width='%1s'>"
		        + "Uses the parameters defined above to build a fragment space "
		        + "and make it available to the graph handler.</html>",400));
		
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
					
					parent.renderForLackOfFragSpace();
					return;
				}
				parent.renderForPresenceOfFragSpace();
				close();
			}
		});
		
		this.btnCanc.setToolTipText("Exit without creating a fragment "
				+ "space.");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Reads all the parameters, calls the interpreters, and eventually
	 * creates the static FragmentSpace object.
	 * @throws Exception
	 */
	private void makeFragSpace() throws Exception
	{
		if (fsParsForm.txtPar1.getText().trim().equals(""))
		{
			throw new DENOPTIMException("No library of fragments");
		}
		
		StringBuilder sbPars = new StringBuilder();
		fsParsForm.putParametersToString(sbPars);
		
		String[] lines = sbPars.toString().split(
				System.getProperty("line.separator"));
		for (String line : lines)
		{
			if ((line.trim()).length() == 0)
            {
                continue;
            }
			if (line.startsWith("#"))
            {
                continue;
            }
			if (line.toUpperCase().startsWith("FS-"))
            {
                FragmentSpaceParameters.interpretKeyword(line);
                continue;
            }
            if (line.toUpperCase().startsWith("RC-"))
            {
                RingClosureParameters.interpretKeyword(line);
                continue;
            }
		}
		
		// This creates the static FragmentSpace object
		if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
            FragmentSpaceParameters.processParameters();
        }
        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
            RingClosureParameters.processParameters();
        }
	}
	
//-----------------------------------------------------------------------------

}