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

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;

public class JmolPanel extends JPanel
{
	
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1699908697703788097L;

	protected JmolViewer viewer;

    private final Dimension hostPanelSize = new Dimension();

    public JmolPanel() {
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("display", this);
        info.put("adapter", new SmarterJmolAdapter());
        info.put("isApp", false);
        info.put("silent", "");
        viewer =  new Viewer(info);
    }
    
//------------------------------------------------------------------------------

	@Override
    public void paint(Graphics g) {
        getSize(hostPanelSize);
        viewer.renderScreenImage(g, hostPanelSize.width, hostPanelSize.height);
    }
	
//------------------------------------------------------------------------------

	public void dispose() {
		viewer.dispose();
	}

//------------------------------------------------------------------------------
	
}
