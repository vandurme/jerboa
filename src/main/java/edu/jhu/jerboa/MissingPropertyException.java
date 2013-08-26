/**
 * 
 */
package edu.jhu.jerboa;

/**
 * @author thomamj1
 *
 */
public class MissingPropertyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3968420245010349740L;

	/**
	 * 
	 */
	public MissingPropertyException() {
		super("One or more referenced properties were not found in the JerboaProperties.filename file.");
	}

	/**
	 * @param missingProperty
	 */
	public MissingPropertyException(String missingProperty) {
		super("The specified property does not exist: " + missingProperty);
	}

	/**
	 * @param cause
	 */
	public MissingPropertyException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MissingPropertyException(String message, Throwable cause) {
		super(message, cause);
	}

}
