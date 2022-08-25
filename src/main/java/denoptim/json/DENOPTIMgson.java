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

package denoptim.json;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import denoptim.graph.APClass;
import denoptim.graph.APClass.APClassDeserializer;
import denoptim.graph.APTreeMap;
import denoptim.graph.APTreeMap.APMapSerializer;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.DGraph.DENOPTIMGraphDeserializer;
import denoptim.graph.DGraph.DENOPTIMGraphSerializer;
import denoptim.graph.Edge;
import denoptim.graph.Edge.DENOPTIMEdgeSerializer;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Ring.DENOPTIMRingSerializer;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.DENOPTIMVertexDeserializer;

/**
 * Class for de/serializing DENOPTIM graphs from/to JSON format.
 */
public class DENOPTIMgson
{
  private static DENOPTIMgson instance = null;

  Gson reader;

  Gson writer;
  
//------------------------------------------------------------------------------

  private DENOPTIMgson()
  {
    writer = new GsonBuilder()
        .registerTypeAdapter(DGraph.class, new DENOPTIMGraphSerializer())
        .setExclusionStrategies(new DENOPTIMExclusionStrategy())
        // Custom serializer to make json string use AP's ID as key in the
        // map. If this is not used, then the key.toString() is used to
        // get a string representation of the key.
        .registerTypeAdapter(APTreeMap.class, new APMapSerializer())
        // Custom serializer that keeps only the IDs to vertices and
        // APs defined in the list of  vertices belonging to the graph.
        .registerTypeAdapter(Edge.class, new DENOPTIMEdgeSerializer())
        // Custom serialized that keeps only the IDs to vertices defined in
        // the list of vertices belonging to the graph
        .registerTypeAdapter(Ring.class, new DENOPTIMRingSerializer())
        // Custom serializer to make serialisation of IAtomContainers feasible.
        // The registerTypeHierarchyAdapter is needed because of IAtomContainer is
        // an interface.
        .registerTypeHierarchyAdapter(IAtomContainer.class, 
              new IAtomContainerSerializer())
        .setPrettyPrinting()
        .create();

    reader = new GsonBuilder()
        .setExclusionStrategies(new DENOPTIMExclusionStrategyNoAPMap())
        // Custom deserializer to dispatch to the correct subclass of Vertex
        .registerTypeAdapter(Vertex.class, 
              new DENOPTIMVertexDeserializer())
        // Custom deserializer takes care of converting ID-based components
        // to references to vertices and APs
        .registerTypeAdapter(DGraph.class, new DENOPTIMGraphDeserializer())
        .registerTypeAdapter(APClass.class, new APClassDeserializer())
        // Custom deserialiser build an IAtomContainer from the light-weight 
        // representations of atoms and bonds.
        .registerTypeHierarchyAdapter(IAtomContainer.class, 
              new IAtomContainerDeserializer())
        .setPrettyPrinting()
        .create();
    
      /*
       * WARNING:
       * If you have to add a Type adapter in the reader, you should consider
       * doing it also in the GSON reader defined in DENOPTIMVertexDeserializer.
       */
    
    }

//------------------------------------------------------------------------------
  
    private static DENOPTIMgson getInstance()
    {
        if (instance == null)
            instance = new DENOPTIMgson();
        return instance;
    }

//------------------------------------------------------------------------------

    public static Gson getReader() 
    {
        return getInstance().reader;
    }

//------------------------------------------------------------------------------

    public static Gson getWriter() {
        return getInstance().writer;
    }

//------------------------------------------------------------------------------

    public static class DENOPTIMExclusionStrategy implements ExclusionStrategy
    {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            // cannot serialize chemical representations:
            //     class org.openscience.cdk.Atom declares multiple JSON
            //     fields named identifier
          
            if (field.getDeclaringClass() == AttachmentPoint.class
                    && field.getName().equals("owner")) {
                return true;
            }
            if (field.getDeclaringClass() == AttachmentPoint.class
                    && field.getName().equals("user")) {
                return true;
            }
            if (field.getDeclaringClass() == Vertex.class
                    && field.getName().equals("owner")) {
                return true;
            }
            if (field.getDeclaringClass() == Fragment.class
                    && field.getName().equals("jGraphFragIsomorphism")) {
                return true;
            }
            if (field.getDeclaringClass() == Template.class
                    && field.getName().equals("mol")) {
                return true;
            }

            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

//------------------------------------------------------------------------------

    public static class DENOPTIMExclusionStrategyNoAPMap 
        implements ExclusionStrategy
    {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            // cannot serialize chemical representations:
            //     class org.openscience.cdk.Atom declares multiple JSON
            //     fields named identifier
            if (field.getDeclaringClass() == AttachmentPoint.class
                    && field.getName().equals("owner")) {
                return true;
            }
            if (field.getDeclaringClass() == AttachmentPoint.class
                   && field.getName().equals("user")) {
                return true;
            }
            if (field.getDeclaringClass() == Vertex.class
                    && field.getName().equals("owner")) {
                return true;
            }
            if (field.getDeclaringClass() == Fragment.class
                    && field.getName().equals("jGraphFragIsomorphism")) {
                return true;
            }
            if (field.getDeclaringClass() == Template.class
                    && field.getName().equals("innerToOuterAPs")) {
                return true;
            }
            if (field.getDeclaringClass() == Template.class
                    && field.getName().equals("innerGraph")) {
                return true;
            }
            if (field.getDeclaringClass() == Template.class
                    && field.getName().equals("mol")) {
                return true;
            }

            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

//------------------------------------------------------------------------------

}
