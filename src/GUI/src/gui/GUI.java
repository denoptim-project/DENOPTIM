package gui;

import java.awt.EventQueue;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.Font;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Image;

import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GUI {

	private final String NL = System.getProperty("line.separator");
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
		frame = new JFrame("DENOPTIM GUI");
		frame.setBounds(100, 100, 300, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Menu bar
		JMenuBar menuBar = new JMenuBar();
		frame.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		JMenu menuDenoptim = new JMenu("DENOPTIM");
		menuDenoptim.setFont(new Font("Lucida Grande", Font.BOLD, 11));
		menuBar.add(menuDenoptim);
		
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
                        getAboutPanel(),
                        "About DENOPTIM",
                        JOptionPane.PLAIN_MESSAGE);
			}
			
			private JPanel getAboutPanel() {
		        JPanel abtPanel = new JPanel();
		        abtPanel.setLayout(new GridLayout(3, 1, 3, 0));
		        JPanel row1 = new JPanel();
		        JLabel t1 = new JLabel("DENPTIM is open source software (Affero GPL-3.0).");
		        row1.add(t1);
		        abtPanel.add(row1);
		        
		        JPanel row2 = new JPanel();
		        JLabel t2 = new JLabel("Get involved at	");
				JLabel h2 = new JLabel("http://github.com/denoptim-project");
				h2.setForeground(Color.BLUE.darker());
				h2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				h2.addMouseListener(new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent e) {
				    	try {
		                    Desktop.getDesktop().browse(new URI("http://github.com/denoptim-project"));
		                } catch (IOException | URISyntaxException e1) {
		                    e1.printStackTrace();
		                }
				    }
				});
				row2.add(t2);
				row2.add(h2);
				abtPanel.add(row2);
				
				JPanel row3 = new JPanel();
		        JLabel t3 = new JLabel("Cite ");
				JLabel h3 = new JLabel("http://doi.org/10.1021/acs.jcim.9b00516");
				h3.setForeground(Color.BLUE.darker());
				h3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				h3.addMouseListener(new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent e) {
				    	try {
		                    Desktop.getDesktop().browse(new URI("https://doi.org/10.1021/acs.jcim.9b00516"));
		                } catch (IOException | URISyntaxException e1) {
		                    e1.printStackTrace();
		                }
				    }
				});
				row3.add(t3);
				row3.add(h3);
		        abtPanel.add(row3);

		        return abtPanel;
		    }
		});
		menuDenoptim.add(about);
		
		JMenuItem open = new JMenuItem("Open...");
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DenoptimGUIFileOpener foGui = new DenoptimGUIFileOpener();
				foGui.pickFile();
			}
		});
		menuDenoptim.add(open);
		
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuDenoptim.add(exit);
		
		JMenu menuHelp = new JMenu("Help");
		menuHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
		menuBar.add(menuHelp);
		JMenuItem usrManual = new JMenuItem("DENOPTIM user manual");
		usrManual.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	try {
                    Desktop.getDesktop().browse(new URI("http://htmlpreview.github.com/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                    //TODO: use local version of user manual
                }
		    }
		});
		menuHelp.add(usrManual);

		
		//Main panel
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new GridLayout(2, 1, 3, 0));
		
		
		//Figure panel
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		
		JLabel show_image = new JLabel("");
		//TODO: create PNG of proper size
		show_image.setIcon(new ImageIcon(new ImageIcon(this.getClass().getClass().getResource("/images/DENOPTIM_extended_logo.png")).getImage().getScaledInstance(200, 87, Image.SCALE_DEFAULT)));
		panel_1.add(show_image);
		
		
		//Shortcuts panel
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new FlowLayout());
		panel.add(panel_2);
		
		JButton btnNewEvolutionaryDe = new JButton("New Evolutionary De Novo Design");
		btnNewEvolutionaryDe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getNonImplementedError();
			}
		});
		panel_2.add(btnNewEvolutionaryDe);
		
		JButton btnNewVirtualScreening = new JButton("New Virtual Screening");
		btnNewVirtualScreening.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getNonImplementedError();
			}
		});
		panel_2.add(btnNewVirtualScreening);
		
		JButton btnNewFragments = new JButton("New Fragments");
		btnNewFragments.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getNonImplementedError();
			}
		});
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
		credPanel.add(email);
		
	}

	private void getNonImplementedError()
	{
		JOptionPane.showMessageDialog(null,
                "Sorry! Function not implemented yet.",
                "Error",
                JOptionPane.PLAIN_MESSAGE,
                UIManager.getIcon("OptionPane.errorIcon"));
	}
}
