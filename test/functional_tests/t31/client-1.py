#!/usr/bin/python
#
# Example of script reading data via the Py4J's JavaGateway
#

import sys
from py4j.java_gateway import JavaGateway

gateway = JavaGateway()
data = gateway.entry_point.loadData("graph-1.sdf")
if data.getGraphId()!=2 or data.getVertexCount()!=11 or data.getEdgeCount()!=10 or data.getRingCount()!=1:
    print("Failing rule 1")
    sys.exit(1)
v2 = data.getVertexAtPosition(2)
if v2.getBuildingBlockId()!=4 or v2.getAttachmentPoints().size()!=3:
    print("Failing rule 2")
    sys.exit(1)
ap = v2.getAP(1)
if ap.getAPClass().toString() != 'p:0' or (abs(ap.getDirectionVector().getX()-0.345296250085735) > 0.000001):
    print("Failing rule 3")
    sys.exit(1)
sys.exit(0)
