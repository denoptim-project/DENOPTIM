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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.gui;


import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;


/**
 * A panel with a 3D molecular viewer and data table.
 * 
 * @author Marco Foscato
 */

public class Mol3DViewPanel extends MolAndPropTableViewPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	private JmolPanel jmolPanel;
	
	private final String NL = System.getProperty("line.separator");
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public Mol3DViewPanel()
	{
		initialize();
	}

//-----------------------------------------------------------------------------

	@Override
	protected void initializeViewer()
	{
        // Jmol viewer panel
        jmolPanel = new JmolPanel();
        this.setTopComponent(jmolPanel);
	}

//-----------------------------------------------------------------------------

	@Override
	protected void displayLoadedStructure(IAtomContainer mol)
	{
        // WARNING! You might be tempted to use 'false' to really clear ('zap') 
	    // the viewer from outdated content. However, that operation is extremely
	    // slow (2-3 seconds) and degrades user experience substantially. 
	    // Instead, we keep outdated data and let the incoming data overwrite
	    // the old ones.
	    // In case of timing, note that the 'zap' script is executed in another 
	    // thread, so the timing of the clear method does not reflect the actual
	    // time it takes to do the operation. The other thread dealing with the 
	    // 'zap' command will, however, stop the execution of this thread when
	    // the latter asks viewer.isScriptExecuting() (see below in this method)
		clearAll(true);
		waitForJmolIdle();
        loadIAtomContainerIntoJmol(mol);
		setJmolViewer();
	}

//-----------------------------------------------------------------------------

	/**
	 * Loads an {@link IAtomContainer} into Jmol via in-memory SDF and
	 * {@code openStringInline}.
	 */
	private void loadIAtomContainerIntoJmol(IAtomContainer mol)
	{
		waitForJmolIdle();
		try {
			String sdf = DenoptimIO.getSDFString(mol);
			jmolPanel.viewer.openStringInline(sdf);
		} catch (DENOPTIMException e) {
			System.out.println("Could not serialize molecular representation "
					+ "to SDF string. Thus, could not load it into Jmol viewer.");
			e.printStackTrace();
			return;
		}
		setJmolViewer();
	}

//-----------------------------------------------------------------------------

	private void waitForJmolIdle()
	{
		// This is often needed to wait for the 'zap' script to finish.
        while (jmolPanel.viewer.isScriptExecuting())
        {
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                // should never happen
                e.printStackTrace();
            }
        }
	}

//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 * @param dataIsComing set <code>true</code> when there is incoming 
	 * molecular data to visualize. In such case we do not run the very slow
	 * <code>zap</code> script in JMol because the molecular data will be 
	 * overwritten anyway. 
	 */
	@Override
	public void clearAll(boolean dataIsComing)
	{
		clearDataTable();
		clearMolecularViewer(dataIsComing);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the molecular viewer. <b>WARNING:</b> this is VERY SLOW: do not do
	 * it unless you are sure you really need to clear the data. Typically,
	 * if there is incoming data, you do not need to run this, as the old data 
	 * will be overwritten anyway.
	 * @param dataIsComing set <code>true</code> when there is incoming 
     * molecular data to visualize.
	 */
	public void clearMolecularViewer(boolean dataIsComing)
	{
	    if (!dataIsComing)
	        jmolPanel.viewer.evalString("zap");
	}

//-----------------------------------------------------------------------------
	
	private void setJmolViewer()
	{
		StringBuilder sb = new StringBuilder();		
		sb.append("select none").append(NL); 
		sb.append("SelectionHalos ON").append(NL);
		sb.append("set picking ATOMS").append(NL);
		jmolPanel.viewer.evalString(sb.toString());
	}

//-----------------------------------------------------------------------------
  
    /*
     * This is needed to stop Jmol threads
     */
	@Override
	public void dispose() 
	{
		jmolPanel.dispose();
	}
  	
//-----------------------------------------------------------------------------

}
