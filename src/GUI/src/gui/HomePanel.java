package gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

	/**
	 * Constructor
	 */
	public HomePanel(GUIMainPanel mainPanel) {
		super(mainPanel, "Home");
		initialize();
	}

	/**
	 * Initialize the panel with figure and shortcuts and credit panels
	 */
	private void initialize() {
		this.setLayout(new BorderLayout(0, 0));
		
		//Central panel (figure and shortcuts)
		JPanel centralPanel = new JPanel();
		this.add(centralPanel, BorderLayout.CENTER);
		centralPanel.setLayout(new GridLayout(2, 1, 3, 0));
		
		//Figure panel
		JPanel figurePanel = new JPanel();
		centralPanel.add(figurePanel);
		
		JLabel show_image = new JLabel("");
		show_image.setIcon(new ImageIcon(new ImageIcon(this.getClass().getClass().getResource("/images/DENOPTIM_extended_logo.png")).getImage()));
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
		buttonsPanel.add(btnNewGA);
		
		JButton btnNewVirtualScreening = new JButton("New Virtual Screening");
		btnNewVirtualScreening.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIPrepareFSERun(mainPanel));
			}
		});
		buttonsPanel.add(btnNewVirtualScreening);
		
		JButton btnNewFragments = new JButton("New Fragments");
		btnNewFragments.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIFragmentInspector(mainPanel));
			}
		});
		buttonsPanel.add(btnNewFragments);
		
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
		email.setForeground(UIManager.getColor("Button.light"));
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
	}
}
