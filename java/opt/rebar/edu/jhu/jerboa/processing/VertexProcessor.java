package edu.jhu.jerboa.processing;

import java.util.List;
import java.io.IOException;
import java.lang.reflect.*;

import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;
import edu.jhu.jerboa.util.JerboaProperties;

public class VertexProcessor {
  String propPrefix;
  
  String labelAttr;
  int attrIdx;

  public VertexProcessor () throws IOException {
    this.propPrefix = "VertexProcessor";
    
    char[] tmpAttr = JerboaProperties.getString(this.propPrefix +
                                                ".labelAttribute").toCharArray();
    tmpAttr[0] = Character.toUpperCase(tmpAttr[0]);
    this.labelAttr = new String(tmpAttr);

    this.attrIdx = JerboaProperties.getInt(this.propPrefix + ".attributeIndex");
  }

  public String getLabel (Vertex v) throws
    ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
  InvocationTargetException {
    // Buckle up. This is going to get ugly quick.
    
    Class attrClass =
      Class.forName("edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge$" +
                    this.labelAttr + "Attribute");
    
    PersonInfo pi = v.getPersonInfo();

    Method m1 = pi.getClass().getMethod("get" + this.labelAttr + "List");
    List attributeList = (List) m1.invoke(pi, new Object[0]);
    
    Object attribute = (GenderAttribute) attributeList.get(this.attrIdx);

    Method m = attrClass.getMethod("get" + this.labelAttr);
    String label = m.invoke(attribute, new Object[0]).toString();
    return label;
  }
}