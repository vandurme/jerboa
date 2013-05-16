/**
 * 
 */
package edu.jhu.jerboa;

/**
 * @author max
 *
 */
public class NoJerboaPropertyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2447020139651157497L;

	/**
	 * 
	 */
	public NoJerboaPropertyException() {
		super("One or more referenced properties were not found in the JerboaProperties.filename file.");
	}
	
	/**
	 * @param missingProperty
	 */
	public NoJerboaPropertyException(String missingProperty) {
		super("The specified property does not exist: " + missingProperty);
	}

	/**
	 * @param cause
	 */
	public NoJerboaPropertyException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NoJerboaPropertyException(String message, Throwable cause) {
		super(message, cause);
	}

}
