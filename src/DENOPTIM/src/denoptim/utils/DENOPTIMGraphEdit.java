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

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.*;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;


/**
 * Definition of a graph editing task.
 *
 * @author Marco Foscato
 */

public class DENOPTIMGraphEdit {
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
    private DENOPTIMEdge focusEdge = null;

    /**
     * Incoming subgraph
     */
    private DENOPTIMGraph incomingGraph = null;


    private final String TYPLAB = "Type=";
    private final String VRTLAB = "FocusVertex=";
    private final String EDGLAB = "FocusEdge=";
    private final String APLAB = "FocusAP=";
    private final String GRPLAB = "IncomingGraph=";

    /**
     * String identifying the task of replacing a child and all its branch
     * with a given sub-graph
     */
    public static final String REPLACECHILD = "REPLACECHILD";

    /**
     * String identifying the task of removing a specific vertex
     */
    public static final String DELETEVERTEX = "DELETEVERTEX";
    public static final Set<String> EDIT_TASKS =
            new HashSet<>(Arrays.asList(REPLACECHILD, DELETEVERTEX));

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit() {
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit(String type, DENOPTIMVertex v,
                             DENOPTIMAttachmentPoint ap, DENOPTIMEdge edg,
                             DENOPTIMGraph inGraph) {
        this.taskType = type;
        this.focusVrtx = v;
        this.focusAP = ap;
        this.focusEdge = edg;
        this.incomingGraph = inGraph;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraphEdit(String line) throws DENOPTIMException {
        if (!line.trim().startsWith("DENOPTIMGraphEdit ")) {
            String msg = "Line '" + line.trim() + "' does not start with the "
                    + " 'DENOPTIMGraphEdit ' label. Cannot be converted "
                    + "into a DENOPTIMGraphEdit.";
            throw new DENOPTIMException(msg);
        }

        int startOfType = line.indexOf(TYPLAB);
        int startOfVrt = line.indexOf(VRTLAB);
        int startOfEdg = line.indexOf(EDGLAB);
        int startOfAP = line.indexOf(APLAB);
        int startOfGraph = line.indexOf(GRPLAB);
        if (-1 == startOfType) {
            String msg = "Cannot fine type of graph editing task in Line '"
                    + line.trim() + "'. Check the input.";
            throw new DENOPTIMException(msg);
        }
        String typStr = line.substring(startOfType + TYPLAB.length());
        typStr = typStr.substring(0, GenUtils.getIdxOfClosing(2, typStr));
        if (!EDIT_TASKS.contains(typStr.toUpperCase())) {
            String msg = "Unrecognized type of graph editing task in Line '"
                    + line.trim() + "'. Check the input.";
            throw new DENOPTIMException(msg);
        }
        this.taskType = typStr;

        if (-1 != startOfVrt) {
            String str = line.substring(startOfVrt + VRTLAB.length());
            str = str.substring(0, GenUtils.getIdxOfClosing(2, str));
            str = str.trim();
            String[] strPrts = str.split("_");
            // vertex id
            int vid = -1;
            if (!strPrts[0].equals("*")) {
                vid = Integer.parseInt(strPrts[0]);
            }
            // molid
            int molid = -1;
            if (!strPrts[1].equals("*")) {
                molid = Integer.parseInt(strPrts[1]) - 1;
            }
            // type scaffold/fragment/capping group
            BBType fragtype = BBType.UNDEFINED;
            if (!strPrts[2].equals("*")) {
                fragtype = BBType.parseInt(Integer.parseInt(strPrts[2]));
            }
            // level
            int level = -2;
            if (!strPrts[3].equals("*")) {
                level = Integer.parseInt(strPrts[3]);
            }

            //TODO-V3: use whatever way to identify a fragment without giving a 
            // vertex ID
            DENOPTIMVertex dv = DENOPTIMVertex.newVertexFromLibrary(vid, molid,
                    fragtype);

            dv.setLevel(level);

            this.focusVrtx = dv;
        }

        if (-1 != startOfEdg) {
            String str = line.substring(startOfEdg + EDGLAB.length());
            str = str.substring(0, GenUtils.getIdxOfClosing(2, str));
            str = str.trim();
            String[] strPrts = str.split("_");
            // source vertex
            int srcVertexId = -1;
            if (!strPrts[0].equals("*")) {
                srcVertexId = Integer.parseInt(strPrts[0]);
            }
            // source attachment point
            int srcAPId = -1;
            if (!strPrts[1].equals("*")) {
                srcAPId = Integer.parseInt(strPrts[1]);
            }
            // target vertex
            int trgVertexId = -1;
            if (!strPrts[2].equals("*")) {
                trgVertexId = Integer.parseInt(strPrts[2]);
            }
            // target attachment point
            int trgAPId = -1;
            if (!strPrts[3].equals("*")) {
                trgAPId = Integer.parseInt(strPrts[3]);
            }
            // bond type
            int btype = -1;
            if (!strPrts[4].equals("*")) {
                btype = Integer.parseInt(strPrts[4]);
            }

            // TODO: Broken code. Only here to allow code to compile.
            // Change this section when wildcard problem solved.
            DENOPTIMVertex srcVertex = new EmptyVertex(srcVertexId);
            // Have to fill source and target vertex with enough APs
            for (int i = 0; i <= srcAPId; i++) {
                srcVertex.addAP(new DENOPTIMAttachmentPoint(srcVertex));
            }
            DENOPTIMVertex trgVertex = new EmptyVertex(trgVertexId);
            for (int i = 0; i <= trgAPId; i++) {
                trgVertex.addAP(new DENOPTIMAttachmentPoint(trgVertex));
            }
		    this.focusEdge = new DENOPTIMEdge(srcVertex.getAP(srcAPId),
                    trgVertex.getAP(trgAPId), srcVertexId, trgVertexId,
                    srcAPId, trgAPId, BondType.parseInt(btype));
            // old code
//            this.focusEdge = new DENOPTIMEdge(srcVertexId, trgVertexId,
//                    srcAPId, trgAPId, BondType.parseInt(btype));

            if (strPrts.length > 5) {
                //source APClass
                String srcAPC = "*";
                if (!strPrts[5].equals("*")) {
                    srcAPC = strPrts[5];
                }
                // old code
//                this.focusEdge.setSrcApClass(srcAPC);
                srcVertex.getAP(srcAPId).setAPClass(srcAPC);

                //target APClass
                String trgAPC = "*";
                if (!strPrts[6].equals("*")) {
                    trgAPC = strPrts[6];
                }
                //old code
//                this.focusEdge.setTrgApClass(trgAPC);
                trgVertex.getAP(trgAPId).setAPClass(trgAPC);

            }
            // TODO: End of broken code section
        }

        if (-1 != startOfAP) {
            String str = line.substring(startOfAP + APLAB.length());
            str = str.substring(0, GenUtils.getIdxOfClosing(2, str));
            str = str.trim();
            String[] strPrts = str.split("\\|");
            // AP index
            int apid = -1;
            if (!strPrts[0].equals("*")) {
                apid = Integer.parseInt(strPrts[0]);
            }
            // number of potential connections
            int conn = -1;
            if (!strPrts[1].equals("*")) {
                conn = Integer.parseInt(strPrts[1]);
            }
            // number of free connections
            int fconn = -1;
            if (!strPrts[2].equals("*")) {
                fconn = Integer.parseInt(strPrts[2]);
            }

            // TODO(Discuss with Marco if ok using dummyVertex)
            EmptyVertex dummyVertex = new EmptyVertex();
            DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(
                    dummyVertex, apid, conn, fconn
            );
            if (strPrts.length > 3) {
                // AP class
                String apClass = "*";
                if (!strPrts[3].equals("*")) {
                    apClass = strPrts[3];
                }
                ap.setAPClass(apClass);
            }
            this.focusAP = ap;
        }

        if (-1 != startOfGraph) {
            String str = line.substring(startOfGraph + GRPLAB.length());
            str = str.substring(0, GenUtils.getIdxOfClosing(2, str));
            str = str.trim();
            this.incomingGraph = GraphConversionTool.getGraphFromString(str);
        }
    }

//------------------------------------------------------------------------------

    public String getType() {
        return taskType;
    }

//------------------------------------------------------------------------------

    public void setType(String t) {
        this.taskType = t;
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex getFocusVertex() {
        return focusVrtx;
    }

//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint getFocusAP() {
        return focusAP;
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge getFocusEdge() {
        return focusEdge;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph getIncomingGraph() {
        return incomingGraph;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "DENOPTIMGraphEdit [[" +
                TYPLAB + "=" + taskType + "] " +
                VRTLAB + "=" + focusVrtx.toString() + "] " +
                EDGLAB + "=" + focusEdge.toString() + "] " +
                APLAB + "=" + focusAP.toString() + "] " +
                GRPLAB + "=" + incomingGraph.toString() +
                "]] ";
    }

//------------------------------------------------------------------------------

}