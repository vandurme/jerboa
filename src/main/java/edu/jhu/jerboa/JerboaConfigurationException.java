/**
 * 
 */
package edu.jhu.jerboa;

/**
 * @author max
 *
 */
public class JerboaConfigurationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2447020139651157497L;

	/**
	 * 
	 */
	public JerboaConfigurationException() {
		super("There was an issue with the Jerboa configuration.");
	}
	
	/**
	 * @param errorMessage
	 */
	public JerboaConfigurationException(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * @param cause
	 */
	public JerboaConfigurationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JerboaConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
