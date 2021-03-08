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

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * The home panel contains shortcuts buttons to perform the most common tasks.
 */

public class HomePanel extends GUICardPanel
{

	/**
	 * Version
	 */
	private static final long serialVersionUID = 5512821342651489833L;

//-----------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public HomePanel(GUIMainPanel mainPanel) {
		super(mainPanel, "Home");
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel with figure and shortcuts and credit panels
	 */
	private void initialize() {
		this.setLayout(new BorderLayout(0, 0));
		
		//Central panel (figure and shortcuts)
		JPanel centralPanel = new JPanel();
		this.add(centralPanel, BorderLayout.CENTER);
		
		//Figure panel
		JPanel figurePanel = new JPanel();
		centralPanel.add(figurePanel);
		
		JLabel show_image = new JLabel("");
		
		try
		{
			show_image.setIcon(new ImageIcon(new ImageIcon(
					this.getClass().getClass().getResource(
					"/images/DENOPTIM_extended_logo.png")).getImage()));
		} catch (Throwable t)
		{
			// Problems loading resources
		}
		figurePanel.add(show_image);
		
		//Shortcuts panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout());
		centralPanel.add(buttonsPanel);
		
		JButton btnNewGA = new JButton("New Evolutionary De Novo Design");
		btnNewGA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIPrepareGARun(mainPanel));
			}
		});
		
		JButton btnNewVirtualScreening = new JButton("New Virtual Screening");
		btnNewVirtualScreening.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIPrepareFSERun(mainPanel));
			}
		});
		
		//TODO: new fragmentation job with GM3DFragmenter "New Fragmentation"
		
		JButton btnNewFragments = new JButton("New Fragments");
		btnNewFragments.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIVertexInspector(mainPanel));
			}
		});
		
		JButton btnNewGraph = new JButton("New DENOPTIM Graph");
		btnNewGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIGraphHandler(mainPanel));
			}
		});
		
		JButton btnReadGAOutput = new JButton("Inspect Evolutionary run");
		btnReadGAOutput.setToolTipText("Analyzes the output folder of an "
				+ "evolutionary experiment  (i.e., folder named RUN...)");
		btnReadGAOutput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = DenoptimGUIFileOpener.pickFolder(btnReadGAOutput);
				if (file == null)
				{
					return;
				}
				GUIInspectGARun inspector = new GUIInspectGARun(mainPanel);
				mainPanel.add(inspector);
				inspector.importGARunData(file);
			}
		});
		
        JButton btnReadFSEOutput = new JButton("Inspect Combinatorial run");
        btnReadFSEOutput.setToolTipText("Analyzes the output folder of an "
				+ "combinatorial experiment  (i.e., folder named FSE...)");
        btnReadFSEOutput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File file = DenoptimGUIFileOpener.pickFolder(btnReadFSEOutput);
                if (file == null)
                {
                        return;
                }
                GUIInspectFSERun inspector = new GUIInspectFSERun(mainPanel);
                mainPanel.add(inspector);
                inspector.importFSERunData(file);
            }
        });
 
		
		//Credits panel
		JPanel creditsPanel = new JPanel();
		this.add(creditsPanel, BorderLayout.SOUTH);
		creditsPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JTextPane txtpnCredits = new JTextPane();
		txtpnCredits.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		txtpnCredits.setBackground(UIManager.getColor("Button.background"));
		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setAlignment(attribs , StyleConstants.ALIGN_CENTER);
		txtpnCredits.setParagraphAttributes(attribs,false);
		txtpnCredits.setText("The DENOPTIM project"+System.getProperty("line.separator")+"University of Bergen and Norwegian University of Science and Technology");
		creditsPanel.add(txtpnCredits);
		
		
		JTextPane email = new JTextPane();
		email.setForeground(Color.BLUE.darker());
		email.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		email.setBackground(UIManager.getColor("Button.background"));
		email.setText("denoptim.project@gmail.com");
		email.setParagraphAttributes(attribs,false);
		email.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		email.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		    	try {
                    Desktop.getDesktop().mail(new URI("mailto:denoptim.project@gmail.com"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
		    }
		});
		creditsPanel.add(email);	
		
		JLabel labShortcuts = new JLabel("Shortcuts:");
		
		JLabel labEmpty = new JLabel("<html><br><br><br><br><br><br></html>");
		
        // Here we define the position of all the components of the central panel
		GroupLayout lyoCentralPanel = new GroupLayout(centralPanel);
		centralPanel.setLayout(lyoCentralPanel);
		lyoCentralPanel.setAutoCreateGaps(true);
		lyoCentralPanel.setAutoCreateContainerGaps(true);
		lyoCentralPanel.setHorizontalGroup(lyoCentralPanel.createParallelGroup(
        		GroupLayout.Alignment.CENTER)
        		.addComponent(figurePanel)
        		.addComponent(labShortcuts)
        		.addGroup(lyoCentralPanel.createSequentialGroup()
        				.addComponent(btnNewFragments)
        				.addComponent(btnNewGraph))
        		.addGroup(lyoCentralPanel.createSequentialGroup()
        				.addComponent(btnNewGA)
        				.addComponent(btnNewVirtualScreening))
        		.addGroup(lyoCentralPanel.createSequentialGroup()
        				.addComponent(btnReadGAOutput)
        				.addComponent(btnReadFSEOutput))
        		.addComponent(labEmpty));
		lyoCentralPanel.setVerticalGroup(lyoCentralPanel.createSequentialGroup()
				.addComponent(figurePanel)
				.addComponent(labShortcuts)
                .addGroup(lyoCentralPanel.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(btnNewFragments)
        				.addComponent(btnNewGraph))
        		.addGroup(lyoCentralPanel.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(btnNewGA)
        				.addComponent(btnNewVirtualScreening))
        		.addGroup(lyoCentralPanel.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(btnReadGAOutput)
        				.addComponent(btnReadFSEOutput))
                .addComponent(labEmpty));
		

		//TODO del: only for devel
		/*
		GUIInspectGARun eiPanel = 
				new GUIInspectGARun(mainPanel);
			mainPanel.add(eiPanel);
			eiPanel.importGARunData(new java.io.File("/tmp/denoptim_PtCO/RUN21122019104034"));
		*/
		
		//TODO del: only for devel
		/*
		GUIInspectFSERun inspector = new GUIInspectFSERun(mainPanel);
		mainPanel.add(inspector);
		inspector.importFSERunData(new File("/tmp/denoptim_FSE/FSE19122019014853"));
		*/
		
		//TODO del: only for devel
		/*
		GUICompatibilityMatrixTab cpmap = new GUICompatibilityMatrixTab(mainPanel);
		mainPanel.add(cpmap);
		cpmap.importCPMapFromFile(new File("/tmp/CPMap.par"));
		*/
		
	}
}
