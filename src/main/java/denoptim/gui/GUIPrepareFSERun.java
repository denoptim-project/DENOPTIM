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

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Master form containing all sub-forms that need to be filled to define the input parameters for FragSpaceExplorer.
 * 
 * @author Marco Foscato
 *
 */

public class GUIPrepareFSERun extends GUIPrepare
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Unique identified for instances of this form
	 */
	public static AtomicInteger prepFSETabUID = new AtomicInteger(1);

	/**
	 * Constructor
	 */
	public GUIPrepareFSERun(GUIMainPanel mainPanel) {
		super(mainPanel, "Prepare FSE experiment #" + prepFSETabUID.getAndIncrement());
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		FSEParametersForm gaParsPane = new FSEParametersForm(mainPanel.getSize());
		super.allParams.add(gaParsPane);
		super.tabbedPane.addTab("Combinatorial Explorer", null, gaParsPane, null);
		
		FSParametersForm fseParsPane = new FSParametersForm(mainPanel.getSize());
		super.allParams.add(fseParsPane);
		super.tabbedPane.addTab("Space of Building Blocks", null, fseParsPane, null);
		
		FitnessParametersForm fitParsPane = new FitnessParametersForm(mainPanel.getSize());
		super.allParams.add(fitParsPane);
		super.tabbedPane.addTab("Fitness Provider", null, fitParsPane, null);
		
	}
}
