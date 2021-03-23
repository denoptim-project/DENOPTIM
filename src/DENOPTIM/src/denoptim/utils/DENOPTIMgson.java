package denoptim.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;

import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;

import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMGraph.DENOPTIMGraphSerializer;
import denoptim.molecule.DENOPTIMGraph.DENOPTIMGraphDeserializer;

import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex.DENOPTIMVertexSerializer;
import denoptim.molecule.DENOPTIMVertex.DENOPTIMVertexDeserializer;

import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMRing.DENOPTIMRingSerializer;

import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.DENOPTIMEdgeSerializer;

import denoptim.molecule.APMap;
import denoptim.molecule.APMap.APMapSerializer;

import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMAttachmentPoint;


public class DENOPTIMgson
{
  private static DENOPTIMgson instance = null;

  Gson reader;

  Gson writer;

  private DENOPTIMgson()
  {
    writer = new GsonBuilder()
      .registerTypeAdapter(DENOPTIMGraph.class,
                           new DENOPTIMGraphSerializer())

      .setExclusionStrategies(new DENOPTIMExclusionStrategy())
      // Custom serializer to make json string use AP's ID as key in the
      // map. If this is not used, then the key.toString() is used to
      // get a string representation of the key.
      .registerTypeAdapter(APMap.class, new APMapSerializer())
      // Custom serializer that keeps only the IDs to vertexes and
      // APs defined in the list of  vertexes belonging to the graph.
      .registerTypeAdapter(DENOPTIMEdge.class, new DENOPTIMEdgeSerializer())
      // Custom serialized that keeps only the IDs to vertexes defined in
      // the list of vertexes belonging to the graph
      .registerTypeAdapter(DENOPTIMRing.class, new DENOPTIMRingSerializer())
      //TODO-V3 add custom de/serialized for symmetric sets
      .setPrettyPrinting()
      .create();


    reader = new GsonBuilder()
      .setExclusionStrategies(new DENOPTIMExclusionStrategyNoAPMap())
      // Custom deserializer to dispatch to the correct subclass of Vertex
      .registerTypeAdapter(DENOPTIMVertex.class, new DENOPTIMVertexDeserializer())
      // Custom deserializer takes care of converting ID-based components
      // to references to vertexes and APs
      .registerTypeAdapter(DENOPTIMGraph.class, new DENOPTIMGraphDeserializer())
      .setPrettyPrinting()
      .create();
  }

  private static DENOPTIMgson getInstance()
  {
    if (instance == null)
      instance = new DENOPTIMgson();
    return instance;
  }

  public static Gson getReader() {
    return getInstance().reader;
  }

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
              if (field.getDeclaringClass() == DENOPTIMFragment.class
                      && field.getName().equals("mol")) {
                  return true;
              }
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

              return false;
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
              return false;
          }
      }

  //------------------------------------------------------------------------------

      public static class DENOPTIMExclusionStrategyNoAPMap implements ExclusionStrategy
      {
          @Override
          public boolean shouldSkipField(FieldAttributes field) {
              // cannot serialize chemical representations:
              //     class org.openscience.cdk.Atom declares multiple JSON
              //     fields named identifier
              if (field.getDeclaringClass() == DENOPTIMFragment.class
                      && field.getName().equals("mol")) {
                  return true;
              }
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

              return false;
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
              return false;
          }
      }


}
