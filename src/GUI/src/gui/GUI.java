package gui;

import java.awt.EventQueue;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.JButton;

import java.awt.Font;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Image;

import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class GUI {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		
		JButton btnDenoptim = new JButton("DENOPTIM");
		btnDenoptim.setFont(new Font("Lucida Grande", Font.BOLD, 11));
		toolBar.add(btnDenoptim);
		
		//Toolbar menu
		JButton btnHelp = new JButton("Help");
		btnHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
		toolBar.add(btnHelp);
		
		//Main panel
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new GridLayout(2, 1, 3, 0));
		
		//Figure panel
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		
		JLabel show_image = new JLabel("");
		//TODO: create PNG of proper size
		show_image.setIcon(new ImageIcon(new ImageIcon("images/DENOPTIM_extended_logo.png").getImage().getScaledInstance(200, 87, Image.SCALE_DEFAULT)));
		panel_1.add(show_image);
		
		//Shortcuts panel
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new FlowLayout());
		panel.add(panel_2);
		
		JButton btnNewEvolutionaryDe = new JButton("New Evolutionary De Novo Design");
		panel_2.add(btnNewEvolutionaryDe);
		
		JButton btnNewVirtualScreening = new JButton("New Virtual Screening");
		panel_2.add(btnNewVirtualScreening);
		
		JButton btnNewFragments = new JButton("New Fragments");
		panel_2.add(btnNewFragments);
		
		//Credits
		JPanel credPanel = new JPanel();
		frame.getContentPane().add(credPanel, BorderLayout.SOUTH);
		credPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JTextPane txtpnTheDenoptimProject = new JTextPane();
		txtpnTheDenoptimProject.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		txtpnTheDenoptimProject.setBackground(UIManager.getColor("Button.background"));
		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setAlignment(attribs , StyleConstants.ALIGN_CENTER);
		txtpnTheDenoptimProject.setParagraphAttributes(attribs,false);
		txtpnTheDenoptimProject.setText("The DENOPTIM project is powered by:"+System.getProperty("line.separator")+"University of Bergen and Norwegian University of Science and Technology");
		credPanel.add(txtpnTheDenoptimProject);
		
		JTextPane email = new JTextPane();
		email.setForeground(UIManager.getColor("Button.light"));
		email.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		email.setBackground(UIManager.getColor("Button.background"));
		email.setText("denoptim.project@gmail.com");
		email.setParagraphAttributes(attribs,false);
		credPanel.add(email);
		
	}

}
