/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EdgeQuery;
import denoptim.rings.ClosableChain;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;


/**
 * Definition of a graph editing task.
 *
 * @author Marco Foscato
 */

public class DENOPTIMGraphEdit
{
    /**
     * Type of editing task
     */
    private String taskType = "NONE";

    /**
     * Vertex subject to editing task
     */
    private DENOPTIMVertex focusVrtx = null;

    /**
     * Attachment point subject to editing task
     */
    private DENOPTIMAttachmentPoint focusAP = null; 

    /**
     * Edge subject to editing task
     */
    private EdgeQuery focusEdge = null;

    /**
     * Incoming subgraph
     */
    private DENOPTIMGraph incomingGraph = null;


    private final String TYPLAB = "Type=";
    private final String VRTLAB = "FocusVertex=";
    private final String EDGLAB = "FocusEdge=";
    private final String APLAB  = "FocusAP=";
    private final String GRPLAB = "IncomingGraph=";

    /**
     * String identifying the task of replacing a chiled and all its branch
     * with a given sub-graph
     */
    public static final String REPLACECHILD = "REPLACECHILD";

    /**
     * String identifying the task of removing a specific vertex
     */
    public static final String DELETEVERTEX = "DELETEVERTEX";
    public static final Set<String> EDIT_TASKS =
                    new HashSet<>(Arrays.asList(new String[] {
                                  REPLACECHILD,
                                  DELETEVERTEX}
                    ));

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit()
    {
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit(String type, DENOPTIMVertex v,
                             DENOPTIMAttachmentPoint ap, EdgeQuery edg,
                             DENOPTIMGraph inGraph)
    {
        this.taskType = type;
        this.focusVrtx = v;
        this.focusAP = ap;
        this.focusEdge = edg;
        this.incomingGraph = inGraph;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit(String line) throws DENOPTIMException
    {
        if (!line.trim().startsWith("DENOPTIMGraphEdit "))
        {
            String msg = "Line '" + line.trim() +"' does not start with the "
                         + " 'DENOPTIMGraphEdit ' label. Cannot be converted "
                         + "into a DENOPTIMGraphEdit.";
            throw new DENOPTIMException(msg);
        }

        int startOfType = line.indexOf(TYPLAB);
        int startOfVrt = line.indexOf(VRTLAB);
        int startOfEdg = line.indexOf(EDGLAB);
        int startOfAP = line.indexOf(APLAB);
        int startOfGraph = line.indexOf(GRPLAB);
        if (-1 == startOfType)
        {
            String msg = "Cannot fine type of graph enditing task in Line '" 
                                          + line.trim() +"'. Check the inptu.";
            throw new DENOPTIMException(msg);
        }
        String typStr = line.substring(startOfType+TYPLAB.length());
        typStr = typStr.substring(0,GenUtils.getIdxOfClosing(2,typStr));
        if (!EDIT_TASKS.contains(typStr.toUpperCase()))
        {
            String msg = "Unrecognized type of graph enditing task in Line '"
                                          + line.trim() +"'. Check the inptu.";
            throw new DENOPTIMException(msg);
        }
        this.taskType = typStr;

        if (-1 != startOfVrt)
        {
            String str = line.substring(startOfVrt+VRTLAB.length());
            str = str.substring(0,GenUtils.getIdxOfClosing(2,str));
	    str = str.trim();
            String strPrts[] = str.split("_");
            // vertex id
	    int vid = -1;
	    if (!strPrts[0].equals("*"))
	    {
		vid = Integer.parseInt(strPrts[0]);
	    }
            // molid
            int molid = -1;
            if (!strPrts[1].equals("*"))
            {
                molid = Integer.parseInt(strPrts[1]) - 1;
            }
            // type scaffold/fragment/capping group
            int fragtype = -1;
            if (!strPrts[2].equals("*"))
            {
                fragtype = Integer.parseInt(strPrts[2]);
            }
            // level
            int level = -2;
            if (!strPrts[3].equals("*"))
            {
                level = Integer.parseInt(strPrts[3]);
            }
            DENOPTIMVertex dv = new DENOPTIMVertex(vid, molid, 
			   new ArrayList<DENOPTIMAttachmentPoint>(), fragtype);
            dv.setLevel(level);

            this.focusVrtx = dv;
        }

        if (-1 != startOfEdg)
        {
            String str = line.substring(startOfEdg+EDGLAB.length());
            str = str.substring(0,GenUtils.getIdxOfClosing(2,str));
            str = str.trim();
            
            this.focusEdge = new EdgeQuery();
            
            String strPrts[] = str.split("_");
            // source vertex
            if (!strPrts[0].equals("*"))
            {
                this.focusEdge.setSourceVertex(Integer.parseInt(strPrts[0]));
            }
            // source attachment point
            if (!strPrts[1].equals("*"))
            {
                this.focusEdge.setSourceDAP(Integer.parseInt(strPrts[1]));
            }
            // target vertex
            if (!strPrts[2].equals("*"))
            {
                this.focusEdge.setTargetVertex(Integer.parseInt(strPrts[2]));
            }
            // target attachment point
            if (!strPrts[3].equals("*"))
            {
                this.focusEdge.setTargetDAP(Integer.parseInt(strPrts[3]));
            }
            // bond type
            if (!strPrts[4].equals("*"))
            {
                this.focusEdge.setBondType(Integer.parseInt(strPrts[4]));
            }
		    
		    if (strPrts.length > 5)
		    {
		        //source APClass
		        if (!strPrts[5].equals("*"))
		        {
                    this.focusEdge.setSourceReaction(strPrts[5]);
                }

                //target APClass
                if (!strPrts[6].equals("*"))
                {
                    this.focusEdge.setTargetReaction(strPrts[6]);
                }
		    }
        }

        if (-1 != startOfAP)
        {
            String str = line.substring(startOfAP+APLAB.length());
            str = str.substring(0,GenUtils.getIdxOfClosing(2,str));
            str = str.trim();
            String strPrts[] = str.split("\\|");
            // AP index
            int apid = -1;
            if (!strPrts[0].equals("*"))
            {
                apid = Integer.parseInt(strPrts[0]);
            }
            // number of potential connections
            int conn = -1;
            if (!strPrts[1].equals("*"))
            {
                conn = Integer.parseInt(strPrts[1]);
            }
            // number of free connections
            int fconn = -1;
            if (!strPrts[2].equals("*"))
            {
                fconn = Integer.parseInt(strPrts[2]);
            }
            DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(apid,conn,
								        fconn);
		    if (strPrts.length > 3)
		    {
                // AP class
                String apClass = "*";
                if (!strPrts[3].equals("*"))
                {
                    apClass = strPrts[3];
                }
                ap.setAPClass(apClass);
		    }
		    this.focusAP = ap;
        }

        if (-1 != startOfGraph)
        {
            String str = line.substring(startOfGraph+GRPLAB.length());
            str = str.substring(0,GenUtils.getIdxOfClosing(2,str));
            str = str.trim();
            this.incomingGraph = GraphConversionTool.getGraphFromString(str);
        }
    }

//------------------------------------------------------------------------------

    public String getType()
    {
        return taskType;
    }
    
//------------------------------------------------------------------------------

    public void setType(String t)
    {
        this.taskType = t;
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex getFocusVertex()
    {
        return focusVrtx;
    }

//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint getFocusAP()
    {
        return focusAP;
    }

//------------------------------------------------------------------------------

    public EdgeQuery getFocusEdge()
    {
        return focusEdge;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph getIncomingGraph()
    {
	return incomingGraph;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(512);
        
        sb.append("DENOPTIMGraphEdit [[");
        sb.append(TYPLAB).append("=").append(taskType).append("] ");
        sb.append(VRTLAB).append("=").append(focusVrtx.toString()).append("] ");
        sb.append(EDGLAB).append("=").append(focusEdge.toString()).append("] ");
        sb.append(APLAB).append("=").append(focusAP.toString()).append("] ");
        sb.append(GRPLAB).append("=").append(incomingGraph.toString());
        sb.append("]] ");

        return sb.toString();
    }

//------------------------------------------------------------------------------

}
