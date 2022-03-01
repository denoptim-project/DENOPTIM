package denoptim.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

/**
 * Standardised horizontal bar with padded components, which are meant to be 
 * JButtons.
 * 
 * @author Marco Foscato
 */

public class ButtonsBar extends JPanel
{
	private GridBagConstraints cmdGridConstraints = new GridBagConstraints();
	private int componerCounter = 0;
	
//------------------------------------------------------------------------------
	
	public ButtonsBar()
	{
		super();
		setLayout(new GridBagLayout());
		cmdGridConstraints.ipadx = 5;
		cmdGridConstraints.ipady = 5;
	}
	
//------------------------------------------------------------------------------
	
	@Override
    public Component add(Component comp) 
	{
		cmdGridConstraints.gridx = componerCounter;
		componerCounter++;
		add(comp,cmdGridConstraints);
        return comp;
    }

//------------------------------------------------------------------------------
	
}
