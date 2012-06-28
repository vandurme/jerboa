package edu.jhu.jerboa.processing;

import java.util.List;
import java.lang.reflect.*;

import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;

public class VertexProcessing {
  public static String getLabel (Vertex v, String labelAttr) throws
    ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
  InvocationTargetException {
    // Buckle up. This is going to get ugly quick.
    
    Class attrClass =
      Class.forName("edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge$" +
                    labelAttr + "Attribute");
    
    PersonInfo pi = v.getPersonInfo();

    Method m1 = pi.getClass().getMethod("get" + labelAttr + "List");
    List attributeList = (List) m1.invoke(pi, new Object[0]);
    
    Object attribute = (GenderAttribute) attributeList.get(0);

    Method m = attrClass.getMethod("get" + labelAttr);
    String label = m.invoke(attribute, new Object[0]).toString();
    return label;
  }
}