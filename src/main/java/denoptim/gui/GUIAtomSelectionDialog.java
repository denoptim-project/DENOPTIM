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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.graph.Vertex;
import denoptim.utils.MoleculeUtils;

/**
 * A dialog window meant only to select atoms one or multiple times.
 */
public class GUIAtomSelectionDialog extends GUIModalDialog
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -1416475901274128714L;
	
    private VertexViewPanel vertexViewer;

    private List<List<IAtom>> selectedAtoms = new ArrayList<>();
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a dialog for setting the preferences of the GUI.
	 * @param refForPlacement the component used to place this dialog.
	 */
	@SuppressWarnings("serial")
    public GUIAtomSelectionDialog(Component refForPlacement, Vertex vertex, 
        List<IAtom> highlightedAtoms,
        int selectionGroupsCount, int selectionGroupIndex)
	{
	    super(refForPlacement);

        if (selectionGroupsCount > 1) {
	        setTitle("Select Atoms of group " + (selectionGroupIndex) + " (out of " + selectionGroupsCount + " groups)");
            this.btnDone.setText("Done");
            this.btnDone.setToolTipText("Save selection and move to next selection group.");
        } else {
            setTitle("Select Atoms");
            this.btnDone.setText("Done");
            this.btnDone.setToolTipText("Save selection.");
        }

        // The viewer with Jmol and non-editable APtable
		vertexViewer = new VertexViewPanel(false);
        addToCentralPane(vertexViewer);
        vertexViewer.loadVertexToViewer(vertex);

        // Highlight the atoms if provided
        if (highlightedAtoms != null) {
            vertexViewer.highlightAtoms(highlightedAtoms);
        }

		// Customize the buttons of the modal dialog
		for (ActionListener listener : btnDone.getActionListeners())
		{
			btnDone.removeActionListener(listener);
		}
		this.btnDone.setText("Done");
		this.btnDone.setToolTipText("Save selection and moves on.");
		this.btnDone.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
                // Store the current selection
                IAtomContainer iac = vertex.getIAtomContainer();
				List<IAtom> selectedAtms = vertexViewer.getAtomsSelectedFromJMol();
                result = selectedAtms;

                //TODO del
                for (IAtom atm : selectedAtms) {
                    System.out.println(MoleculeUtils.getAtomRef(atm, iac));
                }

                close();
			}
		});
		
		this.btnCanc.setEnabled(true);
		this.btnCanc.setVisible(true);
	}

//-----------------------------------------------------------------------------

    @Override
    public void dispose()
    {
        if (vertexViewer != null)
        {
            remove(vertexViewer);
            vertexViewer.dispose();
            vertexViewer = null;
        }
        super.dispose();
    }

//-----------------------------------------------------------------------------

}
