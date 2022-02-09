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
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.DENOPTIMEdgeSerializer;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMGraph.DENOPTIMGraphDeserializer;
import denoptim.graph.DENOPTIMGraph.DENOPTIMGraphSerializer;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMRing.DENOPTIMRingSerializer;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.DENOPTIMVertexDeserializer;


public class DENOPTIMgson
{
  private static DENOPTIMgson instance = null;

  Gson reader;

  Gson writer;
  
//------------------------------------------------------------------------------

  private DENOPTIMgson()
  {
    writer = new GsonBuilder()
      .registerTypeAdapter(DENOPTIMGraph.class, new DENOPTIMGraphSerializer())
      .setExclusionStrategies(new DENOPTIMExclusionStrategy())
      // Custom serializer to make json string use AP's ID as key in the
      // map. If this is not used, then the key.toString() is used to
      // get a string representation of the key.
      .registerTypeAdapter(APTreeMap.class, new APMapSerializer())
      // Custom serializer that keeps only the IDs to vertices and
      // APs defined in the list of  vertices belonging to the graph.
      .registerTypeAdapter(DENOPTIMEdge.class, new DENOPTIMEdgeSerializer())
      // Custom serialized that keeps only the IDs to vertices defined in
      // the list of vertices belonging to the graph
      .registerTypeAdapter(DENOPTIMRing.class, new DENOPTIMRingSerializer())
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
      .registerTypeAdapter(DENOPTIMVertex.class, 
              new DENOPTIMVertexDeserializer())
      // Custom deserializer takes care of converting ID-based components
      // to references to vertices and APs
      .registerTypeAdapter(DENOPTIMGraph.class, new DENOPTIMGraphDeserializer())
      .registerTypeAdapter(APClass.class, new APClassDeserializer())
      // Custom deserialiser build an IAtomContainer from the light-weight 
      // representations of atoms and bonds.
      .registerTypeHierarchyAdapter(IAtomContainer.class, 
              new IAtomContainerDeserializer())
      .setPrettyPrinting()
      .create();
    
    //TODO-gg
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

  public static Gson getReader() {
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
          

          //TODO-gg remove once mol becomes jsonable
          /*
          if (field.getDeclaringClass() == DENOPTIMFragment.class
                  && field.getName().equals("mol")) {
              return true;
          }
          */
          if (field.getDeclaringClass() == DENOPTIMAttachmentPoint.class
                  && field.getName().equals("owner")) {
              return true;
          }
          if (field.getDeclaringClass() == DENOPTIMAttachmentPoint.class
                  && field.getName().equals("user")) {
              return true;
          }
          if (field.getDeclaringClass() == DENOPTIMVertex.class
                  && field.getName().equals("owner")) {
              return true;
          }
          
          //TODO-gg remove once mol becomes jsonable
          if (field.getDeclaringClass() == DENOPTIMTemplate.class
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
            //TODO-gg remove once mol becomes jsonable
              /*
              if (field.getDeclaringClass() == DENOPTIMFragment.class
                      && field.getName().equals("mol")) {
                  return true;
              }
              */
              if (field.getDeclaringClass() == DENOPTIMAttachmentPoint.class
                      && field.getName().equals("owner")) {
                  return true;
              }
              if (field.getDeclaringClass() == DENOPTIMAttachmentPoint.class
                      && field.getName().equals("user")) {
                  return true;
              }
              if (field.getDeclaringClass() == DENOPTIMVertex.class
                      && field.getName().equals("owner")) {
                  return true;
              }
              if (field.getDeclaringClass() == DENOPTIMTemplate.class
                      && field.getName().equals("innerToOuterAPs")) {
                  return true;
              }
              if (field.getDeclaringClass() == DENOPTIMTemplate.class
                      && field.getName().equals("innerGraph")) {
                  return true;
              }
            //TODO-gg remove once mol becomes jsonable
              if (field.getDeclaringClass() == DENOPTIMTemplate.class
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
