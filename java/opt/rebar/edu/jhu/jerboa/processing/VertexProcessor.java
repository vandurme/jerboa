package edu.jhu.jerboa.processing;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.lang.reflect.*;
import java.lang.IllegalStateException;

import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;
import edu.jhu.jerboa.util.JerboaProperties;

public class VertexProcessor {
  String propPrefix;
  
  String labelAttr;
  int attrIdx;
  String attrName;

  private final int INVALID_INT = Integer.MIN_VALUE;
  private final String INVALID_STR = "INVALID_STR";

  public VertexProcessor () throws IOException, IllegalStateException {
    this.propPrefix = "VertexProcessor";
    
    char[] tmpAttr = JerboaProperties.getString(this.propPrefix +
                                                ".labelAttribute").toCharArray();
    tmpAttr[0] = Character.toUpperCase(tmpAttr[0]);
    this.labelAttr = new String(tmpAttr);

    this.attrIdx = JerboaProperties.getInt(this.propPrefix + ".attributeIndex",
                                           this.INVALID_INT);
    this.attrName = JerboaProperties.getString(this.propPrefix + ".attributeName",
                                               this.INVALID_STR);
    
    if (this.attrIdx == this.INVALID_INT && this.attrName == this.INVALID_STR) {
      String errstr = "Neither " + this.propPrefix + ".attributeIndex nor " +
        this.propPrefix + ".attributeName are specified in the .properties " +
        "file";
      throw new IllegalStateException(errstr);
    }
    else if (this.attrIdx != this.INVALID_INT &&
             this.attrName != this.INVALID_STR) {
      String errstr = "Both " + this.propPrefix + ".attributeIndex and " +
        this.propPrefix + ".attributeName are specified in the .properties " +
        "file";
      throw new IllegalStateException(errstr);
    }
  }

  private Object findAttr (List attrList, String target, Class attrClass) throws
    NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    /*
      This method heavily uses the dynamic type casting described in getLabel
     */
    Iterator iter = attrList.iterator();

    while (iter.hasNext()) {
      Object item = iter.next();

      // get metadata
      Method m1 = attrClass.getMethod("getMetadata");
      Object meta = m1.invoke(item, new Object[0]);
      Class metaDataClass = meta.getClass();

      Method m2 = metaDataClass.getMethod("getTool");
      Object tool = m2.invoke(meta, new Object[0]);
      Class toolClass = tool.getClass();

      Method m3 = toolClass.getMethod("getName");
      String name = m3.invoke(tool, new Object[0]).toString();

      // Return our item if it is the correct
      if (name.equals(target)) {
        return item;
      }
    }

    throw new IllegalStateException(target + " is not the name of a tool of " +
                                    attrClass);
  }
  
  public String getLabel (Vertex v) throws
    ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
    InvocationTargetException, IllegalStateException {
    // Buckle up. This is going to get ugly quick.
    
    PersonInfo pi = v.getPersonInfo();

    Method m1 = pi.getClass().getMethod("get" + this.labelAttr + "List");
    List attributeList = (List) m1.invoke(pi, new Object[0]);

    // replaces the possibly subtle error of having both of these set with
    // the obvious error of throwing an exception
    Object attribute = null;
    Class attrClass =
      Class.forName("edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge$" +
                    this.labelAttr + "Attribute");
    if (this.attrIdx != this.INVALID_INT && this.attrName == this.INVALID_STR) {
      attribute = attributeList.get(this.attrIdx);
    }
    else if (this.attrIdx == this.INVALID_INT &&
             this.attrName != this.INVALID_STR) {
      attribute = findAttr(attributeList, this.attrName, attrClass);
    }
    else if (this.attrIdx != this.INVALID_INT &&
             this.attrName != this.INVALID_STR) {
      String errstr = "Neither attrIdx nor attrName are set";
      throw new IllegalStateException(errstr);
    }
    else {
      String errstr = "Both attrIdx and attrName are set";
      throw new IllegalStateException(errstr);
    }

    // `attribute` is of object type, so calling this method will definitely
    // result in an exception. Thus, we must find the name of attribute's class,
    // and call the method on that class instead. It's a dynamic typecast: the
    // class could be anything at runtime.
    Method m3 = attrClass.getMethod("get" + this.labelAttr);
    String label = String.valueOf(m3.invoke(attribute, new Object[0]));
    return label;
  }
}