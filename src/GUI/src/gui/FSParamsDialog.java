package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.rings.RingClosureParameters;

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
		btnDone.setToolTipText("<html>Uses the parameters defined "
				+ "above to"
				+ "<br> build a fragment space and make it available to"
				+ "<br>the graph handler.</html>");
		
		btnDone.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {					
				try {
					fsParsForm.possiblyReadParamsFromFSParFile();
					makeFragSpace();
				} catch (Exception e1) {
				    e1.printStackTrace();
					String msg = "<html>The given parameters did not "
							+ "allow to "
							+ "build a fragment space.<br>"
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
							
					JOptionPane.showMessageDialog(btnDone, msg,
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