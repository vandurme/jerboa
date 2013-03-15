/**
 * 
 */
package edu.jhu.jerboa.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension of {@link java.util.Properties} that resolves substituted
 * variables. Example: given the following properties file:
 * 
 * my.foo = qux 
 * my.bar = ${my.foo}/bar
 * ...
 * 
 * the final Properties object will contain the following keys and values:
 * my.foo = qux 
 * my.bar = qux/bar
 * ...
 * 
 * @author max thomas
 * 
 */
public class JProperties extends Properties {

  /**
   * Eclipse-generated
   */
  private static final long serialVersionUID = -2261834033747708937L;

  private static final Pattern variablePattern = Pattern.compile("\\{[^\\\\}]+\\}");

  /**
	 * 
	 */
  public JProperties() {
    super();
  }

  /**
   * @param defaults
   */
  public JProperties(Properties defaults) {
    super(defaults);
  }

  @Override
  public synchronized void load(InputStream inStream) throws IOException {
    super.load(inStream);
    this.replaceSubstitutedKeys();
  }

  @Override
  public synchronized void load(Reader reader) throws IOException {
    super.load(reader);
    this.replaceSubstitutedKeys();
  }

  /**
   * First, iterate through defined property values and check for substitutions.
   * If substitution syntax is found, find the value to substitute (e.g., qux
   * for my.foo), and replace the matched value in the properties (e.g.,
   * ${foo}/bar becomes qux/bar).
   */
  private void replaceSubstitutedKeys() {
    Set<String> keySet = this.stringPropertyNames();
    for (String key : keySet) {
      String value = this.getProperty(key);
      Matcher m = variablePattern.matcher(value);
      if (m.find()) {
        String group = m.group();
        String trimmed = group.substring(1, group.length() - 1);
        String replacement = this.getProperty(trimmed);
        value = value.replace(group, replacement);
        this.setProperty(key, value);
      }
    }
  }
}
