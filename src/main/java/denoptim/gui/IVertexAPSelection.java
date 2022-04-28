/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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
