package denoptim.gui;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A popup menu' that has a fixed size and can be scrolled to see menu items 
 * that do not fit into the fixed-size panel.
 */
public class ScrollableJPupupMenu
{
    private JPopupMenu menu;
    private JPanel menuPanel;
    private List<JCheckBox> allBChekBoxes = new ArrayList<JCheckBox>();
    
//------------------------------------------------------------------------------
    
    public ScrollableJPupupMenu()
    {}

//------------------------------------------------------------------------------
    
    /**
     * Displays the menu with the components that have been added so far.
     * @param invoker
     * @param x
     * @param y
     */
    public void showMenu(Component invoker, int x, int y)
    {
        menu = new JPopupMenu();
        menu.setLayout(new BorderLayout());
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, SwingConstants.VERTICAL)); 
        for (JCheckBox cb : allBChekBoxes)
        {
            menuPanel.add(cb);
        }        

        JScrollPane scrollablePane = new JScrollPane(menuPanel);
        scrollablePane.setPreferredSize(new Dimension(450,400));
        menu.add(scrollablePane, BorderLayout.CENTER);
        
        JButton doneBtn = new JButton("Done");
        doneBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                menu.setVisible(false);
            }
        });
        menu.add(doneBtn, BorderLayout.SOUTH);
        
        menu.pack();
        menu.setInvoker(invoker);
        Point invokerOrigin = invoker.getLocationOnScreen();
        menu.setLocation((int) invokerOrigin.getX() + x, 
                (int) invokerOrigin.getY() + y);
        menu.setVisible(true);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Add a check box to the list of check boxes to display in the menu
     * @param item
     */
    public void addCheckBox(JCheckBox item)
    {
        allBChekBoxes.add(item);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of check boxes that is displayed in the menu
     * @return
     */
    protected List<JCheckBox> getAllBChekBoxes()
    {
        return allBChekBoxes;
    }
    
//------------------------------------------------------------------------------
    
}
