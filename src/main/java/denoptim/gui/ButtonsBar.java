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

@SuppressWarnings("serial")
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
