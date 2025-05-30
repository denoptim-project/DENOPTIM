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


import java.awt.BorderLayout;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;


/**
 * A panel that allows to print the content of a text file into a GUI tab.
 * 
 * @author Marco Foscato
 */

public class GUITextReader extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger texReaderTabUID = new AtomicInteger(1);
	
	private JTextPane txtPane;
	private TitledBorder border;

	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUITextReader(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Text File Content #" 
					+ texReaderTabUID.getAndIncrement());
		super.setLayout(new BorderLayout());
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize() 
	{	
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
        
		txtPane = new JTextPane();
		txtPane.setContentType("text");
		txtPane.setText("No content");
		txtPane.setEditable(false);
		txtPane.setBackground(null);
		Border bevel = BorderFactory.createLoweredBevelBorder();
		border = BorderFactory.createTitledBorder(bevel, "No source of text");
		border.setTitleJustification(TitledBorder.CENTER);
		txtPane.setBorder(border);
        this.add(txtPane,BorderLayout.CENTER);
        

        ButtonsBar commandsPane = new ButtonsBar();
        this.add(commandsPane, BorderLayout.SOUTH);
        JButton btnCanc = new JButton("Close Tab");
        btnCanc.setToolTipText("Closes this FSERun Inspector.");
        btnCanc.addActionListener(new removeCardActionListener(this));
        commandsPane.add(btnCanc);
	}
    
//-----------------------------------------------------------------------------

	/**
	 * Print the content of the given file in this tab's pane.
	 * @param file the file to be read
	 */
    public void displayContent(File file)
    {
        String fileID = file.getAbsolutePath();
        if (file.getAbsolutePath().length() > 80)
            fileID = file.getName();
        
        try
        {
            txtPane.setText(DenoptimIO.readText(file.getAbsolutePath()));
            setSourceTitle(fileID);
        } catch (DENOPTIMException e)
        {
            setSourceTitle("ERROR while reading '" + fileID + "'");
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); 
            txtPane.setText("Could not read content of file: " 
                 + System.getProperty("line.separator") 
                 + sStackTrace);
        }
    }

//-----------------------------------------------------------------------------
    
    
    private void setSourceTitle(String string)
    {
        border.setTitle("Content of " + string);
    }

//-----------------------------------------------------------------------------
  	
}
