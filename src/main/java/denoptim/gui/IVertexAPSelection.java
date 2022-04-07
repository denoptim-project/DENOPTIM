package denoptim.gui;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import denoptim.graph.AttachmentPoint;

/**
 * Interface for all vertex viewers that intend to allow selection of attachment
 * points.
 */
public interface IVertexAPSelection
{
    public final String APDATACHANGEEVENT = "APDATA";
    
    public ArrayList<Integer> getSelectedAPIDs();

    public Map<Integer,AttachmentPoint> getMapOfAPsInTable();

    public DefaultTableModel getAPTableModel();
    
}
